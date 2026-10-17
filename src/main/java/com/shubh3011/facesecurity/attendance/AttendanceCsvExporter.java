package com.shubh3011.facesecurity.attendance;

import com.shubh3011.facesecurity.model.AttendanceRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** RFC-4180 style CSV export of attendance records. */
public final class AttendanceCsvExporter {

    public static final String HEADER =
            "id,person_id,person_name,occurred_at,day,status,confidence,source";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));

    private AttendanceCsvExporter() {
    }

    public static String toCsv(List<AttendanceRecord> records, ZoneId zone) {
        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        for (AttendanceRecord record : records) {
            builder.append(record.id()).append(',')
                    .append(record.personId()).append(',')
                    .append(escape(record.personName())).append(',')
                    .append(record.occurredAt().atZone(zone).format(TIMESTAMP)).append(',')
                    .append(record.day(zone)).append(',')
                    .append(record.status().name()).append(',')
                    .append(String.format(Locale.ROOT, "%.3f", record.confidence())).append(',')
                    .append(record.source()).append('\n');
        }
        return builder.toString();
    }

    public static void write(Path file, List<AttendanceRecord> records, ZoneId zone) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, toCsv(records, zone), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
