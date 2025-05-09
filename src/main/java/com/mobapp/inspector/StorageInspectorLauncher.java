package com.mobapp.inspector;

/**
 * Main launcher class for the Storage Inspector application.
 * This class serves as the entry point for the fat JAR and handles
 * proper initialization of JavaFX across different platforms.
 */
public class StorageInspectorLauncher {
    
    /**
     * Main method that launches the JavaFX application.
     * This method is designed to work with the fat JAR packaging
     * and handles the proper initialization of JavaFX.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Launch the JavaFX application
        StorageInspectorApp.main(args);
    }
}