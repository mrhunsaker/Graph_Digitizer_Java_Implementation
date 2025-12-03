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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;
import com.digitizer.core.Point;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Handles JSON import and export of graph digitizer projects.
 * <p>
 * The JSON format contains project metadata (title and axis labels),
 * calibration settings (numeric ranges and log flags) and the list of
 * datasets and their points. Export behavior notes:
 * <ul>
 *   <li>When provided, the secondary (right-hand) Y-axis title is written
 *   to the top-level {@code y2label} property in the {@link com.digitizer.io.ProjectJson}.</li>
 *   <li>Each dataset object contains a boolean {@code use_secondary_y} flag
 *   set to {@code true} when that dataset is assigned to the secondary Y axis.</li>
 *   <li>Datasets that contain no points are omitted from the exported JSON
 *   to avoid clutter and empty series entries.</li>
 * </ul>
 * The export path is compatible with the {@link com.digitizer.io.ProjectJson} model.
 */
public class JsonExporter {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private JsonExporter() {
        // Utility class, should not be instantiated
    }

    /**
     * Exports project data to a JSON file.
     * <p>
     * The exporter will skip any datasets that have no points (they will not
     * appear in the resulting JSON). Each dataset entry includes its name,
     * color, point list, visibility and the {@code use_secondary_y} flag.
     *
     * @param filePath        the path where the JSON file will be written
     * @param title           the project title
     * @param xlabel          the x-axis label
     * @param ylabel          the y-axis label
     * @param y2label         the secondary (right-hand) Y-axis label (may be null/empty)
     * @param calibration     the calibration state (may be null; defaults applied if null)
     * @param datasets        the list of datasets to export
     * @throws IOException if writing to the file fails
     */
    public static void exportToJson(String filePath, String title, String xlabel, String ylabel, String y2label,
                                   CalibrationState calibration, List<Dataset> datasets)
            throws IOException {
        
        // Build ProjectJson from input data
        List<DatasetJson> datasetJsonList = new ArrayList<>();
        for (Dataset dataset : datasets) {
            // Skip datasets that have no points (unused)
            if (dataset.getPoints() == null || dataset.getPoints().isEmpty()) continue;

            List<List<Double>> pointsList = new ArrayList<>();
            for (Point point : dataset.getPoints()) {
                List<Double> pointPair = new ArrayList<>();
                pointPair.add(point.x());
                pointPair.add(point.y());
                pointsList.add(pointPair);
            }

            DatasetJson dsJson = new DatasetJson(
                dataset.getName(),
                dataset.getHexColor(),
                pointsList,
                (dataset instanceof com.digitizer.core.Dataset) ? ((com.digitizer.core.Dataset)dataset).isVisible() : true,
                dataset.isUseSecondaryYAxis()
            );
            datasetJsonList.add(dsJson);
        }

        // Apply defaults if calibration absent (un-calibrated export scenario)
        double xMin = 0.0, xMax = 1.0, yMin = 0.0, yMax = 1.0;
        boolean xLog = false, yLog = false;
        Double y2Min = null, y2Max = null;
        Boolean y2Log = null;
        if (calibration != null) {
            xMin = calibration.getDataXMin();
            xMax = calibration.getDataXMax();
            yMin = calibration.getDataYMin();
            yMax = calibration.getDataYMax();
            xLog = calibration.isXLog();
            yLog = calibration.isYLog();
            y2Min = calibration.getDataY2Min();
            y2Max = calibration.getDataY2Max();
            y2Log = calibration.isY2Log();
        }

        ProjectJson project = new ProjectJson(
            title,
            xlabel,
            ylabel,
            xMin,
            xMax,
            yMin,
            yMax,
            xLog,
            yLog,
            datasetJsonList
        );
        // Set secondary Y axis label if provided
        project.y2label = y2label == null ? "" : y2label;
        // Populate secondary Y axis numeric range if present
        if (y2Min != null && y2Max != null) {
            project.y2Min = y2Min;
            project.y2Max = y2Max;
            if (y2Log != null) project.y2Log = y2Log;
        }

        // Write to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(project, writer);
        }
    }

    /**
     * Imports project data from a JSON file.
     *
     * @param filePath the path to the JSON file to read
     * @return a ProjectJson object containing the imported data
     * @throws IOException if reading from the file fails
     * @throws JsonSyntaxException if the JSON is malformed
     */
    public static ProjectJson importFromJson(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, ProjectJson.class);
        }
    }

    /**
     * Converts imported JSON data back into application objects.
     * <p>
     * The converter restores dataset visibility and the {@code use_secondary_y}
     * flag (mapping it to each {@link com.digitizer.core.Dataset}'s
     * {@code setUseSecondaryYAxis} property) so imported projects preserve
     * secondary-axis assignments.
     *
     * @param project the ProjectJson object imported from file
     * @param calibration the CalibrationState to populate
     * @return a list of Dataset objects
     */
    public static List<Dataset> convertJsonToDatasets(ProjectJson project, CalibrationState calibration) {
        List<Dataset> datasets = new ArrayList<>();

        if (project == null) {
            return datasets;
        }

        // Update calibration state
        calibration.setDataXMin(project.xMin);
        calibration.setDataXMax(project.xMax);
        calibration.setDataYMin(project.yMin);
        calibration.setDataYMax(project.yMax);
        calibration.setXLog(project.xLog);
        calibration.setYLog(project.yLog);

        // Convert datasets
        if (project.datasets != null) {
            for (DatasetJson dsJson : project.datasets) {
                Dataset dataset = new Dataset(dsJson.name, dsJson.color);
                // restore visibility if present
                try {
                    dataset.setVisible(dsJson.visible);
                } catch (Exception ignore) { }
                // restore secondary axis assignment if present
                try {
                    dataset.setUseSecondaryYAxis(dsJson.useSecondaryY);
                } catch (Exception ignore) { }
                
                if (dsJson.points != null) {
                    for (List<Double> pointPair : dsJson.points) {
                        if (pointPair.size() >= 2) {
                            dataset.addPoint(new Point(pointPair.get(0), pointPair.get(1)));
                        }
                    }
                }
                
                datasets.add(dataset);
            }
        }

        return datasets;
    }
}
