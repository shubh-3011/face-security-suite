package com.shubh3011.facesecurity.gui;

import com.shubh3011.facesecurity.app.AppConfig;
import com.shubh3011.facesecurity.app.AppContext;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * Desktop entry point.
 *
 * <p>Launched with {@code mvn javafx:run}. The context is opened once and closed
 * when the window is closed.
 */
public class MainMenu extends Application {

    private AppContext context;

    @Override
    public void start(Stage stage) {
        context = AppContext.open(AppConfig.defaults());
        stage.setOnCloseRequest(event -> {
            if (context != null) {
                context.close();
            }
        });

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Dashboard", new DashboardView(context).root()));
        tabs.getTabs().add(new Tab("Face Registry", new FaceRegistryView(context).root()));
        tabs.getTabs().add(new Tab("Attendance", new AttendanceView(context).root()));
        tabs.getTabs().add(new Tab("Antivirus", new AntivirusView(context).root()));
        tabs.getTabs().add(new Tab("Trusted Access", new GuardView(context).root()));

        Scene scene = new Scene(tabs, 1120, 740);
        java.net.URL stylesheet = getClass().getResource("/gui/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        stage.setTitle("Face Security Suite");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
