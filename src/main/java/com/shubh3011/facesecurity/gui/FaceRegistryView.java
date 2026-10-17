package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.face.ImageLoader;
import com.shubh3011.facesecurity.model.FaceMatch;
import com.shubh3011.facesecurity.model.Person;
import javafx.collections.FXCollections;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.awt.image.BufferedImage;

final class FaceRegistryView {

    private final BorderPane root = new BorderPane();
    private final TableView<Person> table = new TableView<>();
    private final TextField nameField = new TextField();
    private final TextField roleField = new TextField("STAFF");

    FaceRegistryView(AppContext context) {
        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        body.getChildren().add(Ui.title("Face Registry"));

        TableColumn<Person, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Person, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Person, String> roleColumn = new TableColumn<>("Role");
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        table.getColumns().addAll(idColumn, nameColumn, roleColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        refreshTable(context);

        HBox enrollRow = Ui.row(Ui.body("Name:"), nameField, Ui.body("Role:"), roleField);
        Button enrollButton = Ui.primary("Enroll from image");
        enrollButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a face image");
            var file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                BufferedImage image = ImageLoader.load(file.toPath());
                context.faceEngine().enroll(nameField.getText().trim(), roleField.getText().trim(), image);
                refreshTable(context);
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        HBox identifyRow = Ui.row();
        Button identifyButton = Ui.secondary("Identify image");
        identifyButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a probe image");
            var file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                BufferedImage image = ImageLoader.load(file.toPath());
                List<FaceMatch> candidates = context.faceEngine().candidates(image, 5);
                if (candidates.isEmpty()) {
                    showError("Nobody is enrolled yet.");
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (FaceMatch match : candidates) {
                    builder.append(match.personName()).append("  ").append(match.confidencePercent())
                            .append(match.accepted() ? "  (accepted)" : "").append("\n");
                }
                showInfo(builder.toString());
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
        identifyRow.getChildren().add(identifyButton);

        body.getChildren().addAll(enrollRow, enrollButton, identifyRow, table);
        root.setCenter(body);
    }

    private void refreshTable(AppContext context) {
        table.setItems(FXCollections.observableArrayList(context.personRepository().findAll()));
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    javafx.scene.Parent root() {
        return root;
    }
}
