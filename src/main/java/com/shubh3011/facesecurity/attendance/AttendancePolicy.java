package com.shubh3011.facesecurity.attendance;

import com.shubh3011.facesecurity.model.AttendanceStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Decides whether a check-in counts as on time.
 *
 * @param workStart expected arrival time
 * @param grace     how late a check-in may be before it is marked late
 */
public record AttendancePolicy(LocalTime workStart, Duration grace) {

    public static final LocalTime DEFAULT_WORK_START = LocalTime.of(9, 0);
    public static final Duration DEFAULT_GRACE = Duration.ofMinutes(15);

    public AttendancePolicy {
        if (workStart == null) {
            throw new IllegalArgumentException("workStart is required");
        }
        if (grace == null || grace.isNegative()) {
            throw new IllegalArgumentException("grace must not be negative");
        }
    }

    /** 09:00 with a 15 minute grace period. */
    public static AttendancePolicy standard() {
        return new AttendancePolicy(DEFAULT_WORK_START, DEFAULT_GRACE);
    }

    public AttendanceStatus statusFor(Instant instant, ZoneId zone) {
        LocalTime arrival = instant.atZone(zone).toLocalTime();
        return arrival.isAfter(workStart.plus(grace)) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }
}
