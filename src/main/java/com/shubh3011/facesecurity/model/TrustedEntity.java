package com.shubh3011.facesecurity.model;

import java.time.Instant;

/**
 * A resource (application, file or folder) that may only be opened when the
 * requesting face matches {@code trustedPersonId}.
 *
 * <p>This is the "Trusted Face Protection" feature: the suite acts as a
 * biometric gate in front of anything you point it at.
 *
 * @param id              database identity
 * @param label           human readable name
 * @param kind            what is being protected
 * @param path            absolute path of the protected resource
 * @param trustedPersonId identity allowed to open it
 * @param createdAt       when the rule was created
 */
public record TrustedEntity(
        long id,
        String label,
        EntityKind kind,
        String path,
        long trustedPersonId,
        Instant createdAt) {

    public TrustedEntity {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (kind == null) {
            kind = EntityKind.FILE;
        }
    }

    public static TrustedEntity create(String label, EntityKind kind, String path, long personId, Instant createdAt) {
        return new TrustedEntity(0L, label, kind, path, personId, createdAt);
    }

    public TrustedEntity withId(long newId) {
        return new TrustedEntity(newId, label, kind, path, trustedPersonId, createdAt);
    }
}
