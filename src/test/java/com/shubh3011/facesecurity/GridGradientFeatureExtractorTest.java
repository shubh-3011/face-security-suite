package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.app.DemoImages;
import com.shubh3011.facesecurity.face.GridGradientFeatureExtractor;
import com.shubh3011.facesecurity.model.FaceEmbedding;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridGradientFeatureExtractorTest {

    @Test
    void isDeterministic() {
        GridGradientFeatureExtractor extractor = new GridGradientFeatureExtractor();
        BufferedImage image = DemoImages.face(12345, 128);
        FaceEmbedding a = extractor.extract(image);
        FaceEmbedding b = extractor.extract(image);
        assertEquals(a, b);
    }

    @Test
    void selfSimilarityIsNearOne() {
        GridGradientFeatureExtractor extractor = new GridGradientFeatureExtractor();
        BufferedImage image = DemoImages.face(7, 128);
        BufferedImage variant = DemoImages.variant(image, 8);
        double similarity = FaceEmbedding.cosineSimilarity(extractor.extract(image), extractor.extract(variant));
        assertTrue(similarity > 0.80, "expected self-similarity > 0.80 but was " + similarity);
    }

    @Test
    void differentIdentitiesAreLessSimilar() {
        GridGradientFeatureExtractor extractor = new GridGradientFeatureExtractor();
        BufferedImage a = DemoImages.face(1, 128);
        BufferedImage b = DemoImages.face(2, 128);
        double similarity = FaceEmbedding.cosineSimilarity(extractor.extract(a), extractor.extract(b));
        assertTrue(similarity < 0.80, "expected cross-identity similarity < 0.80 but was " + similarity);
    }

    @Test
    void dimensionIsStable() {
        GridGradientFeatureExtractor extractor = new GridGradientFeatureExtractor();
        assertEquals(1764, extractor.dimension());
        assertEquals(extractor.dimension(), extractor.extract(DemoImages.face(1, 64)).dimension());
    }

    @Test
    void brightnessShiftKeepsIdentity() {
        GridGradientFeatureExtractor extractor = new GridGradientFeatureExtractor();
        BufferedImage image = DemoImages.face(42, 128);
        BufferedImage bright = DemoImages.variant(image, 42);
        double similarity = FaceEmbedding.cosineSimilarity(extractor.extract(image), extractor.extract(bright));
        assertTrue(similarity > 0.80, "brightness shift collapsed identity: " + similarity);
    }
}
