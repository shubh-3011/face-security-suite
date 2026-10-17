package com.shubh3011.facesecurity.app;

import com.shubh3011.facesecurity.antivirus.QuarantineService;
import com.shubh3011.facesecurity.attendance.AttendanceService;
import com.shubh3011.facesecurity.attendance.UnknownFaceException;
import com.shubh3011.facesecurity.model.AttendanceRecord;
import com.shubh3011.facesecurity.model.AttendanceStats;
import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.GuardDecision;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.model.ScanResult;
import com.shubh3011.facesecurity.model.TrustedEntity;
import com.shubh3011.facesecurity.util.TimeProvider;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * A scripted, self-contained demonstration of every feature.
 *
 * <p>It exists for three reasons: it is the fastest way to see the suite work, it
 * doubles as documentation, and it is an end-to-end regression test. Everything is
 * generated into a caller-supplied workspace, so it never touches real data.
 */
public final class DemoRunner {

    private static final int[] IDENTITY_SEEDS = {101, 202, 303};
    private static final String[][] IDENTITIES = {
        {"Ada Lovelace", "STAFF"},
        {"Grace Hopper", "STAFF"},
        {"Alan Turing", "STUDENT"},
    };
    private static final String EICAR =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    private static final int IMAGE_SIZE = 200;

    /** Everything the demo produced, so callers can render or assert on it. */
    public record Outcome(
            Path workspace,
            List<Person> people,
            List<FaceMatch> identifications,
            List<AttendanceRecord> checkIns,
            AttendanceStats stats,
            List<ScanResult> scans,
            Optional<ScanResult> quarantinedScan,
            Optional<Path> quarantinedPath,
            TrustedEntity protectedResource,
            GuardDecision allowed,
            GuardDecision denied) {

        public long maliciousCount() {
            return scans.stream().filter(ScanResult::isMalicious).count();
        }
    }

    private DemoRunner() {
    }

    public static Outcome run(AppContext context, Path workspace) throws IOException, UnknownFaceException {
        Files.createDirectories(workspace);
        Path images = workspace.resolve("images");
        Path samples = workspace.resolve("samples");
        Files.createDirectories(images);
        Files.createDirectories(samples);

        // 1. Generate one identity per seed, plus a second "capture" of each.
        List<Person> people = new ArrayList<>();
        List<BufferedImage> variants = new ArrayList<>();
        for (int i = 0; i < IDENTITY_SEEDS.length; i++) {
            String name = IDENTITIES[i][0];
            String role = IDENTITIES[i][1];
            BufferedImage base = DemoImages.face(IDENTITY_SEEDS[i], IMAGE_SIZE);
            BufferedImage variant = DemoImages.variant(base, IDENTITY_SEEDS[i] + 1);
            DemoImages.writePng(images.resolve(name.replace(' ', '_') + ".png"), base);
            DemoImages.writePng(images.resolve(name.replace(' ', '_') + "_variant.png"), variant);
            variants.add(variant);
            people.add(context.faceEngine().enroll(name, role, base));
        }

        // 2. Identify each identity from its second capture.
        List<FaceMatch> identifications = new ArrayList<>();
        for (BufferedImage variant : variants) {
            identifications.add(context.faceEngine().identify(variant).orElse(null));
        }

        // 3. Attendance: two on-time check-ins, one late check-in, same day.
        ZoneId zone = context.zone();
        LocalDate day = LocalDate.now(zone);
        Instant onTime = day.atTime(8, 50).atZone(zone).toInstant();
        Instant late = day.atTime(9, 40).atZone(zone).toInstant();

        AttendanceService morning = context.attendanceService(TimeProvider.fixed(onTime));
        AttendanceService endOfMorning = context.attendanceService(TimeProvider.fixed(late));

        List<AttendanceRecord> checkIns = new ArrayList<>();
        checkIns.add(morning.markByFace(variants.get(0)));
        checkIns.add(morning.markByFace(variants.get(1)));
        checkIns.add(endOfMorning.markByFace(variants.get(2)));
        AttendanceStats stats = morning.stats(day);

        // 4. Antivirus: a clean file, the EICAR test artefact, a packed binary and a
        //    double-extension trick.
        Path clean = write(samples.resolve("meeting-notes.txt"),
                "Quarterly planning notes.\nEverything here is benign.\n");
        Path eicar = write(samples.resolve("eicar-test.com"), EICAR);
        Path packed = samples.resolve("payload.bin");
        writeRandom(packed, 8192);
        Path disguised = write(samples.resolve("invoice.pdf.exe"),
                "This file pretends to be a PDF but carries an executable extension.\n");

        List<ScanResult> scans = new ArrayList<>();
        for (Path file : List.of(clean, eicar, packed, disguised)) {
            ScanResult result = context.fileScanner().scan(file);
            context.recordScan(result);
            scans.add(result);
        }
        ScanResult maliciousScan = scans.stream()
                .filter(ScanResult::isMalicious)
                .findFirst()
                .orElse(null);

        // 5. Quarantine the first malicious artefact.
        Optional<ScanResult> quarantinedScan = Optional.ofNullable(maliciousScan);
        Optional<Path> quarantinedPath = Optional.empty();
        if (maliciousScan != null) {
            QuarantineService.QuarantinedFile record =
                    context.quarantineService().quarantine(maliciousScan.file(), maliciousScan);
            quarantinedPath = Optional.of(record.quarantinedPath());
        }

        // 6. Trusted Face Protection: lock a vault file to Ada and test both outcomes.
        Path vault = write(workspace.resolve("vault").resolve("secrets.txt"), "classified material\n");
        Person owner = people.get(0);
        TrustedEntity protectedResource = context.guardian().protect("Quarterly vault", EntityKind.FILE, vault, owner.id());
        GuardDecision allowed = context.guardian().checkAccess(protectedResource.id(), variants.get(0));
        GuardDecision denied = context.guardian().checkAccess(protectedResource.id(), variants.get(1));

        return new Outcome(workspace, people, identifications, checkIns, stats, scans,
                quarantinedScan, quarantinedPath, protectedResource, allowed, denied);
    }

    private static Path write(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void writeRandom(Path file, int bytes) throws IOException {
        byte[] data = new byte[bytes];
        new Random(42).nextBytes(data);
        Files.write(file, data);
    }
}
