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
 * calibration settings (numeric ranges and log flags) and the complete list
 * of datasets and their points. The export path is compatible with the
 * {@link com.digitizer.io.ProjectJson} model.
 */
public class JsonExporter {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private JsonExporter() {
        // Utility class, should not be instantiated
    }

    /**
     * Exports project data to a JSON file.
     *
     * @param filePath        the path where the JSON file will be written
     * @param title           the project title
     * @param xlabel          the x-axis label
     * @param ylabel          the y-axis label
     * @param calibration     the calibration state (may be null; defaults applied if null)
     * @param datasets        the list of datasets to export
     * @throws IOException if writing to the file fails
     */
    public static void exportToJson(String filePath, String title, String xlabel, String ylabel,
                                   CalibrationState calibration, List<Dataset> datasets)
            throws IOException {
        
        // Build ProjectJson from input data
        List<DatasetJson> datasetJsonList = new ArrayList<>();
        for (Dataset dataset : datasets) {
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
                    // include visibility flag
                    (dataset instanceof com.digitizer.core.Dataset) ? ((com.digitizer.core.Dataset)dataset).isVisible() : true
                );
            datasetJsonList.add(dsJson);
        }

        // Apply defaults if calibration absent (un-calibrated export scenario)
        double xMin = 0.0, xMax = 1.0, yMin = 0.0, yMax = 1.0;
        boolean xLog = false, yLog = false;
        if (calibration != null) {
            xMin = calibration.getDataXMin();
            xMax = calibration.getDataXMax();
            yMin = calibration.getDataYMin();
            yMax = calibration.getDataYMax();
            xLog = calibration.isXLog();
            yLog = calibration.isYLog();
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
