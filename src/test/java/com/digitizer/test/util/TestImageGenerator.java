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

package com.digitizer.test.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Utility class for generating test images with various edge cases.
 */
public class TestImageGenerator {

    /**
     * Creates a small test image (50x50 pixels).
     */
    public static void generateSmallImage(File output) throws IOException {
        WritableImage image = new WritableImage(50, 50);
        var writer = image.getPixelWriter();
        
        // Simple gradient
        for (int y = 0; y < 50; y++) {
            for (int x = 0; x < 50; x++) {
                double ratio = (x + y) / 100.0;
                writer.setColor(x, y, Color.color(ratio, 0.5, 1.0 - ratio));
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates a very large test image (4000x3000 pixels).
     */
    public static void generateLargeImage(File output) throws IOException {
        WritableImage image = new WritableImage(4000, 3000);
        var writer = image.getPixelWriter();
        
        // Grid pattern to keep file size reasonable
        for (int y = 0; y < 3000; y += 10) {
            for (int x = 0; x < 4000; x++) {
                writer.setColor(x, y, Color.BLACK);
            }
        }
        for (int x = 0; x < 4000; x += 10) {
            for (int y = 0; y < 3000; y++) {
                writer.setColor(x, y, Color.BLACK);
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates a tall narrow image (100x2000 pixels).
     */
    public static void generateTallNarrowImage(File output) throws IOException {
        WritableImage image = new WritableImage(100, 2000);
        var writer = image.getPixelWriter();
        
        // Vertical gradient
        for (int y = 0; y < 2000; y++) {
            Color c = Color.hsb((y / 2000.0) * 360, 1.0, 1.0);
            for (int x = 0; x < 100; x++) {
                writer.setColor(x, y, c);
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates a wide short image (3000x150 pixels).
     */
    public static void generateWideShortImage(File output) throws IOException {
        WritableImage image = new WritableImage(3000, 150);
        var writer = image.getPixelWriter();
        
        // Horizontal gradient
        for (int x = 0; x < 3000; x++) {
            Color c = Color.hsb((x / 3000.0) * 360, 0.8, 0.9);
            for (int y = 0; y < 150; y++) {
                writer.setColor(x, y, c);
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates a square high-DPI image (1000x1000 pixels with fine detail).
     */
    public static void generateHighDPIImage(File output) throws IOException {
        WritableImage image = new WritableImage(1000, 1000);
        var writer = image.getPixelWriter();
        
        // Fine checkerboard pattern
        for (int y = 0; y < 1000; y++) {
            for (int x = 0; x < 1000; x++) {
                if ((x / 5 + y / 5) % 2 == 0) {
                    writer.setColor(x, y, Color.WHITE);
                } else {
                    writer.setColor(x, y, Color.BLACK);
                }
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates an image with unusual aspect ratio (square: 800x800).
     */
    public static void generateSquareImage(File output) throws IOException {
        WritableImage image = new WritableImage(800, 800);
        var writer = image.getPixelWriter();
        
        // Circular gradient from center
        int centerX = 400;
        int centerY = 400;
        for (int y = 0; y < 800; y++) {
            for (int x = 0; x < 800; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double ratio = Math.min(1.0, dist / 400.0);
                writer.setColor(x, y, Color.color(ratio, ratio, 1.0));
            }
        }
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }

    /**
     * Creates a minimal 1x1 pixel image (extreme edge case).
     */
    public static void generateSinglePixelImage(File output) throws IOException {
        WritableImage image = new WritableImage(1, 1);
        var writer = image.getPixelWriter();
        writer.setColor(0, 0, Color.RED);
        
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", output);
    }
}
