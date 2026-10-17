package com.shubh3011.facesecurity.db;

import com.shubh3011.facesecurity.model.AttendanceRecord;
import com.shubh3011.facesecurity.model.AttendanceStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Stores daily attendance check-ins. */
public final class AttendanceRepository {

    private static final String COLUMNS =
            "id, person_id, person_name, occurred_at, day, status, confidence, source";

    private final Database database;

    public AttendanceRepository(Database database) {
        this.database = database;
    }

    public AttendanceRecord save(AttendanceRecord record, LocalDate day) {
        long id = database.insert(
                """
                INSERT INTO attendance(person_id, person_name, occurred_at, day, status, confidence, source)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, record.personId());
                    ps.setString(2, record.personName());
                    ps.setString(3, Database.toText(record.occurredAt()));
                    ps.setString(4, day.toString());
                    ps.setString(5, record.status().name());
                    ps.setDouble(6, record.confidence());
                    ps.setString(7, record.source());
                });
        return record.withId(id);
    }

    public List<AttendanceRecord> findByDay(LocalDate day) {
        return database.query("SELECT " + COLUMNS + " FROM attendance WHERE day = ? ORDER BY occurred_at",
                ps -> ps.setString(1, day.toString()), AttendanceRepository::map);
    }

    public List<AttendanceRecord> findBetween(LocalDate from, LocalDate to) {
        return database.query(
                "SELECT " + COLUMNS + " FROM attendance WHERE day BETWEEN ? AND ? ORDER BY day, occurred_at",
                ps -> {
                    ps.setString(1, from.toString());
                    ps.setString(2, to.toString());
                }, AttendanceRepository::map);
    }

    public Optional<AttendanceRecord> findByPersonAndDay(long personId, LocalDate day) {
        return database.query(
                "SELECT " + COLUMNS + " FROM attendance WHERE person_id = ? AND day = ?",
                ps -> {
                    ps.setLong(1, personId);
                    ps.setString(2, day.toString());
                }, AttendanceRepository::map).stream().findFirst();
    }

    /** Most recent check-ins, newest first. */
    public List<AttendanceRecord> recent(int limit) {
        return database.query("SELECT " + COLUMNS + " FROM attendance ORDER BY occurred_at DESC LIMIT ?",
                ps -> ps.setInt(1, limit), AttendanceRepository::map);
    }

    public List<AttendanceRecord> findAll() {
        return database.query("SELECT " + COLUMNS + " FROM attendance ORDER BY occurred_at",
                Database.NO_PARAMETERS, AttendanceRepository::map);
    }

    public int count() {
        return database.query("SELECT COUNT(*) AS c FROM attendance", Database.NO_PARAMETERS, rs -> rs.getInt("c"))
                .get(0);
    }

    private static AttendanceRecord map(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getLong("id"),
                rs.getLong("person_id"),
                rs.getString("person_name"),
                Database.toInstant(rs.getString("occurred_at")),
                AttendanceStatus.valueOf(rs.getString("status")),
                rs.getDouble("confidence"),
                rs.getString("source"));
    }
}
