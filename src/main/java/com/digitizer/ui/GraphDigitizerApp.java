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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;
import com.digitizer.logging.LoggingConfig;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main application class for Graph Digitizer.
 * <p>
 * This is the JavaFX application entry point. It prepares the primary
 * application state (calibration and datasets), constructs the primary
 * {@link MainWindow}, and wires accessibility and theming features. Use
 * {@link #main(String[])} to start the application from the command line.
 * <p>
 * <strong>Logging:</strong> The application uses SLF4J with a Log4j2 backend.
 * The configuration file {@code log4j2.xml} defines console, rolling file, and
 * JSON appenders. To enable fully asynchronous logging (lower latency on the
 * JavaFX thread) start the JVM with:
 * <pre>
 *   -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
 * </pre>
 * This activates the {@code <AsyncRoot>} logger in the configuration. The JSON
 * log ({@code graph-digitizer.json}) is newline-delimited for easy ingestion.
 * <p>
 * Fields documented below are application-wide defaults used to create
 * the initial datasets and to provide versioning information.
 */
public class GraphDigitizerApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(GraphDigitizerApp.class);
    /**
     * The application version string.
     * <p>
     * Keep this updated when releasing new builds. It is displayed in the main
     * window title and in the About dialog.
     */
    /**
     * Current application version. Must match the Maven project version.
     */
    public static final String APP_VERSION = "1.1";
    /**
     * Maximum number of datasets supported in the UI.
     * <p>
     * This determines how many color-coded data series are exposed in the
     * {@link ControlPanel} and how many dataset objects are created at startup.
     */
    private static final int MAX_DATASETS = 6;

    /**
     * Default color palette used for datasets created at application startup.
     * Colors are specified in hex (CSS) format - these match the color names
     * used throughout the UI for consistency.
     */
    private static final String[] DEFAULT_COLORS = {
            "#0072B2", "#E69F00", "#009E73",
            "#CC79A7", "#F0E442", "#56B4E9"
    };

    /**
     * Reference to the main application window instance.
     * <p>
     * Stored so the application can perform lifecycle operations and access
     * the main UI components during shutdown or programmatic UI updates.
     */
    private MainWindow mainWindow;

    /**
     * The main entry point for the application.
     * <p>
    * Example: java -jar graph_digitizer_1.0-beta.jar
     * <p>
    * This method initializes logging and delegates to the JavaFX
    * {@link Application#launch} lifecycle.
    *
    * @param args command line arguments passed to the JVM (ignored)
     */
    public static void main(String[] args) {
        // Emit startup banner (centralized log file paths & configuration confirmation)
        LoggingConfig.logStartupBanner(APP_VERSION);
        // Initialize MDC with a generated session id (user id omitted for desktop context)
        LoggingConfig.initializeMdc(LoggingConfig.generateSessionId(), null);
        // Perform environment checks (version, async property, log dir)
        LoggingConfig.runEnvironmentChecks();
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
