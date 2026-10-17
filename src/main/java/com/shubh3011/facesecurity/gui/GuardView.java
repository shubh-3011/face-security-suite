package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.AppContext;
import com.shubh3011.facesecurity.face.ImageLoader;
import com.shubh3011.facesecurity.model.EntityKind;
import com.shubh3011.facesecurity.model.GuardDecision;
import com.shubh3011.facesecurity.model.Person;
import com.shubh3011.facesecurity.model.TrustedEntity;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

final class GuardView {

    private final BorderPane root = new BorderPane();
    private final TextArea output = new TextArea();

    GuardView(AppContext context) {
        VBox body = new VBox(12);
        body.setPadding(new Insets(16));
        body.getChildren().add(Ui.title("Trusted Face Protection"));

        TextField labelField = new TextField("Quarterly vault");
        TextField pathField = new TextField();
        ComboBox<Person> ownerBox = new ComboBox<>();
        ownerBox.setItems(javafx.collections.FXCollections.observableArrayList(context.personRepository().findAll()));
        ownerBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Person p) {
                return p == null ? "" : p.name() + " (#" + p.id() + ")";
            }

            public Person fromString(String s) {
                return null;
            }
        });
        ComboBox<EntityKind> kindBox = new ComboBox<>();
        kindBox.getItems().addAll(EntityKind.values());
        kindBox.setValue(EntityKind.FILE);

        Button protectButton = Ui.primary("Protect resource");
        protectButton.setOnAction(event -> {
            Person owner = ownerBox.getValue();
            if (owner == null) {
                output.setText("Select an owner first.");
                return;
            }
            try {
                TrustedEntity entity = context.guardian().protect(
                        labelField.getText().trim(), kindBox.getValue(),
                        Path.of(pathField.getText().trim()), owner.id());
                output.setText("Protected: " + entity.path() + " (rule #" + entity.id() + ")");
            } catch (Exception e) {
                output.setText("Protect failed: " + e.getMessage());
            }
        });

        Button verifyButton = Ui.secondary("Verify face");
        verifyButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            var file = chooser.showOpenDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            try {
                BufferedImage image = ImageLoader.load(file.toPath());
                String pathText = pathField.getText().trim();
                GuardDecision decision = pathText.isEmpty()
                        ? context.guardian().checkAccess(
                                context.guardian().protectedResources().isEmpty() ? 0L
                                        : context.guardian().protectedResources().get(0).id(), image)
                        : context.guardian().checkAccess(Path.of(pathText), image);
                output.setText(decision.allowed() ? "GRANTED: " + decision.reason()
                        : "DENIED: " + decision.reason());
            } catch (Exception e) {
                output.setText("Verify failed: " + e.getMessage());
            }
        });

        output.setPrefRowCount(8);
        output.setEditable(false);
        body.getChildren().addAll(
                Ui.body("Label:"), labelField,
                Ui.body("Path:"), pathField,
                Ui.body("Owner:"), ownerBox,
                Ui.body("Kind:"), kindBox,
                Ui.row(protectButton, verifyButton),
                Ui.card("Output", output));
        root.setCenter(body);
    }

    javafx.scene.Parent root() {
        return root;
    }
}
