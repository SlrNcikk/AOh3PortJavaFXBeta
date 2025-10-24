package JavaBeta; // Make sure this matches your package

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlLocation = getClass().getResource("/JavaBeta/main-view.fxml");
        if (fxmlLocation == null) {
            System.err.println("CRITICAL ERROR: Cannot find FXML file. Path: /JavaBeta/main-view.fxml");
            return;
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        Scene scene = new Scene(root, 800, 600);

        // --- APPLY STYLESHEET ---
        // Choose the correct path based on Option A or B from Step 1
        URL stylesheetUrl = getClass().getResource("/JavaBeta/dark-theme.css"); // Or "/JavaBeta/styles.css"
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            System.out.println("DEBUG: Dark theme applied."); // For confirmation
        } else {
            System.err.println("Warning: Could not find the stylesheet.");
        }
        // --- END APPLY STYLESHEET ---

        stage.setTitle("Ao3JavaFXPortBeta");
        stage.setScene(scene);
        stage.show();

        // --- End Easter Egg Logic ---

        stage.setTitle("Ao3JavaFXPortBeta");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}