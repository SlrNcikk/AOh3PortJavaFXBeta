package JavaBeta;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ao3Controller {

    // --- FXML UI Elements ---
    @FXML private TextField anyField, titleField, authorField, tagsField;
    @FXML private Button searchButton, refreshLibraryButton;
    @FXML private Button clearButton; // Variable for the Clear button added
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ListView<Work> resultsListView;
    @FXML private TabPane mainTabPane;
    @FXML private ListView<File> libraryListView;

    private Path libraryPath;

    @FXML private ToggleButton themeToggleButton;

    private String darkThemePath;

    @FXML
    protected void onThemeToggle() {
        Scene scene = mainTabPane.getScene(); // Get the scene from any known control
        if (scene == null) {
            System.err.println("Error: Could not get scene to toggle theme.");
            return;
        }

        // Initialize the path if it's not already set
        if (darkThemePath == null) {
            URL stylesheetUrl = getClass().getResource("/JavaBeta/styles.css"); // Or styles.css
            if (stylesheetUrl != null) {
                darkThemePath = stylesheetUrl.toExternalForm();
            } else {
                showError("Could not find the theme stylesheet!");
                themeToggleButton.setDisable(true); // Disable button if CSS is missing
                return;
            }
        }

        // Check if the button is selected (meaning dark mode should be ON)
        if (themeToggleButton.isSelected()) {
            // Add the dark theme stylesheet
            scene.getStylesheets().add(darkThemePath);
            themeToggleButton.setText("Light Mode"); // Update button text
            System.out.println("DEBUG: Dark theme applied.");
        } else {
            // Remove the dark theme stylesheet
            scene.getStylesheets().remove(darkThemePath);
            themeToggleButton.setText("Dark Mode"); // Update button text
            System.out.println("DEBUG: Dark theme removed.");
        }
    }

    @FXML
    public void initialize() {
        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) Files.createDirectories(libraryPath);
        } catch (IOException e) {
            showError("Could not create library directory: " + e.getMessage());
        }

        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, work) -> {
            if (work != null) loadAndShowStory(work);
        });

        libraryListView.getSelectionModel().selectedItemProperty().addListener((obs, old, file) -> {
            if (file != null) loadStoryFromLibrary(file);
        });

        libraryListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                setText(empty || file == null ? null : file.getName().replaceFirst("\\.txt$", ""));
            }
        });

        populateLibraryListView();
    }

    @FXML
    protected void onSearchButtonClick() {
        String query = buildSearchQuery();
        if (query.isEmpty()) {
            showInfo("Please fill in at least one search field.");
            return;
        }

        loadingIndicator.setVisible(true);
        searchButton.setDisable(true);
        resultsListView.getItems().clear();

        Task<List<Work>> fetchWorksTask = createFetchWorksTask(query);
        fetchWorksTask.setOnSucceeded(e -> resultsListView.getItems().setAll(fetchWorksTask.getValue()));
        fetchWorksTask.setOnFailed(e -> showError("Failed to fetch works. Check connection."));
        fetchWorksTask.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            loadingIndicator.setVisible(isRunning);
            searchButton.setDisable(isRunning);
        });

        new Thread(fetchWorksTask).start();
    }

    /**
     * Clears the text from all search input fields.
     */
    @FXML
    protected void onClearButtonClick() {
        // Clear text fields
        anyField.clear();
        titleField.clear();
        authorField.clear();
        tagsField.clear();

        // Clear the search results list
        resultsListView.getItems().clear();
    }


    private void loadAndShowStory(Work work) {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Loading Story");
        loadingAlert.setHeaderText("Please wait, fetching story content...");
        loadingAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        loadingAlert.show();

        Task<String> task = createFetchStoryTask(work);
        task.setOnSucceeded(e -> {
            loadingAlert.close();
            // Pass the Work object AND content
            launchReadingWindow(work, task.getValue());
        });
        task.setOnFailed(e -> {
            loadingAlert.close();
            showError("Failed to load story content.");
        });
        new Thread(task).start();
    }

    private void loadStoryFromLibrary(File file) {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String title = file.getName().replaceFirst("\\.txt$", "");
            // Pass title, content, and the 'isOffline' flag
            launchReadingWindow(title, content, true);
        } catch (IOException e) {
            showError("Could not read story file: " + e.getMessage());
        }
    }

    private void launchReadingWindow(Work work, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReadingView.fxml"));
            Parent root = loader.load();
            ReadingController readingController = loader.getController();

            Stage readerStage = new Stage();
            Scene scene = new Scene(root);
            URL stylesheetUrl = getClass().getResource("/JavaBeta/styles.css");
            if (stylesheetUrl != null) scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            readerStage.setScene(scene);

            readerStage.setTitle(work.getTitle());
            readingController.loadStory(work, content); // Call the correct loadStory method

            readerStage.initModality(Modality.NONE);
            readerStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the reading window: " + e.getMessage());
        }
    }

    private void launchReadingWindow(String title, String content, boolean isOffline) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReadingView.fxml"));
            Parent root = loader.load();
            ReadingController readingController = loader.getController();

            Stage readerStage = new Stage();
            Scene scene = new Scene(root);
            URL stylesheetUrl = getClass().getResource("/JavaBeta/styles.css");
            if (stylesheetUrl != null) scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            readerStage.setScene(scene);

            readerStage.setTitle(title);
            readingController.loadStory(title, content, isOffline); // Call the correct loadStory method

            readerStage.initModality(Modality.NONE);
            readerStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the reading window: " + e.getMessage());
        }
    }

    @FXML
    protected void onRefreshLibraryClick() {
        populateLibraryListView();
    }

    private void populateLibraryListView() {
        libraryListView.getItems().clear();
        try (Stream<Path> stream = Files.list(libraryPath)) {
            List<File> files = stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            libraryListView.getItems().addAll(files);
        } catch (IOException e) {
            showError("Could not read library directory: " + e.getMessage());
        }
    }

    // --- Helper & Utility Methods ---
    private String buildSearchQuery() {
        StringJoiner sj = new StringJoiner(" ");
        addQueryPart(sj, "", anyField.getText(), false);
        addQueryPart(sj, "title:", titleField.getText(), true);
        addQueryPart(sj, "author:", authorField.getText(), true);
        addQueryPart(sj, "", tagsField.getText(), false);
        return sj.toString();
    }
    private void addQueryPart(StringJoiner sj, String p, String v, boolean q) {
        String val = v.trim();
        if (!val.isEmpty()) sj.add(q && val.contains(" ") ? p + "\"" + val + "\"" : p + val);
    }
    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void showInfo(String message) { showInfo(null, message); }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- Web Scraping Tasks ---
    private Task<List<Work>> createFetchWorksTask(String query) {
        return new Task<>() {
            @Override
            protected List<Work> call() throws Exception {
                List<Work> worksList = new ArrayList<>();
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
                String url = "https://archiveofourown.org/works/search?work_search[query]=" + encodedQuery;
                System.out.println("DEBUG: Connecting to URL -> " + url);
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent(userAgent)
                            .referrer("https://www.google.com")
                            .get();
                    Elements workElements = doc.select("li.work.blurb");
                    System.out.println("DEBUG: Connection successful. Found " + workElements.size() + " works on the page.");
                    for (Element workEl : workElements) {
                        Element titleEl = workEl.selectFirst("h4.heading a[href^='/works/']");
                        Element authorEl = workEl.selectFirst("a[rel=author]");
                        if (titleEl != null && authorEl != null) {
                            String title = titleEl.text();
                            String workUrl = "https://archiveofourown.org" + titleEl.attr("href");
                            String author = authorEl.text();
                            worksList.add(new Work(title, author, workUrl));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Scraping failed!");
                    throw e;
                }
                return worksList;
            }
        };
    }
    private Task<String> createFetchStoryTask(Work work) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
                // Fetch the full work page
                Document doc = Jsoup.connect(work.getUrl() + "?view_full_work=true")
                        .userAgent(userAgent)
                        .get();
                // Select the main story content div
                Element workskin = doc.selectFirst("#workskin");
                if (workskin == null) {
                    return "<html><body>Could not find story content. It might be a restricted work.</body></html>";
                }
                // Return the inner HTML of the workskin div
                return workskin.html();
            }
        };
    }

    public void showCredits() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Credits");
        alert.setHeaderText("AO3 Reader Application");
        alert.setContentText("Developed by: [Your Name/Handle Here]\n" +
                "Using JavaFX and Jsoup.\n\n" +
                "Thanks for using the app!");
        alert.showAndWait();
    }
}