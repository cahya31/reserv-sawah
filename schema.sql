PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT NOT NULL, access_code TEXT NOT NULL UNIQUE, role TEXT NOT NULL CHECK(role IN ('ADMIN','USER')), active INTEGER NOT NULL DEFAULT 1);
CREATE TABLE IF NOT EXISTS rooms (id TEXT PRIMARY KEY, name TEXT NOT NULL, location TEXT DEFAULT '', capacity INTEGER NOT NULL DEFAULT 10, facilities TEXT DEFAULT '', active INTEGER NOT NULL DEFAULT 1);
CREATE TABLE IF NOT EXISTS reservations (id TEXT PRIMARY KEY, title TEXT NOT NULL, agenda TEXT DEFAULT '', room_id TEXT NOT NULL REFERENCES rooms(id), user_id TEXT NOT NULL REFERENCES users(id), date TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'PENDING', created_at INTEGER NOT NULL);
CREATE INDEX IF NOT EXISTS idx_reservations_room_date ON reservations(room_id,date);
CREATE INDEX IF NOT EXISTS idx_reservations_user ON reservations(user_id);
CREATE TABLE IF NOT EXISTS sessions (token TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id), expires_at INTEGER NOT NULL);
INSERT OR IGNORE INTO users(id,name,access_code,role,active) VALUES ('admin-awal','Administrator','123456','ADMIN',1),('user-awal','Pengguna Demo','0002','USER',1);
INSERT OR IGNORE INTO rooms(id,name,location,capacity,facilities,active) VALUES ('ruang-utama','Ruang Rapat Utama','Gedung Utama',20,'Proyektor, AC, Whiteboard',1),('ruang-kecil','Ruang Diskusi','Gedung Utama',8,'TV, Whiteboard',1);
