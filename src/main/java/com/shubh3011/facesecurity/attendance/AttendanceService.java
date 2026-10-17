package com.shubh3011.facesecurity.attendance;

import com.shubh3011.facesecurity.db.AttendanceRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.model.AttendanceRecord;
import com.shubh3011.facesecurity.model.AttendanceStats;
import com.shubh3011.facesecurity.model.AttendanceStatus;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Turns recognition events into attendance records.
 *
 * <p>Check-in is idempotent per person per day: scanning twice returns the original
 * record rather than creating a duplicate, which mirrors how a real turnstile
 * behaves and keeps the unique index in the schema satisfied.
 */
public final class AttendanceService {

    private final FaceEngine faceEngine;
    private final PersonRepository personRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendancePolicy policy;
    private final TimeProvider clock;
    private final ZoneId zone;

    public AttendanceService(
            FaceEngine faceEngine,
            PersonRepository personRepository,
            AttendanceRepository attendanceRepository,
            AttendancePolicy policy,
            TimeProvider clock,
            ZoneId zone) {
        this.faceEngine = faceEngine;
        this.personRepository = personRepository;
        this.attendanceRepository = attendanceRepository;
        this.policy = policy;
        this.clock = clock;
        this.zone = zone;
    }

    /** Recognises the face and records a check-in. */
    public AttendanceRecord markByFace(BufferedImage image) throws UnknownFaceException {
        FaceMatch match = faceEngine.identify(image)
                .orElseThrow(() -> new UnknownFaceException(
                        "no enrolled face matched (threshold " + faceEngine.threshold() + ")"));
        return checkIn(match.personId(), match.personName(), match.confidence(), AttendanceRecord.SOURCE_FACE);
    }

    /** Records a check-in without a face (e.g. a manual override at the desk). */
    public AttendanceRecord markManual(long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("unknown person id " + personId));
        return checkIn(person.id(), person.name(), 1.0, AttendanceRecord.SOURCE_MANUAL);
    }

    /** Core check-in use-case, idempotent per person per day. */
    public AttendanceRecord checkIn(long personId, String personName, double confidence, String source) {
        Instant now = clock.now();
        LocalDate day = now.atZone(zone).toLocalDate();

        Optional<AttendanceRecord> existing = attendanceRepository.findByPersonAndDay(personId, day);
        if (existing.isPresent()) {
            return existing.get();
        }

        AttendanceStatus status = policy.statusFor(now, zone);
        AttendanceRecord record = new AttendanceRecord(0L, personId, personName, now, status, confidence, source);
        return attendanceRepository.save(record, day);
    }

    public List<AttendanceRecord> forDate(LocalDate day) {
        return attendanceRepository.findByDay(day);
    }

    public List<AttendanceRecord> today() {
        return forDate(LocalDate.now(zone));
    }

    public List<AttendanceRecord> between(LocalDate from, LocalDate to) {
        return attendanceRepository.findBetween(from, to);
    }

    public List<AttendanceRecord> recent(int limit) {
        return attendanceRepository.recent(limit);
    }

    public AttendanceStats stats(LocalDate day) {
        List<AttendanceRecord> records = forDate(day);
        int present = (int) records.stream().filter(r -> r.status() == AttendanceStatus.PRESENT).count();
        int late = records.size() - present;
        return AttendanceStats.of(day, present, late);
    }

    public AttendancePolicy policy() {
        return policy;
    }

    public ZoneId zone() {
        return zone;
    }
}
