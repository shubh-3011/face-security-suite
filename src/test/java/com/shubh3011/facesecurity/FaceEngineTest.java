package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.app.DemoImages;
import com.shubh3011.facesecurity.db.Database;
import com.shubh3011.facesecurity.db.FaceEmbeddingRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.face.CosineFaceMatcher;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.face.GridGradientFeatureExtractor;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceEngineTest {

    @Test
    void enrollAndIdentifyRoundTrip() {
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);

        FaceEngine engine = new FaceEngine(
                new GridGradientFeatureExtractor(),
                new CosineFaceMatcher(),
                people,
                embeddings,
                TimeProvider.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z")));

        BufferedImage base = DemoImages.face(7, 128);
        BufferedImage probe = DemoImages.variant(base, 8);

        engine.enroll("Ada", "STAFF", base);
        FaceMatch match = engine.identify(probe).orElseThrow();

        assertEquals("Ada", match.personName());
        assertTrue(match.accepted());
        assertTrue(match.confidence() > 0.80);
    }

    @Test
    void forgetRemovesIdentity() {
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);

        FaceEngine engine = new FaceEngine(
                new GridGradientFeatureExtractor(), new CosineFaceMatcher(),
                people, embeddings, TimeProvider.system());

        engine.enroll("Grace", "STAFF", DemoImages.face(9, 128));
        assertTrue(engine.enrolledPeople() == 1);
        assertTrue(engine.forget(1));
        assertTrue(engine.enrolledPeople() == 0);
    }
}
