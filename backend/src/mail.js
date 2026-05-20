import nodemailer from 'nodemailer';

let transporter;

function buildResetBody(username, code) {
  const greeting = username ? `Здравствуйте, ${username}!` : 'Здравствуйте!';
  const text = `${greeting}\n\nКод подтверждения: ${code}\nДействителен 15 минут.\n\nЕсли вы не запрашивали сброс пароля — проигнорируйте это письмо.`;
  const html = `<p>${greeting}</p><p>Код подтверждения: <strong>${code}</strong></p><p>Действителен 15 минут.</p><p style="color:#666;font-size:12px">Если вы не запрашивали сброс — проигнорируйте письмо.</p>`;
  return { greeting, text, html };
}

async function sendViaResend({ to, username, code }) {
  const apiKey = process.env.RESEND_API_KEY?.trim();
  const from =
    process.env.RESEND_FROM?.trim() || process.env.MAIL_FROM?.trim() || 'onboarding@resend.dev';
  const subj = process.env.MAIL_SUBJECT_RESET?.trim() || 'Код для сброса пароля';
  const { text, html } = buildResetBody(username, code);

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      from,
      to: [to],
      subject: subj,
      text,
      html
    })
  });

  const raw = await res.text();
  if (!res.ok) {
    const err = new Error(`Resend ${res.status}: ${raw.slice(0, 200)}`);
    err.status = res.status;
    throw err;
  }
}

function getTransporter() {
  const host = process.env.SMTP_HOST?.trim();
  if (!host) return null;

  if (!transporter) {
    const port = Number.parseInt(process.env.SMTP_PORT ?? '587', 10);
    const secure =
      process.env.SMTP_SECURE === '1' || process.env.SMTP_SECURE === 'true' || port === 465;
    transporter = nodemailer.createTransport({
      host,
      port,
      secure,
      auth: {
        user: process.env.SMTP_USER ?? '',
        pass: process.env.SMTP_PASS ?? ''
      }
    });
  }
  return transporter;
}

/** Отправка кода на email (Resend или SMTP). */
export async function sendPasswordResetEmail({ to, username, code }) {
  if (process.env.RESEND_API_KEY?.trim()) {
    await sendViaResend({ to, username, code });
    return { sent: true };
  }

  const t = getTransporter();
  if (!t) {
    throw new Error('Почта не настроена: задайте RESEND_API_KEY или SMTP_*');
  }

  const from = process.env.MAIL_FROM?.trim() || process.env.SMTP_USER;
  const subj = process.env.MAIL_SUBJECT_RESET?.trim() || 'Код для сброса пароля';
  const { text, html } = buildResetBody(username, code);

  await t.sendMail({
    from,
    to,
    subject: subj,
    text,
    html
  });
  return { sent: true };
}

/** true, если можно отправить письмо (Resend API или SMTP). */
export function isMailDeliveryConfigured() {
  if (process.env.RESEND_API_KEY?.trim()) return true;
  return Boolean(process.env.SMTP_HOST?.trim() && process.env.SMTP_USER?.trim());
}
