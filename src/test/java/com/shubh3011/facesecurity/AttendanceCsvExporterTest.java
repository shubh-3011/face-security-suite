package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.attendance.AttendanceCsvExporter;
import com.shubh3011.facesecurity.model.AttendanceRecord;
import com.shubh3011.facesecurity.model.AttendanceStatus;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceCsvExporterTest {

    @Test
    void csvContainsHeaderAndRows() {
        ZoneId zone = ZoneId.of("UTC");
        AttendanceRecord record = new AttendanceRecord(1L, 2L, "Ada, \"The\" Countess", Instant.parse("2026-10-17T08:50:00Z"),
                AttendanceStatus.PRESENT, 0.95, "FACE");
        String csv = AttendanceCsvExporter.toCsv(java.util.List.of(record), zone);
        assertTrue(csv.startsWith("id,person_id,person_name,occurred_at,day,status,confidence,source"));
        assertTrue(csv.contains("\"Ada, \"\"The\"\" Countess\""));
        assertTrue(csv.contains("2026-10-17"));
    }
}
