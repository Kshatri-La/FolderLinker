package com.filelinker;

/**
 * Custom Launcher class.
 * This is required to bypass a strict Java module system restriction when compiling
 * a JavaFX 11+ application into a "fat JAR" for wrapping into an .exe file.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
