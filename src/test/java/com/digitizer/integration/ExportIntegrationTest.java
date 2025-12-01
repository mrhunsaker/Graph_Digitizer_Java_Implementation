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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.digitizer.core.Dataset;
import com.digitizer.core.Point;
import com.digitizer.io.CsvExporter;
import com.digitizer.io.JsonExporter;

/**
 * Integration tests for JSON and CSV export functionality.
 */
class ExportIntegrationTest {

    @TempDir
    Path tempDir;

    private List<Dataset> datasets;

    @BeforeEach
    void setUp() {
        datasets = new ArrayList<>();
        
        Dataset ds1 = new Dataset("Test Dataset 1", "#0072B2");
        ds1.addPoint(new Point(1.0, 2.0));
        ds1.addPoint(new Point(3.0, 4.0));
        ds1.addPoint(new Point(5.0, 6.0));
        
        Dataset ds2 = new Dataset("Test Dataset 2", "#E69F00");
        ds2.addPoint(new Point(2.0, 3.0));
        ds2.addPoint(new Point(4.0, 5.0));
        
        datasets.add(ds1);
        datasets.add(ds2);
    }

    @Test
    void testCsvExport() throws Exception {
        File csvFile = tempDir.resolve("test_export.csv").toFile();
        
        CsvExporter.exportToCsv(csvFile.getAbsolutePath(), datasets);
        
        assertTrue(csvFile.exists(), "CSV file should be created");
        assertTrue(csvFile.length() > 0, "CSV file should not be empty");
        
        // Read and verify content
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertTrue(lines.size() > 0, "CSV should have content");
        
        // First line should be header
        String header = lines.get(0);
        assertTrue(header.contains("dataset") || header.contains("x") || header.contains("y"), 
                "CSV should have appropriate headers");
        
        // Should have 5 data lines (3 from ds1, 2 from ds2) plus header
        assertTrue(lines.size() >= 6, "CSV should have all data points");
    }

    @Test
    void testJsonExport() throws Exception {
        File jsonFile = tempDir.resolve("test_export.json").toFile();
        
        JsonExporter.exportToJson(
            jsonFile.getAbsolutePath(),
            "Test Title",
            "X Axis",
            "Y Axis",
            "",
            null, // no calibration state for this test
            datasets
        );
        
        assertTrue(jsonFile.exists(), "JSON file should be created");
        assertTrue(jsonFile.length() > 0, "JSON file should not be empty");
        
        // Read and verify content
        String content = Files.readString(jsonFile.toPath());
        assertTrue(content.contains("Test Title"), "JSON should contain title");
        assertTrue(content.contains("X Axis"), "JSON should contain X label");
        assertTrue(content.contains("Y Axis"), "JSON should contain Y label");
        assertTrue(content.contains("Test Dataset 1"), "JSON should contain dataset 1 name");
        assertTrue(content.contains("Test Dataset 2"), "JSON should contain dataset 2 name");
        assertTrue(content.contains("#0072B2"), "JSON should contain dataset 1 color");
        assertTrue(content.contains("#E69F00"), "JSON should contain dataset 2 color");
    }

    @Test
    void testExportWithEmptyDataset() throws Exception {
        Dataset emptyDs = new Dataset("Empty Dataset", "#00FF00");
        List<Dataset> singleDataset = List.of(emptyDs);
        
        File csvFile = tempDir.resolve("empty_export.csv").toFile();
        CsvExporter.exportToCsv(csvFile.getAbsolutePath(), singleDataset);
        
        assertTrue(csvFile.exists(), "CSV should be created even with empty dataset");
        
        List<String> lines = Files.readAllLines(csvFile.toPath());
        // Should have header but no data lines
        assertTrue(lines.size() >= 1, "Should have at least header");
    }

    @Test
    void testExportWithSpecialCharacters() throws Exception {
        Dataset specialDs = new Dataset("Test & Dataset <with> \"special\" chars", "#FF0000");
        specialDs.addPoint(new Point(1.5, 2.5));
        List<Dataset> singleDataset = List.of(specialDs);
        
        File jsonFile = tempDir.resolve("special_chars.json").toFile();
        JsonExporter.exportToJson(
            jsonFile.getAbsolutePath(),
            "Title & <Special>",
            "X",
            "Y",
            "",
            null,
            singleDataset
        );
        
        assertTrue(jsonFile.exists(), "JSON should handle special characters");
        String content = Files.readString(jsonFile.toPath());
        // JSON should escape special characters properly
        assertTrue(content.length() > 0, "JSON content should be generated");
    }
}
