package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReadingController {

    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private TextArea storyTextArea;
    @FXML private Button downloadButton; // Added button variable

    private final String[] themeClasses = {"text-area-sepia", "text-area-dark"};

    // Variables to store story details for downloading
    private String storyTitle;
    private String storyAuthor; // We'll get this from the Work object later
    private boolean isOfflineStory = false; // Flag to disable download for library stories

    @FXML
    public void initialize() {
        themeChoiceBox.getItems().addAll("Default", "Sepia", "Dark Mode");
        themeChoiceBox.setValue("Default");
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTheme, newTheme) -> updateTheme(newTheme)
        );

        // Initially disable download button
        downloadButton.setDisable(true);
    }

    /**
     * Updated method to receive Work object for online stories.
     */
    public void loadStory(Work work, String content) {
        this.storyTitle = work.getTitle();
        this.storyAuthor = work.getAuthor();
        this.isOfflineStory = false; // This is an online story

        Stage stage = (Stage) storyTextArea.getScene().getWindow();
        if (stage != null) stage.setTitle(storyTitle);

        storyTextArea.setText(content);
        downloadButton.setDisable(false); // Enable download for online stories
    }

    /**
     * Updated method for offline stories (disables download).
     */
    public void loadStory(String title, String content, boolean isOffline) {
        this.storyTitle = title;
        this.storyAuthor = "Unknown"; // Author info isn't stored in the filename
        this.isOfflineStory = isOffline;

        Stage stage = (Stage) storyTextArea.getScene().getWindow();
        if (stage != null) stage.setTitle(storyTitle);

        storyTextArea.setText(content);
        downloadButton.setDisable(isOfflineStory); // Disable download for offline stories
    }

    @FXML
    protected void onDownloadButtonClick() {
        if (isOfflineStory || storyTitle == null || storyAuthor == null || storyTextArea.getText().isEmpty()) {
            showError("Cannot download this story.");
            return;
        }

        // Define the library path (same logic as in Ao3Controller)
        Path libraryPath;
        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) {
                Files.createDirectories(libraryPath);
            }
        } catch (IOException e) {
            showError("Could not access library directory: " + e.getMessage());
            return;
        }

        // Create filename (same logic as before)
        String safeTitle = storyTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = safeTitle + " - " + storyAuthor + ".txt";
        Path filePath = libraryPath.resolve(fileName);

        try {
            Files.writeString(filePath, storyTextArea.getText(), StandardCharsets.UTF_8);
            showInfo("Download Complete!", "Saved '" + fileName + "' to your offline library.");
            // Optionally, disable the button after successful download
            // downloadButton.setDisable(true);
        } catch (IOException e) {
            showError("Could not save file: " + e.getMessage());
        }
    }

    private void updateTheme(String themeName) {
        storyTextArea.getStyleClass().removeAll(themeClasses);
        if ("Sepia".equals(themeName)) {
            storyTextArea.getStyleClass().add("text-area-sepia");
        } else if ("Dark Mode".equals(themeName)) {
            storyTextArea.getStyleClass().add("text-area-dark");
        }
    }

    // --- Helper methods for showing alerts ---
    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}