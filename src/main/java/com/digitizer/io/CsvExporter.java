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

package com.digitizer.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.digitizer.core.Dataset;
import com.digitizer.core.Point;

/**
 * Handles CSV export of graph digitizer datasets.
 * <p>
 * The CSV is written in "wide" format where the first column contains
 * distinct X values (sorted) and each following column contains the
 * corresponding Y value for a given dataset. The exporter collates values
 * from multiple series and leaves empty cells when a dataset lacks a value
 * for a particular X.
 */
public class CsvExporter {

    private static final double TOLERANCE = 1e-8;

    private CsvExporter() {
        // Utility class, should not be instantiated
    }

    /**
     * Exports datasets to a CSV file in wide format.
     * The first column contains x values, followed by one column per dataset containing y values.
     * Missing values are left blank.
     *
     * @param filePath the path where the CSV file will be written
     * @param datasets the datasets to export
     * @throws IOException if writing to the file fails
     */
    public static void exportToCsv(String filePath, List<Dataset> datasets) throws IOException {
        // Filter out datasets that have no points (unused)
        List<Dataset> filteredDatasets = new ArrayList<>();
        List<Integer> originalIndexes = new ArrayList<>();
        for (int i = 0; i < datasets.size(); i++) {
            Dataset ds = datasets.get(i);
            if (ds.getPoints() != null && !ds.getPoints().isEmpty()) {
                filteredDatasets.add(ds);
                originalIndexes.add(i);
            }
        }

        // Collect all unique x values across filtered datasets
        Set<Double> xValues = new TreeSet<>();
        for (Dataset dataset : filteredDatasets) {
            for (Point point : dataset.getPoints()) {
                xValues.add(point.x());
            }
        }

        List<Double> sortedXValues = new ArrayList<>(xValues);

        // Create a map of x -> (dataset_index -> y_value)
        Map<Double, Map<Integer, Double>> dataMap = new HashMap<>();
        for (int dsIdx = 0; dsIdx < filteredDatasets.size(); dsIdx++) {
            Dataset dataset = filteredDatasets.get(dsIdx);
            for (Point point : dataset.getPoints()) {
                // Find or create the map for this x value with tolerance
                Double key = findMatchingX(sortedXValues, point.x());
                dataMap.computeIfAbsent(key, k -> new HashMap<>()).put(dsIdx, point.y());
            }
        }

        // Write CSV
        try (FileWriter fw = new FileWriter(filePath);
             CSVPrinter printer = new CSVPrinter(fw, CSVFormat.DEFAULT)) {

            // Write header
            printer.print("x");
            for (Dataset dataset : filteredDatasets) {
                printer.print(sanitizeHeaderName(dataset.getName()));
            }
            printer.println();

            // Write data rows
            for (Double x : sortedXValues) {
                printer.print(String.format("%.10g", x));
                Map<Integer, Double> yValues = dataMap.get(x);

                for (int dsIdx = 0; dsIdx < filteredDatasets.size(); dsIdx++) {
                    if (yValues != null && yValues.containsKey(dsIdx)) {
                        printer.print(String.format("%.10g", yValues.get(dsIdx)));
                    } else {
                        printer.print("");  // Empty cell for missing value
                    }
                }
                printer.println();
            }
        }
    }

    /**
     * Finds a matching x value from the list within tolerance.
     *
     * @param sortedXValues the sorted list of x values
     * @param targetX the target x value to match
     * @return the matching x value from the list (or targetX if not found)
     */
    private static Double findMatchingX(List<Double> sortedXValues, double targetX) {
        for (Double x : sortedXValues) {
            double tolerance = TOLERANCE * Math.max(1.0, Math.abs(x));
            if (Math.abs(x - targetX) <= tolerance) {
                return x;
            }
        }
        return targetX;  // Not found, return target
    }

    /**
     * Sanitizes a dataset name for use as a CSV column header.
     * Removes or replaces characters that might cause CSV parsing issues.
     *
     * @param name the original dataset name
     * @return a sanitized name suitable for CSV header
     */
    private static String sanitizeHeaderName(String name) {
        if (name == null || name.isEmpty()) {
            return "Dataset";
        }
        return name.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
