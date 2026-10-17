package com.shubh3011.facesecurity.face;

import com.shubh3011.facesecurity.model.FaceEmbedding;
import java.awt.image.BufferedImage;

/**
 * Turns a face image into a comparable {@link FaceEmbedding}.
 *
 * <p>Implementations must be stateless and thread-safe, and must always return
 * vectors of {@link #dimension()} length.
 */
public interface FaceFeatureExtractor {

    FaceEmbedding extract(BufferedImage image);

    /** Length of every embedding this extractor produces. */
    int dimension();
}
