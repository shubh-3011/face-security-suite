package com.shubh3011.facesecurity.model;

import java.time.Instant;

/**
 * A person known to the suite: someone who can be recognised by face and whose
 * attendance is tracked.
 *
 * @param id        database identity, {@code 0} before the first persist
 * @param name      unique display name
 * @param role      free-form role such as {@code STUDENT} or {@code STAFF}
 * @param createdAt enrolment timestamp
 */
public record Person(long id, String name, String role, Instant createdAt) {

    public static final String DEFAULT_ROLE = "MEMBER";

    public Person {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("person name must not be blank");
        }
        if (role == null || role.isBlank()) {
            role = DEFAULT_ROLE;
        }
    }

    /** A not-yet-persisted person. */
    public static Person create(String name, String role, Instant createdAt) {
        return new Person(0L, name, role, createdAt);
    }

    public Person withId(long newId) {
        return new Person(newId, name, role, createdAt);
    }
}
