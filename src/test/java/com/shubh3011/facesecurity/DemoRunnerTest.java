package com.shubh3011.facesecurity;

import com.shubh3011.facesecurity.app.AppConfig;
import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.app.DemoRunner;
import com.shubh3011.facesecurity.model.GuardDecision;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void runsEndToEnd() throws Exception {
        AppConfig config = AppConfig.of(tempDir.resolve(".fss"));
        try (AppContext context = AppContext.open(config)) {
            DemoRunner.Outcome outcome = DemoRunner.run(context, tempDir.resolve("demo"));
            assertEquals(3, outcome.people().size());
            assertEquals(3, outcome.checkIns().size());
            assertEquals(3, outcome.identifications().size());
            for (var match : outcome.identifications()) {
                assertTrue(match != null && match.accepted(), "expected all identifications to be accepted");
            }
            assertTrue(outcome.maliciousCount() >= 1);
            assertTrue(outcome.allowed().allowed());
            assertTrue(!outcome.denied().allowed());
            assertTrue(Files.exists(outcome.workspace()));
        }
    }
}
