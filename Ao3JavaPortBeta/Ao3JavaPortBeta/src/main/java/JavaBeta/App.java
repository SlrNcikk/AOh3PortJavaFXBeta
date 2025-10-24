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

        // Use FXMLLoader instance to get the controller
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        Ao3Controller controller = loader.getController(); // Get the controller instance

        Scene scene = new Scene(root, 800, 600); // Or your preferred size

        // --- Easter Egg Logic ---
        // Define the key combination (Ctrl + W)
        KeyCombination ctrlW = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);

        // Add an event handler to the scene
        scene.setOnKeyPressed(event -> {
            if (ctrlW.match(event)) {
                System.out.println("DEBUG: Ctrl+W pressed!"); // For testing
                controller.showCredits(); // Call the method in the controller
                event.consume(); // Prevent further processing of the key event
            }
        });
        // --- End Easter Egg Logic ---

        stage.setTitle("Ao3JavaFXPortBeta");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}