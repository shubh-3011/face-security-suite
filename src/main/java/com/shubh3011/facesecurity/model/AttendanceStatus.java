package com.shubh3011.facesecurity.model;

/** Attendance outcome for a single person on a single day. */
public enum AttendanceStatus {
    /** Arrived inside the policy window. */
    PRESENT("Present"),
    /** Arrived after the configured grace period. */
    LATE("Late");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
