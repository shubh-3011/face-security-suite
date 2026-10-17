package com.shubh3011.facesecurity.model;

/**
 * Result of comparing a probe face against one enrolled identity.
 *
 * @param personId   identity that produced this score
 * @param personName display name of that identity
 * @param confidence cosine similarity in {@code [-1, 1]}
 * @param accepted   whether the confidence cleared the matcher threshold
 */
public record FaceMatch(long personId, String personName, double confidence, boolean accepted) {

    public String confidencePercent() {
        return String.format("%.1f%%", confidence * 100.0);
    }
}
