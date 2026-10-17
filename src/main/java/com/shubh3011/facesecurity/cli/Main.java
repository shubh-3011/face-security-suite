package com.shubh3011.facesecurity.cli;

import com.shubh3011.facesecurity.antivirus.FileScanner;
import com.shubh3011.facesecurity.antivirus.QuarantineService;
import com.shubh3011.facesecurity.antivirus.ScannerConfig;
import com.shubh3011.facesecurity.antivirus.SignatureDatabase;
import com.shubh3011.facesecurity.app.AppConfig;
import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.app.DemoRunner;
import com.shubh3011.facesecurity.attendance.AttendanceCsvExporter;
import com.shubh3011.facesecurity.attendance.AttendancePdfExporter;
import com.shubh3011.facesecurity.face.ImageLoader;
import com.shubh3011.facesecurity.model.AttendanceRecord;
import com.shubh3011.facesecurity.model.AttendanceStats;
import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.GuardDecision;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.model.ScanRecord;
import com.shubh3011.facesecurity.model.ScanResult;
import com.shubh3011.facesecurity.model.Threat;
import com.shubh3011.facesecurity.model.ThreatSignature;
import com.shubh3011.facesecurity.model.TrustedEntity;
import com.shubh3011.facesecurity.model.Verdict;
import com.shubh3011.facesecurity.util.Hashing;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Headless entry point.
 *
 * <p>Packaged as the executable jar, this is what runs in CI and on machines without
 * a desktop environment. Exit codes: {@code 0} success, {@code 1} error or a
 * {@link Verdict#MALICIOUS} detection during {@code scan}, {@code 2} bad usage.
 */
public final class Main {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        int code = new Main().execute(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    int execute(String[] argv) {
        Args args = Args.parse(argv);
        Console out = args.has("no-color") ? new Console(false) : Console.withColourDefault();
        String command = args.command();

        if (command == null || "help".equals(command) || args.has("help")) {
            printHelp(out);
            return 0;
        }

        try {
            return switch (command) {
                case "demo" -> demo(args, out);
                case "enroll" -> enroll(args, out);
                case "identify" -> identify(args, out);
                case "people" -> people(args, out);
                case "attendance" -> attendance(args, out);
                case "export" -> export(args, out);
                case "scan" -> scan(args, out);
                case "history" -> history(args, out);
                case "signatures" -> signatures(args, out);
                case "protect" -> protect(args, out);
                case "access" -> access(args, out);
                case "protected" -> protectedResources(args, out);
                case "revoke" -> revoke(args, out);
                case "info" -> info(args, out);
                default -> {
                    out.error("unknown command: " + command);
                    printHelp(out);
                    yield 2;
                }
            };
        } catch (CliException e) {
            out.error(e.getMessage());
            return 1;
        } catch (Exception e) {
            out.error(e.getMessage() == null ? e.toString() : e.getMessage());
            return 1;
        }
    }

    // ---------------------------------------------------------------- commands

    private int demo(Args args, Console out) throws Exception {
        Path workspace = args.get("workspace").map(Path::of).orElseGet(Main::temporaryDirectory);
        AppConfig config = args.get("data-dir")
                .map(dir -> AppConfig.of(Path.of(dir)))
                .orElseGet(() -> AppConfig.of(workspace.resolve(".fss-data")));

        try (AppContext context = AppContext.open(config)) {
            DemoRunner.Outcome outcome = DemoRunner.run(context, workspace);
            renderDemo(outcome, out, config);
        }
        return 0;
    }

    private int enroll(Args args, Console out) throws Exception {
        String name = args.require("name");
        String role = args.get("role", Person.DEFAULT_ROLE);
        BufferedImage image = ImageLoader.load(Path.of(args.require("image")));
        try (AppContext context = open(args)) {
            Person person = context.faceEngine().enroll(name, role, image);
            out.ok("enrolled \"" + person.name() + "\" as " + person.role() + " (id " + person.id() + ")");
            out.key("people", context.faceEngine().enrolledPeople());
            out.key("samples", context.faceEngine().enrolledSamples());
        }
        return 0;
    }

    private int identify(Args args, Console out) throws Exception {
        BufferedImage image = ImageLoader.load(Path.of(args.require("image")));
        int top = args.getInt("top", 3);
        try (AppContext context = open(args)) {
            List<FaceMatch> candidates = context.faceEngine().candidates(image, top);
            if (candidates.isEmpty()) {
                out.warn("nobody is enrolled yet - run `enroll` first");
                return 0;
            }
            List<String[]> rows = new ArrayList<>();
            int rank = 1;
            for (FaceMatch match : candidates) {
                rows.add(new String[] {
                    String.valueOf(rank++),
                    match.personName(),
                    match.confidencePercent(),
                    match.accepted() ? "yes" : "no"
                });
            }
            out.section("Candidates (threshold " + String.format(Locale.ROOT, "%.2f", context.faceEngine().threshold()) + ")");
            out.table(new String[] {"#", "person", "confidence", "accepted"}, rows);

            candidates.stream().filter(FaceMatch::accepted).findFirst().ifPresentOrElse(
                    match -> out.ok("identified as " + match.personName()),
                    () -> out.warn("no candidate cleared the threshold"));
        }
        return 0;
    }

    private int people(Args args, Console out) {
        try (AppContext context = open(args)) {
            List<Person> people = context.personRepository().findAll();
            if (people.isEmpty()) {
                out.warn("no people enrolled");
                return 0;
            }
            List<String[]> rows = new ArrayList<>();
            for (Person person : people) {
                boolean hasFace = context.embeddingRepository().findFirstByPersonId(person.id()).isPresent();
                rows.add(new String[] {
                    String.valueOf(person.id()), person.name(), person.role(),
                    person.createdAt().toString(), hasFace ? "yes" : "no"
                });
            }
            out.table(new String[] {"id", "name", "role", "enrolled at", "face"}, rows);
        }
        return 0;
    }

    private int attendance(Args args, Console out) {
        try (AppContext context = open(args)) {
            ZoneId zone = context.zone();
            if (args.has("recent")) {
                List<AttendanceRecord> records = context.attendanceService().recent(args.getInt("recent", 10));
                out.section("Recent check-ins");
                printAttendance(records, out, zone);
                return 0;
            }
            LocalDate day = args.get("date").map(LocalDate::parse).orElse(LocalDate.now(zone));
            List<AttendanceRecord> records = context.attendanceService().forDate(day);
            AttendanceStats stats = context.attendanceService().stats(day);
            out.section("Attendance for " + day);
            printAttendance(records, out, zone);
            out.key("check-ins", stats.total());
            out.key("on time", stats.present());
            out.key("late", stats.late());
            out.key("on-time rate", stats.onTimeRatePercent() + "%");
        }
        return 0;
    }

    private int export(Args args, Console out) throws IOException {
        String format = args.get("format", "csv").toLowerCase(Locale.ROOT);
        Path target = Path.of(args.require("out"));
        try (AppContext context = open(args)) {
            ZoneId zone = context.zone();
            LocalDate day = args.get("date").map(LocalDate::parse).orElse(LocalDate.now(zone));
            List<AttendanceRecord> records = context.attendanceService().forDate(day);
            switch (format) {
                case "csv" -> AttendanceCsvExporter.write(target, records, zone);
                case "pdf" -> AttendancePdfExporter.write(target, day, records,
                        context.attendanceService().stats(day), "Face Security Suite - Attendance Report", zone);
                default -> throw new CliException("unsupported format '" + format + "' (use csv or pdf)");
            }
            out.ok("wrote " + records.size() + " records to " + target.toAbsolutePath());
        }
        return 0;
    }

    private int scan(Args args, Console out) throws IOException {
        List<Path> files = args.positionals().stream().map(Path::of).toList();
        if (files.isEmpty()) {
            throw new CliException("scan needs at least one file: `scan <file> [file...]`");
        }
        boolean quarantine = args.has("quarantine");

        try (AppContext context = open(args)) {
            FileScanner scanner = context.fileScanner();
            if (args.has("signatures")) {
                scanner = new FileScanner(SignatureDatabase.load(Path.of(args.require("signatures"))),
                        ScannerConfig.defaults());
            }
            boolean foundMalware = false;
            for (Path file : files) {
                ScanResult result = scanner.scan(file);
                context.recordScan(result);
                renderScan(result, out);
                if (result.isMalicious()) {
                    foundMalware = true;
                    if (quarantine) {
                        QuarantineService.QuarantinedFile record =
                                context.quarantineService().quarantine(file, result);
                        out.info("quarantined as " + record.id());
                    }
                }
            }
            return foundMalware ? 1 : 0;
        }
    }

    private int history(Args args, Console out) {
        try (AppContext context = open(args)) {
            List<ScanRecord> records = context.scanHistoryRepository().recent(args.getInt("limit", 15));
            if (records.isEmpty()) {
                out.warn("no scans recorded yet");
                return 0;
            }
            List<String[]> rows = new ArrayList<>();
            for (ScanRecord record : records) {
                rows.add(new String[] {
                    record.scannedAt().toString(),
                    record.verdict().name(),
                    Hashing.abbreviate(record.sha256(), 12),
                    shorten(record.filePath(), 42),
                    record.threatSummary().isEmpty() ? "-" : record.threatSummary()
                });
            }
            out.table(new String[] {"scanned at", "verdict", "sha256", "file", "threats"}, rows);
        }
        return 0;
    }

    private int signatures(Args args, Console out) {
        SignatureDatabase database = args.get("signatures")
                .map(path -> {
                    try {
                        return SignatureDatabase.load(Path.of(path));
                    } catch (IOException e) {
                        throw new CliException("could not load signatures from " + path + ": " + e.getMessage());
                    }
                })
                .orElseGet(SignatureDatabase::builtin);
        out.section("Loaded " + database.size() + " signatures");
        List<String[]> rows = new ArrayList<>();
        for (ThreatSignature signature : database.all()) {
            rows.add(new String[] {
                signature.id(), signature.severity().name(), signature.type().name(), signature.name()
            });
        }
        out.table(new String[] {"id", "severity", "type", "name"}, rows);
        return 0;
    }

    private int protect(Args args, Console out) {
        String label = args.require("label");
        Path target = Path.of(args.require("path")).toAbsolutePath();
        long personId = Long.parseLong(args.require("person-id"));
        EntityKind kind = EntityKind.valueOf(args.get("kind", "FILE").toUpperCase(Locale.ROOT));
        try (AppContext context = open(args)) {
            TrustedEntity entity = context.guardian().protect(label, kind, target, personId);
            out.ok("protecting " + entity.path() + " (rule #" + entity.id() + ", owner person #" + entity.trustedPersonId() + ")");
            out.info("verify with: access --entity-id " + entity.id() + " --image <face.png>");
        }
        return 0;
    }

    private int access(Args args, Console out) throws Exception {
        BufferedImage probe = ImageLoader.load(Path.of(args.require("image")));
        try (AppContext context = open(args)) {
            GuardDecision decision;
            if (args.has("entity-id")) {
                decision = context.guardian().checkAccess(Long.parseLong(args.require("entity-id")), probe);
            } else if (args.has("path")) {
                decision = context.guardian().checkAccess(Path.of(args.require("path")), probe);
            } else {
                throw new CliException("access needs --entity-id or --path");
            }
            if (decision.allowed()) {
                out.ok("ACCESS GRANTED - " + decision.reason() + " (confidence " + decision.confidencePercent() + ")");
            } else {
                out.bad("ACCESS DENIED - " + decision.reason() + " (confidence " + decision.confidencePercent() + ")");
                return 1;
            }
        }
        return 0;
    }

    private int protectedResources(Args args, Console out) {
        try (AppContext context = open(args)) {
            List<TrustedEntity> entities = context.guardian().protectedResources();
            if (entities.isEmpty()) {
                out.warn("nothing is protected yet - use `protect`");
                return 0;
            }
            List<String[]> rows = new ArrayList<>();
            for (TrustedEntity entity : entities) {
                rows.add(new String[] {
                    String.valueOf(entity.id()), entity.label(), entity.kind().name(),
                    "person #" + entity.trustedPersonId(), shorten(entity.path(), 46)
                });
            }
            out.table(new String[] {"id", "label", "kind", "owner", "path"}, rows);
        }
        return 0;
    }

    private int revoke(Args args, Console out) {
        long id = Long.parseLong(args.require("entity-id"));
        try (AppContext context = open(args)) {
            if (context.guardian().revoke(id)) {
                out.ok("revoked protection rule #" + id);
            } else {
                out.warn("no protection rule with id " + id);
            }
        }
        return 0;
    }

    private int info(Args args, Console out) {
        try (AppContext context = open(args)) {
            AppConfig config = context.config();
            out.section("Configuration");
            out.key("data dir", config.dataDir());
            out.key("database", config.databaseFile());
            out.key("quarantine", config.quarantineDir());
            out.key("recogniser", context.faceEngine().extractor().getClass().getSimpleName()
                    + " (dim " + context.faceEngine().extractor().dimension() + ")");
            out.key("threshold", String.format(Locale.ROOT, "%.2f", context.faceEngine().threshold()));
            out.key("signatures", context.fileScanner().signatures().size());
            out.section("State");
            out.key("people", context.faceEngine().enrolledPeople());
            out.key("face samples", context.faceEngine().enrolledSamples());
            out.key("check-ins", context.attendanceRepository().count());
            out.key("scans", context.scanHistoryRepository().count());
            out.key("protected", context.guardian().count());
        }
        return 0;
    }

    // ---------------------------------------------------------------- rendering

    private void renderDemo(DemoRunner.Outcome outcome, Console out, AppConfig config) throws IOException {
        out.title("Face Security Suite - end-to-end demo");

        out.section("1. Enrolled identities (synthetic, generated on the fly)");
        List<String[]> people = new ArrayList<>();
        for (Person person : outcome.people()) {
            people.add(new String[] {String.valueOf(person.id()), person.name(), person.role()});
        }
        out.table(new String[] {"id", "name", "role"}, people);

        out.section("2. Identification from a second capture of each face");
        List<String[]> idRows = new ArrayList<>();
        for (FaceMatch match : outcome.identifications()) {
            idRows.add(new String[] {
                match == null ? "(no match)" : match.personName(),
                match == null ? "-" : match.confidencePercent(),
                match != null && match.accepted() ? "matched" : "rejected"
            });
        }
        out.table(new String[] {"identified as", "confidence", "result"}, idRows);

        out.section("3. Attendance");
        printAttendance(outcome.checkIns(), out, ZoneId.systemDefault());
        out.key("check-ins", outcome.stats().total());
        out.key("on-time rate", outcome.stats().onTimeRatePercent() + "%");

        out.section("4. Antivirus");
        for (ScanResult result : outcome.scans()) {
            renderScan(result, out);
        }

        outcome.quarantinedScan().ifPresent(scan -> {
            out.section("5. Quarantine");
            out.info("moved " + scan.fileName() + " into isolation");
            outcome.quarantinedPath().ifPresent(path -> out.key("payload", path));
        });

        out.section("6. Trusted Face Protection");
        out.key("protected", outcome.protectedResource().label() + " (" + outcome.protectedResource().path() + ")");
        renderDecision(outcome.allowed(), out);
        renderDecision(outcome.denied(), out);

        out.section("Artifacts");
        out.key("workspace", outcome.workspace());
        out.key("data dir", config.dataDir());
        out.key("quarantine", Files.exists(config.quarantineDir()) ? "populated" : "empty");
        out.println();
        out.ok("demo complete - every subsystem executed end to end");
    }

    private void renderDecision(GuardDecision decision, Console out) {
        String text = decision.reason() + " (confidence " + decision.confidencePercent() + ")";
        if (decision.allowed()) {
            out.ok("granted: " + text);
        } else {
            out.bad("denied: " + text);
        }
    }

    private void renderScan(ScanResult result, Console out) {
        String line = result.fileName() + "  " + result.verdict() + "  entropy "
                + String.format(Locale.ROOT, "%.2f", result.entropy()) + "  " + result.sizeBytes() + " bytes";
        switch (result.verdict()) {
            case CLEAN -> out.ok(line);
            case SUSPICIOUS -> out.warn(line);
            case MALICIOUS -> out.bad(line);
            case ERROR -> out.error(line + " (" + result.detail() + ")");
        }
        for (Threat threat : result.threats()) {
            out.println("         " + threat.severity() + "  " + threat.name() + "  " + threat.evidence());
        }
        if (!result.detail().isBlank() && result.threats().isEmpty()) {
            out.println("         note: " + result.detail());
        }
    }

    private void printAttendance(List<AttendanceRecord> records, Console out, ZoneId zone) {
        if (records.isEmpty()) {
            out.info("no check-ins");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AttendanceRecord record : records) {
            rows.add(new String[] {
                record.personName(),
                record.occurredAt().atZone(zone).format(time),
                record.status().name(),
                String.format(Locale.ROOT, "%.1f%%", record.confidence() * 100),
                record.source()
            });
        }
        out.table(new String[] {"person", "when", "status", "confidence", "source"}, rows);
    }

    // ---------------------------------------------------------------- helpers

    private AppContext open(Args args) {
        AppConfig config = args.get("data-dir")
                .map(dir -> AppConfig.of(Path.of(dir)))
                .orElseGet(AppConfig::defaults);
        return AppContext.open(config);
    }

    private static Path temporaryDirectory() {
        try {
            return Files.createTempDirectory("face-security-demo");
        } catch (IOException e) {
            throw new CliException("could not create a temporary workspace: " + e.getMessage(), e);
        }
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : "..." + value.substring(value.length() - (max - 3));
    }

    private void printHelp(Console out) {
        out.title("Face Security Suite " + version());
        out.println("  Offline face recognition, attendance tracking, a signature/heuristic antivirus");
        out.println("  engine and a face-gated access guard. No cloud, no native dependencies.");
        out.section("Usage");
        out.println("  face-security-suite <command> [options]");
        out.section("Commands");
        out.println("  demo                      run every feature end to end on synthetic data");
        out.println("  enroll --name N --image P [--role R]");
        out.println("  identify --image P [--top N]");
        out.println("  people                    list enrolled identities");
        out.println("  attendance [--date D | --recent N]");
        out.println("  export --format csv|pdf --out F [--date D]");
        out.println("  scan <file...> [--quarantine] [--signatures F]");
        out.println("  history [--limit N]       recent scan history");
        out.println("  signatures                list loaded threat signatures");
        out.println("  protect --label L --path P --person-id ID [--kind FILE|FOLDER|APPLICATION]");
        out.println("  access (--entity-id ID | --path P) --image P");
        out.println("  protected                 list protected resources");
        out.println("  revoke --entity-id ID");
        out.println("  info                      show configuration and stored state");
        out.section("Global options");
        out.println("  --data-dir DIR            override the data directory ($FSS_HOME)");
        out.println("  --no-color                disable ANSI colour");
        out.println("  --help                    show this help");
        out.section("Launch the desktop UI");
        out.println("  mvn javafx:run");
        out.println();
    }

    private static String version() {
        String implementation = Main.class.getPackage().getImplementationVersion();
        return implementation == null ? "(dev)" : implementation;
    }
}
