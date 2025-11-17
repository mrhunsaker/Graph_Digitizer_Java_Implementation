/*
 * Copyright 2025 Michael Ryan Hunsaker, M.Ed., Ph.D.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitizer.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javafx.scene.Scene;

/**
 * Manages theme selection and application for the Graph Digitizer application.
 * Applies themes using JavaFX CSS stylesheets instead of Swing Look and Feel.
 */
public class ThemeManager {

    private static final Map<String, String> THEMES = new HashMap<>();
    private static Scene currentScene;
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".graph-digitizer", "config.properties");
    private static final String THEME_KEY = "theme";

    static {
        // Define available themes with their CSS characteristics
        THEMES.put("Light", "-fx-base: #f5f5f5; -fx-control-inner-background: #ffffff; -fx-text-fill: #000000;");
        THEMES.put("Dark", "-fx-base: #2b2b2b; -fx-control-inner-background: #1e1e1e; -fx-text-fill: #ffffff;");
        THEMES.put("Darcula", "-fx-base: #3c3f41; -fx-control-inner-background: #2b2d30; -fx-text-fill: #a9b7c6;");
        THEMES.put("Dracula", "-fx-base: #282a36; -fx-control-inner-background: #21222c; -fx-text-fill: #f8f8f2;");
        THEMES.put("Material Dark", "-fx-base: #121212; -fx-control-inner-background: #1e1e1e; -fx-text-fill: #ffffff;");
        THEMES.put("Nord", "-fx-base: #2e3440; -fx-control-inner-background: #3b4252; -fx-text-fill: #eceff4;");
        THEMES.put("Solarized Light", "-fx-base: #fdf6e3; -fx-control-inner-background: #ffffff; -fx-text-fill: #657b83;");
        THEMES.put("Solarized Dark", "-fx-base: #002b36; -fx-control-inner-background: #073642; -fx-text-fill: #839496;");
        THEMES.put("One Dark", "-fx-base: #282c34; -fx-control-inner-background: #21252b; -fx-text-fill: #abb2bf;");
        THEMES.put("Arc", "-fx-base: #f5f6f7; -fx-control-inner-background: #ffffff; -fx-text-fill: #2c3e50;");
        THEMES.put("Arc Dark", "-fx-base: #383c4a; -fx-control-inner-background: #2f333d; -fx-text-fill: #c1c1c1;");
        THEMES.put("Atom One Light", "-fx-base: #fafafa; -fx-control-inner-background: #ffffff; -fx-text-fill: #383a42;");
        THEMES.put("Atom One Dark", "-fx-base: #282c34; -fx-control-inner-background: #21252b; -fx-text-fill: #abb2bf;");
        THEMES.put("Gruvbox Dark", "-fx-base: #282828; -fx-control-inner-background: #3c3836; -fx-text-fill: #ebdbb2;");
    }

    /**
     * Sets the current scene for theme application.
     *
     * @param scene the JavaFX scene to apply themes to
     */
    public static void setScene(Scene scene) {
        currentScene = scene;
        // On scene set, try to apply persisted theme if present
        String persisted = loadPersistedTheme();
        if (persisted != null && THEMES.containsKey(persisted)) {
            applyTheme(persisted);
        }
    }

    /**
     * Gets all available themes.
     *
     * @return list of available themes sorted alphabetically
     */
    public static List<String> getAvailableThemes() {
        List<String> themes = new ArrayList<>(THEMES.keySet());
        themes.sort(String::compareTo);
        return themes;
    }

    /**
     * Applies a theme by name using CSS styling.
     *
     * @param themeName the name of the theme to apply
     * @return true if theme was successfully applied, false otherwise
     */
    public static boolean applyTheme(String themeName) {
        if (currentScene == null) {
            System.err.println("Scene not set. Call setScene() first.");
            return false;
        }

        String themeStyle = THEMES.get(themeName);
        if (themeStyle == null) {
            System.err.println("Theme not found: " + themeName);
            return false;
        }

        try {
            // Apply the theme by setting inline CSS on the root
            currentScene.getRoot().setStyle(themeStyle);
            // persist selection
            persistTheme(themeName);
            System.out.println("Applied theme: " + themeName);
            return true;
        } catch (Exception e) {
            System.err.println("Error applying theme: " + themeName + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current theme colors as a style string.
     *
     * @return the CSS style string of the current theme
     */
    public static String getCurrentThemeStyle() {
        if (currentScene != null && currentScene.getRoot() != null) {
            return currentScene.getRoot().getStyle();
        }
        return "";
    }

    private static void persistTheme(String themeName) {
        try {
            Path dir = CONFIG_PATH.getParent();
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Properties p = new Properties();
            // If config exists, load it first to preserve other keys
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                    p.load(in);
                }
            }
            p.setProperty(THEME_KEY, themeName);
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, "Graph Digitizer configuration");
            }
        } catch (IOException e) {
            System.err.println("Failed to persist theme: " + e.getMessage());
        }
    }

    private static String loadPersistedTheme() {
        try {
            if (!Files.exists(CONFIG_PATH)) return null;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                p.load(in);
            }
            return p.getProperty(THEME_KEY);
        } catch (IOException e) {
            System.err.println("Failed to load persisted theme: " + e.getMessage());
            return null;
        }
    }
}
