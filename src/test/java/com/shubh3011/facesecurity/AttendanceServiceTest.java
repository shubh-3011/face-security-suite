package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.app.DemoImages;
import com.shubh3011.facesecurity.db.Database;
import com.shubh3011.facesecurity.db.AttendanceRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.face.CosineFaceMatcher;
import com.shubh3011.facesecurity.face.GridGradientFeatureExtractor;
import com.shubh3011.facesecurity.attendance.AttendancePolicy;
import com.shubh3011.facesecurity.attendance.AttendanceService;
import com.shubh3011.facesecurity.db.FaceEmbeddingRepository;
import com.shubh3011.facesecurity.model.AttendanceStatus;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceServiceTest {

    private static final Instant ON_TIME = Instant.parse("2026-10-17T08:45:00Z");
    private static final Instant LATE = Instant.parse("2026-10-17T09:45:00Z");

    @Test
    void marksOnTimeAndLate() throws Exception {
        ZoneId zone = ZoneId.of("UTC");
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);
        AttendanceRepository attendance = new AttendanceRepository(database);
        FaceEngine engine = new FaceEngine(new GridGradientFeatureExtractor(), new CosineFaceMatcher(),
                people, embeddings, TimeProvider.fixed(ON_TIME));
        BufferedImage face = DemoImages.face(1, 128);
        var person = engine.enroll("Ada", "STAFF", face);

        AttendanceService service = new AttendanceService(engine, people, attendance,
                AttendancePolicy.standard(), TimeProvider.fixed(ON_TIME), zone);
        var record = service.markByFace(DemoImages.variant(face, 2));
        assertEquals(AttendanceStatus.PRESENT, record.status());

        BufferedImage lateFace = DemoImages.face(2, 128);
        var latePerson = engine.enroll("Grace", "STAFF", lateFace);

        AttendanceService lateService = new AttendanceService(engine, people, attendance,
                AttendancePolicy.standard(), TimeProvider.fixed(LATE), zone);
        var lateRecord = lateService.markByFace(DemoImages.variant(lateFace, 3));
        assertEquals(AttendanceStatus.LATE, lateRecord.status());
    }

    @Test
    void checkInIsIdempotentPerDay() {
        ZoneId zone = ZoneId.of("UTC");
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);
        AttendanceRepository attendance = new AttendanceRepository(database);
        FaceEngine engine = new FaceEngine(new GridGradientFeatureExtractor(), new CosineFaceMatcher(),
                people, embeddings, TimeProvider.fixed(ON_TIME));
        var person = engine.enroll("Ada", "STAFF", DemoImages.face(1, 128));

        AttendanceService service = new AttendanceService(engine, people, attendance,
                AttendancePolicy.standard(), TimeProvider.fixed(ON_TIME), zone);
        var first = service.checkIn(person.id(), person.name(), 0.9, "FACE");
        var second = service.checkIn(person.id(), person.name(), 0.9, "FACE");
        assertEquals(first.id(), second.id());
        assertEquals(1, attendance.count());
    }

    @Test
    void statsCountPresentAndLate() {
        ZoneId zone = ZoneId.of("UTC");
        Database database = Database.inMemory();
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);
        AttendanceRepository attendance = new AttendanceRepository(database);
        FaceEngine engine = new FaceEngine(new GridGradientFeatureExtractor(), new CosineFaceMatcher(),
                people, embeddings, TimeProvider.fixed(ON_TIME));
        var person = engine.enroll("Ada", "STAFF", DemoImages.face(1, 128));

        AttendanceService service = new AttendanceService(engine, people, attendance,
                AttendancePolicy.standard(), TimeProvider.fixed(ON_TIME), zone);
        service.checkIn(person.id(), person.name(), 1.0, "MANUAL");
        var stats = service.stats(LocalDate.parse("2026-10-17"));
        assertEquals(1, stats.total());
        assertEquals(1, stats.present());
        assertEquals(0, stats.late());
        assertEquals(100, stats.onTimeRatePercent());
    }
}
