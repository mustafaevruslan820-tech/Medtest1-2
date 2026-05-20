import admin from 'firebase-admin';
import fs from 'node:fs';

/**
 * Инициализация Firebase Admin для проверки idToken (синхронизация пароля после сброса в Firebase).
 * Задайте FIREBASE_SERVICE_ACCOUNT_PATH (путь к JSON) или FIREBASE_SERVICE_ACCOUNT_JSON (строка JSON).
 */
export function initFirebaseAdmin() {
  if (admin.apps.length) return true;
  try {
    const path = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
    if (path) {
      const raw = fs.readFileSync(path, 'utf8');
      admin.initializeApp({ credential: admin.credential.cert(JSON.parse(raw)) });
      return true;
    }
    const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    if (rawJson) {
      admin.initializeApp({ credential: admin.credential.cert(JSON.parse(rawJson)) });
      return true;
    }
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn('[firebase-admin] init failed:', e?.message ?? e);
  }
  return false;
}

export async function verifyFirebaseIdToken(idToken) {
  return admin.auth().verifyIdToken(idToken);
}

export async function sendSupportReplyPush({ token, body, messageId }) {
  if (!admin.apps.length || !token) return false;
  try {
    const text = body || 'Вам ответили на сообщение';
    await admin.messaging().send({
      token,
      notification: {
        title: 'Техподдержка',
        body: text,
      },
      data: {
        type: 'support_reply',
        body: text,
        ...(messageId != null ? { messageId: String(messageId) } : {}),
      },
      android: {
        priority: 'high',
        notification: { channelId: 'support_replies' },
      },
    });
    return true;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn('[firebase-admin] support push failed:', e?.message ?? e);
    return false;
  }
}
