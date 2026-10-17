package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.model.FaceEmbedding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceEmbeddingTest {

    @Test
    void identicalVectorsHaveUnitSimilarity() {
        float[] values = {1f, 2f, 3f, 4f};
        FaceEmbedding a = new FaceEmbedding(values);
        assertEquals(1.0, FaceEmbedding.cosineSimilarity(a, a), 1e-9);
    }

    @Test
    void orthogonalVectorsHaveZeroSimilarity() {
        FaceEmbedding a = new FaceEmbedding(new float[]{1f, 0f});
        FaceEmbedding b = new FaceEmbedding(new float[]{0f, 1f});
        assertEquals(0.0, FaceEmbedding.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void byteRoundTripPreservesValues() {
        float[] values = {0.1f, -0.25f, 1.5f, 0.0f, -3.25f};
        FaceEmbedding original = new FaceEmbedding(values);
        FaceEmbedding restored = FaceEmbedding.fromBytes(original.toBytes());
        assertEquals(original, restored);
        assertEquals(original.dimension(), restored.dimension());
    }

    @Test
    void l2NormalizedHasUnitNorm() {
        FaceEmbedding embedding = new FaceEmbedding(new float[]{3f, 4f}).l2Normalized();
        assertEquals(1.0, embedding.norm(), 1e-6);
    }

    @Test
    void cosineIsScaleInvariant() {
        FaceEmbedding a = new FaceEmbedding(new float[]{1f, 2f, 3f});
        FaceEmbedding b = new FaceEmbedding(new float[]{2f, 4f, 6f});
        assertTrue(FaceEmbedding.cosineSimilarity(a, b) > 0.999);
    }
}
