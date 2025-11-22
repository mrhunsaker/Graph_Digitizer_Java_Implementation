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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.digitizer.test.util.TestImageGenerator;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Integration tests for edge-case image scenarios.
 */
class EdgeCaseImageTest {

    @TempDir
    static Path tempDir;

    private static File smallImage;
    private static File largeImage;
    private static File tallNarrowImage;
    private static File wideShortImage;
    private static File highDPIImage;
    private static File squareImage;
    private static File singlePixelImage;

    @BeforeAll
    static void generateTestImages() throws Exception {
        smallImage = tempDir.resolve("small_50x50.png").toFile();
        largeImage = tempDir.resolve("large_4000x3000.png").toFile();
        tallNarrowImage = tempDir.resolve("tall_100x2000.png").toFile();
        wideShortImage = tempDir.resolve("wide_3000x150.png").toFile();
        highDPIImage = tempDir.resolve("high_dpi_1000x1000.png").toFile();
        squareImage = tempDir.resolve("square_800x800.png").toFile();
        singlePixelImage = tempDir.resolve("single_1x1.png").toFile();

        TestImageGenerator.generateSmallImage(smallImage);
        TestImageGenerator.generateLargeImage(largeImage);
        TestImageGenerator.generateTallNarrowImage(tallNarrowImage);
        TestImageGenerator.generateWideShortImage(wideShortImage);
        TestImageGenerator.generateHighDPIImage(highDPIImage);
        TestImageGenerator.generateSquareImage(squareImage);
        TestImageGenerator.generateSinglePixelImage(singlePixelImage);
    }

    private BufferedImage loadBuffered(File f) throws Exception {
        return ImageIO.read(f);
    }

    @Test
    void testSmallImageLoading() throws Exception {
        BufferedImage image = loadBuffered(smallImage);
        assertNotNull(image);
        assertEquals(50, image.getWidth(), "Small image width");
        assertEquals(50, image.getHeight(), "Small image height");
    }

    @Test
    void testLargeImageLoading() throws Exception {
        BufferedImage image = loadBuffered(largeImage);
        assertNotNull(image);
        assertEquals(4000, image.getWidth(), "Large image width");
        assertEquals(3000, image.getHeight(), "Large image height");
    }

    @Test
    void testTallNarrowAspectRatio() throws Exception {
        BufferedImage image = loadBuffered(tallNarrowImage);
        assertNotNull(image);
        assertEquals(100, image.getWidth());
        assertEquals(2000, image.getHeight());
        double aspectRatio = (double) image.getHeight() / (double) image.getWidth();
        assertTrue(aspectRatio > 10, "Should be very tall and narrow");
    }

    @Test
    void testWideShortAspectRatio() throws Exception {
        BufferedImage image = loadBuffered(wideShortImage);
        assertNotNull(image);
        assertEquals(3000, image.getWidth());
        assertEquals(150, image.getHeight());
        double aspectRatio = (double) image.getWidth() / (double) image.getHeight();
        assertTrue(aspectRatio > 10, "Should be very wide and short");
    }

    @Test
    void testHighDPIImageLoading() throws Exception {
        BufferedImage image = loadBuffered(highDPIImage);
        assertNotNull(image);
        assertEquals(1000, image.getWidth());
        assertEquals(1000, image.getHeight());
    }

    @Test
    void testSquareImageLoading() throws Exception {
        BufferedImage image = loadBuffered(squareImage);
        assertNotNull(image);
        assertEquals(800, image.getWidth());
        assertEquals(800, image.getHeight());
        assertEquals(1.0, (double) image.getWidth() / (double) image.getHeight(), 0.01, "Should be perfectly square");
    }

    @Test
    void testSinglePixelImage() throws Exception {
        BufferedImage image = loadBuffered(singlePixelImage);
        assertNotNull(image);
        assertEquals(1, image.getWidth(), "Single pixel width");
        assertEquals(1, image.getHeight(), "Single pixel height");
    }

    @Test
    void testAllEdgeCaseImagesExist() {
        assertTrue(smallImage.exists(), "Small image should be generated");
        assertTrue(largeImage.exists(), "Large image should be generated");
        assertTrue(tallNarrowImage.exists(), "Tall narrow image should be generated");
        assertTrue(wideShortImage.exists(), "Wide short image should be generated");
        assertTrue(highDPIImage.exists(), "High DPI image should be generated");
        assertTrue(squareImage.exists(), "Square image should be generated");
        assertTrue(singlePixelImage.exists(), "Single pixel image should be generated");
    }
}
