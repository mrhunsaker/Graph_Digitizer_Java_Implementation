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

package com.digitizer.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Utility class for loading images from disk.
 * <p>
 * Prefer {@link #loadImage(File)} which first tries JavaFX's {@code Image}
 * to read common formats (PNG/JPG/BMP) and falls back to {@link ImageIO}
 * for extended formats (TIFF, WebP). The utility also contains convenience
 * methods for obtaining image dimensions.
 */
public final class ImageLoader {

    private ImageLoader() {
        // Utility class, should not be instantiated
    }

    /**
     * Loads an image from the specified file.
     *
         * @param file the image file (PNG, JPEG, BMP, TIFF, WebP, etc.)
     * @return the loaded JavaFX Image
     * @throws IOException if the file cannot be read or is not a valid image
     */
    public static Image loadImage(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist: " + file);
        }

        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + file);
        }

            // Try JavaFX Image first (fast path for PNG/JPEG/BMP)
            Image image = null;
            try {
                try (FileInputStream fis = new FileInputStream(file)) {
                    image = new Image(fis);
                    if (!image.isError()) {
                        return image;
                    }
                }
            } catch (Exception e) {
                // Fall through to ImageIO
            }

            // Fallback to ImageIO for TIFF/WebP and other formats
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage != null) {
                    return SwingFXUtils.toFXImage(bufferedImage, null);
                }
            } catch (IOException e) {
                throw new IOException("Could not read image: " + file.getName(), e);
        }

            throw new IOException("Unsupported or corrupted image format: " + file.getName());
    }

    /**
     * Loads an image from the specified file path.
     *
     * @param filePath the path to the image file
     * @return the loaded JavaFX Image
     * @throws IOException if the file cannot be read or is not a valid image
     */
    public static Image loadImage(String filePath) throws IOException {
        return loadImage(new File(filePath));
    }

    /**
     * Gets the width of an image in pixels.
     *
     * @param image the image
     * @return the width, or 0 if image is null
     */
    public static int getImageWidth(Image image) {
        return (image != null) ? (int) image.getWidth() : 0;
    }

    /**
     * Gets the height of an image in pixels.
     *
     * @param image the image
     * @return the height, or 0 if image is null
     */
    public static int getImageHeight(Image image) {
        return (image != null) ? (int) image.getHeight() : 0;
    }
}
