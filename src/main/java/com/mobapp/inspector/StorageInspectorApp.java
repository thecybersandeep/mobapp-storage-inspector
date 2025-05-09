package com.mobapp.inspector;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.mobapp.inspector.ui.FileSystemTreeView;
import com.mobapp.inspector.ui.ContentViewPane;
import com.mobapp.inspector.ui.ThemeIcons;

public class StorageInspectorApp extends Application {

    private static final String APP_TITLE = "MobApp-Storage Inspector";
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    private static final double DIVIDER_POSITION = 0.2;

    private boolean isDarkMode = false;
    private Scene scene;
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        root.setTop(null);
        
        SplitPane splitPane = new SplitPane();
        splitPane.getStyleClass().add("main-split-pane");
        
        FileSystemTreeView fileSystemTreeView = new FileSystemTreeView();
        fileSystemTreeView.setMinWidth(400);
        fileSystemTreeView.setPrefWidth(450);
        
        ContentViewPane contentViewPane = new ContentViewPane();
        
        fileSystemTreeView.setOnFileSelectedListener(contentViewPane::displayFile);
        
        splitPane.getItems().addAll(fileSystemTreeView, contentViewPane);
        
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            double minPosition = Math.max(0.3, 400 / splitPane.getWidth());
            if (newVal.doubleValue() < minPosition) {
                splitPane.setDividerPosition(0, minPosition);
            }
        });
        
        root.setCenter(splitPane);
        
        scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        scene.getStylesheets().add(getClass().getResource("/com/mobapp/inspector/css/modern-style.css").toExternalForm());
        
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        Platform.runLater(() -> {
            double stageWidth = primaryStage.getWidth();
            double leftPanelWidth = Math.max(450, stageWidth * 0.32);
            double dividerPosition = leftPanelWidth / stageWidth;
            splitPane.setDividerPosition(0, dividerPosition);
        });
    }
    
    private void updateTheme() {
        if (isDarkMode) {
            if (!scene.getStylesheets().contains(getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm())) {
                scene.getStylesheets().add(getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm());
            }
        } else {
            scene.getStylesheets().remove(getClass().getResource("/com/mobapp/inspector/css/dark-theme.css").toExternalForm());
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}