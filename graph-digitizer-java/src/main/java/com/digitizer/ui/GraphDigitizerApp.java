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

package com.digitizer.ui;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for Graph Digitizer.
 * Serves as the entry point for the JavaFX application.
 */
public class GraphDigitizerApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(GraphDigitizerApp.class);
    private static final String APP_VERSION = "1.2.0";
    private static final int MAX_DATASETS = 6;

    private static final String[] DEFAULT_COLORS = {
            "#0072B2", "#E69F00", "#009E73",
            "#CC79A7", "#F0E442", "#56B4E9"
    };

    private MainWindow mainWindow;

    /**
     * The main entry point for the application.
     */
    public static void main(String[] args) {
        logger.info("Starting Graph Digitizer version {}", APP_VERSION);
        launch(args);
    }

    /**
     * Called when the application starts.
     *
     * @param primaryStage the primary stage for the application
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing Graph Digitizer GUI");

            // Create application data
            CalibrationState calibration = new CalibrationState();
            List<Dataset> datasets = createDefaultDatasets();

            // Create main window
            mainWindow = new MainWindow(primaryStage, calibration, datasets, MAX_DATASETS, DEFAULT_COLORS);
            mainWindow.initialize();

            // Show the window
            primaryStage.setTitle("Graph Digitizer v" + APP_VERSION);
            primaryStage.setWidth(1000);
            primaryStage.setHeight(800);
            
            // Log accessibility startup
            AccessibilityHelper.announceStatus("Graph Digitizer application started. Version " + APP_VERSION);
            AccessibilityHelper.announceStatus("Press Tab to navigate through all controls. Press Alt+H for help.");
            
            primaryStage.show();

            logger.info("Graph Digitizer GUI initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    /**
     * Creates the default set of empty datasets with default colors.
     *
     * @return a list of initialized datasets
     */
    private List<Dataset> createDefaultDatasets() {
        List<Dataset> datasets = new ArrayList<>();
        for (int i = 0; i < MAX_DATASETS; i++) {
            String name = "Dataset " + (i + 1);
            String color = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
            datasets.add(new Dataset(name, color));
        }
        return datasets;
    }

    /**
     * Called when the application is shutting down.
     */
    @Override
    public void stop() throws Exception {
        logger.info("Stopping Graph Digitizer");
        super.stop();
    }
}
