package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.model.ScanResult;
import com.shubh3011.facesecurity.model.Threat;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

final class AntivirusView {

    private final BorderPane root = new BorderPane();
    private final TextArea resultArea = new TextArea();

    AntivirusView(AppContext context) {
        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        body.getChildren().add(Ui.title("Antivirus"));

        Button scanButton = Ui.primary("Scan a file");
        scanButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            var file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            ScanResult result = context.fileScanner().scan(file.toPath());
            context.recordScan(result);
            StringBuilder builder = new StringBuilder();
            builder.append("File:      ").append(result.fileName()).append("\n");
            builder.append("Verdict:   ").append(result.verdict()).append("\n");
            builder.append("SHA-256:   ").append(result.sha256()).append("\n");
            builder.append("Entropy:   ").append(String.format("%.2f", result.entropy())).append(" bits/byte\n");
            builder.append("Size:      ").append(result.sizeBytes()).append(" bytes\n");
            builder.append("Duration:  ").append(result.durationMillis()).append(" ms\n");
            if (!result.threats().isEmpty()) {
                builder.append("\nDetections:\n");
                for (Threat threat : result.threats()) {
                    builder.append("  ").append(threat.severity()).append("  ").append(threat.name())
                            .append("  ").append(threat.evidence()).append("\n");
                }
            }
            if (!result.detail().isBlank()) {
                builder.append("\nDetail:    ").append(result.detail()).append("\n");
            }
            resultArea.setText(builder.toString());

            if (result.verdict().isThreat()) {
                Button quarantineButton = Ui.secondary("Quarantine");
                quarantineButton.setOnAction(qe -> {
                    try {
                        var record = context.quarantineService().quarantine(result.file(), result);
                        resultArea.appendText("\nQuarantined as " + record.id() + "\n");
                    } catch (Exception ex) {
                        resultArea.appendText("\nQuarantine failed: " + ex.getMessage() + "\n");
                    }
                });
                if (body.getChildren().size() > 3) {
                    body.getChildren().remove(body.getChildren().size() - 1);
                }
                body.getChildren().add(quarantineButton);
            }
        });

        resultArea.setPrefRowCount(18);
        resultArea.setEditable(false);
        body.getChildren().addAll(scanButton, Ui.card("Result", resultArea));
        root.setCenter(body);
    }

    javafx.scene.Parent root() {
        return root;
    }
}
