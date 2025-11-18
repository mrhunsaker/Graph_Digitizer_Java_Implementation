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

package com.digitizer.core;

import javafx.scene.paint.Color;

/**
 * Utility class for color operations including hex parsing, RGB distance,
 * and blending. This class is used by image processing code such as
 * {@link com.digitizer.image.AutoTracer} to compare pixel colors to a
 * dataset color.
 *
 * <p>All helper methods are static and the class is not instantiable.
 */
public final class ColorUtils {

    private ColorUtils() {
        // Utility class, should not be instantiated
    }

    /**
     * Parses a hex color string to a JavaFX Color.
     * Supports formats: "#RRGGBB", "RRGGBB", "#RGB", "RGB"
     *
     * @param hex the hex color string
     * @return the parsed Color, or Color.BLACK if parsing fails
     */
    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return Color.BLACK;
        }

        String h = hex.trim();
        if (h.startsWith("#")) {
            h = h.substring(1);
        }

        try {
            if (h.length() == 3) {
                // Expand shorthand: "f80" -> "ff8800"
                h = "" + h.charAt(0) + h.charAt(0) + 
                    h.charAt(1) + h.charAt(1) + 
                    h.charAt(2) + h.charAt(2);
            }

            if (h.length() == 6) {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                return Color.rgb(r, g, b);
            }
        } catch (NumberFormatException e) {
            // Fall through to return black
        }

        return Color.BLACK;
    }

    /**
     * Converts a JavaFX Color to a hex string.
     *
     * @param color the color to convert
     * @return hex string in format "#RRGGBB"
     */
    public static String colorToHex(Color color) {
        if (color == null) {
            return "#000000";
        }
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Calculates the Euclidean distance between two RGB colors.
     * Each component is expected to be in the range [0, 1].
     *
     * @param r1 red component of first color
     * @param g1 green component of first color
     * @param b1 blue component of first color
     * @param r2 red component of second color
     * @param g2 green component of second color
     * @param b2 blue component of second color
     * @return Euclidean distance in RGB space
     */
    public static double colorDistance(double r1, double g1, double b1,
                                      double r2, double g2, double b2) {
        double dr = r1 - r2;
        double dg = g1 - g2;
        double db = b1 - b2;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Calculates the Euclidean distance between two JavaFX Color objects.
     *
     * @param color1 first color
     * @param color2 second color
     * @return Euclidean distance in RGB space
     */
    public static double colorDistance(Color color1, Color color2) {
        if (color1 == null || color2 == null) {
            return 0.0;
        }
        return colorDistance(
                color1.getRed(), color1.getGreen(), color1.getBlue(),
                color2.getRed(), color2.getGreen(), color2.getBlue()
        );
    }

    /**
     * Blends two colors together using linear interpolation.
     *
     * @param color1 first color
     * @param color2 second color
     * @param t blending factor (0.0 = color1, 1.0 = color2)
     * @return the blended color
     */
    public static Color blendColors(Color color1, Color color2, double t) {
        if (color1 == null) color1 = Color.BLACK;
        if (color2 == null) color2 = Color.BLACK;

        t = Math.max(0.0, Math.min(1.0, t));

        double r = color1.getRed() * (1 - t) + color2.getRed() * t;
        double g = color1.getGreen() * (1 - t) + color2.getGreen() * t;
        double b = color1.getBlue() * (1 - t) + color2.getBlue() * t;
        double a = color1.getOpacity() * (1 - t) + color2.getOpacity() * t;

        return Color.color(r, g, b, a);
    }

    /**
     * Helper to ensure a hex color is normalized and returned as a 6-character
     * hex string preceded by '#'. If the input is shorthand ("RGB") it will
     * be expanded. Returns "#000000" on error.
     *
     * @param hex input color string, may be null
     * @return normalized hex string like "#RRGGBB"
     */
    public static String normalizeHex(String hex) {
        Color c = hexToColor(hex);
        return colorToHex(c);
    }
}
