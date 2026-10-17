package com.shubh3011.facesecurity.face;

import com.shubh3011.facesecurity.db.FaceEmbeddingRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.model.FaceEmbedding;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.model.StoredFace;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates enrolment and identification.
 *
 * <p>It owns the "who is this?" question and nothing else: feature extraction,
 * comparison and persistence are delegated, so this class reads as the use-cases
 * of the face feature.
 */
public final class FaceEngine {

    private final FaceFeatureExtractor extractor;
    private final FaceMatcher matcher;
    private final PersonRepository personRepository;
    private final FaceEmbeddingRepository embeddingRepository;
    private final TimeProvider clock;

    public FaceEngine(
            FaceFeatureExtractor extractor,
            FaceMatcher matcher,
            PersonRepository personRepository,
            FaceEmbeddingRepository embeddingRepository,
            TimeProvider clock) {
        this.extractor = extractor;
        this.matcher = matcher;
        this.personRepository = personRepository;
        this.embeddingRepository = embeddingRepository;
        this.clock = clock;
    }

    /**
     * Enrols a face. A person with the same name is reused and gains an additional
     * sample, which lets recognition tolerate pose/lighting variation.
     */
    public Person enroll(String name, String role, BufferedImage image) {
        FaceEmbedding embedding = extractor.extract(image);
        Person person = personRepository.findByName(name)
                .orElseGet(() -> personRepository.save(Person.create(name, role, clock.now())));
        embeddingRepository.save(person.id(), embedding, clock.now());
        return person;
    }

    /** The best identity for the supplied image, present only if it clears the threshold. */
    public Optional<FaceMatch> identify(BufferedImage image) {
        return candidates(image, 1).stream().filter(FaceMatch::accepted).findFirst();
    }

    /** Ranked candidates, useful for "did you mean" UI and diagnostics. */
    public List<FaceMatch> candidates(BufferedImage image, int limit) {
        FaceEmbedding probe = extractor.extract(image);
        return embeddingRepository.findAll().stream()
                .map(face -> toMatch(probe, face))
                .sorted(Comparator.comparingDouble(FaceMatch::confidence).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private FaceMatch toMatch(FaceEmbedding probe, StoredFace face) {
        double similarity = matcher.similarity(probe, face.embedding());
        return new FaceMatch(face.personId(), face.personName(), similarity, matcher.matches(similarity));
    }

    /** Removes an identity and all of its enrolled samples. */
    public boolean forget(long personId) {
        embeddingRepository.deleteByPersonId(personId);
        return personRepository.delete(personId);
    }

    public int enrolledPeople() {
        return personRepository.count();
    }

    public int enrolledSamples() {
        return embeddingRepository.count();
    }

    public double threshold() {
        return matcher.threshold();
    }

    public FaceFeatureExtractor extractor() {
        return extractor;
    }
}
