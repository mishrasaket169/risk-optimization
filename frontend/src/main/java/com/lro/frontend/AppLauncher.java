package com.lro.frontend;

/**
 * Main entry point for launching JavaFX Desktop Applications without requiring
 * module system command line flags on standard JVMs.
 */
public class AppLauncher {
    public static void main(String[] args) {
        MainApp.launch(MainApp.class, args);
    }
}
