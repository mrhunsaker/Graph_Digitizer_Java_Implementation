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

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;
import com.digitizer.core.FileUtils;
import com.digitizer.io.CsvExporter;
import com.digitizer.io.JsonExporter;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main window for the Graph Digitizer application.
 * Manages the primary UI layout and orchestrates interactions between UI components.
 */
public class MainWindow {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private final Stage primaryStage;
    private final CalibrationState calibration;
    private final List<Dataset> datasets;
    private final int maxDatasets;
    private final String[] defaultColors;

    private CanvasPanel canvasPanel;
    private ControlPanel controlPanel;
    private StatusBar statusBar;
    // Scroll pane and left scrollbar are fields so we can update metrics and wire zoom/fit
    private javafx.scene.control.ScrollPane scrollPane;
    private javafx.scene.control.ScrollBar leftScroll;
    private Slider zoomSlider;

    /**
     * Constructs a new MainWindow.
     *
     * @param primaryStage   the JavaFX Stage
     * @param calibration    the calibration state
     * @param datasets       the datasets to manage
     * @param maxDatasets    maximum number of datasets allowed
     * @param defaultColors  default color palette
     */
    public MainWindow(Stage primaryStage, CalibrationState calibration, List<Dataset> datasets,
                      int maxDatasets, String[] defaultColors) {
        this.primaryStage = primaryStage;
        this.calibration = calibration;
        this.datasets = datasets;
        this.maxDatasets = maxDatasets;
        this.defaultColors = defaultColors;
    }

    /**
     * Initializes the main window and displays it.
     */
    public void initialize() {
        // Create UI components
        canvasPanel = new CanvasPanel(calibration, datasets);
        controlPanel = new ControlPanel(calibration, datasets, canvasPanel);
        statusBar = new StatusBar();

        // Create root layout
        BorderPane root = new BorderPane();

        // Top: Menu bar and Toolbar
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createToolbar());
        root.setTop(topContainer);

        // Center: Canvas wrapped in a ScrollPane so we can show the image at its natural resolution
        this.scrollPane = new javafx.scene.control.ScrollPane(canvasPanel);
        this.scrollPane.setPannable(true);
        this.scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // we'll provide a left-side scrollbar that syncs to vertical scroll
        this.scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        // Left-side scrollbar synchronized with the ScrollPane's vvalue (pixel mode)
        this.leftScroll = new javafx.scene.control.ScrollBar();
        this.leftScroll.setOrientation(javafx.geometry.Orientation.VERTICAL);
        this.leftScroll.setMin(0.0);
        this.leftScroll.setMax(0.0);
        this.leftScroll.setUnitIncrement(20.0);

        // Sync ScrollPane -> leftScroll (convert vvalue 0..1 to pixel offset)
        this.scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            try {
                Platform.runLater(() -> {
                    double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                    double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                    double max = Math.max(0.0, contentHeight - viewportHeight);
                    if (max <= 0) {
                        leftScroll.setValue(0);
                    } else {
                        leftScroll.setValue(newV.doubleValue() * max);
                    }
                });
            } catch (Exception ignore) { }
        });

        // Sync leftScroll -> ScrollPane (convert pixel offset to vvalue 0..1)
        this.leftScroll.valueProperty().addListener((obs, oldV, newV) -> {
            try {
                Platform.runLater(() -> {
                    double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                    double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                    double max = Math.max(0.0, contentHeight - viewportHeight);
                    if (max <= 0) {
                        this.scrollPane.setVvalue(0);
                    } else {
                        this.scrollPane.setVvalue(newV.doubleValue() / max);
                    }
                });
            } catch (Exception ignore) { }
        });

        // When content size or viewport changes, update scrollbar metrics
        this.scrollPane.viewportBoundsProperty().addListener((obs, oldB, newB) -> updateScrollMetrics());
        canvasPanel.boundsInParentProperty().addListener((obs, oldB, newB) -> updateScrollMetrics());

        root.setLeft(this.leftScroll);
        root.setCenter(this.scrollPane);

    // Right: Control panel (use the instantiated ControlPanel so added UI is visible)
    root.setRight(controlPanel);

        // Bottom: Status bar
        root.setBottom(statusBar);

        // Create and set scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Register scene with theme manager
        ThemeManager.setScene(scene);

    // Ensure the stage is shown maximized so controls and large images are visible
    primaryStage.setTitle("Graph Digitizer");
    primaryStage.setMinWidth(1000);
    primaryStage.setMinHeight(700);
    primaryStage.setScene(scene);
    primaryStage.setMaximized(true);
    primaryStage.show();

        logger.info("Main window initialized and shown");
    }

    /**
     * Creates the menu bar with File and Themes menus.
     *
     * @return a MenuBar with accessible menus
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        // File menu (Save/Exit/About)
        Menu fileMenu = new Menu("File");

        MenuItem saveCsvItem = new MenuItem("Save CSV");
        saveCsvItem.setOnAction(e -> handleSaveCsv());
        saveCsvItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

        MenuItem saveJsonItem = new MenuItem("Save JSON");
        saveJsonItem.setOnAction(e -> handleSaveJson());
        saveJsonItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        fileMenu.getItems().addAll(saveCsvItem, saveJsonItem, new SeparatorMenuItem(), aboutItem, exitItem);

        // Themes menu
        Menu themesMenu = new Menu("Themes");
        for (String themeName : ThemeManager.getAvailableThemes()) {
            MenuItem themeItem = new MenuItem(themeName);
            themeItem.setOnAction(e -> {
                ThemeManager.applyTheme(themeName);
                String message = "Theme changed to: " + themeName;
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Theme changed to: {}", themeName);
            });
            themesMenu.getItems().add(themeItem);
        }

        menuBar.getMenus().addAll(fileMenu, themesMenu);

        return menuBar;
    }

    private void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("About Graph Digitizer");
        alert.setHeaderText("Graph Digitizer");
        String content = "Graph Digitizer\nVersion 1.2.0\n\n" +
                "A Java port of the Graph Digitizer application.\n" +
                "Author: Michael Ryan Hunsaker\n" +
                "Licensed under the Apache 2.0 License.";
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Creates the top toolbar with accessible buttons.
     * All buttons are configured for keyboard navigation and screen reader support.
     *
     * @return an HBox containing toolbar buttons
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        // Load Image button
        Button loadImageBtn = new Button("Load Image");
        AccessibilityHelper.setButtonAccessibility(loadImageBtn, "Load Image", 
            "Load a PNG or JPEG image for digitization", "Ctrl+O");
        loadImageBtn.setOnAction(e -> handleLoadImage());

        // Calibrate button
        Button calibrateBtn = new Button("Calibrate");
        AccessibilityHelper.setButtonAccessibility(calibrateBtn, "Calibrate", 
            "Enter calibration mode to set axis reference points by clicking 4 locations", "Ctrl+L");
        calibrateBtn.setOnAction(e -> handleCalibrate());

        // Auto Trace button
        Button autoTraceBtn = new Button("Auto Trace");
        AccessibilityHelper.setButtonAccessibility(autoTraceBtn, "Auto Trace", 
            "Automatically detect and trace the data curve using color matching", "Ctrl+T");
        autoTraceBtn.setOnAction(e -> handleAutoTrace());

        // Save JSON button
        Button saveJsonBtn = new Button("Save JSON");
        AccessibilityHelper.setButtonAccessibility(saveJsonBtn, "Save JSON", 
            "Export all data and calibration to a JSON file for later editing", "Ctrl+S");
        saveJsonBtn.setOnAction(e -> handleSaveJson());

        // Save CSV button
        Button saveCsvBtn = new Button("Save CSV");
        AccessibilityHelper.setButtonAccessibility(saveCsvBtn, "Save CSV", 
            "Export data points to a CSV file for use in spreadsheets and other tools", "Ctrl+E");
        saveCsvBtn.setOnAction(e -> handleSaveCsv());

        toolbar.getChildren().addAll(loadImageBtn, calibrateBtn, autoTraceBtn, saveJsonBtn, saveCsvBtn);

        // --- Zoom controls ---
        Label zoomLabel = new Label("Zoom:");
        Button fitBtn = new Button("Fit");
        Button oneBtn = new Button("100%");
        this.zoomSlider = new Slider(0.25, 3.0, 1.0);
        this.zoomSlider.setPrefWidth(160);
        this.zoomSlider.valueProperty().addListener((obs, oldV, newV) -> {
            try {
                canvasPanel.setZoom(newV.doubleValue());
            } catch (Exception ignore) { }
        });
        fitBtn.setOnAction(e -> {
            Bounds vp = this.scrollPane.getViewportBounds();
            canvasPanel.fitToViewport(vp.getWidth(), vp.getHeight());
            this.zoomSlider.setValue(canvasPanel.getZoom());
        });
        oneBtn.setOnAction(e -> {
            canvasPanel.setZoom(1.0);
            this.zoomSlider.setValue(1.0);
        });

        HBox zoomBox = new HBox(6, zoomLabel, fitBtn, oneBtn, this.zoomSlider);
        zoomBox.setPadding(new Insets(0, 0, 0, 20));
        toolbar.getChildren().add(zoomBox);

        return toolbar;
    }

    /**
     * Creates the right control panel with accessible form controls.
     * All input fields are labeled for screen readers and have helpful tooltips.
     *
     * @return a VBox containing accessible controls
     */
    // createRightPanel removed — ControlPanel instance is used directly

    private void handleLoadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                canvasPanel.loadImage(file);
                String message = "Loaded image: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Loaded image: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error loading image: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error loading image", e);
            }
        }
    }

    private void handleCalibrate() {
        String message = "Calibration mode: Click 4 points (X-left, X-right, Y-bottom, Y-top)";
        statusBar.setStatus(message);
        AccessibilityHelper.announceModeChange("Calibration Mode", 
            "Click in the image canvas to mark 4 calibration points: " +
            "1. Left X value, 2. Right X value, 3. Bottom Y value, 4. Top Y value");
        canvasPanel.enterCalibrationMode();
        logger.info("Entered calibration mode");
    }

    private void handleAutoTrace() {
        if (!calibration.isCalibrated()) {
            String message = "Error: Image must be calibrated before auto-trace";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction("Error - Image calibration required");
            return;
        }

        try {
            canvasPanel.performAutoTrace();
            String message = "Auto-trace complete";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            logger.info("Auto-trace performed");
        } catch (Exception e) {
            String message = "Error: " + e.getMessage();
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction("Error during auto-trace - " + e.getMessage());
            logger.error("Error during auto-trace", e);
        }
    }

    private void handleSaveJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project as JSON");
        // Require a title and use it to build the default filename
        String title = controlPanel.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String message = "Please enter a title before saving";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            return;
        }
        String safe = FileUtils.sanitizeFilename(title.trim());
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fileChooser.setInitialFileName(safe + "_" + ts + ".json");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
        JsonExporter.exportToJson(
            file.getAbsolutePath(),
            title.trim(),
            controlPanel.getXLabel(),
            controlPanel.getYLabel(),
            calibration,
            datasets
        );
                String message = "Saved JSON to: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Saved JSON to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error saving JSON: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error saving JSON", e);
            }
        }
    }

    private void handleSaveCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project as CSV");
        // Require a title and use it to build the default filename
        String title = controlPanel.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String message = "Please enter a title before saving";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            return;
        }
        String safe = FileUtils.sanitizeFilename(title.trim());
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fileChooser.setInitialFileName(safe + "_" + ts + ".csv");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                CsvExporter.exportToCsv(file.getAbsolutePath(), datasets);
                String message = "Saved CSV to: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Saved CSV to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error saving CSV: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error saving CSV", e);
            }
        }
    }

    /**
     * Gets the status bar for displaying messages.
     *
     * @return the StatusBar component
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Gets the canvas panel.
     *
     * @return the CanvasPanel component
     */
    public CanvasPanel getCanvasPanel() {
        return canvasPanel;
    }

    /**
     * Recompute left scrollbar metrics based on content and viewport sizes.
     */
    private void updateScrollMetrics() {
        Platform.runLater(() -> {
            try {
                double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                double max = Math.max(0.0, contentHeight - viewportHeight);
                this.leftScroll.setMax(max);
                this.leftScroll.setVisibleAmount(viewportHeight);
                this.leftScroll.setBlockIncrement(Math.max(1.0, viewportHeight));
                this.leftScroll.setUnitIncrement(Math.max(1.0, viewportHeight / 20.0));
                double v = this.scrollPane.getVvalue();
                if (max <= 0) this.leftScroll.setValue(0);
                else this.leftScroll.setValue(v * max);
            } catch (Exception ignore) { }
        });
    }
}
