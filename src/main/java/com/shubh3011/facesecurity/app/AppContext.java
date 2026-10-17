package com.shubh3011.facesecurity.app;

import com.shubh3011.facesecurity.antivirus.FileScanner;
import com.shubh3011.facesecurity.antivirus.QuarantineService;
import com.shubh3011.facesecurity.attendance.AttendancePolicy;
import com.shubh3011.facesecurity.attendance.AttendanceService;
import com.shubh3011.facesecurity.db.AttendanceRepository;
import com.shubh3011.facesecurity.db.Database;
import com.shubh3011.facesecurity.db.FaceEmbeddingRepository;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.db.ScanHistoryRepository;
import com.shubh3011.facesecurity.db.TrustedEntityRepository;
import com.shubh3011.facesecurity.face.CosineFaceMatcher;
import com.shubh3011.facesecurity.face.FaceEngine;
import com.shubh3011.facesecurity.face.GridGradientFeatureExtractor;
import com.shubh3011.facesecurity.guard.TrustedFaceGuardian;
import com.shubh3011.facesecurity.model.ScanRecord;
import com.shubh3011.facesecurity.model.ScanResult;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.time.ZoneId;

/**
 * Composition root: builds the object graph once and shares it with the CLI and
 * the JavaFX UI.
 *
 * <p>Everything is constructor-injected, so the same wiring powers production, the
 * demo and the tests — the only difference is the {@link TimeProvider} and the
 * {@link Database} backend.
 */
public final class AppContext implements AutoCloseable {

    private final AppConfig config;
    private final Database database;
    private final PersonRepository personRepository;
    private final FaceEmbeddingRepository embeddingRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final TrustedEntityRepository trustedEntityRepository;
    private final FaceEngine faceEngine;
    private final AttendanceService attendanceService;
    private final FileScanner fileScanner;
    private final QuarantineService quarantineService;
    private final TrustedFaceGuardian guardian;
    private final TimeProvider clock;
    private final ZoneId zone;

    private AppContext(
            AppConfig config,
            Database database,
            PersonRepository personRepository,
            FaceEmbeddingRepository embeddingRepository,
            AttendanceRepository attendanceRepository,
            ScanHistoryRepository scanHistoryRepository,
            TrustedEntityRepository trustedEntityRepository,
            FaceEngine faceEngine,
            AttendanceService attendanceService,
            FileScanner fileScanner,
            QuarantineService quarantineService,
            TrustedFaceGuardian guardian,
            TimeProvider clock,
            ZoneId zone) {
        this.config = config;
        this.database = database;
        this.personRepository = personRepository;
        this.embeddingRepository = embeddingRepository;
        this.attendanceRepository = attendanceRepository;
        this.scanHistoryRepository = scanHistoryRepository;
        this.trustedEntityRepository = trustedEntityRepository;
        this.faceEngine = faceEngine;
        this.attendanceService = attendanceService;
        this.fileScanner = fileScanner;
        this.quarantineService = quarantineService;
        this.guardian = guardian;
        this.clock = clock;
        this.zone = zone;
    }

    public static AppContext open(AppConfig config) {
        return open(config, TimeProvider.system(), ZoneId.systemDefault());
    }

    public static AppContext open(AppConfig config, TimeProvider clock) {
        return open(config, clock, ZoneId.systemDefault());
    }

    public static AppContext open(AppConfig config, TimeProvider clock, ZoneId zone) {
        Database database = Database.open(config.databaseFile());
        PersonRepository people = new PersonRepository(database);
        FaceEmbeddingRepository embeddings = new FaceEmbeddingRepository(database);
        AttendanceRepository attendance = new AttendanceRepository(database);
        ScanHistoryRepository scans = new ScanHistoryRepository(database);
        TrustedEntityRepository trusted = new TrustedEntityRepository(database);

        FaceEngine faceEngine = new FaceEngine(
                new GridGradientFeatureExtractor(), new CosineFaceMatcher(), people, embeddings, clock);
        AttendanceService attendanceService = new AttendanceService(
                faceEngine, people, attendance, AttendancePolicy.standard(), clock, zone);
        FileScanner fileScanner = new FileScanner();
        QuarantineService quarantine = new QuarantineService(config.quarantineDir(), clock);
        TrustedFaceGuardian guardian = new TrustedFaceGuardian(faceEngine, trusted, clock);

        return new AppContext(config, database, people, embeddings, attendance, scans, trusted,
                faceEngine, attendanceService, fileScanner, quarantine, guardian, clock, zone);
    }

    /** An attendance service driven by a different clock, used by the demo to show both outcomes. */
    public AttendanceService attendanceService(TimeProvider alternativeClock) {
        return new AttendanceService(faceEngine, personRepository, attendanceRepository,
                AttendancePolicy.standard(), alternativeClock, zone);
    }

    /** Persists a scan in the history table. */
    public ScanRecord recordScan(ScanResult result) {
        return scanHistoryRepository.save(ScanRecord.of(result, clock.now()));
    }

    public AppConfig config() {
        return config;
    }

    public Database database() {
        return database;
    }

    public PersonRepository personRepository() {
        return personRepository;
    }

    public FaceEmbeddingRepository embeddingRepository() {
        return embeddingRepository;
    }

    public AttendanceRepository attendanceRepository() {
        return attendanceRepository;
    }

    public ScanHistoryRepository scanHistoryRepository() {
        return scanHistoryRepository;
    }

    public TrustedEntityRepository trustedEntityRepository() {
        return trustedEntityRepository;
    }

    public FaceEngine faceEngine() {
        return faceEngine;
    }

    public AttendanceService attendanceService() {
        return attendanceService;
    }

    public FileScanner fileScanner() {
        return fileScanner;
    }

    public QuarantineService quarantineService() {
        return quarantineService;
    }

    public TrustedFaceGuardian guardian() {
        return guardian;
    }

    public TimeProvider clock() {
        return clock;
    }

    public ZoneId zone() {
        return zone;
    }

    @Override
    public void close() {
        database.close();
    }
}
