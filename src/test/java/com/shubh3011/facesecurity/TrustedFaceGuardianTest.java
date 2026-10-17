package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.app.DemoImages;
import com.shubh3011.facesecurity.db.Database;
import com.shubh3011.facesecurity.db.FaceEmbeddingRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.db.TrustedEntityRepository;
import com.shubh3011.facesecurity.face.CosineFaceMatcher;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.face.GridGradientFeatureExtractor;
import com.shubh3011.facesecurity.guard.TrustedFaceGuardian;
import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedFaceGuardianTest {

    @Test
    void allowsOwnerAndDeniesOthers() {
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);
        TrustedEntityRepository trusted = new TrustedEntityRepository(database);
        FaceEngine engine = new FaceEngine(new GridGradientFeatureExtractor(), new CosineFaceMatcher(),
                people, embeddings, TimeProvider.system());

        BufferedImage ownerFace = DemoImages.face(1, 128);
        BufferedImage otherFace = DemoImages.face(2, 128);
        var owner = engine.enroll("Owner", "STAFF", ownerFace);
        var other = engine.enroll("Other", "STAFF", otherFace);

        TrustedFaceGuardian guardian = new TrustedFaceGuardian(engine, trusted, TimeProvider.system());
        var entity = guardian.protect("vault", EntityKind.FILE, Path.of("/tmp/vault.txt"), owner.id());

        assertTrue(guardian.checkAccess(entity.id(), DemoImages.variant(ownerFace, 2)).allowed());
        assertFalse(guardian.checkAccess(entity.id(), DemoImages.variant(otherFace, 3)).allowed());
    }
}
