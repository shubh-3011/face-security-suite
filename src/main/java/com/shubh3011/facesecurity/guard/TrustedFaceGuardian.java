package com.shubh3011.facesecurity.guard;

import com.shubh3011.facesecurity.db.TrustedEntityRepository;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.GuardDecision;
import com.shubh3011.facesecurity.model.TrustedEntity;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * "Trusted Face Protection": binds a resource to one identity and only lets that
 * identity through.
 *
 * <p>This is where the two halves of the suite meet — the antivirus side knows what
 * is dangerous, the face side knows who is allowed — and it is the feature that
 * turns a scanner plus a face matcher into a coherent security product.
 */
public final class TrustedFaceGuardian {

    private final FaceEngine faceEngine;
    private final TrustedEntityRepository entities;
    private final TimeProvider clock;

    public TrustedFaceGuardian(FaceEngine faceEngine, TrustedEntityRepository entities, TimeProvider clock) {
        this.faceEngine = faceEngine;
        this.entities = entities;
        this.clock = clock;
    }

    /** Protects a path, allowing only {@code personId} to open it. */
    public TrustedEntity protect(String label, EntityKind kind, Path target, long personId) {
        if (personId <= 0) {
            throw new IllegalArgumentException("a persisted person id is required");
        }
        String absolute = target.toAbsolutePath().toString();
        Optional<TrustedEntity> existing = entities.findByPath(absolute);
        TrustedEntity entity = existing
                .map(current -> new TrustedEntity(current.id(), label, kind, absolute, personId, current.createdAt()))
                .orElseGet(() -> TrustedEntity.create(label, kind, absolute, personId, clock.now()));
        return entities.save(entity);
    }

    /** Whether the probe face may open the protected resource. */
    public GuardDecision checkAccess(long entityId, BufferedImage probe) {
        TrustedEntity entity = entities.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("unknown protected resource " + entityId));

        Optional<FaceMatch> match = faceEngine.identify(probe);
        if (match.isEmpty()) {
            return GuardDecision.denied("no enrolled face matched the probe", null);
        }
        FaceMatch best = match.get();
        if (best.personId() != entity.trustedPersonId()) {
            return GuardDecision.denied(
                    "recognised as " + best.personName() + ", not the protected owner", best.confidence());
        }
        return GuardDecision.granted("welcome back, " + best.personName(), best.confidence());
    }

    public GuardDecision checkAccess(Path target, BufferedImage probe) {
        TrustedEntity entity = entities.findByPath(target.toAbsolutePath().toString())
                .orElseThrow(() -> new IllegalArgumentException("path is not protected: " + target));
        return checkAccess(entity.id(), probe);
    }

    public boolean isProtected(Path target) {
        return entities.findByPath(target.toAbsolutePath().toString()).isPresent();
    }

    public List<TrustedEntity> protectedResources() {
        return entities.findAll();
    }

    public boolean revoke(long entityId) {
        return entities.delete(entityId);
    }

    public int count() {
        return entities.count();
    }

    /** Exposed for the GUI, which shows how many samples feed the guard. */
    public int enrolledSamples() {
        return faceEngine.enrolledSamples();
    }
}
