import Database from 'better-sqlite3';

export function openDb({ filename }) {
  const db = new Database(filename);
  db.exec('PRAGMA journal_mode = WAL;');
  db.exec('PRAGMA foreign_keys = ON;');
  migrate(db);
  return db;
}

function migrate(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      email TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      last_login_at INTEGER
    );

    CREATE TABLE IF NOT EXISTS installs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      device_id TEXT NOT NULL UNIQUE,
      first_seen_at INTEGER NOT NULL,
      last_seen_at INTEGER NOT NULL,
      app_version TEXT,
      platform TEXT
    );

    CREATE TABLE IF NOT EXISTS install_user_links (
      install_id INTEGER NOT NULL,
      user_id INTEGER NOT NULL,
      linked_at INTEGER NOT NULL,
      UNIQUE(install_id, user_id),
      FOREIGN KEY(install_id) REFERENCES installs(id) ON DELETE CASCADE,
      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS support_conversations (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL UNIQUE,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS support_messages (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      conversation_id INTEGER NOT NULL,
      sender TEXT NOT NULL CHECK(sender IN ('user', 'admin')),
      text TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY(conversation_id) REFERENCES support_conversations(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS idx_support_messages_conversation_created
      ON support_messages(conversation_id, created_at);

    CREATE TABLE IF NOT EXISTS password_reset_codes (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      code TEXT NOT NULL,
      expires_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS idx_password_reset_user
      ON password_reset_codes(user_id);
  `);

  const supportColumns = db.prepare(`PRAGMA table_info(support_conversations)`).all();
  const hasAdminLastReadAt = supportColumns.some((c) => c.name === 'admin_last_read_at');
  if (!hasAdminLastReadAt) {
    db.exec(`ALTER TABLE support_conversations ADD COLUMN admin_last_read_at INTEGER NOT NULL DEFAULT 0;`);
  }
  const hasUserLastReadAt = supportColumns.some((c) => c.name === 'user_last_read_at');
  if (!hasUserLastReadAt) {
    db.exec(`ALTER TABLE support_conversations ADD COLUMN user_last_read_at INTEGER NOT NULL DEFAULT 0;`);
  }
  const hasAdminRequestedAt = supportColumns.some((c) => c.name === 'admin_requested_at');
  if (!hasAdminRequestedAt) {
    db.exec(`ALTER TABLE support_conversations ADD COLUMN admin_requested_at INTEGER NOT NULL DEFAULT 0;`);
  }

  const messageColumns = db.prepare(`PRAGMA table_info(support_messages)`).all();
  if (!messageColumns.some((c) => c.name === 'reply_to_message_id')) {
    db.exec(`ALTER TABLE support_messages ADD COLUMN reply_to_message_id INTEGER;`);
  }
  if (!messageColumns.some((c) => c.name === 'image_url')) {
    db.exec(`ALTER TABLE support_messages ADD COLUMN image_url TEXT;`);
  }

  const userColumns = db.prepare(`PRAGMA table_info(users)`).all();
  if (!userColumns.some((c) => c.name === 'fcm_token')) {
    db.exec(`ALTER TABLE users ADD COLUMN fcm_token TEXT;`);
  }
}
