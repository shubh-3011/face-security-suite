package com.shubh3011.facesecurity.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A fixed-length, L2-normalisable feature vector describing one face image.
 *
 * <p>Embeddings are produced by a {@code FaceFeatureExtractor} and compared with a
 * cosine-similarity matcher. Storing them as a plain float vector keeps the engine
 * portable: no model files, no native libraries.
 */
public final class FaceEmbedding {

    private final float[] values;

    public FaceEmbedding(float[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("embedding must contain at least one value");
        }
        this.values = values.clone();
    }

    public int dimension() {
        return values.length;
    }

    public float[] toArray() {
        return values.clone();
    }

    public float valueAt(int index) {
        return values[index];
    }

    public double norm() {
        double sum = 0.0;
        for (float v : values) {
            sum += (double) v * v;
        }
        return Math.sqrt(sum);
    }

    /** Returns a unit-length copy; a zero vector is returned unchanged. */
    public FaceEmbedding l2Normalized() {
        double norm = norm();
        if (norm == 0.0) {
            return new FaceEmbedding(values);
        }
        float[] scaled = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            scaled[i] = (float) (values[i] / norm);
        }
        return new FaceEmbedding(scaled);
    }

    /**
     * Cosine similarity in {@code [-1, 1]}. {@code 1.0} means identical orientation,
     * {@code 0.0} means orthogonal (unrelated) descriptors.
     */
    public static double cosineSimilarity(FaceEmbedding a, FaceEmbedding b) {
        if (a.dimension() != b.dimension()) {
            throw new IllegalArgumentException(
                    "dimension mismatch: " + a.dimension() + " vs " + b.dimension());
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.values.length; i++) {
            dot += (double) a.values[i] * b.values[i];
            normA += (double) a.values[i] * a.values[i];
            normB += (double) b.values[i] * b.values[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Big-endian {@code float32} layout, used for SQLite BLOB storage. */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float v : values) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    public static FaceEmbedding fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("not a float32 payload: " + (bytes == null ? -1 : bytes.length) + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        float[] values = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getFloat();
        }
        return new FaceEmbedding(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FaceEmbedding other)) {
            return false;
        }
        return Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "FaceEmbedding[dim=" + values.length + ", norm=" + String.format("%.3f", norm()) + "]";
    }
}
