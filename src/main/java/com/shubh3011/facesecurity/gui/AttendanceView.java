package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.attendance.AttendanceCsvExporter;
import com.shubh3011.facesecurity.attendance.UnknownFaceException;
import com.shubh3011.facesecurity.face.ImageLoader;
import com.shubh3011.facesecurity.model.AttendanceRecord;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;

final class AttendanceView {

    private final BorderPane root = new BorderPane();
    private final TableView<AttendanceRecord> table = new TableView<>();

    AttendanceView(AppContext context) {
        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        body.getChildren().add(Ui.title("Attendance"));

        TableColumn<AttendanceRecord, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<AttendanceRecord, String> nameColumn = new TableColumn<>("Person");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("personName"));
        TableColumn<AttendanceRecord, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().status().label()));
        TableColumn<AttendanceRecord, Double> confidenceColumn = new TableColumn<>("Confidence");
        confidenceColumn.setCellValueFactory(new PropertyValueFactory<>("confidence"));
        TableColumn<AttendanceRecord, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        table.getColumns().addAll(idColumn, nameColumn, statusColumn, confidenceColumn, sourceColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        HBox controls = Ui.row();
        Button refresh = Ui.secondary("Refresh");
        refresh.setOnAction(event -> refreshTable(context));
        Button checkIn = Ui.primary("Check in from image");
        checkIn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            var file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                BufferedImage image = ImageLoader.load(file.toPath());
                context.attendanceService().markByFace(image);
                refreshTable(context);
            } catch (UnknownFaceException e) {
                showError("Unknown face: " + e.getMessage());
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        Button export = Ui.secondary("Export CSV");
        export.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("attendance.csv");
            var file = chooser.showSaveDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                AttendanceCsvExporter.write(file.toPath(),
                        context.attendanceService().forDate(LocalDate.now(context.zone())),
                        context.zone());
                showInfo("Wrote " + file.toPath());
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        controls.getChildren().addAll(refresh, checkIn, export);

        body.getChildren().addAll(controls, table);
        root.setCenter(body);
        refreshTable(context);
    }

    private void refreshTable(AppContext context) {
        table.setItems(FXCollections.observableArrayList(
                context.attendanceService().forDate(LocalDate.now(context.zone()))));
    }

    private void showError(String message) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, message).showAndWait();
    }

    private void showInfo(String message) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, message).showAndWait();
    }

    javafx.scene.Parent root() {
        return root;
    }
}
