package com.shubh3011.facesecurity.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * One attendance event. A given person is recorded at most once per day, so a
 * record is effectively a daily check-in.
 *
 * @param id         database identity, {@code 0} before the first persist
 * @param personId   identity that checked in
 * @param personName denormalised display name for reporting
 * @param occurredAt instant the check-in was captured
 * @param status     {@link AttendanceStatus} derived from the attendance policy
 * @param confidence face-match confidence, {@code 1.0} for a manual check-in
 * @param source     where the event came from, e.g. {@code FACE} or {@code MANUAL}
 */
public record AttendanceRecord(
        long id,
        long personId,
        String personName,
        Instant occurredAt,
        AttendanceStatus status,
        double confidence,
        String source) {

    public static final String SOURCE_FACE = "FACE";
    public static final String SOURCE_MANUAL = "MANUAL";

    public AttendanceRecord {
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        source = (source == null || source.isBlank()) ? SOURCE_MANUAL : source;
    }

    public AttendanceRecord withId(long newId) {
        return new AttendanceRecord(newId, personId, personName, occurredAt, status, confidence, source);
    }

    public LocalDate day(ZoneId zone) {
        return occurredAt.atZone(zone).toLocalDate();
    }
}
