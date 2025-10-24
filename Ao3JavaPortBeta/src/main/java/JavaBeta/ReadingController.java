package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.web.WebView; // Import WebView
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReadingController {

    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private WebView storyWebView; // Correctly declared as WebView
    @FXML private Button downloadButton;

    // Base HTML structure with placeholders for theme class and content
    private final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { margin: 20px; font-family: sans-serif; }
                .default { background-color: white; color: black; }
                .sepia { background-color: #fbf0d9; color: #5b4636; }
                .dark { background-color: #1e1e1e; color: #dcdcdc; }
                /* Basic AO3 styles */
                p { margin-bottom: 1em; line-height: 1.5; }
                h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; }
                hr { border: none; border-top: 1px solid #ccc; margin: 2em 0; }
                em { font-style: italic; }
                strong { font-weight: bold; }
                blockquote { margin-left: 2em; padding-left: 1em; border-left: 3px solid #ccc; }
                a { color: #990000; text-decoration: none; } /* Basic link styling */
                a:hover { text-decoration: underline; }
            </style>
        </head>
        <body class="%s">
            %s
        </body>
        </html>
        """;

    private String storyTitle;
    private String storyAuthor;
    private String rawHtmlContent; // Store the raw scraped HTML
    private boolean isOfflineStory = false;

    @FXML
    public void initialize() {
        themeChoiceBox.getItems().addAll("Default", "Sepia", "Dark Mode");
        themeChoiceBox.setValue("Default");
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTheme, newTheme) -> updateTheme(newTheme)
        );
        downloadButton.setDisable(true);
        // Basic WebView setup if needed (e.g., disable context menu)
        storyWebView.setContextMenuEnabled(false);
    }

    // Load method for ONLINE stories (receives Work object)
    public void loadStory(Work work, String htmlContent) {
        this.storyTitle = work.getTitle();
        this.storyAuthor = work.getAuthor();
        this.rawHtmlContent = htmlContent; // Store the raw HTML
        this.isOfflineStory = false;

        Stage stage = getStage();
        if (stage != null) stage.setTitle(storyTitle);

        updateWebViewContent("default"); // Load content with default theme
        downloadButton.setDisable(false); // Enable download
    }

    // Load method for OFFLINE stories (receives title, file content, flag)
    public void loadStory(String title, String fileContent, boolean isOffline) {
        this.storyTitle = title;
        this.storyAuthor = "Unknown"; // Author info not stored offline
        // Assume file content is raw HTML if it's likely HTML
        if (fileContent != null && (fileContent.trim().toLowerCase().startsWith("<!doctype") || fileContent.trim().startsWith("<p"))) {
            this.rawHtmlContent = fileContent;
        } else if (fileContent != null) {
            // If it looks like plain text, wrap it in basic HTML
            this.rawHtmlContent = "<p>" + fileContent.replace("\n", "</p><p>") + "</p>";
        } else {
            this.rawHtmlContent = "<html><body>Error: Could not load content.</body></html>";
        }
        this.isOfflineStory = isOffline;

        Stage stage = getStage();
        if (stage != null) stage.setTitle(title);

        updateWebViewContent("default"); // Load content with default theme
        downloadButton.setDisable(true); // Disable download for offline stories
    }

    @FXML
    protected void onDownloadButtonClick() {
        if (isOfflineStory || storyTitle == null || storyAuthor == null || rawHtmlContent == null || rawHtmlContent.isEmpty()) {
            showError("Cannot download this story (it might be offline or not fully loaded).");
            return;
        }

        Path libraryPath = getLibraryPath();
        if (libraryPath == null) return; // Error shown in helper

        // Save as .html
        String safeTitle = storyTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = safeTitle + " - " + storyAuthor + ".html"; // Save as .html
        Path filePath = libraryPath.resolve(fileName);

        try {
            // Write the raw HTML content to the file
            Files.writeString(filePath, rawHtmlContent, StandardCharsets.UTF_8);
            showInfo("Download Complete!", "Saved '" + fileName + "' as HTML to your offline library.");
            // Consider disabling button after save if desired: downloadButton.setDisable(true);
        } catch (IOException e) {
            showError("Could not save file: " + e.getMessage());
        }
    }

    private void updateTheme(String themeName) {
        String themeClass = "default"; // Default CSS class name in HTML_TEMPLATE
        if ("Sepia".equals(themeName)) {
            themeClass = "sepia";
        } else if ("Dark Mode".equals(themeName)) {
            themeClass = "dark";
        }
        updateWebViewContent(themeClass); // Reload content with the new theme class
    }

    /** Helper to load the stored HTML into WebView using the HTML_TEMPLATE */
    private void updateWebViewContent(String themeClass) {
        if (rawHtmlContent != null && storyWebView != null && storyWebView.getEngine() != null) {
            // Format the template with the chosen theme class and the raw story HTML
            String styledHtml = String.format(HTML_TEMPLATE, themeClass, rawHtmlContent);
            // Load the complete HTML string into the WebView
            storyWebView.getEngine().loadContent(styledHtml);
        } else if (storyWebView != null && storyWebView.getEngine() != null){
            storyWebView.getEngine().loadContent("<html><body>Error: No content available to display.</body></html>");
        }
    }

    /** Helper to safely get the library path */
    private Path getLibraryPath() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException e) {
            showError("Could not access library directory: " + e.getMessage());
            return null;
        }
    }

    /** Helper to safely get the stage */
    private Stage getStage() {
        if (storyWebView != null && storyWebView.getScene() != null) {
            return (Stage) storyWebView.getScene().getWindow();
        }
        return null;
    }

    // --- Alert Helpers ---
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