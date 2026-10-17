package com.shubh3011.facesecurity.db;

/**
 * Idempotent schema creation. Executed on every {@link Database} open, so it must
 * remain safe to run against an already-initialised file.
 */
final class SchemaInitializer {

    private static final String[] STATEMENTS = {
        """
        CREATE TABLE IF NOT EXISTS person (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            name        TEXT NOT NULL UNIQUE,
            role        TEXT NOT NULL,
            created_at  TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS face_embedding (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            person_id   INTEGER NOT NULL REFERENCES person(id) ON DELETE CASCADE,
            dimension   INTEGER NOT NULL,
            vector      BLOB NOT NULL,
            created_at  TEXT NOT NULL
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_face_person ON face_embedding(person_id)",
        """
        CREATE TABLE IF NOT EXISTS attendance (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            person_id   INTEGER NOT NULL REFERENCES person(id) ON DELETE CASCADE,
            person_name TEXT NOT NULL,
            occurred_at TEXT NOT NULL,
            day         TEXT NOT NULL,
            status      TEXT NOT NULL,
            confidence  REAL NOT NULL,
            source      TEXT NOT NULL,
            UNIQUE(person_id, day)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_attendance_day ON attendance(day)",
        """
        CREATE TABLE IF NOT EXISTS scan_record (
            id             INTEGER PRIMARY KEY AUTOINCREMENT,
            file_path      TEXT NOT NULL,
            sha256         TEXT NOT NULL,
            verdict        TEXT NOT NULL,
            threat_summary TEXT NOT NULL,
            entropy        REAL NOT NULL,
            size_bytes     INTEGER NOT NULL,
            scanned_at     TEXT NOT NULL
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_scan_verdict ON scan_record(verdict)",
        """
        CREATE TABLE IF NOT EXISTS trusted_entity (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            label       TEXT NOT NULL,
            kind        TEXT NOT NULL,
            path        TEXT NOT NULL UNIQUE,
            person_id   INTEGER NOT NULL REFERENCES person(id) ON DELETE CASCADE,
            created_at  TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS schema_version (
            version INTEGER NOT NULL
        )
        """,
    };

    private SchemaInitializer() {
    }

    static void initialize(Database database) {
        for (String statement : STATEMENTS) {
            database.execute(statement);
        }
        int existing = database.query("SELECT COUNT(*) AS c FROM schema_version", Database.NO_PARAMETERS,
                rs -> rs.getInt("c")).get(0);
        if (existing == 0) {
            database.update("INSERT INTO schema_version(version) VALUES (?)",
                    ps -> ps.setInt(1, SCHEMA_VERSION));
        }
    }

    static final int SCHEMA_VERSION = 1;
}
