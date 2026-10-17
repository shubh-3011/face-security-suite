package com.shubh3011.facesecurity.face;

import com.shubh3011.facesecurity.model.FaceEmbedding;

/**
 * Cosine-similarity matcher.
 *
 * <p>Cosine similarity ignores vector magnitude, which makes it robust to small
 * global brightness changes between two captures of the same person. The default
 * threshold was tuned against the synthetic fixtures in the test-suite.
 */
public final class CosineFaceMatcher implements FaceMatcher {

    public static final double DEFAULT_THRESHOLD = 0.80;

    private final double threshold;

    public CosineFaceMatcher() {
        this(DEFAULT_THRESHOLD);
    }

    public CosineFaceMatcher(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be within [0, 1]");
        }
        this.threshold = threshold;
    }

    @Override
    public double similarity(FaceEmbedding a, FaceEmbedding b) {
        return FaceEmbedding.cosineSimilarity(a, b);
    }

    @Override
    public boolean matches(double similarity) {
        return similarity >= threshold;
    }

    @Override
    public double threshold() {
        return threshold;
    }
}
