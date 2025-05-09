package com.mobapp.inspector.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class FileSystemTreeView extends VBox {
    
    private TreeView<File> treeView;
    private Consumer<File> fileSelectedListener;
    private TextField searchField;
    private HBox breadcrumbBar;
    private List<Hyperlink> breadcrumbs = new ArrayList<>();
    private PauseTransition searchDebounceTimer;
    private ToggleButton contentSearchToggle;
    private ProgressIndicator searchProgressIndicator;
    private VBox searchResultsBox;
    private ScrollPane searchResultsScrollPane;
    private boolean isSearchingContent = false;
    private Scene scene;
    private File rootDir;
    private boolean isDarkTheme = false;
    
    private final Image folderIcon = new Image(getClass().getResourceAsStream("/com/mobapp/inspector/icons/folder.png"));
    private final Image fileIcon = new Image(getClass().getResourceAsStream("/com/mobapp/inspector/icons/file.png"));
    private final Image textFileIcon = new Image(getClass().getResourceAsStream("/com/mobapp/inspector/icons/text-file.png"));
    private final Image imageFileIcon = new Image(getClass().getResourceAsStream("/com/mobapp/inspector/icons/image-file.png"));
    private final Image databaseFileIcon = new Image(getClass().getResourceAsStream("/com/mobapp/inspector/icons/database-file.png"));
    
    public FileSystemTreeView() {
        initializeUI();
    }
    
    private void initializeUI() {
        setPadding(new Insets(0));
        setSpacing(0);
        getStyleClass().add("file-browser-panel");
        
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                scene = newScene;
            }
        });
        
        ToolBar toolbar = createToolbar();
        
        breadcrumbBar = createBreadcrumbBar();
        
        VBox searchContainer = createSearchBox();
        
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("file-tree");
        
        treeView.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(file.getName().isEmpty() ? file.getPath() : file.getName());
                    setGraphic(new ImageView(getIconForFile(file)));
                }
            }
        });
        
        VBox placeholderBox = new VBox(10);
        placeholderBox.setAlignment(Pos.CENTER);
        placeholderBox.setPadding(new Insets(20));
        
        Label folderIconLabel = new Label("📁");
        folderIconLabel.setStyle("-fx-font-size: 48px;");
        
        Label messageLabel = new Label("No folder selected");
        messageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label instructionsLabel = new Label("Click 'Open Folder' to select a folder for inspection");
        instructionsLabel.setStyle("-fx-font-size: 14px;");
        
        Button openFolderButton = new Button("Open Folder");
        openFolderButton.setOnAction(e -> openFolder());
        openFolderButton.getStyleClass().add("primary-button");
        openFolderButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16;");
        
        placeholderBox.getChildren().addAll(folderIconLabel, messageLabel, instructionsLabel, openFolderButton);
        
        treeView.setRoot(null);
        
        StackPane treeContainer = new StackPane();
        treeContainer.getChildren().addAll(placeholderBox, treeView, searchResultsScrollPane);
        VBox.setVgrow(treeContainer, Priority.ALWAYS);
        
        treeView.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            placeholderBox.setVisible(newRoot == null);
            treeView.setVisible(newRoot != null);
        });
        
        placeholderBox.setVisible(true);
        treeView.setVisible(false);
        
        getChildren().addAll(toolbar, breadcrumbBar, searchContainer, treeContainer);
        
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && fileSelectedListener != null) {
                File selectedFile = newValue.getValue();
                if (selectedFile.isFile()) {
                    fileSelectedListener.accept(selectedFile);
                }
                updateBreadcrumbs(selectedFile);
            }
        });
        
        breadcrumbBar.getChildren().clear();
        Label noFolderLabel = new Label("No folder selected");
        noFolderLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
        breadcrumbBar.getChildren().add(noFolderLabel);
    }
    

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.getStyleClass().add("browser-toolbar");
        
        Button openFolderButton = new Button("Open Folder");
        openFolderButton.setGraphic(new ImageView(folderIcon));
        openFolderButton.getStyleClass().addAll("toolbar-button", "primary-action");
        openFolderButton.setOnAction(e -> openFolder());
        
        Button homeButton = new Button("Home");
        homeButton.getStyleClass().add("toolbar-button");
        homeButton.setTooltip(new Tooltip("Go to home directory"));
        
        Label homeIcon = new Label("⌂");
        homeIcon.getStyleClass().add("button-icon");
        homeButton.setGraphic(homeIcon);
        
        homeButton.setOnAction(e -> navigateToHome());
        
        Button upButton = new Button("Up");
        upButton.getStyleClass().add("toolbar-button");
        upButton.setTooltip(new Tooltip("Go to parent directory"));
        
        Label upIcon = new Label("↑");
        upIcon.getStyleClass().add("button-icon");
        upButton.setGraphic(upIcon);
        
        upButton.setOnAction(e -> navigateUp());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("toolbar-button");
        refreshButton.setTooltip(new Tooltip("Refresh current folder to show changes made outside the application"));
        
        Label refreshIcon = new Label("↻");
        refreshIcon.getStyleClass().add("button-icon");
        refreshButton.setGraphic(refreshIcon);
        
        refreshButton.setOnAction(e -> refreshCurrentFolder());
        
        toolbar.getItems().addAll(
            openFolderButton, 
            new Separator(Orientation.VERTICAL),
            homeButton, 
            upButton,
            refreshButton
        );
        
        return toolbar;
    }
    

    private HBox createBreadcrumbBar() {
        HBox breadcrumbBar = new HBox(5);
        breadcrumbBar.setPadding(new Insets(5, 10, 5, 10));
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBar.getStyleClass().add("breadcrumb-bar");
        
        return breadcrumbBar;
    }
    

    private VBox createSearchBox() {
        HBox searchBox = new HBox(5);
        searchBox.setPadding(new Insets(5, 10, 5, 10));
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box");
        
        searchField = new TextField();
        searchField.setPromptText("Enter search term...");
        searchField.getStyleClass().add("search-field");
        searchField.setTooltip(new Tooltip("Enter: Search\nEscape: Clear search"));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        RadioButton filenameRadio = new RadioButton("Filename");
        filenameRadio.setTooltip(new Tooltip("Search by filename"));
        filenameRadio.setSelected(true);
        
        RadioButton contentRadio = new RadioButton("Content");
        contentRadio.setTooltip(new Tooltip("Search in file contents"));
        
        ToggleGroup searchModeGroup = new ToggleGroup();
        filenameRadio.setToggleGroup(searchModeGroup);
        contentRadio.setToggleGroup(searchModeGroup);
        
        HBox searchModeBox = new HBox(10);
        searchModeBox.getChildren().addAll(filenameRadio, contentRadio);
        
        searchModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == contentRadio) {
                isSearchingContent = true;
                searchField.setPromptText("Search in file contents...");
            } else {
                isSearchingContent = false;
                searchField.setPromptText("Search by filename...");
            }
            
            if (!searchField.getText().isEmpty()) {
                if (searchDebounceTimer != null) {
                    searchDebounceTimer.stop();
                }
                searchFiles(searchField.getText());
            }
        });
        
        searchProgressIndicator = new ProgressIndicator();
        searchProgressIndicator.setVisible(false);
        searchProgressIndicator.setPrefSize(20, 20);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                if (newValue.isEmpty()) {
                    refreshCurrentFolder();
                } else {
                    if (searchDebounceTimer != null) {
                        searchDebounceTimer.stop();
                    }
                    
                    searchDebounceTimer = new PauseTransition(Duration.millis(300));
                    searchDebounceTimer.setOnFinished(e -> searchFiles(newValue));
                    searchDebounceTimer.play();
                }
            }
        });
        
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (searchDebounceTimer != null) {
                    searchDebounceTimer.stop();
                }
                searchFiles(searchField.getText());
            } else if (e.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
                refreshCurrentFolder();
            }
        });
        
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("clear-button");
        clearButton.setTooltip(new Tooltip("Clear search"));
        clearButton.setOnAction(e -> {
            searchField.clear();
            refreshCurrentFolder();
        });
        
        Button copyPathButton = new Button("Copy Path");
        copyPathButton.getStyleClass().add("toolbar-button");
        copyPathButton.setId("copyPathButton");
        copyPathButton.setTooltip(new Tooltip("Copy path of selected file/folder"));
        copyPathButton.setOnAction(e -> {
            TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                File selectedFile = selectedItem.getValue();
                ClipboardContent content = new ClipboardContent();
                content.putString(selectedFile.getAbsolutePath());
                Clipboard.getSystemClipboard().setContent(content);
                
                Tooltip tooltip = new Tooltip("Path copied to clipboard!");
                tooltip.setAutoHide(true);
                tooltip.show(copyPathButton, 
                             copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMinX(),
                             copyPathButton.localToScreen(copyPathButton.getBoundsInLocal()).getMaxY());
                
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> tooltip.hide());
                delay.play();
            }
        });
        
        searchResultsBox = new VBox(5);
        searchResultsBox.setPadding(new Insets(5));
        searchResultsBox.getStyleClass().add("search-results-box");
        searchResultsBox.setVisible(false);
        
        searchResultsScrollPane = new ScrollPane(searchResultsBox);
        searchResultsScrollPane.setFitToWidth(true);
        searchResultsScrollPane.setFitToHeight(true);
        searchResultsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        searchResultsScrollPane.setVisible(false);
        
        VBox searchFieldBox = new VBox(5);
        searchFieldBox.setPadding(new Insets(0, 0, 0, 0));
        
        Button searchButton = new Button("Find");
        searchButton.getStyleClass().addAll("toolbar-button", "primary-action");
        searchButton.setOnAction(e -> searchFiles(searchField.getText()));
        
        MenuButton menuButton = new MenuButton();
        menuButton.getStyleClass().addAll("icon-button", "settings-button", "small-settings-button");
        
        Label gearIcon = new Label("⚙");
        gearIcon.getStyleClass().add("small-gear-icon");
        menuButton.setGraphic(gearIcon);
        menuButton.setTooltip(new Tooltip("Preferences"));
        
        MenuItem aboutItem = new MenuItem("About MobApp-Storage Inspector...");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        Menu themeMenu = new Menu("Theme");
        
        RadioMenuItem lightThemeItem = new RadioMenuItem("Light");
        RadioMenuItem darkThemeItem = new RadioMenuItem("Dark");
        
        ToggleGroup themeToggleGroup = new ToggleGroup();
        lightThemeItem.setToggleGroup(themeToggleGroup);
        darkThemeItem.setToggleGroup(themeToggleGroup);
        
        boolean isDarkMode = scene != null && scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("dark-theme.css"));
        lightThemeItem.setSelected(!isDarkMode);
        darkThemeItem.setSelected(isDarkMode);
        
        lightThemeItem.setOnAction(e -> toggleTheme(false));
        darkThemeItem.setOnAction(e -> toggleTheme(true));
        
        themeMenu.getItems().addAll(lightThemeItem, darkThemeItem);
        
        menuButton.getItems().addAll(aboutItem, new SeparatorMenuItem(), themeMenu);
        
        HBox searchInputBox = new HBox(5);
        searchInputBox.getChildren().addAll(
            searchField, 
            searchButton,
            copyPathButton,
            menuButton
        );
        
        searchFieldBox.getChildren().addAll(searchInputBox, searchModeBox);
        
        VBox searchContainer = new VBox(5);
        searchContainer.getChildren().add(searchBox);
        
        searchBox.getChildren().add(searchFieldBox);
        
        return searchContainer;
    }
    

    private void updateBreadcrumbs(File file) {
        breadcrumbBar.getChildren().clear();
        breadcrumbs.clear();
        
        TreeItem<File> rootItem = treeView.getRoot();
        if (rootItem == null || rootItem.getValue() == null) {
            return;
        }
        
        File rootDir = rootItem.getValue();
        
        List<File> pathSegments = new ArrayList<>();
        File current = file;
        
        while (current != null) {
            pathSegments.add(0, current);
            
            if (current.equals(rootDir)) {
                break;
            }
            
            current = current.getParentFile();
        }
        
        Button rootButton = new Button();
        rootButton.getStyleClass().addAll("icon-button", "breadcrumb-home");
        ImageView folderIconView = new ImageView(this.folderIcon);
        folderIconView.setFitWidth(16);
        folderIconView.setFitHeight(16);
        rootButton.setGraphic(folderIconView);
        rootButton.setTooltip(new Tooltip("Root Folder"));
        rootButton.setOnAction(e -> navigateToFolder(rootDir));
        breadcrumbBar.getChildren().add(rootButton);
        
        Label firstSeparator = new Label("/");
        firstSeparator.getStyleClass().add("breadcrumb-separator");
        breadcrumbBar.getChildren().add(firstSeparator);
        
        for (int i = 0; i < pathSegments.size(); i++) {
            File segment = pathSegments.get(i);
            
            if (i == 0 && segment.getParentFile() == null) {
                continue;
            }
            
            Hyperlink link = new Hyperlink(segment.getName().isEmpty() ? segment.getPath() : segment.getName());
            link.getStyleClass().add("breadcrumb-link");
            
            // Set the action to navigate to this path segment
            final File targetFile = segment;
            link.setOnAction(e -> navigateToFolder(targetFile));
            
            // Add tooltip with full path
            link.setTooltip(new Tooltip(segment.getAbsolutePath()));
            
            // Add context menu for additional actions
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem copyPathItem = new MenuItem("Copy Path");
            copyPathItem.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(segment.getAbsolutePath());
                Clipboard.getSystemClipboard().setContent(content);
            });
            
            MenuItem openInNewWindowItem = new MenuItem("Open in System Explorer");
            openInNewWindowItem.setOnAction(e -> {
                try {
                    // Use Desktop API instead of Runtime.exec for better security and cross-platform support
                    File fileToOpen = segment;
                    if (fileToOpen.exists()) {
                        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                        if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                            desktop.open(fileToOpen);
                        } else if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                            desktop.browse(fileToOpen.toURI());
                        } else {
                            showErrorDialog("System Error", "Opening files in system explorer is not supported on this platform.");
                        }
                    }
                } catch (Exception ex) {
                    showErrorDialog("Error", "Could not open file in system explorer: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            
            contextMenu.getItems().addAll(copyPathItem, openInNewWindowItem);
            link.setContextMenu(contextMenu);
            
            // Add the link to the breadcrumb bar
            breadcrumbs.add(link);
            breadcrumbBar.getChildren().add(link);
            
            // Add separator if not the last segment
            if (i < pathSegments.size() - 1) {
                Label separator = new Label("/");
                separator.getStyleClass().add("breadcrumb-separator");
                breadcrumbBar.getChildren().add(separator);
            }
        }
        
        // Add a dropdown menu for sibling directories if this is a directory
        if (file.isDirectory()) {
            // Add a separator
            Label separator = new Label(" ");
            breadcrumbBar.getChildren().add(separator);
            
            // Create a dropdown button
            MenuButton siblingDirsButton = new MenuButton("▼");
            siblingDirsButton.getStyleClass().add("breadcrumb-dropdown");
            siblingDirsButton.setTooltip(new Tooltip("Show sibling directories"));
            
            // Get parent directory
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                // Get all sibling directories
                File[] siblings = parentDir.listFiles(File::isDirectory);
                if (siblings != null) {
                    // Sort alphabetically
                    Arrays.sort(siblings, Comparator.comparing(File::getName));
                    
                    // Add menu items for each sibling
                    for (File sibling : siblings) {
                        if (!sibling.isHidden()) {
                            MenuItem item = new MenuItem(sibling.getName());
                            item.setOnAction(e -> navigateToFolder(sibling));
                            siblingDirsButton.getItems().add(item);
                        }
                    }
                    
                    // Add the dropdown to the breadcrumb bar if it has items
                    if (!siblingDirsButton.getItems().isEmpty()) {
                        breadcrumbBar.getChildren().add(siblingDirsButton);
                    }
                }
            }
        }
    }
    
    /**
     * Opens a folder selection dialog and navigates to the selected folder.
     */
    private void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");
        
        // Set initial directory to current directory if available
        TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            File selectedFile = selectedItem.getValue();
            if (selectedFile.isDirectory()) {
                directoryChooser.setInitialDirectory(selectedFile);
            } else if (selectedFile.getParentFile() != null) {
                directoryChooser.setInitialDirectory(selectedFile.getParentFile());
            }
        }
        
        // Show the dialog and get the selected directory
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory != null) {
            navigateToFolder(selectedDirectory);
        }
    }
    
    /**
     * Navigates to the user's home directory.
     */
    private void navigateToHome() {
        File homeDir = new File(System.getProperty("user.home"));
        navigateToFolder(homeDir);
    }
    
    /**
     * Navigates up one level in the directory hierarchy.
     */
    private void navigateUp() {
        // Get the current root directory
        TreeItem<File> rootItem = treeView.getRoot();
        if (rootItem != null && rootItem.getValue() != null) {
            File currentDir = rootItem.getValue();
            File parentFile = currentDir.getParentFile();
            
            // If there's a parent directory, navigate to it
            if (parentFile != null && parentFile.exists() && parentFile.isDirectory()) {
                navigateToFolder(parentFile);
            }
        }
    }
    
    /**
     * Refreshes the current folder view to show any changes made outside the application.
     */
    private void refreshCurrentFolder() {
        // Get the current root directory
        TreeItem<File> rootItem = treeView.getRoot();
        if (rootItem != null && rootItem.getValue() != null) {
            File currentDir = rootItem.getValue();
            
            // Only refresh if it's a directory
            if (currentDir.isDirectory()) {
                // Clear existing children
                rootItem.getChildren().clear();
                
                // Reload the directory contents
                loadDirectoryContents(rootItem);
                
                // Expand the root to show the refreshed contents
                rootItem.setExpanded(true);
            }
        }
    }
    
    /**
     * Searches for files in the current directory tree.
     * 
     * @param searchText the text to search for
     */
    private void searchFiles(String searchText) {
        // Clear previous search results
        searchResultsBox.getChildren().clear();
        searchResultsBox.setVisible(false);
        searchResultsScrollPane.setVisible(false);
        
        if (searchText == null || searchText.trim().isEmpty()) {
            refreshCurrentFolder(); // Reset to normal view
            treeView.setVisible(true); // Make sure tree is visible
            return;
        }
        
        // Get the current root directory
        TreeItem<File> rootItem = treeView.getRoot();
        if (rootItem == null || rootItem.getValue() == null) {
            return;
        }
        
        File rootDir = rootItem.getValue();
        
        // If content search is enabled, perform content search
        if (isSearchingContent) {
            // Show progress indicator in the search results area
            searchResultsBox.getChildren().clear();
            searchResultsBox.setVisible(true);
            searchResultsScrollPane.setVisible(true);
            treeView.setVisible(false);
            
            // Add a label and the progress indicator
            Label searchingLabel = new Label("Searching file contents for \"" + searchText + "\"...");
            searchingLabel.setStyle("-fx-font-weight: bold;");
            
            // Configure progress indicator
            searchProgressIndicator.setVisible(true);
            searchProgressIndicator.setProgress(-1); // Indeterminate progress
            searchProgressIndicator.setPrefSize(30, 30);
            
            // Add to search results box
            HBox progressBox = new HBox(10);
            progressBox.setAlignment(Pos.CENTER);
            progressBox.getChildren().addAll(searchingLabel, searchProgressIndicator);
            searchResultsBox.getChildren().add(progressBox);
            
            // Use a background thread for content search to keep UI responsive
            Thread searchThread = new Thread(() -> {
                List<SearchResult> results = new ArrayList<>();
                
                // Perform the content search
                searchFileContents(rootDir, searchText.toLowerCase(), results);
                
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    // Hide progress indicator
                    searchProgressIndicator.setVisible(false);
                    
                    // Display results
                    if (!results.isEmpty()) {
                        displaySearchResults(results);
                    } else {
                        Label noResultsLabel = new Label("No matches found for '" + searchText + "'");
                        noResultsLabel.getStyleClass().add("no-results-label");
                        searchResultsBox.getChildren().add(noResultsLabel);
                        searchResultsBox.setVisible(true);
                        searchResultsScrollPane.setVisible(true);
                    }
                });
            });
            
            searchThread.setDaemon(true);
            searchThread.start();
        } else {
            // Perform regular filename search
            
            // Clear the current tree and create a new filtered tree
            TreeItem<File> newRoot = createTreeItem(rootDir);
            newRoot.setExpanded(true);
            
            // Perform the search
            searchDirectory(rootDir, searchText.toLowerCase(), newRoot);
            
            // Update the tree view
            treeView.setRoot(newRoot);
        }
    }
    
    /**
     * Recursively searches a directory for files matching the search text.
     * 
     * @param directory the directory to search
     * @param searchText the text to search for
     * @param parentItem the parent tree item to add matching files to
     */
    private void searchDirectory(File directory, String searchText, TreeItem<File> parentItem) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isHidden()) {
                continue;
            }
            
            boolean matches = file.getName().toLowerCase().contains(searchText);
            
            if (matches) {
                TreeItem<File> item = createTreeItem(file);
                parentItem.getChildren().add(item);
                
                // If it's a directory, expand it and continue searching
                if (file.isDirectory()) {
                    item.setExpanded(true);
                    searchDirectory(file, searchText, item);
                }
            } else if (file.isDirectory()) {
                // Continue searching in subdirectories even if the directory name doesn't match
                searchDirectory(file, searchText, parentItem);
            }
        }
    }
    
    /**
     * Navigates to the specified folder.
     * 
     * @param folder the folder to navigate to
     */
    private void navigateToFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            try {
                // Normalize the path for cross-platform compatibility
                folder = folder.getCanonicalFile();
                
                // Clear the current tree first
                treeView.setRoot(null);
                
                // Create a new root item for the selected folder without using createTreeItem
                // to avoid adding a dummy child that would cause duplication
                TreeItem<File> newRoot = new TreeItem<>(folder);
                newRoot.setExpanded(true);
                
                // Set the new root and load its contents
                treeView.setRoot(newRoot);
                loadDirectoryContents(newRoot);
                
                // Update breadcrumbs
                updateBreadcrumbs(folder);
                
                // Make sure the tree is visible and the placeholder is hidden
                treeView.setVisible(true);
                
                // Find the placeholder in the parent StackPane
                StackPane parent = (StackPane) treeView.getParent();
                if (parent != null) {
                    for (javafx.scene.Node node : parent.getChildren()) {
                        if (node != treeView) {
                            node.setVisible(false);
                        }
                    }
                }
                
                // Store the current directory for future reference
                rootDir = folder;
            } catch (IOException e) {
                showErrorDialog("Navigation Error", "Could not navigate to folder: " + e.getMessage());
            }
        }
    }
    
    /**
     * Creates a tree item for the specified file.
     * 
     * @param file the file to create a tree item for
     * @return the created tree item
     */
    private TreeItem<File> createTreeItem(File file) {
        // Create a tree item for the file
        TreeItem<File> item = new TreeItem<>(file);
        
        // Add a dummy child if the file is a directory to enable the expand arrow
        if (file.isDirectory()) {
            item.getChildren().add(new TreeItem<>());
            
            // Set up the expand listener to load children on demand
            item.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue && item.getChildren().size() == 1 && item.getChildren().get(0).getValue() == null) {
                    item.getChildren().clear();
                    loadDirectoryContents(item);
                }
            });
        }
        
        return item;
    }
    
    /**
     * Loads the contents of a directory into a tree item.
     * 
     * @param directoryItem the tree item to load the directory contents into
     */
    private void loadDirectoryContents(TreeItem<File> directoryItem) {
        File directory = directoryItem.getValue();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                // Sort files: directories first, then files, both in alphabetical order
                java.util.Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1;
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1;
                    } else {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });
                
                // Add the files to the tree item
                for (File file : files) {
                    // Skip hidden files
                    if (!file.isHidden()) {
                        directoryItem.getChildren().add(createTreeItem(file));
                    }
                }
            }
        }
    }
    
    /**
     * Gets the appropriate icon for a file based on its type.
     * 
     * @param file the file to get the icon for
     * @return the icon for the file
     */
    private Image getIconForFile(File file) {
        if (file.isDirectory()) {
            return folderIcon;
        } else {
            String fileName = file.getName().toLowerCase();
            if (isTextFile(fileName)) {
                return textFileIcon;
            } else if (isImageFile(fileName)) {
                return imageFileIcon;
            } else if (isDatabaseFile(fileName)) {
                return databaseFileIcon;
            } else {
                return fileIcon;
            }
        }
    }
    
    /**
     * Sets the listener to be called when a file is selected.
     * 
     * @param listener the listener to be called
     */
    public void setOnFileSelectedListener(Consumer<File> listener) {
        this.fileSelectedListener = listener;
    }
    
    /**
     * Checks if a file is a text file based on its extension.
     * 
     * @param fileName the file name
     * @return true if the file is a text file, false otherwise
     */
    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".txt") || fileName.endsWith(".xml") || 
               fileName.endsWith(".plist") || fileName.endsWith(".py") ||
               fileName.endsWith(".java") || fileName.endsWith(".json") ||
               fileName.endsWith(".html") || fileName.endsWith(".css") ||
               fileName.endsWith(".js") || fileName.endsWith(".md") ||
               fileName.endsWith(".log") || fileName.endsWith(".csv") ||
               fileName.endsWith(".properties") || fileName.endsWith(".yml") ||
               fileName.endsWith(".yaml") || fileName.endsWith(".sh") ||
               fileName.endsWith(".bat") || fileName.endsWith(".c") ||
               fileName.endsWith(".cpp") || fileName.endsWith(".h");
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
     * Searches for the given text in file contents.
     * 
     * @param directory the directory to search
     * @param searchText the text to search for
     * @param results the list to add results to
     */
    private void searchFileContents(File directory, String searchText, List<SearchResult> results) {
        // Use a queue to avoid deep recursion which can cause stack overflow
        java.util.Queue<File> directoriesToSearch = new java.util.LinkedList<>();
        directoriesToSearch.add(directory);
        
        int filesProcessed = 0;
        int maxFilesToProcess = 1000; // Limit the number of files to process to prevent UI freezing
        
        while (!directoriesToSearch.isEmpty() && filesProcessed < maxFilesToProcess) {
            File currentDir = directoriesToSearch.poll();
            File[] files = currentDir.listFiles();
            
            if (files == null) {
                continue;
            }
            
            for (File file : files) {
                if (file.isHidden()) {
                    continue;
                }
                
                if (file.isDirectory()) {
                    // Add directory to queue instead of recursing
                    directoriesToSearch.add(file);
                } else if (file.isFile() && file.length() < 10 * 1024 * 1024) { // Limit to 10MB files
                    // Search file contents
                    try {
                        List<String> matchingLines = searchInFile(file, searchText);
                        if (!matchingLines.isEmpty()) {
                            results.add(new SearchResult(file, matchingLines));
                            
                            // Update UI periodically to show progress
                            if (results.size() % 5 == 0) {
                                final int resultCount = results.size();
                                javafx.application.Platform.runLater(() -> {
                                    // Update progress indicator text
                                    searchProgressIndicator.setProgress(-1); // Keep indeterminate
                                    // Update UI with interim results
                                    if (resultCount > 0) {
                                        displaySearchResults(new ArrayList<>(results));
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        // Skip files that can't be read
                        System.err.println("Error reading file: " + file.getAbsolutePath() + " - " + e.getMessage());
                    }
                    
                    filesProcessed++;
                    if (filesProcessed >= maxFilesToProcess) {
                        break;
                    }
                }
            }
        }
        
        // If we hit the file limit, add a note to the results
        if (filesProcessed >= maxFilesToProcess) {
            javafx.application.Platform.runLater(() -> {
                Label limitLabel = new Label("Search limited to " + maxFilesToProcess + " files. Narrow your search or specify a subfolder.");
                limitLabel.setStyle("-fx-text-fill: #cc6600; -fx-font-style: italic;");
                searchResultsBox.getChildren().add(0, limitLabel);
            });
        }
    }
    
    /**
     * Searches for the given text in a file.
     * 
     * @param file the file to search
     * @param searchText the text to search for
     * @return a list of matching lines with context
     */
    private List<String> searchInFile(File file, String searchText) throws IOException {
        List<String> matchingLines = new ArrayList<>();
        
        // For text files, use line-by-line search
        if (isTextFile(file.getName())) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNumber = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    if (line.toLowerCase().contains(searchText)) {
                        // Add line number and content
                        matchingLines.add("Line " + lineNumber + ": " + line.trim());
                        
                        // Limit the number of matches per file to avoid overwhelming the UI
                        if (matchingLines.size() >= 10) {
                            matchingLines.add("... (more matches found)");
                            break;
                        }
                    }
                }
            }
        } 
        // For binary files, use a different approach
        else {
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long offset = 0;
                StringBuilder context = new StringBuilder();
                boolean foundMatch = false;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    // Convert buffer to string for searching
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.ISO_8859_1);
                    
                    if (chunk.toLowerCase().contains(searchText)) {
                        foundMatch = true;
                        matchingLines.add("Binary file contains match at offset: " + offset);
                        
                        // Try to extract some context around the match
                        int matchIndex = chunk.toLowerCase().indexOf(searchText);
                        int contextStart = Math.max(0, matchIndex - 20);
                        int contextEnd = Math.min(chunk.length(), matchIndex + searchText.length() + 20);
                        
                        // Extract printable characters for context
                        context.setLength(0);
                        for (int i = contextStart; i < contextEnd; i++) {
                            char c = chunk.charAt(i);
                            if (c >= 32 && c <= 126) { // Printable ASCII
                                context.append(c);
                            } else {
                                context.append('.');
                            }
                        }
                        
                        matchingLines.add("Context: \"" + context.toString() + "\"");
                        break; // Just report the first match for binary files
                    }
                    
                    offset += bytesRead;
                }
                
                if (foundMatch) {
                    matchingLines.add("Note: This is a binary file. Content may not display correctly.");
                }
            }
        }
        
        return matchingLines;
    }
    
    /**
     * Displays search results in the UI.
     * 
     * @param results the search results to display
     */
    private void displaySearchResults(List<SearchResult> results) {
        searchResultsBox.getChildren().clear();
        
        // Add a header with result count and clear button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label headerLabel = new Label("Search Results (" + results.size() + " files)");
        headerLabel.getStyleClass().add("search-results-header");
        headerLabel.setStyle("-fx-font-weight: bold;");
        
        Button clearResultsButton = new Button("Clear");
        clearResultsButton.setOnAction(e -> {
            searchResultsBox.getChildren().clear();
            searchResultsBox.setVisible(false);
            searchResultsScrollPane.setVisible(false);
            
            // Show the tree view again
            treeView.setVisible(true);
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerBox.getChildren().addAll(headerLabel, spacer, clearResultsButton);
        searchResultsBox.getChildren().add(headerBox);
        
        // Add a separator
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        searchResultsBox.getChildren().add(separator);
        
        // If no results, show a message
        if (results.isEmpty()) {
            Label noResultsLabel = new Label("No matches found");
            noResultsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            searchResultsBox.getChildren().add(noResultsLabel);
        } else {
            // Add results for each file
            for (SearchResult result : results) {
                // Create a flat panel for each file instead of a titled pane
                VBox filePanel = new VBox(5);
                filePanel.setPadding(new Insets(10, 5, 10, 5));
                filePanel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 5;");
                
                // Add file name as a header
                Label fileNameLabel = new Label(result.getFile().getName());
                fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                
                // Add file path
                Label pathLabel = new Label("Path: " + result.getFile().getParentFile().getAbsolutePath());
                pathLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666666; -fx-font-size: 11px;");
                
                // Create a VBox to hold the matching lines
                VBox matchesBox = new VBox(5);
                matchesBox.setPadding(new Insets(5));
                
                // Add file name and path
                filePanel.getChildren().addAll(fileNameLabel, pathLabel);
                
                // Add a separator
                Separator lineSeparator = new Separator();
                lineSeparator.setPadding(new Insets(2, 0, 5, 0));
                filePanel.getChildren().add(lineSeparator);
                
                // Create a label for match count
                Label matchCountLabel = new Label(result.getMatchingLines().size() + " matches found");
                matchCountLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #007700;");
                filePanel.getChildren().add(matchCountLabel);
                
                // Add each matching line (limited to 5 for better display)
                int displayLimit = Math.min(5, result.getMatchingLines().size());
                for (int i = 0; i < displayLimit; i++) {
                    String line = result.getMatchingLines().get(i);
                    Label lineLabel = new Label("• " + line);
                    lineLabel.setWrapText(true);
                    lineLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
                    filePanel.getChildren().add(lineLabel);
                }
                
                // If there are more matches, show a "more" label
                if (result.getMatchingLines().size() > 5) {
                    Label moreLabel = new Label("... and " + (result.getMatchingLines().size() - 5) + " more matches");
                    moreLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666666;");
                    filePanel.getChildren().add(moreLabel);
                }
                
                // Add buttons in a horizontal box
                HBox buttonBox = new HBox(10);
                buttonBox.setPadding(new Insets(10, 0, 0, 0));
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                
                // Add a button to open the file
                Button openButton = new Button("Open File");
                openButton.setOnAction(e -> {
                    if (fileSelectedListener != null) {
                        fileSelectedListener.accept(result.getFile());
                    }
                    
                    // Find and select the file in the tree
                    selectFileInTree(result.getFile());
                });
                
                // Add a button to copy the file path
                Button copyPathButton = new Button("Copy Path");
                copyPathButton.setOnAction(e -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(result.getFile().getAbsolutePath());
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
                
                buttonBox.getChildren().addAll(copyPathButton, openButton);
                filePanel.getChildren().add(buttonBox);
                
                // Add spacing between file panels
                Region fileSpacer = new Region();
                fileSpacer.setPrefHeight(10);
                
                // Add the file panel to the results box
                searchResultsBox.getChildren().addAll(filePanel, fileSpacer);
            }
        }
        
        // Show the results and hide the tree view
        searchResultsBox.setVisible(true);
        searchResultsScrollPane.setVisible(true);
        
        // Hide the tree view when showing search results
        treeView.setVisible(false);
    }
    
    /**
     * Selects a file in the tree view.
     * 
     * @param file the file to select
     */
    private void selectFileInTree(File file) {
        // Navigate to the parent directory
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            navigateToFolder(parentDir);
            
            // Find and select the file in the tree
            TreeItem<File> rootItem = treeView.getRoot();
            if (rootItem != null) {
                for (TreeItem<File> item : rootItem.getChildren()) {
                    if (item.getValue().equals(file)) {
                        treeView.getSelectionModel().select(item);
                        treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * A class to represent a search result.
     */
    private static class SearchResult {
        private final File file;
        private final List<String> matchingLines;
        
        public SearchResult(File file, List<String> matchingLines) {
            this.file = file;
            this.matchingLines = matchingLines;
        }
        
        public File getFile() {
            return file;
        }
        
        public List<String> getMatchingLines() {
            return matchingLines;
        }
    }
    
    /**
     * Shows an error dialog with the specified title and message
     * 
     * @param title The dialog title
     * @param message The error message
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        Scene scene = alert.getDialogPane().getScene();
        scene.getStylesheets().clear();
        if (isDarkTheme) {
            scene.getStylesheets().add(getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/mobapp/inspector/css/modern-style.css").toExternalForm());
        }
        
        alert.showAndWait();
    }
    
    private void openUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URI uri = new java.net.URI(url);
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(uri);
                } else {
                    showErrorDialog("Browser Error", "Could not open the default browser on this platform.");
                }
            } else {
                showErrorDialog("Invalid URL", "The URL format is invalid: " + url);
            }
        } catch (Exception ex) {
            showErrorDialog("Error", "Could not open URL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    

    private void showAboutDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("About MobApp-Storage Inspector");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setMinWidth(450);
        content.setMaxWidth(450);
        
        Label titleLabel = new Label("MobApp-Storage Inspector");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        
        Label versionLabel = new Label("Version 1.0");
        versionLabel.setStyle("-fx-font-size: 14px;");
        
        Label descriptionLabel = new Label("A tool for inspecting and analyzing mobile application storage files.");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descriptionLabel.setStyle("-fx-font-size: 14px;");
        
        Hyperlink issueLink = new Hyperlink("Report an issue");
        issueLink.setStyle("-fx-font-size: 14px;");
        issueLink.setOnAction(e -> openUrl("https://github.com/thecybersandeep/mobapp-storage-inspector/issues/new"));
        

        
        content.getChildren().addAll(
            titleLabel,
            versionLabel,
            new Separator(),
            descriptionLabel,
            issueLink
        );
        
        // Set the dialog content
        dialog.getDialogPane().setContent(content);
        
        // Add a close button
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Apply the current theme to the dialog
        if (scene != null && scene.getStylesheets().stream().anyMatch(s -> s.contains("dark-theme.css"))) {
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm()
            );
        }
        
        // Show the dialog
        dialog.showAndWait();
    }
    
    /**
     * Toggles between light and dark themes with a smooth fade animation
     * 
     * @param darkMode true to enable dark mode, false for light mode
     */
    private void toggleTheme(boolean darkMode) {
        if (scene != null) {
            String darkThemePath = getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm();
            
            // Create a fade transition for smooth theme switching
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), this);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.9);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), this);
            fadeIn.setFromValue(0.9);
            fadeIn.setToValue(1.0);
            
            // Apply the theme change during the transition
            fadeOut.setOnFinished(e -> {
                if (darkMode) {
                    if (!scene.getStylesheets().contains(darkThemePath)) {
                        scene.getStylesheets().add(darkThemePath);
                    }
                } else {
                    scene.getStylesheets().remove(darkThemePath);
                }
                
                // Start the fade-in transition
                fadeIn.play();
            });
            
            // Start the fade-out transition
            fadeOut.play();
        }
    }
}