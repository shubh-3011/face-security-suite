package com.shubh3011.facesecurity.model;

/**
 * Verdict of the access guard for one attempt to open a protected resource.
 *
 * @param allowed    whether access may proceed
 * @param reason     short human readable justification
 * @param confidence face-match confidence when a match was attempted, else {@code null}
 */
public record GuardDecision(boolean allowed, String reason, Double confidence) {

    public static GuardDecision granted(String reason, double confidence) {
        return new GuardDecision(true, reason, confidence);
    }

    public static GuardDecision denied(String reason, Double confidence) {
        return new GuardDecision(false, reason, confidence);
    }

    public String confidencePercent() {
        return confidence == null ? "n/a" : String.format("%.1f%%", confidence * 100.0);
    }
}
