package com.shubh3011.facesecurity.face;

import com.shubh3011.facesecurity.model.FaceEmbedding;

/**
 * Compares two embeddings.
 *
 * <p>Separated from {@link FaceFeatureExtractor} so the scoring rule can be
 * swapped (cosine today, an L2/PLDA matcher tomorrow) without touching the
 * feature pipeline.
 */
public interface FaceMatcher {

    double similarity(FaceEmbedding a, FaceEmbedding b);

    /** Whether a similarity score is high enough to accept the identity. */
    boolean matches(double similarity);

    double threshold();
}
