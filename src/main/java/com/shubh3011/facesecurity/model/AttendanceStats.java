package com.shubh3011.facesecurity.model;

import java.time.LocalDate;

/**
 * Aggregate figures for one day of attendance.
 *
 * @param day    the reporting day
 * @param total  number of check-ins
 * @param present check-ins that were on time
 * @param late   check-ins that were late
 */
public record AttendanceStats(LocalDate day, int total, int present, int late) {

    public static AttendanceStats of(LocalDate day, int present, int late) {
        return new AttendanceStats(day, present + late, present, late);
    }

    /** Percentage of check-ins that were on time, rounded to the nearest integer. */
    public int onTimeRatePercent() {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(100.0 * present / total);
    }
}
