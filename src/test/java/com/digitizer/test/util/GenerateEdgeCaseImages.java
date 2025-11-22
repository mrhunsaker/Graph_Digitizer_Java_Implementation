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

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Utility to generate edge-case test images in src/test/resources/images/.
 * Run this test to populate the test image directory with edge cases.
 */
public class GenerateEdgeCaseImages {

    @Test
    void generateAllEdgeCaseImages() {
        try {
            // Determine output directory
            File outputDir = new File("src/test/resources/images");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            System.out.println("Generating edge-case test images to: " + outputDir.getAbsolutePath());

            // Generate all edge-case images
            File smallImage = new File(outputDir, "edge_small_50x50.png");
            TestImageGenerator.generateSmallImage(smallImage);
            System.out.println("✓ Generated: " + smallImage.getName());

            File largeImage = new File(outputDir, "edge_large_4000x3000.png");
            TestImageGenerator.generateLargeImage(largeImage);
            System.out.println("✓ Generated: " + largeImage.getName());

            File tallNarrowImage = new File(outputDir, "edge_tall_100x2000.png");
            TestImageGenerator.generateTallNarrowImage(tallNarrowImage);
            System.out.println("✓ Generated: " + tallNarrowImage.getName());

            File wideShortImage = new File(outputDir, "edge_wide_3000x150.png");
            TestImageGenerator.generateWideShortImage(wideShortImage);
            System.out.println("✓ Generated: " + wideShortImage.getName());

            File highDPIImage = new File(outputDir, "edge_high_dpi_1000x1000.png");
            TestImageGenerator.generateHighDPIImage(highDPIImage);
            System.out.println("✓ Generated: " + highDPIImage.getName());

            File squareImage = new File(outputDir, "edge_square_800x800.png");
            TestImageGenerator.generateSquareImage(squareImage);
            System.out.println("✓ Generated: " + squareImage.getName());

            File singlePixelImage = new File(outputDir, "edge_single_1x1.png");
            TestImageGenerator.generateSinglePixelImage(singlePixelImage);
            System.out.println("✓ Generated: " + singlePixelImage.getName());

            System.out.println("\n✅ Successfully generated 7 edge-case test images!");

        } catch (Exception e) {
            throw new RuntimeException("Error generating edge-case images", e);
        }
    }
}
