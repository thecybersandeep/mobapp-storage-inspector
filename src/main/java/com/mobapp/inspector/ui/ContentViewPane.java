package com.mobapp.inspector.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

public class ContentViewPane extends BorderPane {
    
    private static final int MAX_TEXT_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_PRINTABLE_STRINGS_LENGTH = 100000;
    
    private Label fileInfoLabel;
    private TabPane contentTabPane;
    
    public ContentViewPane() {
        initializeUI();
    }
    
    private void initializeUI() {
        setPadding(new Insets(10));
        
        fileInfoLabel = new Label("No file selected");
        fileInfoLabel.getStyleClass().add("file-info-label");
        
        contentTabPane = new TabPane();
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        setTop(fileInfoLabel);
        setCenter(contentTabPane);
        
        showWelcomeMessage();
    }
    
    private void showWelcomeMessage() {
        VBox welcomeBox = new VBox(30);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setPadding(new Insets(50));
        welcomeBox.setMaxWidth(600);
        
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        
        Label appTitle = new Label("MobApp-Storage Inspector");
        appTitle.getStyleClass().add("welcome-app-title");
        
        Label tagline = new Label("A tool for inspecting and analyzing mobile application storage files");
        tagline.getStyleClass().add("welcome-tagline");
        
        titleBox.getChildren().addAll(appTitle, tagline);
        
        HBox hintBox = new HBox(15);
        hintBox.setAlignment(Pos.CENTER);
        hintBox.setPadding(new Insets(20));
        hintBox.getStyleClass().add("welcome-hint-box");
        
        Label tipIcon = new Label("💡");
        tipIcon.getStyleClass().add("welcome-tip-icon");
        
        Label tipText = new Label("Select a file from the tree view on the left to inspect its contents");
        tipText.getStyleClass().add("welcome-tip-text");
        tipText.setWrapText(true);
        
        hintBox.getChildren().addAll(tipIcon, tipText);
        
        welcomeBox.getChildren().addAll(titleBox, hintBox);
        
        Tab welcomeTab = new Tab("Welcome");
        welcomeTab.setContent(welcomeBox);
        
        contentTabPane.getTabs().add(welcomeTab);
    }
    
    public void displayFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            fileInfoLabel.setText("Invalid file");
            contentTabPane.getTabs().clear();
            return;
        }
        
        fileInfoLabel.setText(file.getAbsolutePath() + " (" + formatFileSize(file.length()) + ")");
        contentTabPane.getTabs().clear();
        
        String fileName = file.getName().toLowerCase();
        
        if (isPlistFile(fileName)) {
            displayPlistFile(file);
        } else if (isTextFile(fileName)) {
            displayTextFile(file);
        } else if (isImageFile(fileName)) {
            displayImageFile(file);
        } else if (isDatabaseFile(fileName)) {
            displayDatabaseFile(file);
        } else {
            displayBinaryFile(file);
        }
    }
    
    private void displayTextFile(File file) {
        try {
            if (file.length() > MAX_TEXT_FILE_SIZE) {
                showErrorMessage("File is too large to display (max size: " + formatFileSize(MAX_TEXT_FILE_SIZE) + ")");
                return;
            }
            
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            
            TextArea textArea = new TextArea(content);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.getStyleClass().add("text-file-view");
            
            HBox searchBox = new HBox(5);
            searchBox.setPadding(new Insets(5));
            searchBox.setAlignment(Pos.CENTER_LEFT);
            
            TextField searchField = new TextField();
            searchField.setPromptText("Enter search term...");
            HBox.setHgrow(searchField, Priority.ALWAYS);
            
            searchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    searchInTextArea(textArea, searchField.getText());
                }
            });
            
            Button searchButton = new Button("Find");
            searchButton.setOnAction(e -> searchInTextArea(textArea, searchField.getText()));
            
            Button clearButton = new Button("Clear");
            clearButton.setOnAction(e -> {
                searchField.clear();
                textArea.deselect();
                textArea.setStyle("");
            });
            
            searchBox.getChildren().addAll(searchField, searchButton, clearButton);
            
            VBox contentBox = new VBox(5);
            contentBox.getChildren().addAll(searchBox, textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            
            Tab tab = new Tab("Text View");
            tab.setContent(contentBox);
            
            contentTabPane.getTabs().add(tab);
            
        } catch (IOException e) {
            showErrorMessage("Error reading file: " + e.getMessage());
        }
    }
    
    private void displayImageFile(File file) {
        try {
            final Image image;
            try (FileInputStream fis = new FileInputStream(file)) {
                image = new Image(fis);
            }
            
            if (image == null || image.isError()) {
                throw new Exception("Failed to load image" + 
                    (image != null && image.getException() != null ? ": " + image.getException().getMessage() : ""));
            }
            
            final ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
            
            double initialScale = Math.min(1.0, Math.min(800 / image.getWidth(), 600 / image.getHeight()));
            imageView.setFitWidth(image.getWidth() * initialScale);
            imageView.setFitHeight(image.getHeight() * initialScale);
            
            final StackPane imagePane = new StackPane(imageView);
            imagePane.setAlignment(Pos.CENTER);
            imagePane.getStyleClass().add("image-pane");
            
            final ScrollPane scrollPane = new ScrollPane(imagePane);
            scrollPane.setPannable(true);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            
            final Slider zoomSlider = new Slider(0.1, 5.0, 1.0);
            zoomSlider.getStyleClass().add("zoom-slider");
            zoomSlider.setBlockIncrement(0.1);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setShowTickMarks(true);
            zoomSlider.setMajorTickUnit(1.0);
            zoomSlider.setMinorTickCount(4);
            
            final Label zoomLabel = new Label("Zoom: 100%");
            zoomLabel.getStyleClass().add("zoom-label");
            
            Button zoomInButton = new Button("+");
            zoomInButton.getStyleClass().add("zoom-button");
            zoomInButton.setTooltip(new Tooltip("Zoom In"));
            zoomInButton.setOnAction(e -> {
                double newValue = Math.min(zoomSlider.getValue() + 0.1, zoomSlider.getMax());
                zoomSlider.setValue(newValue);
            });
            
            Button zoomOutButton = new Button("-");
            zoomOutButton.getStyleClass().add("zoom-button");
            zoomOutButton.setTooltip(new Tooltip("Zoom Out"));
            zoomOutButton.setOnAction(e -> {
                double newValue = Math.max(zoomSlider.getValue() - 0.1, zoomSlider.getMin());
                zoomSlider.setValue(newValue);
            });
            
            Button resetZoomButton = new Button("100%");
            resetZoomButton.getStyleClass().add("zoom-button");
            resetZoomButton.setTooltip(new Tooltip("Reset Zoom"));
            resetZoomButton.setOnAction(e -> zoomSlider.setValue(1.0));
            
            Button fitToWindowButton = new Button("Fit");
            fitToWindowButton.getStyleClass().add("zoom-button");
            fitToWindowButton.setTooltip(new Tooltip("Fit to Window"));
            fitToWindowButton.setOnAction(e -> {
                double scale = Math.min(scrollPane.getWidth() / image.getWidth(), 
                                       scrollPane.getHeight() / image.getHeight());
                zoomSlider.setValue(scale);
            });
            
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double scale = newVal.doubleValue();
                imageView.setFitWidth(image.getWidth() * scale);
                imageView.setFitHeight(image.getHeight() * scale);
                zoomLabel.setText(String.format("Zoom: %.0f%%", scale * 100));
            });
            
            scrollPane.setOnScroll(e -> {
                if (e.isControlDown()) {
                    double deltaY = e.getDeltaY();
                    double zoomFactor = deltaY > 0 ? 1.1 : 0.9;
                    double currentZoom = zoomSlider.getValue();
                    double newZoom = currentZoom * zoomFactor;
                    newZoom = Math.max(zoomSlider.getMin(), Math.min(zoomSlider.getMax(), newZoom));
                    zoomSlider.setValue(newZoom);
                    e.consume();
                }
            });
            
            imagePane.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    zoomSlider.setValue(1.0);
                }
            });
            
            HBox zoomControls = new HBox(10);
            zoomControls.getStyleClass().add("zoom-controls");
            zoomControls.setAlignment(Pos.CENTER);
            zoomControls.getChildren().addAll(
                zoomOutButton, zoomSlider, zoomInButton, zoomLabel, resetZoomButton, fitToWindowButton
            );
            
            Button copyPathButton = new Button("Copy Path");
            copyPathButton.getStyleClass().add("toolbar-button");
            copyPathButton.setTooltip(new Tooltip("Copy image file path to clipboard"));
            copyPathButton.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(file.getAbsolutePath());
                Clipboard.getSystemClipboard().setContent(content);
                
                Tooltip tooltip = new Tooltip("Path copied to clipboard!");
                tooltip.setAutoHide(true);
                tooltip.show(copyPathButton, 
                             copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMinX(),
                             copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMaxY());
                
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> tooltip.hide());
                delay.play();
            });
            
            zoomControls.getChildren().add(copyPathButton);
            
            VBox imageContainer = new VBox(5);
            imageContainer.getChildren().addAll(zoomControls, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            
            Tab tab = new Tab("Image View");
            tab.setContent(imageContainer);
            
            contentTabPane.getTabs().add(tab);
            
            Tab infoTab = new Tab("Image Info");
            VBox infoBox = new VBox(10);
            infoBox.setPadding(new Insets(10));
            infoBox.getChildren().addAll(
                new Label("File: " + file.getName()),
                new Label("Path: " + file.getAbsolutePath()),
                new Label("Size: " + formatFileSize(file.length())),
                new Label("Width: " + (int)image.getWidth() + " pixels"),
                new Label("Height: " + (int)image.getHeight() + " pixels"),
                new Label("Format: " + getFileExtension(file.getName()).toUpperCase())
            );
            infoTab.setContent(infoBox);
            contentTabPane.getTabs().add(infoTab);
            
        } catch (Exception e) {
            showErrorMessage("Error loading image: " + e.getMessage());
        }
    }
    
    private void displayDatabaseFile(File file) {
        try {
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(100, 100);
            
            VBox loadingBox = new VBox(10);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.getChildren().addAll(
                progressIndicator,
                new Label("Loading database...")
            );
            
            Tab loadingTab = new Tab("Loading...");
            loadingTab.setContent(loadingBox);
            contentTabPane.getTabs().add(loadingTab);
            
            Task<Void> loadTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Connection connection = null;
                    try {
                        String jdbcUrl = "jdbc:sqlite:" + file.toURI().getPath();
                        connection = DriverManager.getConnection(jdbcUrl);
                        
                        try (Statement timeoutStmt = connection.createStatement()) {
                            timeoutStmt.execute("PRAGMA busy_timeout = 30000;");
                            timeoutStmt.execute("PRAGMA journal_mode = WAL;");
                            timeoutStmt.execute("PRAGMA synchronous = NORMAL;");
                            timeoutStmt.execute("PRAGMA temp_store = MEMORY;");
                            timeoutStmt.execute("PRAGMA cache_size = 10000;");
                        }
                        
                        // Get the list of tables
                        final List<String> tableNames = getTableNames(connection);
                        
                        if (tableNames.isEmpty()) {
                            Platform.runLater(() -> {
                                contentTabPane.getTabs().clear();
                                showErrorMessage("No tables found in the database");
                            });
                            return null;
                        }
                        
                        // Create tabs on the JavaFX thread
                        final Connection finalConnection = connection;
                        Platform.runLater(() -> {
                            try {
                                contentTabPane.getTabs().clear();
                                
                                // Create a single tab for table data with a dropdown selector
                                Tab dataTab = new Tab("Table Data");
                                dataTab.setContent(createTableSelectorView(finalConnection, tableNames));
                                contentTabPane.getTabs().add(dataTab);
                                
                                // Create a tab for the database schema
                                Tab schemaTab = new Tab("Schema");
                                schemaTab.setContent(createSchemaView(finalConnection, tableNames));
                                contentTabPane.getTabs().add(schemaTab);
                                
                                // Add a tab with database information
                                Tab infoTab = new Tab("Database Info");
                                infoTab.setContent(createDatabaseInfoView(file, finalConnection, tableNames));
                                contentTabPane.getTabs().add(infoTab);
                                
                            } catch (SQLException e) {
                                showErrorMessage("Error loading database content: " + e.getMessage());
                            }
                        });
                        
                        // Note: We intentionally don't close the connection here
                        // It will be kept open for the pagination to work
                        // The connection will be closed when the application exits
                        
                    } catch (SQLException e) {
                        final String errorMessage = e.getMessage();
                        Platform.runLater(() -> {
                            contentTabPane.getTabs().clear();
                            showErrorMessage("Error reading database: " + errorMessage);
                        });
                        
                        if (connection != null) {
                            try {
                                connection.close();
                            } catch (SQLException closeEx) {
                                // Ignore close exception
                            }
                        }
                    }
                    return null;
                }
            };
            
            // Start the loading task
            Thread loadThread = new Thread(loadTask);
            loadThread.setDaemon(true);
            loadThread.start();
            
        } catch (Exception e) {
            showErrorMessage("Error opening database: " + e.getMessage());
        }
    }
    
    /**
     * Creates a view with database information.
     * 
     * @param file the database file
     * @param connection the database connection
     * @param tableNames the list of table names
     * @return a VBox containing database information
     */
    private VBox createDatabaseInfoView(File file, Connection connection, List<String> tableNames) {
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(20));
        
        // Add file information
        TitledPane fileInfoPane = new TitledPane();
        fileInfoPane.setText("File Information");
        
        VBox fileInfoBox = new VBox(5);
        fileInfoBox.setPadding(new Insets(10));
        fileInfoBox.getChildren().addAll(
            new Label("Path: " + file.getAbsolutePath()),
            new Label("Size: " + formatFileSize(file.length())),
            new Label("Last Modified: " + new java.util.Date(file.lastModified()))
        );
        
        fileInfoPane.setContent(fileInfoBox);
        fileInfoPane.setExpanded(true);
        
        // Add database statistics
        TitledPane statsPane = new TitledPane();
        statsPane.setText("Database Statistics");
        
        VBox statsBox = new VBox(5);
        statsBox.setPadding(new Insets(10));
        
        try {
            // Get SQLite version
            Statement statement = connection.createStatement();
            ResultSet versionResult = statement.executeQuery("SELECT sqlite_version()");
            if (versionResult.next()) {
                statsBox.getChildren().add(new Label("SQLite Version: " + versionResult.getString(1)));
            }
            versionResult.close();
            
            // Get table counts
            statsBox.getChildren().add(new Label("Number of Tables: " + tableNames.size()));
            
            // Get row counts for each table (with a limit to avoid long operations)
            VBox tableStatsBox = new VBox(5);
            tableStatsBox.setPadding(new Insets(5, 0, 0, 20));
            
            for (String tableName : tableNames) {
                try {
                    // Use prepared statement to prevent SQL injection
                    ResultSet countResult = null;
                    try (PreparedStatement ps = connection.prepareStatement(
                            "SELECT COUNT(*) FROM \"" + tableName.replace("\"", "\"\"") + "\" LIMIT 10000")) {
                        countResult = ps.executeQuery();
                        if (countResult.next()) {
                            long count = countResult.getLong(1);
                            String countText = count >= 10000 ? "10000+ rows" : count + " rows";
                            tableStatsBox.getChildren().add(new Label(tableName + ": " + countText));
                        }
                    }
                } catch (SQLException e) {
                    tableStatsBox.getChildren().add(new Label(tableName + ": Error getting count"));
                }
            }
            
            statsBox.getChildren().add(tableStatsBox);
            
        } catch (SQLException e) {
            statsBox.getChildren().add(new Label("Error getting statistics: " + e.getMessage()));
        }
        
        statsPane.setContent(statsBox);
        statsPane.setExpanded(true);
        
        // Add a button to copy the database path
        Button copyPathButton = new Button("Copy Database Path");
        copyPathButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(file.getAbsolutePath());
            Clipboard.getSystemClipboard().setContent(content);
            
            // Show a tooltip that the path was copied
            Tooltip tooltip = new Tooltip("Path copied to clipboard!");
            tooltip.setAutoHide(true);
            tooltip.show(copyPathButton, 
                         copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMinX(),
                         copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMaxY());
            
            // Hide the tooltip after 1.5 seconds
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(event -> tooltip.hide());
            delay.play();
        });
        
        infoBox.getChildren().addAll(fileInfoPane, statsPane, copyPathButton);
        
        return infoBox;
    }
    
    /**
     * Displays a binary file by showing printable strings.
     * 
     * @param file the binary file to display
     */
    private void displayBinaryFile(File file) {
        try {
            // Extract printable strings from the binary file
            List<String> printableStrings = extractPrintableStrings(file);
            
            // Create a text area to display the printable strings
            TextArea textArea = new TextArea();
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.getStyleClass().add("printable-strings-view");
            
            // Add the printable strings to the text area
            for (String str : printableStrings) {
                textArea.appendText(str + "\n");
            }
            
            // Create a search box
            HBox searchBox = new HBox(5);
            searchBox.setPadding(new Insets(5));
            searchBox.setAlignment(Pos.CENTER_LEFT);
            
            // Create search label
            
            // Create search field
            TextField searchField = new TextField();
            searchField.setPromptText("Enter search term...");
            HBox.setHgrow(searchField, Priority.ALWAYS);
            
            // Add Enter key support for search field
            searchField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    searchInTextArea(textArea, searchField.getText());
                }
            });
            
            // Create search button
            Button searchButton = new Button("Find");
            searchButton.setOnAction(e -> searchInTextArea(textArea, searchField.getText()));
            
            // Create clear button
            Button clearButton = new Button("Clear");
            clearButton.setOnAction(e -> {
                searchField.clear();
                textArea.deselect();
                // Reset any highlighting
                textArea.setStyle("");
            });
            
            // Add components to the search box
            searchBox.getChildren().addAll(searchField, searchButton, clearButton);
            
            // Create a VBox to hold the search box and text area
            VBox contentBox = new VBox(5);
            contentBox.getChildren().addAll(searchBox, textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            
            // Create a tab for the printable strings
            Tab tab = new Tab("Printable Strings");
            tab.setContent(contentBox);
            
            // Add the tab to the tab pane
            contentTabPane.getTabs().add(tab);
            
            // Add a hex view tab
            Tab hexTab = new Tab("Hex View");
            hexTab.setContent(createHexView(file));
            contentTabPane.getTabs().add(hexTab);
            
        } catch (IOException e) {
            showErrorMessage("Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Creates a TableView to display the contents of a database table.
     * 
     * @param connection the database connection
     * @param tableName the name of the table to display
     * @return a VBox containing the TableView and pagination controls
     */
    private VBox createTableView(final Connection connection, final String tableName) throws SQLException {
        // Constants for pagination
        final int PAGE_SIZE = 100;
        final int MAX_ROWS_TO_COUNT = 10000;
        
        // Create a TableView with proper generic type
        final TableView<List<String>> tableView = new TableView<>();
        tableView.setEditable(false);
        
        // Get the table metadata
        // Use prepared statement with escaped table name to prevent SQL injection
        final String safeTableName = tableName.replace("\"", "\"\"");
        final PreparedStatement ps = connection.prepareStatement("SELECT * FROM \"" + safeTableName + "\" LIMIT 0");
        final ResultSet resultSet = ps.executeQuery();
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        
        // Add columns to the TableView
        for (int i = 1; i <= columnCount; i++) {
            final int columnIndex = i - 1;
            String columnName = metaData.getColumnName(i);
            TableColumn<List<String>, String> column = new TableColumn<>(columnName);
            
            // Add tooltip to show full column name
            Label columnLabel = new Label(columnName);
            columnLabel.setTooltip(new Tooltip(columnName));
            column.setGraphic(columnLabel);
            
            // Set a wider column for timestamp fields
            if (columnName.toLowerCase().contains("stamp") || 
                columnName.toLowerCase().contains("time") || 
                columnName.toLowerCase().contains("date")) {
                column.setPrefWidth(200);
            }
            
            column.setCellValueFactory(param -> {
                List<String> row = param.getValue();
                String value = columnIndex < row.size() ? row.get(columnIndex) : "";
                
                // Format timestamp columns for better readability
                if (columnName.toLowerCase().contains("stamp") || 
                    columnName.toLowerCase().contains("time") || 
                    columnName.toLowerCase().contains("date")) {
                    try {
                        // Try to parse as a timestamp (Unix epoch in seconds or milliseconds)
                        long timestamp;
                        if (value.length() > 10) {
                            // Milliseconds format
                            timestamp = Long.parseLong(value);
                        } else if (value.length() > 0) {
                            // Seconds format
                            timestamp = Long.parseLong(value) * 1000;
                        } else {
                            return new javafx.beans.property.SimpleStringProperty(value);
                        }
                        
                        // Format the timestamp
                        java.util.Date date = new java.util.Date(timestamp);
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        return new javafx.beans.property.SimpleStringProperty(
                            sdf.format(date) + " (" + value + ")");
                    } catch (NumberFormatException e) {
                        // If it's not a number, just return the original value
                        return new javafx.beans.property.SimpleStringProperty(value);
                    }
                }
                
                return new javafx.beans.property.SimpleStringProperty(value);
            });
            tableView.getColumns().add(column);
        }
        
        // Get approximate row count (limit to avoid long counting operations)
        final long rowCount = getTableRowCount(ps, tableName, MAX_ROWS_TO_COUNT);
        
        // Create pagination controls
        final HBox paginationControls = new HBox(10);
        paginationControls.setAlignment(Pos.CENTER);
        paginationControls.setPadding(new Insets(10, 0, 10, 0));
        paginationControls.setMinHeight(40);
        
        final Label pageInfoLabel = new Label("Page 1" + (rowCount >= 0 ? " of " + Math.ceil((double)rowCount / PAGE_SIZE) : ""));
        pageInfoLabel.setMinWidth(100);
        
        // Create buttons with icons and tooltips for small screens
        final Button prevButton = new Button();
        prevButton.setGraphic(createButtonIcon("◀"));
        prevButton.setTooltip(new Tooltip("Previous Page"));
        prevButton.setMinWidth(40);
        prevButton.setDisable(true);
        
        final Button nextButton = new Button();
        nextButton.setGraphic(createButtonIcon("▶"));
        nextButton.setTooltip(new Tooltip("Next Page"));
        nextButton.setMinWidth(40);
        if (rowCount >= 0 && rowCount <= PAGE_SIZE) {
            nextButton.setDisable(true);
        }
        
        final TextField pageField = new TextField("1");
        pageField.setPrefWidth(60);
        pageField.setMinWidth(40);
        pageField.setOnAction(e -> {
            try {
                int page = Integer.parseInt(pageField.getText());
                if (page > 0) {
                    loadTablePage(connection, tableName, tableView, columnCount, page, PAGE_SIZE, pageInfoLabel, prevButton, nextButton, rowCount);
                }
            } catch (NumberFormatException ex) {
                pageField.setText("1");
            } catch (SQLException ex) {
                showErrorMessage("Error loading data: " + ex.getMessage());
            }
        });
        
        prevButton.setOnAction(e -> {
            try {
                int currentPage = Integer.parseInt(pageField.getText());
                if (currentPage > 1) {
                    int newPage = currentPage - 1;
                    pageField.setText(String.valueOf(newPage));
                    loadTablePage(connection, tableName, tableView, columnCount, newPage, PAGE_SIZE, pageInfoLabel, prevButton, nextButton, rowCount);
                }
            } catch (SQLException ex) {
                showErrorMessage("Error loading data: " + ex.getMessage());
            }
        });
        
        nextButton.setOnAction(e -> {
            try {
                int currentPage = Integer.parseInt(pageField.getText());
                int newPage = currentPage + 1;
                pageField.setText(String.valueOf(newPage));
                loadTablePage(connection, tableName, tableView, columnCount, newPage, PAGE_SIZE, pageInfoLabel, prevButton, nextButton, rowCount);
            } catch (SQLException ex) {
                showErrorMessage("Error loading data: " + ex.getMessage());
            }
        });
        
        Label pageLabel = new Label("Page:");
        pageLabel.setMinWidth(40);
        
        Label rowCountLabel = new Label(rowCount >= 0 ? "Total rows: " + rowCount : "Total rows: more than " + MAX_ROWS_TO_COUNT);
        rowCountLabel.setMinWidth(150);
        
        // Create a spacer to push elements to the left and right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        paginationControls.getChildren().addAll(
            prevButton, 
            pageLabel, 
            pageField, 
            nextButton, 
            spacer,
            pageInfoLabel,
            rowCountLabel
        );
        
        // Load the first page
        loadTablePage(connection, tableName, tableView, columnCount, 1, PAGE_SIZE, pageInfoLabel, prevButton, nextButton, rowCount);
        
        // Create a VBox to hold the table and pagination controls
        VBox tableContainer = new VBox(10);
        tableContainer.getChildren().addAll(paginationControls, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        
        return tableContainer;
    }
    
    /**
     * Gets the approximate row count for a table, with a limit to avoid long counting operations.
     * 
     * @param statement the SQL statement to use
     * @param tableName the name of the table
     * @param maxRowsToCount the maximum number of rows to count
     * @return the row count, or -1 if the count exceeds maxRowsToCount or if an error occurs
     */
    private long getTableRowCount(final PreparedStatement statement, final String tableName, final int maxRowsToCount) {
        try {
            // Use prepared statement with escaped table name to prevent SQL injection
            final String safeTableName = tableName.replace("\"", "\"\"");
            try (PreparedStatement ps = statement.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM \"" + safeTableName + "\" LIMIT ?")) {
                ps.setInt(1, maxRowsToCount);
                try (ResultSet countResult = ps.executeQuery()) {
                    countResult.next();
                    long count = countResult.getLong(1);
                    
                    // If we hit the limit, indicate there might be more
                    if (count >= maxRowsToCount) {
                        return -1; // Use -1 to indicate "more than maxRowsToCount"
                    } else {
                        return count;
                    }
                }
            }
        } catch (SQLException e) {
            // If COUNT(*) fails, just use -1 to indicate unknown count
            return -1;
        }
    }
    
    /**
     * Loads a specific page of data into the table view.
     * 
     * @param connection the database connection
     * @param tableName the name of the table
     * @param tableView the TableView to populate
     * @param columnCount the number of columns
     * @param page the page number (1-based)
     * @param pageSize the number of rows per page
     * @param pageInfoLabel the label to update with page information
     * @param prevButton the previous page button
     * @param nextButton the next page button
     * @param totalRowCount the total number of rows (-1 if unknown)
     * @throws SQLException if a database error occurs
     */
    private void loadTablePage(final Connection connection, final String tableName, final TableView<List<String>> tableView, 
                              final int columnCount, final int page, final int pageSize, final Label pageInfoLabel, 
                              final Button prevButton, final Button nextButton, final long totalRowCount) throws SQLException {
        // Clear existing data
        tableView.getItems().clear();
        
        // Calculate offset
        int offset = (page - 1) * pageSize;
        
        // Create a new statement
        try (Statement statement = connection.createStatement()) {
            // Set timeout to avoid hanging on large queries
            statement.setQueryTimeout(30);
            
            // Query with pagination using prepared statement to prevent SQL injection
            final String safeTableName = tableName.replace("\"", "\"\"");
            String query = String.format("SELECT * FROM \"%s\" LIMIT ? OFFSET ?", safeTableName);
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, pageSize);
                ps.setInt(2, offset);
                try (ResultSet resultSet = ps.executeQuery()) {
                    // Add rows to the TableView
                    while (resultSet.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String value = resultSet.getString(i);
                            row.add(value != null ? value : "NULL");
                        }
                        tableView.getItems().add(row);
                    }
                }
            }
            
            // Check if there are more pages
            boolean hasMore = false;
            if (totalRowCount < 0) {
                // If we don't know the total count, check if there's at least one more row
                // Use prepared statement to prevent SQL injection
                try (PreparedStatement ps = connection.prepareStatement(
                        String.format("SELECT 1 FROM \"%s\" LIMIT ? OFFSET ?", safeTableName))) {
                    ps.setInt(1, 1);
                    ps.setInt(2, offset + pageSize);
                    try (ResultSet moreResult = ps.executeQuery()) {
                        hasMore = moreResult.next();
                    }
                }
            } else {
                hasMore = offset + pageSize < totalRowCount;
            }
            
            // Update UI
            prevButton.setDisable(page <= 1);
            nextButton.setDisable(!hasMore);
            
            if (totalRowCount >= 0) {
                int totalPages = (int) Math.ceil((double) totalRowCount / pageSize);
                pageInfoLabel.setText("Page " + page + " of " + totalPages);
            } else {
                pageInfoLabel.setText("Page " + page);
            }
        }
    }
    
    /**
     * Creates a view with a table selector dropdown and table content.
     * 
     * @param connection the database connection
     * @param tableNames the list of table names
     * @return a VBox containing the table selector and content
     * @throws SQLException if a database error occurs
     */
    private VBox createTableSelectorView(final Connection connection, final List<String> tableNames) throws SQLException {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        
        // Create a toolbar with a minimum width
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5));
        toolbar.setMinHeight(40);
        toolbar.getStyleClass().add("table-selector-toolbar");
        
        // Create a label for the dropdown
        Label selectLabel = new Label("Select Table:");
        selectLabel.getStyleClass().add("header-label");
        selectLabel.setMinWidth(120);
        
        // Create the table dropdown
        ComboBox<String> tableSelector = new ComboBox<>();
        tableSelector.getItems().addAll(tableNames);
        tableSelector.setMinWidth(250);
        tableSelector.setPrefWidth(300);
        tableSelector.setPromptText("Select a table to view");
        tableSelector.setId("tableSelector"); // Add ID for specific styling
        
        // Add components to the toolbar
        toolbar.getChildren().addAll(selectLabel, tableSelector);
        
        // Create a placeholder for the table content
        StackPane tableContentPane = new StackPane();
        tableContentPane.setAlignment(Pos.CENTER);
        tableContentPane.getChildren().add(new Label("Select a table from the dropdown to view its data"));
        VBox.setVgrow(tableContentPane, Priority.ALWAYS);
        
        // Add change listener to the dropdown
        tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    // Show loading indicator
                    ProgressIndicator progress = new ProgressIndicator();
                    progress.setMaxSize(50, 50);
                    tableContentPane.getChildren().setAll(progress);
                    
                    // Load table data in background
                    Task<VBox> loadTableTask = new Task<>() {
                        @Override
                        protected VBox call() throws Exception {
                            return createTableView(connection, newVal);
                        }
                    };
                    
                    loadTableTask.setOnSucceeded(e -> {
                        tableContentPane.getChildren().setAll(loadTableTask.getValue());
                    });
                    
                    loadTableTask.setOnFailed(e -> {
                        Label errorLabel = new Label("Error loading table: " + loadTableTask.getException().getMessage());
                        errorLabel.setStyle("-fx-text-fill: red;");
                        tableContentPane.getChildren().setAll(errorLabel);
                    });
                    
                    Thread thread = new Thread(loadTableTask);
                    thread.setDaemon(true);
                    thread.start();
                    
                } catch (Exception e) {
                    Label errorLabel = new Label("Error loading table: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: red;");
                    tableContentPane.getChildren().setAll(errorLabel);
                }
            }
        });
        
        // Add components to the container
        container.getChildren().addAll(toolbar, tableContentPane);
        
        // If there's only one table, select it automatically
        if (tableNames.size() == 1) {
            tableSelector.getSelectionModel().select(0);
        }
        
        return container;
    }
    
    /**
     * Creates a view to display the database schema.
     * 
     * @param connection the database connection
     * @param tableNames the list of table names
     * @return a ScrollPane containing the schema view
     */
    private ScrollPane createSchemaView(Connection connection, List<String> tableNames) throws SQLException {
        // Create a VBox to hold the schema information
        VBox schemaBox = new VBox(20);
        schemaBox.setPadding(new Insets(10));
        
        // Add schema information for each table
        for (String tableName : tableNames) {
            TitledPane tablePane = new TitledPane();
            tablePane.setText(tableName);
            
            // Create a TableView for the column information
            TableView<ColumnInfo> columnTable = new TableView<>();
            columnTable.setEditable(false);
            
            // Add columns to the TableView
            TableColumn<ColumnInfo, String> nameColumn = new TableColumn<>("Column Name");
            nameColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().name));
            
            TableColumn<ColumnInfo, String> typeColumn = new TableColumn<>("Type");
            typeColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().type));
            
            TableColumn<ColumnInfo, String> notNullColumn = new TableColumn<>("Not Null");
            notNullColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().notNull));
            
            TableColumn<ColumnInfo, String> defaultColumn = new TableColumn<>("Default Value");
            defaultColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().defaultValue));
            
            TableColumn<ColumnInfo, String> pkColumn = new TableColumn<>("Primary Key");
            pkColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().primaryKey));
            
            // Add columns individually to avoid unchecked generic array creation warning
            columnTable.getColumns().add(nameColumn);
            columnTable.getColumns().add(typeColumn);
            columnTable.getColumns().add(notNullColumn);
            columnTable.getColumns().add(defaultColumn);
            columnTable.getColumns().add(pkColumn);
            
            try {
                // Query the table schema with a timeout
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(10); // 10 seconds timeout
                
                // Use escaped table name to prevent SQL injection
                final String safeTableName = tableName.replace("\"", "\"\"");
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(\"" + safeTableName + "\")");
                
                // Add rows to the TableView
                while (resultSet.next()) {
                    ColumnInfo columnInfo = new ColumnInfo(
                        resultSet.getString("name"),
                        resultSet.getString("type"),
                        resultSet.getInt("notnull") == 1 ? "Yes" : "No",
                        resultSet.getString("dflt_value") != null ? resultSet.getString("dflt_value") : "NULL",
                        resultSet.getInt("pk") == 1 ? "Yes" : "No"
                    );
                    columnTable.getItems().add(columnInfo);
                }
                
                // Close the statement and result set
                resultSet.close();
                statement.close();
                
                // Add indexes information if available
                try {
                    Statement indexStatement = connection.createStatement();
                    indexStatement.setQueryTimeout(5);
                    
                    // Use the same escaped table name to prevent SQL injection
                    ResultSet indexList = indexStatement.executeQuery("PRAGMA index_list(\"" + safeTableName + "\")");
                    
                    if (indexList.next()) {
                        // Reset to beginning
                        indexList.beforeFirst();
                        
                        // Create a section for indexes
                        VBox indexBox = new VBox(5);
                        indexBox.setPadding(new Insets(10, 0, 0, 0));
                        Label indexLabel = new Label("Indexes:");
                        indexLabel.setStyle("-fx-font-weight: bold;");
                        indexBox.getChildren().add(indexLabel);
                        
                        while (indexList.next()) {
                            String indexName = indexList.getString("name");
                            boolean unique = indexList.getInt("unique") == 1;
                            
                            StringBuilder indexInfo = new StringBuilder();
                            indexInfo.append("• ").append(indexName);
                            if (unique) {
                                indexInfo.append(" (UNIQUE)");
                            }
                            
                            // Get the columns in this index
                            try {
                                // Use escaped index name to prevent SQL injection
                                final String safeIndexName = indexName.replace("\"", "\"\"");
                                ResultSet indexInfo2 = indexStatement.executeQuery("PRAGMA index_info(\"" + safeIndexName + "\")");
                                List<String> indexColumns = new ArrayList<>();
                                
                                while (indexInfo2.next()) {
                                    indexColumns.add(indexInfo2.getString("name"));
                                }
                                
                                if (!indexColumns.isEmpty()) {
                                    indexInfo.append(" - Columns: ").append(String.join(", ", indexColumns));
                                }
                                
                                indexInfo2.close();
                            } catch (SQLException e) {
                                // If we can't get index details, just show the name
                            }
                            
                            indexBox.getChildren().add(new Label(indexInfo.toString()));
                        }
                        
                        // Add a separator
                        Separator separator = new Separator();
                        separator.setPadding(new Insets(5, 0, 5, 0));
                        
                        // Create a VBox to hold both the table and indexes
                        VBox tableContentBox = new VBox(10);
                        tableContentBox.getChildren().addAll(columnTable, separator, indexBox);
                        
                        // Set the content of the titled pane
                        tablePane.setContent(tableContentBox);
                    } else {
                        // No indexes, just show the column table
                        tablePane.setContent(columnTable);
                    }
                    
                    indexList.close();
                    indexStatement.close();
                } catch (SQLException e) {
                    // If we can't get index information, just show the column table
                    tablePane.setContent(columnTable);
                }
                
            } catch (SQLException e) {
                // If there's an error getting the schema, show an error message
                Label errorLabel = new Label("Error loading schema: " + e.getMessage());
                errorLabel.setStyle("-fx-text-fill: red;");
                tablePane.setContent(errorLabel);
            }
            
            // Set the titled pane to be initially collapsed to improve performance
            tablePane.setExpanded(false);
            
            // Add the titled pane to the schema box
            schemaBox.getChildren().add(tablePane);
        }
        
        // Create a scroll pane to allow scrolling
        ScrollPane scrollPane = new ScrollPane(schemaBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }
    
    /**
     * Creates a hex view for binary files.
     * 
     * @param file the binary file
     * @return a ScrollPane containing the hex view
     * @throws IOException if an I/O error occurs
     */
    private ScrollPane createHexView(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("File is not accessible or does not exist: " + 
                (file != null ? file.getAbsolutePath() : "null"));
        }
        
        // Create a text area for the hex view
        TextArea hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.getStyleClass().add("hex-view");
        hexArea.setStyle("-fx-font-family: monospace;");
        
        // Read the file in chunks to avoid memory issues
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[16]; // 16 bytes per line
            int bytesRead;
            long offset = 0;
            
            // Limit the hex view to the first 10KB to avoid performance issues
            long maxBytes = 10 * 1024;
            
            StringBuilder hexBuilder = new StringBuilder();
            
            while ((bytesRead = bis.read(buffer)) != -1 && offset < maxBytes) {
                // Append the offset
                hexBuilder.append(String.format("%08X: ", offset));
                
                // Append the hex representation
                for (int i = 0; i < bytesRead; i++) {
                    hexBuilder.append(String.format("%02X ", buffer[i] & 0xFF));
                }
                
                // Pad with spaces if the line is incomplete
                for (int i = bytesRead; i < 16; i++) {
                    hexBuilder.append("   ");
                }
                
                // Append the ASCII representation
                hexBuilder.append(" | ");
                for (int i = 0; i < bytesRead; i++) {
                    char c = (char) (buffer[i] & 0xFF);
                    hexBuilder.append(isPrintableChar(c) ? c : '.');
                }
                
                hexBuilder.append("\n");
                offset += bytesRead;
                
                // Safety check to prevent excessive memory usage
                if (hexBuilder.length() > 100000) {
                    hexBuilder.append("\n... (output truncated, buffer limit reached)");
                    break;
                }
            }
            
            if (offset >= maxBytes && file.length() > maxBytes) {
                hexBuilder.append("\n... (file truncated, showing first ").append(formatFileSize(maxBytes)).append(")");
            }
            
            hexArea.setText(hexBuilder.toString());
        } catch (SecurityException e) {
            throw new IOException("Security error accessing file: " + e.getMessage(), e);
        }
        
        // Create a scroll pane to allow scrolling
        ScrollPane scrollPane = new ScrollPane(hexArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }
    
    /**
     * Searches for text in a TextArea and highlights/selects the first occurrence.
     * 
     * @param textArea the TextArea to search in
     * @param searchText the text to search for
     */
    private void searchInTextArea(TextArea textArea, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        
        String text = textArea.getText();
        int index = text.toLowerCase().indexOf(searchText.toLowerCase());
        
        if (index >= 0) {
            // Select the found text
            textArea.selectRange(index, index + searchText.length());
            
            // Scroll to the found text
            textArea.requestFocus();
            
            // Highlight the found text
            textArea.setStyle("-fx-highlight-fill: #ffff00; -fx-highlight-text-fill: #000000;");
        } else {
            // Show a message if text not found
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search Result");
            alert.setHeaderText(null);
            alert.setContentText("Text '" + searchText + "' not found.");
            alert.showAndWait();
        }
    }
    
    /**
     * Extracts printable strings from a binary file.
     * 
     * @param file the binary file
     * @return a list of printable strings
     * @throws IOException if an I/O error occurs
     */
    private List<String> extractPrintableStrings(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("File is not accessible or does not exist: " + 
                (file != null ? file.getAbsolutePath() : "null"));
        }
        
        List<String> strings = new ArrayList<>();
        StringBuilder currentString = new StringBuilder();
        
        // Use buffered input stream for better performance
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int totalChars = 0;
            int c;
            
            while ((c = bis.read()) != -1 && totalChars < MAX_PRINTABLE_STRINGS_LENGTH) {
                char ch = (char) c;
                
                if (isPrintableChar(ch)) {
                    currentString.append(ch);
                    totalChars++;
                } else if (currentString.length() >= 4) { // Only keep strings of at least 4 characters
                    strings.add(currentString.toString());
                    currentString = new StringBuilder();
                } else {
                    currentString = new StringBuilder();
                }
                
                // Safety check to prevent excessive memory usage
                if (strings.size() > 10000) {
                    strings.add("... (output truncated, too many strings found)");
                    break;
                }
            }
            
            // Add the last string if it's long enough
            if (currentString.length() >= 4) {
                strings.add(currentString.toString());
            }
        } catch (SecurityException e) {
            throw new IOException("Security error accessing file: " + e.getMessage(), e);
        }
        
        return strings;
    }
    
    /**
     * Checks if a character is printable.
     * 
     * @param c the character to check
     * @return true if the character is printable, false otherwise
     */
    private boolean isPrintableChar(char c) {
        return c >= 32 && c <= 126;
    }
    
    /**
     * Gets the list of table names from a database.
     * 
     * @param connection the database connection
     * @return the list of table names
     */
    private List<String> getTableNames(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        
        // Query the sqlite_master table to get the list of tables
        // Using a fixed query with no user input, so SQL injection is not a concern here
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString("name"));
                }
            }
        }
        
        return tableNames;
    }
    
    /**
     * Creates a text-based icon for buttons.
     * 
     * @param iconText the text to use as an icon
     * @return a Label styled as an icon
     */
    private Label createButtonIcon(String iconText) {
        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return icon;
    }
    
    /**
     * Shows an error message.
     * 
     * @param message the error message
     */
    private void showErrorMessage(String message) {
        // Create a text area to display the error message
        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().addAll("error-text", "error-message-view");
        
        // Create a tab for the error message
        Tab tab = new Tab("Error");
        tab.setContent(textArea);
        
        // Add the tab to the tab pane
        contentTabPane.getTabs().clear();
        contentTabPane.getTabs().add(tab);
    }
    
    /**
     * Formats a file size in bytes to a human-readable string.
     * 
     * @param size the file size in bytes
     * @return a human-readable string representation of the file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Gets the file extension from a file name.
     * 
     * @param fileName the file name
     * @return the file extension
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
    

    
    /**
     * Checks if a file is a text file based on its extension.
     * 
     * @param fileName the file name
     * @return true if the file is a text file, false otherwise
     */
    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".txt") || fileName.endsWith(".xml") || 
               fileName.endsWith(".py") || fileName.endsWith(".java") || 
               fileName.endsWith(".json") || fileName.endsWith(".html") || 
               fileName.endsWith(".css") || fileName.endsWith(".js") || 
               fileName.endsWith(".md") || fileName.endsWith(".log") || 
               fileName.endsWith(".csv") || fileName.endsWith(".properties") || 
               fileName.endsWith(".yml") || fileName.endsWith(".yaml") || 
               fileName.endsWith(".sh") || fileName.endsWith(".bat") || 
               fileName.endsWith(".c") || fileName.endsWith(".cpp") || 
               fileName.endsWith(".h");
    }
    
    /**
     * Checks if a file is a property list (plist) file based on its extension.
     * 
     * @param fileName the file name
     * @return true if the file is a plist file, false otherwise
     */
    private boolean isPlistFile(String fileName) {
        return fileName.endsWith(".plist");
    }
    
    /**
     * Displays a property list (plist) file.
     * 
     * @param file the plist file to display
     */
    private void displayPlistFile(File file) {
        try {
            // Parse the plist file
            NSObject rootObject = PropertyListParser.parse(file);
            
            // Create a TreeView to display the plist structure
            TreeView<String> treeView = new TreeView<>();
            treeView.setShowRoot(true);
            
            // Create the root item
            TreeItem<String> rootItem = new TreeItem<>("Root");
            rootItem.setExpanded(true);
            
            // Populate the tree with plist data
            populatePlistTree(rootItem, rootObject);
            
            // Set the root item
            treeView.setRoot(rootItem);
            
            // Create a tab for the plist view
            Tab plistTab = new Tab("Plist View");
            plistTab.setContent(treeView);
            
            // Add the tab to the tab pane
            contentTabPane.getTabs().add(plistTab);
            
            // Also display as text for reference
            displayTextFile(file);
            
        } catch (Exception e) {
            showErrorMessage("Error parsing plist file: " + e.getMessage());
            // Fall back to text display
            displayTextFile(file);
        }
    }
    
    /**
     * Recursively populates a TreeView with plist data.
     * 
     * @param parentItem the parent tree item
     * @param plistObject the plist object to add
     */
    private void populatePlistTree(TreeItem<String> parentItem, NSObject plistObject) {
        if (plistObject instanceof NSDictionary) {
            NSDictionary dict = (NSDictionary) plistObject;
            for (String key : dict.allKeys()) {
                NSObject value = dict.objectForKey(key);
                TreeItem<String> item = new TreeItem<>(key + ": " + getShortDescription(value));
                parentItem.getChildren().add(item);
                
                // Recursively add children
                populatePlistTree(item, value);
            }
        } else if (plistObject instanceof NSArray) {
            NSArray array = (NSArray) plistObject;
            NSObject[] values = array.getArray();
            for (int i = 0; i < values.length; i++) {
                NSObject value = values[i];
                TreeItem<String> item = new TreeItem<>("[" + i + "]: " + getShortDescription(value));
                parentItem.getChildren().add(item);
                
                // Recursively add children
                populatePlistTree(item, value);
            }
        }
    }
    
    /**
     * Gets a short description of a plist object for display in the tree.
     * 
     * @param object the plist object
     * @return a short description of the object
     */
    private String getShortDescription(NSObject object) {
        if (object instanceof NSDictionary) {
            NSDictionary dict = (NSDictionary) object;
            return "{Dictionary: " + dict.count() + " entries}";
        } else if (object instanceof NSArray) {
            NSArray array = (NSArray) object;
            return "[Array: " + array.count() + " items]";
        } else {
            // For simple values, just return the string representation
            return object.toString();
        }
    }
    
    /**
     * Checks if a file is an image file based on its extension.
     * 
     * @param fileName the file name
     * @return true if the file is an image file, false otherwise
     */
    private boolean isImageFile(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") || fileName.endsWith(".tiff") ||
               fileName.endsWith(".webp") || fileName.endsWith(".svg") ||
               fileName.endsWith(".ico");
    }
    
    /**
     * Checks if a file is a database file based on its extension.
     * 
     * @param fileName the file name
     * @return true if the file is a database file, false otherwise
     */
    private boolean isDatabaseFile(String fileName) {
        return fileName.endsWith(".db") || fileName.endsWith(".db3") || 
               fileName.endsWith(".sqlite") || fileName.endsWith(".sqlite3");
    }
    
    /**
     * A class to hold column information for the database schema view.
     */
    private static class ColumnInfo {
        private final String name;
        private final String type;
        private final String notNull;
        private final String defaultValue;
        private final String primaryKey;
        
        public ColumnInfo(String name, String type, String notNull, String defaultValue, String primaryKey) {
            this.name = name;
            this.type = type;
            this.notNull = notNull;
            this.defaultValue = defaultValue;
            this.primaryKey = primaryKey;
        }
    }
}