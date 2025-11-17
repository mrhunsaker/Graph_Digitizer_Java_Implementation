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

package com.digitizer.integration;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.digitizer.image.ImageLoader;

import javafx.scene.image.Image;

/**
 * Integration tests for loading various image formats from test resources.
 * Tests that the application can handle PNG, JPEG, WebP, BMP, and TIFF formats.
 */
class ImageLoadingTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "images/BarGraph1.png",
            "images/LineChart1.png",
            "images/LineChart2.png",
            "images/LineGraph.png",
            "images/LineGraph_fullquality.jpeg",
            "images/LineGraph_midquality.jpeg",
            "images/LineGraph.bmp",
            "images/LineGraph.tiff",
            "images/LineChart3.webp",
            "images/edge_small_50x50.png",
            "images/edge_large_4000x3000.png",
            "images/edge_tall_100x2000.png",
            "images/edge_wide_3000x150.png",
            "images/edge_high_dpi_1000x1000.png",
            "images/edge_square_800x800.png",
            "images/edge_single_1x1.png"
    })
    void testLoadTestImages(String imagePath) {
        // Load image from test resources
        URL resource = getClass().getClassLoader().getResource(imagePath);
        assertNotNull(resource, "Image resource should exist: " + imagePath);

        try (InputStream is = resource.openStream()) {
                // Save to temp file to use ImageLoader (which supports TIFF/WebP)
                File tempFile = File.createTempFile("test_image_", imagePath.substring(imagePath.lastIndexOf(".")));
                tempFile.deleteOnExit();
                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
                Image image = ImageLoader.loadImage(tempFile);
            assertNotNull(image, "Image should be loaded");
            assertFalse(image.isError(), "Image should not have errors: " + imagePath);
            assertTrue(image.getWidth() > 0, "Image width should be positive");
            assertTrue(image.getHeight() > 0, "Image height should be positive");
        } catch (Exception e) {
            fail("Failed to load image " + imagePath + ": " + e.getMessage());
        }
    }

    @Test
    void testImageDimensions() {
        // Test that we can read dimensions from a known test image
        URL resource = getClass().getClassLoader().getResource("images/LineGraph.png");
        assertNotNull(resource);

        try (InputStream is = resource.openStream()) {
            Image image = new Image(is);
            assertTrue(image.getWidth() > 100, "Test image should have reasonable width");
            assertTrue(image.getHeight() > 100, "Test image should have reasonable height");
        } catch (Exception e) {
            fail("Failed to read image dimensions: " + e.getMessage());
        }
    }

    @Test
    void testImageQualityVariations() {
        // Verify that both high and mid quality JPEGs load successfully
        String[] jpegVariants = {
                "images/LineGraph_fullquality.jpeg",
                "images/LineGraph_midquality.jpeg"
        };

        for (String path : jpegVariants) {
            URL resource = getClass().getClassLoader().getResource(path);
            assertNotNull(resource, "JPEG variant should exist: " + path);

            try (InputStream is = resource.openStream()) {
                Image image = new Image(is);
                assertFalse(image.isError(), "JPEG should load without error: " + path);
            } catch (Exception e) {
                fail("Failed to load JPEG variant " + path + ": " + e.getMessage());
            }
        }
    }

    @Test
    void testDifferentGraphTypes() {
        // Verify different graph types load correctly
        String barGraph = "images/BarGraph1.png";
        String lineChart = "images/LineChart1.png";

        URL barResource = getClass().getClassLoader().getResource(barGraph);
        URL lineResource = getClass().getClassLoader().getResource(lineChart);

        assertNotNull(barResource, "Bar graph should exist");
        assertNotNull(lineResource, "Line chart should exist");

        try (InputStream barIs = barResource.openStream();
             InputStream lineIs = lineResource.openStream()) {

            Image barImage = new Image(barIs);
            Image lineImage = new Image(lineIs);

            assertFalse(barImage.isError(), "Bar graph should load");
            assertFalse(lineImage.isError(), "Line chart should load");
        } catch (Exception e) {
            fail("Failed to load graph types: " + e.getMessage());
        }
    }
}
