package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.DemoRunner;
import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.model.FaceMatch;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Landing screen: live counters plus a one-click end-to-end demo.
 */
final class DashboardView {

    private final BorderPane root = new BorderPane();
    private final TextArea log = new TextArea();

    DashboardView(AppContext context) {
        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        body.getChildren().add(Ui.title("Dashboard"));
        body.getChildren().add(Ui.subtitle("Privacy-first face recognition, attendance, antivirus and a biometric access guard."));

        Label counters = Ui.body("");
        refreshCounters(context, counters);
        body.getChildren().add(Ui.card("Current state", counters));

        javafx.scene.control.Button runDemo = Ui.primary("Run end-to-end demo");
        runDemo.setOnAction(event -> {
            try {
                var outcome = DemoRunner.run(context, java.nio.file.Files.createTempDirectory("fss-gui-demo"));
                StringBuilder builder = new StringBuilder();
                builder.append("Identified: ");
                for (FaceMatch match : outcome.identifications()) {
                    builder.append(match == null ? "?" : match.personName()).append(" (")
                            .append(match == null ? "-" : match.confidencePercent()).append(")  ");
                }
                builder.append("\nAttendance: ").append(outcome.stats().total())
                        .append(" check-ins, on-time rate ").append(outcome.stats().onTimeRatePercent()).append("%\n");
                builder.append("Scans: ").append(outcome.scans().size()).append(" (")
                        .append(outcome.maliciousCount()).append(" malicious)\n");
                builder.append("Guard: ").append(outcome.allowed().reason()).append(" / ")
                        .append(outcome.denied().reason()).append("\n");
                log.setText(builder.toString());
                refreshCounters(context, counters);
            } catch (Exception e) {
                log.setText("Demo failed: " + e.getMessage());
            }
        });
        body.getChildren().add(runDemo);

        log.setPrefRowCount(10);
        log.setEditable(false);
        body.getChildren().add(Ui.card("Demo output", log));
        root.setCenter(body);
    }

    private static void refreshCounters(AppContext context, Label label) {
        label.setText("People: " + context.faceEngine().enrolledPeople()
                + "   |   Face samples: " + context.faceEngine().enrolledSamples()
                + "   |   Check-ins: " + context.attendanceRepository().count()
                + "   |   Scans: " + context.scanHistoryRepository().count()
                + "   |   Protected: " + context.guardian().count());
    }

    javafx.scene.Parent root() {
        return root;
    }
}
