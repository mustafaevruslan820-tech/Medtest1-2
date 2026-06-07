import bcrypt from 'bcryptjs';
import {
  sendDoctorAssignmentPush,
  sendDoctorMessagePush,
  sendDoctorReportPush,
  sendPatientPrescriptionPush,
  sendPatientReportConclusionPush,
} from './firebaseAdmin.js';

export function registerDoctorRoutes(app, {
  db,
  requireAuth,
  authFromReq,
  jwt,
  JWT_SECRET,
  ADMIN_KEY,
  nowMs,
  normalizeUsername,
  normalizeEmail,
}) {
  function getUserRole(userId) {
    const row = db.prepare('SELECT role FROM users WHERE id = ? LIMIT 1').get(Number(userId));
    return row?.role ? String(row.role) : 'patient';
  }

  function requireRole(...roles) {
    return (req, res, next) => {
      const userId = Number(req.user?.sub);
      if (!Number.isFinite(userId)) return res.status(401).json({ ok: false, error: 'unauthorized' });
      const role = getUserRole(userId);
      if (!roles.includes(role)) return res.status(403).json({ ok: false, error: 'forbidden' });
      req.userRole = role;
      next();
    };
  }

  function requireAdminJwtOrKey(req, res, next) {
    const key = req.header('x-admin-key') ?? '';
    if (key && key === ADMIN_KEY) {
      req.isAdminKey = true;
      return next();
    }
    const token = authFromReq(req);
    if (!token) return res.status(401).json({ ok: false, error: 'unauthorized' });
    try {
      req.user = jwt.verify(token, JWT_SECRET);
      const role = getUserRole(Number(req.user.sub));
      if (role !== 'admin') return res.status(403).json({ ok: false, error: 'forbidden' });
      return next();
    } catch {
      return res.status(401).json({ ok: false, error: 'unauthorized' });
    }
  }

  function mapDoctorProfileRow(row, username) {
    if (!row) return null;
    const photo = row.photo_base64 ? String(row.photo_base64) : null;
    const hasProfile = Boolean(
      String(row.specialty ?? '').trim() &&
        String(row.full_name ?? '').trim() &&
        photo
    );
    return {
      userId: Number(row.user_id),
      username: username ?? row.username ?? '',
      specialty: String(row.specialty ?? ''),
      fullName: String(row.full_name ?? ''),
      experienceYears: Number(row.experience_years ?? 0),
      education: String(row.education ?? ''),
      bio: String(row.bio ?? ''),
      photoBase64: photo,
      profileComplete: hasProfile,
      onDuty: Boolean(row.on_duty),
    };
  }

  function mapAssignmentRow(r) {
    return {
      id: Number(r.id),
      doctorUserId: Number(r.doctor_user_id),
      patientUserId: Number(r.patient_user_id),
      doctorUsername: r.doctor_username ?? '',
      patientUsername: r.patient_username ?? '',
      status: String(r.status ?? 'active'),
      assignedAt: Number(r.assigned_at),
      patientProfileJson: r.patient_profile_json ? String(r.patient_profile_json) : null,
      treatmentSyncJson: r.treatment_sync_json ? String(r.treatment_sync_json) : null,
    };
  }

  app.post('/api/admin/doctors', requireAdminJwtOrKey, (req, res) => {
    const { username, email, password, specialty } = req.body ?? {};
    const uname = normalizeUsername(username);
    const em = normalizeEmail(email);
    const pw = String(password ?? '');
    const spec = String(specialty ?? '').trim();

    if (!uname || uname.length < 3) return res.status(400).json({ ok: false, error: 'invalid_username' });
    if (!em || !em.includes('@')) return res.status(400).json({ ok: false, error: 'invalid_email' });
    if (!pw || pw.length < 6) return res.status(400).json({ ok: false, error: 'invalid_password' });
    if (!spec) return res.status(400).json({ ok: false, error: 'specialty_required' });

    const existing = db
      .prepare('SELECT id FROM users WHERE username = ? OR email = ? LIMIT 1')
      .get(uname, em);
    if (existing) return res.status(409).json({ ok: false, error: 'user_exists' });

    const ts = nowMs();
    const hash = bcrypt.hashSync(pw, 10);
    const info = db
      .prepare(
        `INSERT INTO users (username, email, password_hash, created_at, last_login_at, role)
         VALUES (?, ?, ?, ?, ?, 'doctor')`
      )
      .run(uname, em, hash, ts, ts);
    const userId = Number(info.lastInsertRowid);
    db.prepare(
      `INSERT INTO doctor_profiles (user_id, specialty, full_name, updated_at)
       VALUES (?, ?, ?, ?)`
    ).run(userId, spec, uname, ts);

    res.status(201).json({
      ok: true,
      doctor: { id: userId, username: uname, email: em, specialty: spec, role: 'doctor' },
    });
  });

  app.delete('/api/admin/doctors/:id', requireAdminJwtOrKey, (req, res) => {
    const userId = Number(req.params.id);
    if (!Number.isFinite(userId) || userId <= 0) {
      return res.status(400).json({ ok: false, error: 'bad_id' });
    }
    const doctor = db
      .prepare(`SELECT id, username FROM users WHERE id = ? AND role = 'doctor' LIMIT 1`)
      .get(userId);
    if (!doctor) return res.status(404).json({ ok: false, error: 'not_found' });

    db.prepare('DELETE FROM doctor_messages WHERE assignment_id IN (SELECT id FROM doctor_assignments WHERE doctor_user_id = ?)').run(userId);
    db.prepare('DELETE FROM doctor_prescriptions WHERE assignment_id IN (SELECT id FROM doctor_assignments WHERE doctor_user_id = ?)').run(userId);
    db.prepare('DELETE FROM treatment_reports WHERE assignment_id IN (SELECT id FROM doctor_assignments WHERE doctor_user_id = ?)').run(userId);
    db.prepare('DELETE FROM doctor_assignments WHERE doctor_user_id = ?').run(userId);
    db.prepare('DELETE FROM doctor_shifts WHERE doctor_user_id = ?').run(userId);
    db.prepare('DELETE FROM doctor_profiles WHERE user_id = ?').run(userId);
    db.prepare('DELETE FROM users WHERE id = ?').run(userId);

    res.json({ ok: true, deletedUsername: doctor.username });
  });

  app.get('/api/admin/doctors', requireAdminJwtOrKey, (req, res) => {
    const rows = db
      .prepare(
        `SELECT u.id, u.username, u.email, u.created_at, u.last_login_at,
                p.specialty, p.full_name, p.experience_years
         FROM users u
         LEFT JOIN doctor_profiles p ON p.user_id = u.id
         WHERE u.role = 'doctor'
         ORDER BY u.created_at DESC`
      )
      .all()
      .map((r) => ({
        id: Number(r.id),
        username: r.username,
        email: r.email,
        specialty: r.specialty ?? '',
        fullName: r.full_name ?? '',
        experienceYears: Number(r.experience_years ?? 0),
        createdAt: Number(r.created_at),
        lastLoginAt: r.last_login_at != null ? Number(r.last_login_at) : null,
      }));
    res.json({ ok: true, doctors: rows });
  });

  app.get('/api/doctors/on-duty', requireAuth, (req, res) => {
    const rows = db
      .prepare(
        `SELECT u.id AS user_id, u.username,
                p.specialty, p.full_name, p.experience_years, p.education, p.bio, p.photo_base64,
                1 AS on_duty
         FROM doctor_shifts s
         JOIN users u ON u.id = s.doctor_user_id
         JOIN doctor_profiles p ON p.user_id = u.id
         WHERE s.is_active = 1
           AND p.specialty != ''
           AND p.full_name != ''
           AND p.photo_base64 IS NOT NULL
           AND TRIM(p.photo_base64) != ''
         ORDER BY s.started_at DESC`
      )
      .all();
    res.json({
      ok: true,
      doctors: rows.map((r) => mapDoctorProfileRow(r, r.username)),
    });
  });

  app.get('/api/doctors/:userId/profile', requireAuth, (req, res) => {
    const doctorId = Number(req.params.userId);
    if (!Number.isFinite(doctorId)) return res.status(400).json({ ok: false, error: 'bad_id' });
    const row = db
      .prepare(
        `SELECT p.*, u.username,
                CASE WHEN EXISTS(
                  SELECT 1 FROM doctor_shifts s
                  WHERE s.doctor_user_id = u.id AND s.is_active = 1
                ) THEN 1 ELSE 0 END AS on_duty
         FROM users u
         LEFT JOIN doctor_profiles p ON p.user_id = u.id
         WHERE u.id = ? AND u.role = 'doctor'
         LIMIT 1`
      )
      .get(doctorId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });
    res.json({ ok: true, profile: mapDoctorProfileRow(row, row.username) });
  });

  app.get('/api/doctors/me/profile', requireAuth, requireRole('doctor'), (req, res) => {
    const userId = Number(req.user.sub);
    const row = db
      .prepare(
        `SELECT p.*, u.username,
                CASE WHEN EXISTS(
                  SELECT 1 FROM doctor_shifts s
                  WHERE s.doctor_user_id = u.id AND s.is_active = 1
                ) THEN 1 ELSE 0 END AS on_duty
         FROM users u
         LEFT JOIN doctor_profiles p ON p.user_id = u.id
         WHERE u.id = ?
         LIMIT 1`
      )
      .get(userId);
    res.json({ ok: true, profile: mapDoctorProfileRow(row, row?.username) });
  });

  app.put('/api/doctors/me/profile', requireAuth, requireRole('doctor'), (req, res) => {
    const userId = Number(req.user.sub);
    const { specialty, fullName, experienceYears, education, bio, photoBase64 } = req.body ?? {};
    const ts = nowMs();
    const existing = db.prepare('SELECT user_id FROM doctor_profiles WHERE user_id = ?').get(userId);
    const fields = {
      specialty: String(specialty ?? '').trim(),
      full_name: String(fullName ?? '').trim(),
      experience_years: Number(experienceYears ?? 0) || 0,
      education: String(education ?? '').trim(),
      bio: String(bio ?? '').trim(),
      photo_base64: photoBase64 ? String(photoBase64) : null,
      updated_at: ts,
    };
    if (existing) {
      db.prepare(
        `UPDATE doctor_profiles
         SET specialty = ?, full_name = ?, experience_years = ?, education = ?, bio = ?,
             photo_base64 = COALESCE(?, photo_base64), updated_at = ?
         WHERE user_id = ?`
      ).run(
        fields.specialty,
        fields.full_name,
        fields.experience_years,
        fields.education,
        fields.bio,
        fields.photo_base64,
        fields.updated_at,
        userId
      );
    } else {
      db.prepare(
        `INSERT INTO doctor_profiles
          (user_id, specialty, full_name, experience_years, education, bio, photo_base64, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
      ).run(
        userId,
        fields.specialty,
        fields.full_name,
        fields.experience_years,
        fields.education,
        fields.bio,
        fields.photo_base64,
        fields.updated_at
      );
    }
    const row = db
      .prepare(
        `SELECT p.*, u.username,
                CASE WHEN EXISTS(
                  SELECT 1 FROM doctor_shifts s WHERE s.doctor_user_id = u.id AND s.is_active = 1
                ) THEN 1 ELSE 0 END AS on_duty
         FROM users u
         JOIN doctor_profiles p ON p.user_id = u.id
         WHERE u.id = ?`
      )
      .get(userId);
    res.json({ ok: true, profile: mapDoctorProfileRow(row, row.username) });
  });

  app.get('/api/doctors/me/shift', requireAuth, requireRole('doctor'), (req, res) => {
    const userId = Number(req.user.sub);
    const shift = db
      .prepare(
        `SELECT id, started_at, ended_at, is_active
         FROM doctor_shifts
         WHERE doctor_user_id = ? AND is_active = 1
         ORDER BY started_at DESC LIMIT 1`
      )
      .get(userId);
    res.json({
      ok: true,
      onDuty: Boolean(shift?.is_active),
      shift: shift
        ? {
            id: Number(shift.id),
            startedAt: Number(shift.started_at),
            endedAt: shift.ended_at != null ? Number(shift.ended_at) : null,
            isActive: Boolean(shift.is_active),
          }
        : null,
    });
  });

  app.post('/api/doctors/me/shift/start', requireAuth, requireRole('doctor'), (req, res) => {
    const userId = Number(req.user.sub);
    const profile = db.prepare('SELECT specialty, full_name, photo_base64 FROM doctor_profiles WHERE user_id = ?').get(userId);
    if (!profile?.specialty?.trim() || !profile?.full_name?.trim() || !profile?.photo_base64?.trim()) {
      return res.status(400).json({ ok: false, error: 'profile_incomplete' });
    }
    const active = db
      .prepare('SELECT id FROM doctor_shifts WHERE doctor_user_id = ? AND is_active = 1 LIMIT 1')
      .get(userId);
    if (active) return res.json({ ok: true, alreadyActive: true });

    const ts = nowMs();
    const info = db
      .prepare(
        `INSERT INTO doctor_shifts (doctor_user_id, started_at, is_active) VALUES (?, ?, 1)`
      )
      .run(userId, ts);
    res.json({ ok: true, shiftId: Number(info.lastInsertRowid), startedAt: ts });
  });

  app.post('/api/doctors/me/shift/end', requireAuth, requireRole('doctor'), (req, res) => {
    const userId = Number(req.user.sub);
    const ts = nowMs();
    db.prepare(
      `UPDATE doctor_shifts SET is_active = 0, ended_at = ? WHERE doctor_user_id = ? AND is_active = 1`
    ).run(ts, userId);
    res.json({ ok: true });
  });

  app.post('/api/doctors/assign', requireAuth, requireRole('patient', 'admin'), (req, res) => {
    const patientId = Number(req.user.sub);
    const { doctorUserId, patientProfileJson } = req.body ?? {};
    const docId = Number(doctorUserId);
    if (!Number.isFinite(docId)) return res.status(400).json({ ok: false, error: 'bad_doctor_id' });

    const doctor = db
      .prepare(`SELECT id, username, fcm_token FROM users WHERE id = ? AND role = 'doctor' LIMIT 1`)
      .get(docId);
    if (!doctor) return res.status(404).json({ ok: false, error: 'doctor_not_found' });

    const onDuty = db
      .prepare('SELECT id FROM doctor_shifts WHERE doctor_user_id = ? AND is_active = 1 LIMIT 1')
      .get(docId);
    if (!onDuty) return res.status(400).json({ ok: false, error: 'doctor_not_on_duty' });

    db.prepare(
      `UPDATE doctor_assignments SET status = 'replaced'
       WHERE patient_user_id = ? AND status = 'active'`
    ).run(patientId);

    const ts = nowMs();
    const patientRow = db.prepare('SELECT username FROM users WHERE id = ?').get(patientId);
    const info = db
      .prepare(
        `INSERT INTO doctor_assignments
          (doctor_user_id, patient_user_id, status, assigned_at, patient_profile_json)
         VALUES (?, ?, 'active', ?, ?)`
      )
      .run(docId, patientId, ts, patientProfileJson ? String(patientProfileJson) : null);

    const assignmentId = Number(info.lastInsertRowid);
    if (doctor.fcm_token) {
      void sendDoctorAssignmentPush({
        token: doctor.fcm_token,
        patientName: patientRow?.username ?? 'Пациент',
        assignmentId,
      });
    }

    res.status(201).json({
      ok: true,
      assignment: {
        id: assignmentId,
        doctorUserId: docId,
        doctorUsername: doctor.username,
        assignedAt: ts,
      },
    });
  });

  app.get('/api/patient/assignment', requireAuth, (req, res) => {
    const patientId = Number(req.user.sub);
    const row = db
      .prepare(
        `SELECT a.*, du.username AS doctor_username, pu.username AS patient_username
         FROM doctor_assignments a
         JOIN users du ON du.id = a.doctor_user_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.patient_user_id = ? AND a.status = 'active'
         ORDER BY a.assigned_at DESC LIMIT 1`
      )
      .get(patientId);
    res.json({ ok: true, assignment: row ? mapAssignmentRow(row) : null });
  });

  app.get('/api/doctors/me/assignments', requireAuth, requireRole('doctor'), (req, res) => {
    const doctorId = Number(req.user.sub);
    const rows = db
      .prepare(
        `SELECT a.*, du.username AS doctor_username, pu.username AS patient_username
         FROM doctor_assignments a
         JOIN users du ON du.id = a.doctor_user_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.doctor_user_id = ? AND a.status IN ('active', 'continued')
         ORDER BY a.assigned_at DESC`
      )
      .all(doctorId)
      .map(mapAssignmentRow);
    res.json({ ok: true, assignments: rows });
  });

  app.get('/api/assignments/:id', requireAuth, (req, res) => {
    const assignmentId = Number(req.params.id);
    const userId = Number(req.user.sub);
    const row = db
      .prepare(
        `SELECT a.*, du.username AS doctor_username, pu.username AS patient_username
         FROM doctor_assignments a
         JOIN users du ON du.id = a.doctor_user_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.id = ? LIMIT 1`
      )
      .get(assignmentId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });
    if (row.doctor_user_id !== userId && row.patient_user_id !== userId) {
      return res.status(403).json({ ok: false, error: 'forbidden' });
    }
    const prescriptions = db
      .prepare(
        `SELECT id, prescription_text, treatment_plan_text, created_at
         FROM doctor_prescriptions WHERE assignment_id = ? ORDER BY created_at ASC`
      )
      .all(assignmentId)
      .map((p) => ({
        id: Number(p.id),
        prescriptionText: p.prescription_text,
        treatmentPlanText: p.treatment_plan_text,
        createdAt: Number(p.created_at),
      }));
    const reports = db
      .prepare(
        `SELECT id, status, doctor_conclusion, doctor_signed_at, created_at
         FROM treatment_reports WHERE assignment_id = ? ORDER BY created_at DESC`
      )
      .all(assignmentId)
      .map((r) => ({
        id: Number(r.id),
        status: r.status,
        doctorConclusion: r.doctor_conclusion,
        doctorSignedAt: r.doctor_signed_at != null ? Number(r.doctor_signed_at) : null,
        createdAt: Number(r.created_at),
      }));
    res.json({
      ok: true,
      assignment: mapAssignmentRow(row),
      prescriptions,
      reports,
    });
  });

  app.put('/api/assignments/:id/patient-sync', requireAuth, (req, res) => {
    const assignmentId = Number(req.params.id);
    const userId = Number(req.user.sub);
    const { treatmentSyncJson } = req.body ?? {};
    const row = db.prepare('SELECT patient_user_id FROM doctor_assignments WHERE id = ?').get(assignmentId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });
    if (row.patient_user_id !== userId) return res.status(403).json({ ok: false, error: 'forbidden' });
    db.prepare('UPDATE doctor_assignments SET treatment_sync_json = ? WHERE id = ?').run(
      treatmentSyncJson ? String(treatmentSyncJson) : null,
      assignmentId
    );
    res.json({ ok: true });
  });

  app.get('/api/assignments/:id/messages', requireAuth, (req, res) => {
    const assignmentId = Number(req.params.id);
    const userId = Number(req.user.sub);
    const row = db
      .prepare('SELECT doctor_user_id, patient_user_id FROM doctor_assignments WHERE id = ?')
      .get(assignmentId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });
    if (row.doctor_user_id !== userId && row.patient_user_id !== userId) {
      return res.status(403).json({ ok: false, error: 'forbidden' });
    }
    const messages = db
      .prepare(
        `SELECT id, assignment_id, sender, text, created_at
         FROM doctor_messages WHERE assignment_id = ? ORDER BY created_at ASC`
      )
      .all(assignmentId)
      .map((m) => ({
        id: Number(m.id),
        assignmentId: Number(m.assignment_id),
        sender: m.sender,
        text: m.text,
        createdAt: Number(m.created_at),
      }));
    res.json({ ok: true, messages });
  });

  app.post('/api/assignments/:id/messages', requireAuth, (req, res) => {
    const assignmentId = Number(req.params.id);
    const userId = Number(req.user.sub);
    const { text } = req.body ?? {};
    const clean = String(text ?? '').trim();
    if (!clean || clean.length > 2000) return res.status(400).json({ ok: false, error: 'invalid_text' });

    const row = db
      .prepare(
        `SELECT a.doctor_user_id, a.patient_user_id, du.fcm_token AS doctor_token,
                pu.fcm_token AS patient_token, pu.username AS patient_username,
                du.username AS doctor_username
         FROM doctor_assignments a
         JOIN users du ON du.id = a.doctor_user_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.id = ? LIMIT 1`
      )
      .get(assignmentId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });

    let sender;
    let notifyToken = null;
    let notifyTitle = '';
    if (row.doctor_user_id === userId) {
      sender = 'doctor';
      notifyToken = row.patient_token;
      notifyTitle = row.doctor_username ?? 'Врач';
    } else if (row.patient_user_id === userId) {
      sender = 'patient';
      notifyToken = row.doctor_token;
      notifyTitle = row.patient_username ?? 'Пациент';
    } else {
      return res.status(403).json({ ok: false, error: 'forbidden' });
    }

    const ts = nowMs();
    const info = db
      .prepare(
        `INSERT INTO doctor_messages (assignment_id, sender, text, created_at) VALUES (?, ?, ?, ?)`
      )
      .run(assignmentId, sender, clean, ts);
    const messageId = Number(info.lastInsertRowid);

    if (notifyToken) {
      void sendDoctorMessagePush({
        token: notifyToken,
        senderName: notifyTitle,
        body: clean,
        assignmentId,
        messageId,
      });
    }

    res.status(201).json({
      ok: true,
      message: { id: messageId, assignmentId, sender, text: clean, createdAt: ts },
    });
  });

  app.post('/api/assignments/:id/prescription', requireAuth, requireRole('doctor'), (req, res) => {
    const assignmentId = Number(req.params.id);
    const doctorId = Number(req.user.sub);
    const { prescriptionText, treatmentPlanText } = req.body ?? {};
    const rx = String(prescriptionText ?? '').trim();
    const plan = String(treatmentPlanText ?? '').trim();
    if (!rx) return res.status(400).json({ ok: false, error: 'prescription_required' });

    const row = db
      .prepare(
        `SELECT a.patient_user_id, pu.fcm_token AS patient_token
         FROM doctor_assignments a
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.id = ? AND a.doctor_user_id = ?`
      )
      .get(assignmentId, doctorId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });

    const ts = nowMs();
    const info = db
      .prepare(
        `INSERT INTO doctor_prescriptions (assignment_id, prescription_text, treatment_plan_text, created_at)
         VALUES (?, ?, ?, ?)`
      )
      .run(assignmentId, rx, plan, ts);

    const msgText = plan
      ? `Рецепт:\n${rx}\n\nПлан лечения:\n${plan}`
      : `Рецепт:\n${rx}`;
    db.prepare(
      `INSERT INTO doctor_messages (assignment_id, sender, text, created_at) VALUES (?, 'doctor', ?, ?)`
    ).run(assignmentId, msgText, ts);

    if (row.patient_token) {
      void sendPatientPrescriptionPush({
        token: row.patient_token,
        assignmentId,
      });
    }

    res.status(201).json({
      ok: true,
      prescription: {
        id: Number(info.lastInsertRowid),
        prescriptionText: rx,
        treatmentPlanText: plan,
        createdAt: ts,
      },
    });
  });

  app.post('/api/assignments/:id/report', requireAuth, (req, res) => {
    const assignmentId = Number(req.params.id);
    const userId = Number(req.user.sub);
    const { reportDataJson, pdfBase64 } = req.body ?? {};
    if (!reportDataJson) return res.status(400).json({ ok: false, error: 'report_required' });

    const row = db
      .prepare(
        `SELECT a.doctor_user_id, du.fcm_token AS doctor_token, pu.username AS patient_username
         FROM doctor_assignments a
         JOIN users du ON du.id = a.doctor_user_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE a.id = ? AND a.patient_user_id = ?`
      )
      .get(assignmentId, userId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });

    const ts = nowMs();
    const info = db
      .prepare(
        `INSERT INTO treatment_reports
          (assignment_id, patient_user_id, report_data_json, pdf_base64, status, created_at)
         VALUES (?, ?, ?, ?, 'pending', ?)`
      )
      .run(assignmentId, userId, String(reportDataJson), pdfBase64 ? String(pdfBase64) : null, ts);

    if (row.doctor_token) {
      void sendDoctorReportPush({
        token: row.doctor_token,
        patientName: row.patient_username ?? 'Пациент',
        assignmentId,
        reportId: Number(info.lastInsertRowid),
      });
    }

    res.status(201).json({
      ok: true,
      report: { id: Number(info.lastInsertRowid), status: 'pending', createdAt: ts },
    });
  });

  app.put('/api/assignments/:assignmentId/report/:reportId/conclusion', requireAuth, requireRole('doctor'), (req, res) => {
    const assignmentId = Number(req.params.assignmentId);
    const reportId = Number(req.params.reportId);
    const doctorId = Number(req.user.sub);
    const { action, conclusion, newTreatmentPlanText } = req.body ?? {};
    const act = String(action ?? '').trim();
    if (!['complete', 'continue'].includes(act)) {
      return res.status(400).json({ ok: false, error: 'invalid_action' });
    }

    const row = db
      .prepare(
        `SELECT r.id, a.patient_user_id, pu.fcm_token AS patient_token
         FROM treatment_reports r
         JOIN doctor_assignments a ON a.id = r.assignment_id
         JOIN users pu ON pu.id = a.patient_user_id
         WHERE r.id = ? AND r.assignment_id = ? AND a.doctor_user_id = ?`
      )
      .get(reportId, assignmentId, doctorId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });

    const ts = nowMs();
    const status = act === 'complete' ? 'completed' : 'continued';
    const conclusionText = String(conclusion ?? '').trim() ||
      (act === 'complete' ? 'Лечение завершено.' : 'Продолжить лечение с новым планом.');

    db.prepare(
      `UPDATE treatment_reports
       SET status = ?, doctor_conclusion = ?, doctor_signed_at = ?
       WHERE id = ?`
    ).run(status, conclusionText, ts, reportId);

    if (act === 'complete') {
      db.prepare(`UPDATE doctor_assignments SET status = 'completed' WHERE id = ?`).run(assignmentId);
    } else {
      db.prepare(`UPDATE doctor_assignments SET status = 'continued' WHERE id = ?`).run(assignmentId);
      const planText = String(newTreatmentPlanText ?? '').trim();
      if (planText) {
        db.prepare(
          `INSERT INTO doctor_prescriptions (assignment_id, prescription_text, treatment_plan_text, created_at)
           VALUES (?, ?, ?, ?)`
        ).run(assignmentId, 'Продолжение лечения', planText, ts);
        db.prepare(
          `INSERT INTO doctor_messages (assignment_id, sender, text, created_at) VALUES (?, 'doctor', ?, ?)`
        ).run(assignmentId, `Новый план лечения:\n${planText}`, ts);
      }
    }

    if (row.patient_token) {
      void sendPatientReportConclusionPush({
        token: row.patient_token,
        status,
        assignmentId,
      });
    }

    res.json({ ok: true, status, conclusion: conclusionText, signedAt: ts });
  });

  app.get('/api/users/me', requireAuth, (req, res) => {
    const userId = Number(req.user.sub);
    const row = db
      .prepare('SELECT id, username, email, role, created_at, last_login_at FROM users WHERE id = ?')
      .get(userId);
    if (!row) return res.status(404).json({ ok: false, error: 'not_found' });
    res.json({
      ok: true,
      user: {
        id: Number(row.id),
        username: row.username,
        email: row.email,
        role: row.role ?? 'patient',
        createdAt: Number(row.created_at),
        lastLoginAt: row.last_login_at != null ? Number(row.last_login_at) : null,
      },
    });
  });
}
