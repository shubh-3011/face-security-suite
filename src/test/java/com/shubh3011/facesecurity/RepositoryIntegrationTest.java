package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.db.Database;
import com.shubh3011.facesecurity.db.PersonRepository;
import com.shubh3011.facesecurity.db.ScanHistoryRepository;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.model.ScanRecord;
import com.shubh3011.facesecurity.model.ScanResult;
import com.shubh3011.facesecurity.model.Verdict;
import java.time.Instant;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryIntegrationTest {

    @Test
    void personRoundTrip() {
        Database database = Database.inMemory();
        PersonRepository repository = new PersonRepository(database);
        Person saved = repository.save(Person.create("Ada", "STAFF", Instant.parse("2026-01-01T00:00:00Z")));
        assertTrue(saved.id() > 0);
        assertEquals("Ada", repository.findById(saved.id()).orElseThrow().name());
        assertEquals(1, repository.count());
        assertTrue(repository.delete(saved.id()));
        assertEquals(0, repository.count());
    }

    @Test
    void scanHistoryPersists() {
        Database database = Database.inMemory();
        ScanHistoryRepository repository = new ScanHistoryRepository(database);
        ScanResult result = new ScanResult(Path.of("x.txt"), Verdict.MALICIOUS, "abc", 4.2, 10,
                java.util.List.of(), 5, "test");
        ScanRecord record = repository.save(ScanRecord.of(result, Instant.parse("2026-01-01T00:00:00Z")));
        assertTrue(record.id() > 0);
        assertEquals(1, repository.countByVerdict(Verdict.MALICIOUS));
        assertEquals(1, repository.recent(10).size());
    }
}
