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
import java.util.Properties;

/**
 * Manages accessibility preferences for the Graph Digitizer application.
 * Stores user preferences for font size, point size, focus indicator style,
 * and high contrast mode.
 */
public class AccessibilityPreferences {

    private static final Path CONFIG_PATH = Paths.get(
        System.getProperty("user.home"), 
        ".graph-digitizer", 
        "accessibility.properties"
    );

    public enum FontSize {
        SMALL(10, 12, 14),
        NORMAL(12, 14, 16),
        LARGE(14, 16, 18),
        EXTRA_LARGE(16, 18, 20);

        public final double body, label, heading;

        FontSize(double body, double label, double heading) {
            this.body = body;
            this.label = label;
            this.heading = heading;
        }
    }

    public enum PointSize {
        SMALL(4),
        NORMAL(6),
        LARGE(8),
        EXTRA_LARGE(12);

        public final double size;

        PointSize(double size) {
            this.size = size;
        }
    }

    private FontSize fontSize = FontSize.NORMAL;
    private PointSize pointSize = PointSize.NORMAL;
    private boolean useShapeVariation = true;
    private boolean highContrastMode = false;
    private double focusBorderWidth = 3.0;
    // Palette preference (name and comma-separated hex colors)
    private String paletteName = "";
    private String paletteColorsCsv = "";
    // Persist per-dataset colors as comma-separated hex values
    private String datasetColorsCsv = "";
    private String datasetVisibilitiesCsv = "";

    public AccessibilityPreferences() {
        load();
    }

    /**
     * Returns the stored palette name (may be empty).
     */
    public String getPaletteName() {
        return paletteName == null ? "" : paletteName;
    }

    /**
     * Returns the stored palette colors as an array of hex strings, or null if none.
     */
    public String[] getPaletteColors() {
        if (paletteColorsCsv == null || paletteColorsCsv.trim().isEmpty()) return null;
        return paletteColorsCsv.split(",");
    }

    /**
     * Returns stored per-dataset colors as an array of hex strings, or null if none.
     */
    public String[] getDatasetColors() {
        if (datasetColorsCsv == null || datasetColorsCsv.trim().isEmpty()) return null;
        return datasetColorsCsv.split(",");
    }

    /**
     * Returns stored per-dataset visibility flags as an array of "true"/"false" strings.
     */
    public String[] getDatasetVisibilities() {
        if (datasetVisibilitiesCsv == null || datasetVisibilitiesCsv.trim().isEmpty()) return null;
        return datasetVisibilitiesCsv.split(",");
    }

    /**
     * Sets the palette and persists it. Use an empty or null palette to clear.
     */
    public void setPalette(String name, String[] colors) {
        this.paletteName = name == null ? "" : name;
        if (colors == null || colors.length == 0) {
            this.paletteColorsCsv = "";
        } else {
            // join with commas
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(colors[i]);
            }
            this.paletteColorsCsv = sb.toString();
        }
        save();
    }

    /**
     * Persist per-dataset colors. Pass null or empty to clear.
     */
    public void setDatasetColors(String[] colors) {
        if (colors == null || colors.length == 0) {
            this.datasetColorsCsv = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(colors[i]);
            }
            this.datasetColorsCsv = sb.toString();
        }
        save();
    }

    /**
     * Persist per-dataset visibility flags. Pass null/empty to clear.
     */
    public void setDatasetVisibilities(String[] vis) {
        if (vis == null || vis.length == 0) {
            this.datasetVisibilitiesCsv = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vis.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(vis[i]);
            }
            this.datasetVisibilitiesCsv = sb.toString();
        }
        save();
    }

    public FontSize getFontSize() {
        return fontSize;
    }

    public void setFontSize(FontSize fontSize) {
        this.fontSize = fontSize;
        save();
    }

    public PointSize getPointSize() {
        return pointSize;
    }

    public void setPointSize(PointSize pointSize) {
        this.pointSize = pointSize;
        save();
    }

    public boolean isUseShapeVariation() {
        return useShapeVariation;
    }

    public void setUseShapeVariation(boolean useShapeVariation) {
        this.useShapeVariation = useShapeVariation;
        save();
    }

    public boolean isHighContrastMode() {
        return highContrastMode;
    }

    public void setHighContrastMode(boolean highContrastMode) {
        this.highContrastMode = highContrastMode;
        save();
    }

    public double getFocusBorderWidth() {
        return focusBorderWidth;
    }

    public void setFocusBorderWidth(double focusBorderWidth) {
        this.focusBorderWidth = focusBorderWidth;
        save();
    }

    private void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return;
            }
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            }

            String fontSizeStr = props.getProperty("fontSize", "NORMAL");
            try {
                fontSize = FontSize.valueOf(fontSizeStr);
            } catch (IllegalArgumentException e) {
                fontSize = FontSize.NORMAL;
            }

            String pointSizeStr = props.getProperty("pointSize", "NORMAL");
            try {
                pointSize = PointSize.valueOf(pointSizeStr);
            } catch (IllegalArgumentException e) {
                pointSize = PointSize.NORMAL;
            }

            useShapeVariation = Boolean.parseBoolean(
                props.getProperty("useShapeVariation", "true")
            );
            highContrastMode = Boolean.parseBoolean(
                props.getProperty("highContrastMode", "false")
            );
            focusBorderWidth = Double.parseDouble(
                props.getProperty("focusBorderWidth", "3.0")
            );
            paletteName = props.getProperty("paletteName", "");
            paletteColorsCsv = props.getProperty("paletteColors", "");
            datasetColorsCsv = props.getProperty("datasetColors", "");
            datasetVisibilitiesCsv = props.getProperty("datasetVisibilities", "");
        } catch (IOException e) {
            System.err.println("Failed to load accessibility preferences: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Path dir = CONFIG_PATH.getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Properties props = new Properties();
            props.setProperty("fontSize", fontSize.name());
            props.setProperty("pointSize", pointSize.name());
            props.setProperty("useShapeVariation", String.valueOf(useShapeVariation));
            props.setProperty("highContrastMode", String.valueOf(highContrastMode));
            props.setProperty("focusBorderWidth", String.valueOf(focusBorderWidth));
            props.setProperty("paletteName", paletteName == null ? "" : paletteName);
            props.setProperty("paletteColors", paletteColorsCsv == null ? "" : paletteColorsCsv);
            props.setProperty("datasetColors", datasetColorsCsv == null ? "" : datasetColorsCsv);
            props.setProperty("datasetVisibilities", datasetVisibilitiesCsv == null ? "" : datasetVisibilitiesCsv);

            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "Graph Digitizer Accessibility Preferences");
            }
        } catch (IOException e) {
            System.err.println("Failed to save accessibility preferences: " + e.getMessage());
        }
    }
}
