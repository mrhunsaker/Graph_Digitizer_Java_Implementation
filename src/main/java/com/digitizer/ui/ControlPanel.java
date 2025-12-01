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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Control panel for managing dataset metadata and calibration parameters.
 * <p>
 * This panel contains accessible controls for:
 * <ul>
 *   <li>Selecting and renaming datasets</li>
 *   <li>Editing global plot metadata (title, X/Y labels)</li>
 *   <li>Calibration numeric inputs and applying calibration</li>
 *   <li>Snap-to-X configuration for snapping existing or new points</li>
 * </ul>
 *
 * The {@link MainWindow} queries this panel to read the title and axis labels
 * for saving metadata using {@link com.digitizer.io.JsonExporter}.
 */
public class ControlPanel extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    // Exposed fields for title and axis labels so MainWindow can read them when saving
    private TextField titleField;
    private TextField xlabelField;
    private TextField ylabelField;
    private TextField y2labelField;
    // Keep a reference to the datasets so UI can be refreshed externally
    private java.util.List<com.digitizer.core.Dataset> datasets;
    // Canvas reference so color picker changes can trigger a redraw
    private CanvasPanel canvasPanel;
    // Accessibility preferences to persist per-dataset colors
    private AccessibilityPreferences accessibilityPrefs;
    // Dataset selector exposed for keyboard shortcuts
    private javafx.scene.control.ComboBox<String> datasetSelector;

    /**
     * Constructs a new ControlPanel with accessibility features.
     *
     * @param calibration the shared calibration state for coordinate transformation
     * @param datasets    the list of datasets managed by the application
     * @param canvasPanel the canvas panel for triggering redraws when snap values change
     */
    private final UndoManager undoManager;

    public ControlPanel(CalibrationState calibration, List<Dataset> datasets, CanvasPanel canvasPanel, AccessibilityPreferences accessibilityPrefs, UndoManager undoManager) {
        setSpacing(10);
        setPadding(new Insets(10));
        this.datasets = datasets;
        this.canvasPanel = canvasPanel;
        this.accessibilityPrefs = accessibilityPrefs;
        this.undoManager = undoManager;
        
        // Create section header and dataset selector
        Label datasetLabel = new Label("Series Selection:");
        AccessibilityHelper.setLabelAccessibility(datasetLabel, "Series Selection:", "Section heading");
        datasetLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Selector for Dataset 1..6
        datasetSelector = new ComboBox<>(FXCollections.observableArrayList(
            "Dataset 1","Dataset 2","Dataset 3","Dataset 4","Dataset 5","Dataset 6"));
        datasetSelector.getSelectionModel().selectFirst();

        TextField seriesTitleField = new TextField();
        seriesTitleField.setPrefColumnCount(16);
        seriesTitleField.setPromptText("Series title");

        Button setTitleBtn = new Button("Set Series Title");

        HBox selectorBox = new HBox(8, datasetSelector, seriesTitleField, setTitleBtn);
        selectorBox.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(datasetLabel, selectorBox, new Separator());

        // Dataset info area (kept below the selector)
        VBox datasetInfoBox = new VBox(6);
        datasetInfoBox.setPadding(new Insets(4, 0, 0, 0));
        getChildren().add(datasetInfoBox);

        // Helper to refresh dataset info display
        Runnable refreshDatasetInfo = () -> refreshDatasetInfoDisplay();
        // Initialize datasetInfo
        refreshDatasetInfo.run();

        // When selector changes, show current name in the seriesTitleField
        datasetSelector.setOnAction(evt -> {
            int idx = datasetSelector.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < datasets.size()) {
                seriesTitleField.setText(datasets.get(idx).getName());
            } else {
                seriesTitleField.setText("Dataset " + (idx + 1));
            }
            // Inform canvas which dataset is active so new points/auto-trace apply there
            if (this.canvasPanel != null) this.canvasPanel.setActiveDatasetIndex(idx);
        });

        setTitleBtn.setOnAction(evt -> {
            int idx = datasetSelector.getSelectionModel().getSelectedIndex();
            String title = seriesTitleField.getText();
            if (title == null || title.trim().isEmpty()) {
                AccessibilityHelper.announceAction("Series title cannot be empty");
                return;
            }
            if (idx >= 0 && idx < datasets.size()) {
                datasets.get(idx).setName(title.trim());
            } else if (idx >= datasets.size()) {
                // Create missing datasets up to selected index
                for (int i = datasets.size(); i <= idx; i++) {
                    String defaultName = "Dataset " + (i + 1);
                    com.digitizer.core.Dataset newDs = new com.digitizer.core.Dataset(defaultName, "#0072B2");
                    datasets.add(newDs);
                }
                datasets.get(idx).setName(title.trim());
            }
            AccessibilityHelper.announceAction("Series title set to " + title.trim());
            refreshDatasetInfoDisplay();
        });

        // Basic plot metadata (Title, X/Y labels)
        getChildren().add(new Separator());
    Label titleLabel = new Label("Title:");
    AccessibilityHelper.setLabelAccessibility(titleLabel, "Title:", "Form label");
    this.titleField = new TextField();
    AccessibilityHelper.setTextFieldAccessibility(this.titleField, "Plot Title", "Enter plot title", "The title displayed at the top of the plot");

    Label xlabelLabel = new Label("X Label:");
    AccessibilityHelper.setLabelAccessibility(xlabelLabel, "X Label:", "Form label");
    this.xlabelField = new TextField();
    AccessibilityHelper.setTextFieldAccessibility(this.xlabelField, "X-axis Label", "Enter x-axis label", "Label for the horizontal axis (e.g., 'Time', 'Distance')");

    Label ylabelLabel = new Label("Y Label:");
    AccessibilityHelper.setLabelAccessibility(ylabelLabel, "Y Label:", "Form label");
    this.ylabelField = new TextField();
    AccessibilityHelper.setTextFieldAccessibility(this.ylabelField, "Y-axis Label", "Enter y-axis label", "Label for the vertical axis (e.g., 'Temperature', 'Voltage')");

    getChildren().addAll(
        titleLabel, this.titleField,
        xlabelLabel, this.xlabelField,
        ylabelLabel, this.ylabelField
    );
    // Secondary Y-axis label (right-hand axis)
    Label y2Label = new Label("Y2 Label (secondary):");
    AccessibilityHelper.setLabelAccessibility(y2Label, "Y2 Label (secondary):", "Form label for secondary Y axis");
    this.y2labelField = new TextField();
    AccessibilityHelper.setTextFieldAccessibility(this.y2labelField, "Secondary Y-axis Label", "Enter secondary Y-axis label", "Label for the secondary (right-hand) Y axis");
    getChildren().addAll(y2Label, this.y2labelField);

        // Calibration section
        getChildren().add(new Separator());
        Label calibLabel = new Label("Calibration Settings:");
        AccessibilityHelper.setLabelAccessibility(calibLabel, "Calibration Settings:", "Section heading");
        calibLabel.setStyle("-fx-font-weight: bold;");

        Label xMinLabel = new Label("X Min:");
        AccessibilityHelper.setLabelAccessibility(xMinLabel, "X Min:", "Form label");
        TextField xMinField = new TextField("0");
        AccessibilityHelper.setTextFieldAccessibility(xMinField, "Minimum X Value", "0", "Minimum value on the X axis. Left-click on image to set automatically.");

        Label xMaxLabel = new Label("X Max:");
        AccessibilityHelper.setLabelAccessibility(xMaxLabel, "X Max:", "Form label");
        TextField xMaxField = new TextField("1");
        AccessibilityHelper.setTextFieldAccessibility(xMaxField, "Maximum X Value", "1", "Maximum value on the X axis. Right-click on image to set automatically.");

        Label yMinLabel = new Label("Y Min (primary):");
        AccessibilityHelper.setLabelAccessibility(yMinLabel, "Y Min (primary):", "Form label");
        TextField yMinField = new TextField("0");
        AccessibilityHelper.setTextFieldAccessibility(yMinField, "Minimum Y Value", "0", "Minimum value on the Y axis. Bottom-click on image to set automatically.");

        Label yMaxLabel = new Label("Y Max (primary):");
        AccessibilityHelper.setLabelAccessibility(yMaxLabel, "Y Max (primary):", "Form label");
        TextField yMaxField = new TextField("1");
        AccessibilityHelper.setTextFieldAccessibility(yMaxField, "Maximum Y Value", "1", "Maximum value on the Y axis. Top-click on image to set automatically.");

        // Secondary Y axis fields (right-hand axis)
        Label y2MinLabel = new Label("Y2 Min (secondary):");
        AccessibilityHelper.setLabelAccessibility(y2MinLabel, "Y2 Min (secondary):", "Form label");
        TextField y2MinField = new TextField();
        AccessibilityHelper.setTextFieldAccessibility(y2MinField, "Minimum secondary Y Value", "", "Minimum value on the secondary (right-hand) Y axis.");

        Label y2MaxLabel = new Label("Y2 Max (secondary):");
        AccessibilityHelper.setLabelAccessibility(y2MaxLabel, "Y2 Max (secondary):", "Form label");
        TextField y2MaxField = new TextField();
        AccessibilityHelper.setTextFieldAccessibility(y2MaxField, "Maximum secondary Y Value", "", "Maximum value on the secondary (right-hand) Y axis.");

        CheckBox y2LogCheckBox = new CheckBox("Y2 Log Scale");
        AccessibilityHelper.setCheckBoxAccessibility(y2LogCheckBox, "Y2 Log Scale", "Check to use logarithmic scaling on the secondary Y axis");

        CheckBox xLogCheckBox = new CheckBox("X Log Scale");
        AccessibilityHelper.setCheckBoxAccessibility(xLogCheckBox, "X Log Scale", "Check to use logarithmic scaling on the X axis");

        CheckBox yLogCheckBox = new CheckBox("Y Log Scale");
        AccessibilityHelper.setCheckBoxAccessibility(yLogCheckBox, "Y Log Scale", "Check to use logarithmic scaling on the Y axis");

        Button applyCalibBtn = new Button("Apply Calibration");
        applyCalibBtn.setOnAction(evt -> {
            // Parse numeric inputs
            double dxmin, dxmax, dymin, dymax;
            try {
                dxmin = Double.parseDouble(xMinField.getText().trim());
                dxmax = Double.parseDouble(xMaxField.getText().trim());
                dymin = Double.parseDouble(yMinField.getText().trim());
                dymax = Double.parseDouble(yMaxField.getText().trim());
            } catch (NumberFormatException nfe) {
                AccessibilityHelper.announceAction("Invalid numeric calibration value");
                logger.warn("Invalid calibration numeric input", nfe);
                return;
            }
            boolean xlog = xLogCheckBox.isSelected();
            boolean ylog = yLogCheckBox.isSelected();

            // Secondary Y values are optional; parse if provided
            Double dy2min = null;
            Double dy2max = null;
            Boolean y2log = null;
            try {
                String t2min = y2MinField.getText();
                String t2max = y2MaxField.getText();
                if (t2min != null && !t2min.trim().isEmpty()) dy2min = Double.parseDouble(t2min.trim());
                if (t2max != null && !t2max.trim().isEmpty()) dy2max = Double.parseDouble(t2max.trim());
                if (dy2min != null && dy2max == null) {
                    AccessibilityHelper.announceAction("Please provide both Y2 min and max or leave both empty");
                    return;
                }
                if (dy2max != null && dy2min == null) {
                    AccessibilityHelper.announceAction("Please provide both Y2 min and max or leave both empty");
                    return;
                }
                if (dy2min != null && dy2max != null) {
                    y2log = y2LogCheckBox.isSelected();
                }
            } catch (NumberFormatException nfe) {
                AccessibilityHelper.announceAction("Invalid numeric secondary Y value");
                logger.warn("Invalid secondary Y calibration numeric input", nfe);
                return;
            }

            boolean ok = canvasPanel.confirmCalibration(dxmin, dxmax, dymin, dymax, xlog, ylog, dy2min, dy2max, y2log);
            if (ok) {
                AccessibilityHelper.announceAction("Calibration applied");
            }
        });

        // Replace row HBoxes with a GridPane so labels/fields align in columns
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(140);
        c0.setPrefWidth(140);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        grid.add(xMinLabel, 0, 0);
        grid.add(xMinField, 1, 0);
        grid.add(xMaxLabel, 0, 1);
        grid.add(xMaxField, 1, 1);
        grid.add(yMinLabel, 0, 2);
        grid.add(yMinField, 1, 2);
        grid.add(yMaxLabel, 0, 3);
        grid.add(yMaxField, 1, 3);
        grid.add(y2MinLabel, 0, 4);
        grid.add(y2MinField, 1, 4);
        grid.add(y2MaxLabel, 0, 5);
        grid.add(y2MaxField, 1, 5);

        HBox checks = new HBox(12, xLogCheckBox, yLogCheckBox, y2LogCheckBox);
        checks.setPadding(new Insets(6, 0, 0, 0));
        HBox bottomRow = new HBox(12, checks, applyCalibBtn);
        bottomRow.setPadding(new Insets(6, 0, 0, 0));

        getChildren().addAll(calibLabel, grid, bottomRow);

        // Snap-to-X UI
        getChildren().add(new Separator());
        Label snapLabel = new Label("Snap X values (comma-separated):");
        AccessibilityHelper.setLabelAccessibility(snapLabel, "Snap X values", "Enter comma-separated X values to snap input points to");
        TextField snapField = new TextField();
        snapField.setPromptText("e.g. 0.1, 0.2, 0.5 or 0:0.2:1.0");
        snapField.setTooltip(new javafx.scene.control.Tooltip(
            "Enter comma-separated X values or ranges. Examples:\n" +
            "- Single values: 0.1,0.5,1.0\n" +
            "- Colon range: start:step:end (e.g. 0:0.2:1.0)\n" +
            "- Dot-dot range: start..end:step (e.g. 0..1:0.25)\n" +
            "- Dash/slash range: start-end/step (e.g. 0-1/0.25)\n" +
            "Ranges are inclusive and limited to 10000 generated values."));
        snapField.setPrefColumnCount(20);

        CheckBox snapExistingCheck = new CheckBox("Also snap existing points");
        AccessibilityHelper.setCheckBoxAccessibility(snapExistingCheck, "Snap existing points", "When checked, existing dataset points will be snapped to the configured X values");

    // Snap lines visibility and style controls
    CheckBox showSnapLinesCheck = new CheckBox("Show snap lines");
    showSnapLinesCheck.setSelected(true);
    AccessibilityHelper.setCheckBoxAccessibility(showSnapLinesCheck, "Show snap lines", "Toggle visibility of vertical snap lines on the canvas");

    javafx.scene.control.ColorPicker snapColorPicker = new javafx.scene.control.ColorPicker(javafx.scene.paint.Color.LIGHTGRAY);
    snapColorPicker.setTooltip(new javafx.scene.control.Tooltip("Choose color for snap lines"));

    javafx.scene.control.ComboBox<String> snapStyleBox = new javafx.scene.control.ComboBox<>(
        javafx.collections.FXCollections.observableArrayList("Dotted", "Dashed", "Solid")
    );
    snapStyleBox.getSelectionModel().select("Dotted");
    snapStyleBox.setTooltip(new javafx.scene.control.Tooltip("Line style for snap lines"));

    Label snapInfo = new Label("? ");
    snapInfo.setStyle("-fx-font-weight: bold; -fx-border-color: transparent; -fx-padding: 0 6 0 6;");
    javafx.scene.control.Tooltip.install(snapInfo, new javafx.scene.control.Tooltip(
        "Snap target lines show the X positions you configured. \n" +
        "When points are added or you choose to snap existing points, their X values will be moved to the nearest snap target.\n" +
        "Snap lines are not shown until the image has been calibrated."
    ));

        Button applyBtn = new Button("Apply");
        applyBtn.setOnAction(evt -> {
            String text = snapField.getText();
            if (text == null || text.trim().isEmpty()) {
                AccessibilityHelper.announceAction("No snap values provided");
                return;
            }

            String[] parts = text.split(",");
            java.util.List<Double> xs = new java.util.ArrayList<>();
            final int MAX_GENERATED = 10000;

            java.util.regex.Pattern pColon = java.util.regex.Pattern.compile("^\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*$");
            java.util.regex.Pattern pDot = java.util.regex.Pattern.compile("^\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*\\.\\.\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*:\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*$");
            java.util.regex.Pattern pDash = java.util.regex.Pattern.compile("^\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*-\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*/\\s*([+-]?[0-9]*\\.?[0-9]+)\\s*$");

            for (String p : parts) {
                String token = p.trim();
                if (token.isEmpty()) continue;

                java.util.regex.Matcher m;
                // Colon form: start:step:end
                m = pColon.matcher(token);
                if (m.matches()) {
                    try {
                        double start = Double.parseDouble(m.group(1));
                        double step = Double.parseDouble(m.group(2));
                        double end = Double.parseDouble(m.group(3));
                        if (step == 0.0) {
                            AccessibilityHelper.announceAction("Step cannot be zero in range: " + token);
                            logger.warn("Zero step in snap range: {}", token);
                            return;
                        }
                        int generated = 0;
                        if (step > 0) {
                            for (double v = start; v <= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        } else {
                            for (double v = start; v >= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        }
                        if (generated > MAX_GENERATED) {
                            AccessibilityHelper.announceAction("Range generates too many values: " + token);
                            logger.warn("Snap range {} produced more than {} values", token, MAX_GENERATED);
                            return;
                        }
                        continue;
                    } catch (NumberFormatException nfe) {
                        AccessibilityHelper.announceAction("Invalid numeric value in range: " + token);
                        logger.warn("Invalid numeric in snap range: {}", token, nfe);
                        return;
                    }
                }

                // Dot-dot form: start..end:step
                m = pDot.matcher(token);
                if (m.matches()) {
                    try {
                        double start = Double.parseDouble(m.group(1));
                        double end = Double.parseDouble(m.group(2));
                        double step = Double.parseDouble(m.group(3));
                        if (step == 0.0) {
                            AccessibilityHelper.announceAction("Step cannot be zero in range: " + token);
                            logger.warn("Zero step in snap range: {}", token);
                            return;
                        }
                        int generated = 0;
                        if (step > 0) {
                            for (double v = start; v <= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        } else {
                            for (double v = start; v >= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        }
                        if (generated > MAX_GENERATED) {
                            AccessibilityHelper.announceAction("Range generates too many values: " + token);
                            logger.warn("Snap range {} produced more than {} values", token, MAX_GENERATED);
                            return;
                        }
                        continue;
                    } catch (NumberFormatException nfe) {
                        AccessibilityHelper.announceAction("Invalid numeric value in range: " + token);
                        logger.warn("Invalid numeric in snap range: {}", token, nfe);
                        return;
                    }
                }

                // Dash/slash form: start-end/step
                m = pDash.matcher(token);
                if (m.matches()) {
                    try {
                        double start = Double.parseDouble(m.group(1));
                        double end = Double.parseDouble(m.group(2));
                        double step = Double.parseDouble(m.group(3));
                        if (step == 0.0) {
                            AccessibilityHelper.announceAction("Step cannot be zero in range: " + token);
                            logger.warn("Zero step in snap range: {}", token);
                            return;
                        }
                        int generated = 0;
                        if (step > 0) {
                            for (double v = start; v <= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        } else {
                            for (double v = start; v >= end; v = v + step) {
                                xs.add(v);
                                if (++generated > MAX_GENERATED) break;
                            }
                        }
                        if (generated > MAX_GENERATED) {
                            AccessibilityHelper.announceAction("Range generates too many values: " + token);
                            logger.warn("Snap range {} produced more than {} values", token, MAX_GENERATED);
                            return;
                        }
                        continue;
                    } catch (NumberFormatException nfe) {
                        AccessibilityHelper.announceAction("Invalid numeric value in range: " + token);
                        logger.warn("Invalid numeric in snap range: {}", token, nfe);
                        return;
                    }
                }

                // Fallback: single numeric value
                try {
                    double v = Double.parseDouble(token);
                    xs.add(v);
                } catch (NumberFormatException nfe) {
                    AccessibilityHelper.announceAction("Invalid number: " + token);
                    logger.warn("Invalid snap value entered: {}", token, nfe);
                    return;
                }
            }

            if (xs.isEmpty()) {
                AccessibilityHelper.announceAction("No valid snap values parsed");
                return;
            }

            canvasPanel.getSnapper().setSnapXValues(xs);
            // Optionally snap existing points
            if (snapExistingCheck.isSelected()) {
                int changed = 0;
                for (Dataset ds : datasets) {
                    List<com.digitizer.core.Point> pts = ds.getPoints();
                    for (int i = 0; i < pts.size(); i++) {
                        com.digitizer.core.Point p = pts.get(i);
                        com.digitizer.core.Point snapped = canvasPanel.getSnapper().snapPoint(p);
                        if (snapped.x() != p.x()) {
                            pts.set(i, snapped);
                            changed++;
                        }
                    }
                }
                AccessibilityHelper.announceAction("Snapped existing points: " + changed + " updated");
                logger.info("Snapped {} existing points to configured X values", changed);
            }
            AccessibilityHelper.announceAction("Snap X values configured: " + xs.size() + " values");
            logger.info("Configured {} snap X values", xs.size());
            // Apply visual settings from the controls
            canvasPanel.setSnapLinesVisible(showSnapLinesCheck.isSelected());
            canvasPanel.setSnapLineColor(snapColorPicker.getValue());
            canvasPanel.setSnapLineStyle(snapStyleBox.getSelectionModel().getSelectedItem());
            // Request a redraw so snap lines and any updated points are visible immediately
            canvasPanel.redraw();
        });

        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(evt -> {
            canvasPanel.getSnapper().clearSnapXValues();
            snapField.clear();
            AccessibilityHelper.announceAction("Snap X values cleared");
            logger.info("Cleared snap X values");
            canvasPanel.redraw();
        });

        HBox snapBox = new HBox(8, snapField, applyBtn, clearBtn);
        snapBox.setPadding(new Insets(4, 0, 0, 0));

        HBox snapControls = new HBox(8,
                showSnapLinesCheck,
                new Label("Color:"), snapColorPicker,
                new Label("Style:"), snapStyleBox,
                snapInfo
        );
        snapControls.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(snapLabel, snapBox, snapExistingCheck, snapControls);
    }

    /**
     * Rebuilds the dataset info area showing a colored swatch and dataset name/point count.
     * This method can be called externally after programmatic changes to dataset colors/names.
     */
    public void refreshDatasetInfoDisplay() {
        // Find the dataset info box (it is the child after the first separator)
        // Simpler: search children for the VBox that contains dataset info by looking for a VBox with Labels
        javafx.scene.layout.VBox datasetInfoBox = null;
        for (javafx.scene.Node n : getChildren()) {
            if (n instanceof javafx.scene.layout.VBox vb) {
                datasetInfoBox = vb;
                break;
            }
        }
        if (datasetInfoBox == null) return;
        datasetInfoBox.getChildren().clear();
        for (com.digitizer.core.Dataset dataset : this.datasets) {
            // Swatch box
            javafx.scene.layout.Region swatch = new javafx.scene.layout.Region();
            swatch.setMinSize(14, 14);
            swatch.setMaxSize(14, 14);
            swatch.setPrefSize(14, 14);
            // Apply background color and border
            String hex = dataset.getHexColor();
            swatch.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #00000055; -fx-border-width: 0.5; -fx-background-radius:2; -fx-border-radius:2;", hex));
            // Make swatch focusable and provide focus styling for keyboard users
            swatch.setFocusTraversable(true);
            swatch.focusedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    swatch.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #000000; -fx-border-width: 1.5; -fx-background-radius:2; -fx-border-radius:2;", hex));
                } else {
                    swatch.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #00000055; -fx-border-width: 0.5; -fx-background-radius:2; -fx-border-radius:2;", hex));
                }
            });

            javafx.scene.text.Text datasetInfo = new javafx.scene.text.Text(dataset.getName() + " (" + dataset.getPoints().size() + " points)");
            String colorName = getColorName(dataset.getHexColor());
            AccessibilityHelper.setTextAccessibility(datasetInfo,
                dataset.getName() + ", " + colorName + ", containing " +
                dataset.getPoints().size() + " data points",
                "Dataset");
            AccessibilityHelper.announceColor(dataset.getName(), dataset.getHexColor(), colorName);
            swatch.setAccessibleText("Color swatch for " + dataset.getName() + ", " + colorName);
            // Apply strikethrough and muted style when hidden
            if (!dataset.isVisible()) {
                datasetInfo.setStrikethrough(true);
                datasetInfo.setStyle("-fx-fill: #777777; -fx-opacity: 0.9;");
            } else {
                datasetInfo.setStrikethrough(false);
                datasetInfo.setStyle("");
            }
            // Inline color picker for per-dataset customization
            javafx.scene.control.ColorPicker picker = new javafx.scene.control.ColorPicker(dataset.getColor());
            picker.setPrefWidth(36);
            picker.setTooltip(new javafx.scene.control.Tooltip("Change color for " + dataset.getName()));
            picker.setOnAction(evt -> {
                javafx.scene.paint.Color c = picker.getValue();
                String newHex = com.digitizer.core.ColorUtils.colorToHex(c);
                dataset.setHexColor(newHex);
                // Update swatch style
                swatch.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #00000055; -fx-border-width: 0.5; -fx-background-radius:2; -fx-border-radius:2;", newHex));
                // Announce the color change
                String newName = getColorName(newHex);
                AccessibilityHelper.announceColor(dataset.getName(), newHex, newName);
                // Redraw canvas to reflect change
                if (this.canvasPanel != null) this.canvasPanel.redraw();
                // Persist per-dataset colors to accessibility prefs so app-level color overrides survive restarts
                if (this.accessibilityPrefs != null) {
                    String[] hexes = new String[this.datasets.size()];
                    for (int i = 0; i < this.datasets.size(); i++) hexes[i] = this.datasets.get(i).getHexColor();
                    this.accessibilityPrefs.setDatasetColors(hexes);
                }
            });

            // Align controls: spacer ensures picker/checkboxes align at right
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            HBox line = new HBox(8, swatch, datasetInfo, spacer, picker);
            line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            // Visibility checkbox
            javafx.scene.control.CheckBox visibleBox = new javafx.scene.control.CheckBox("Visible");
            visibleBox.setSelected(dataset.isVisible());
            visibleBox.setMinWidth(70);
            visibleBox.setPrefWidth(72);
            visibleBox.setOnAction(evt -> {
                boolean vis = visibleBox.isSelected();
                if (this.undoManager != null) {
                    UndoManager.ToggleVisibilityAction act = new UndoManager.ToggleVisibilityAction(dataset, !vis, vis);
                    this.undoManager.push(act);
                } else {
                    dataset.setVisible(vis);
                    if (this.accessibilityPrefs != null) {
                        String[] visArr = new String[this.datasets.size()];
                        for (int i = 0; i < this.datasets.size(); i++) visArr[i] = String.valueOf(this.datasets.get(i).isVisible());
                        this.accessibilityPrefs.setDatasetVisibilities(visArr);
                    }
                    if (this.canvasPanel != null) this.canvasPanel.redraw();
                }
                AccessibilityHelper.announceAction(dataset.getName() + (vis ? " shown" : " hidden"));
            });
            line.getChildren().add(visibleBox);
            // Axis assignment: Primary / Secondary (secondary maps to right-hand axis)
            javafx.scene.control.CheckBox axisCheck = new javafx.scene.control.CheckBox("Secondary Y");
            axisCheck.setSelected(dataset.isUseSecondaryYAxis());
            axisCheck.setTooltip(new javafx.scene.control.Tooltip("When checked, this dataset is plotted against the secondary (right-hand) Y axis"));
            axisCheck.setOnAction(evt -> {
                boolean useSec = axisCheck.isSelected();
                dataset.setUseSecondaryYAxis(useSec);
                if (this.canvasPanel != null) this.canvasPanel.redraw();
                AccessibilityHelper.announceAction(dataset.getName() + (useSec ? " assigned to secondary Y axis" : " assigned to primary Y axis"));
            });
            axisCheck.setMinWidth(120);
            axisCheck.setPrefWidth(140);
            line.getChildren().add(axisCheck);
            line.setPadding(new Insets(2, 0, 0, 0));
            datasetInfoBox.getChildren().add(line);
        }
    }
    /**
     * Programmatically select a dataset by index (0-based) and update the title field.
     */
    public void selectDataset(int index) {
        if (datasetSelector == null) return;
        int safe = Math.max(0, Math.min(index, datasetSelector.getItems().size() - 1));
        datasetSelector.getSelectionModel().select(safe);
        // Update title field to reflect selected dataset
        if (safe >= 0 && safe < datasets.size()) {
            // nothing else to do; the selector's listener will update UI
        }
    }

    /**
     * Returns the currently selected dataset index (0-based). Safe to call any time.
     */
    public int getSelectedDatasetIndex() {
        if (datasetSelector == null) return 0;
        int idx = datasetSelector.getSelectionModel().getSelectedIndex();
        return idx < 0 ? 0 : idx;
    }

    /**
     * Toggle visibility for the dataset at the given index and refresh UI.
     * This honors the undo manager when present.
     */
    public void toggleVisibility(int index) {
        if (index < 0 || index >= this.datasets.size()) return;
        Dataset ds = this.datasets.get(index);
        boolean newVis = !ds.isVisible();
        if (this.undoManager != null) {
            UndoManager.ToggleVisibilityAction act = new UndoManager.ToggleVisibilityAction(ds, ds.isVisible(), newVis);
            this.undoManager.push(act);
        } else {
            ds.setVisible(newVis);
            if (this.accessibilityPrefs != null) {
                String[] visArr = new String[this.datasets.size()];
                for (int i = 0; i < this.datasets.size(); i++) visArr[i] = String.valueOf(this.datasets.get(i).isVisible());
                this.accessibilityPrefs.setDatasetVisibilities(visArr);
            }
            if (this.canvasPanel != null) this.canvasPanel.redraw();
            refreshDatasetInfoDisplay();
        }
        AccessibilityHelper.announceAction(ds.getName() + (newVis ? " shown" : " hidden"));
    }

    // Public setters so MainWindow can update the fields when importing a project
    public void setTitle(String title) {
        if (this.titleField != null) this.titleField.setText(title == null ? "" : title);
    }

    public void setXLabel(String xlabel) {
        if (this.xlabelField != null) this.xlabelField.setText(xlabel == null ? "" : xlabel);
    }

    public void setYLabel(String ylabel) {
        if (this.ylabelField != null) this.ylabelField.setText(ylabel == null ? "" : ylabel);
    }

    /**
     * Sets the secondary (right-hand) Y-axis label text.
     * @param y2label secondary y label
     */
    public void setY2Label(String y2label) {
        if (this.y2labelField != null) this.y2labelField.setText(y2label == null ? "" : y2label);
    }

    /**
     * Returns the current secondary (right-hand) Y-axis label text.
     * @return secondary y label
     */
    public String getY2Label() {
        return y2labelField == null ? "" : y2labelField.getText();
    }

    /**
     * Returns the current plot title (may be empty).
     * @return title text
     */
    public String getTitle() {
        return titleField == null ? "" : titleField.getText();
    }

    /**
     * Returns the current X-axis label text.
     * @return x label
     */
    public String getXLabel() {
        return xlabelField == null ? "" : xlabelField.getText();
    }

    /**
     * Returns the current Y-axis label text.
     * @return y label
     */
    public String getYLabel() {
        return ylabelField == null ? "" : ylabelField.getText();
    }
    
    /**
     * Maps hex color codes to accessible color names.
     * This helps screen reader users understand which color is being used.
     *
     * @param hexColor the hex color code
     * @return an accessible color name
     */
    private static final java.util.Map<String, String> NAMED_COLORS = new java.util.HashMap<>() {{
        put("#0072B2", "Blue"); put("#E69F00", "Orange"); put("#009E73", "Green"); put("#CC79A7", "Pink"); put("#F0E442", "Yellow"); put("#56B4E9", "Light Blue");
        put("#377EB8", "Blue"); put("#FF7F00", "Orange"); put("#4DAF4A", "Green"); put("#F781BF", "Pink"); put("#A65628", "Brown"); put("#984EA3", "Purple");
        // Common web color names
        put("#FFFFFF", "White"); put("#000000", "Black"); put("#FF0000", "Red"); put("#00FF00", "Lime"); put("#0000FF", "Blue");
    }};

    private String getColorName(String hexColor) {
        if (hexColor == null) return "Unknown";
        String key = hexColor.toUpperCase();
        if (!key.startsWith("#")) key = "#" + key;
        if (NAMED_COLORS.containsKey(key)) return NAMED_COLORS.get(key);

        // Fallback: find nearest named color by RGB distance
        javafx.scene.paint.Color c = com.digitizer.core.ColorUtils.hexToColor(key);
        double cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue();
        String bestName = null;
        double bestDist = Double.MAX_VALUE;
        for (java.util.Map.Entry<String, String> e : NAMED_COLORS.entrySet()) {
            javafx.scene.paint.Color nc = com.digitizer.core.ColorUtils.hexToColor(e.getKey());
            double dr = cr - nc.getRed();
            double dg = cg - nc.getGreen();
            double db = cb - nc.getBlue();
            double dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                bestName = e.getValue();
            }
        }
        if (bestName != null) return bestName + " (approx.)";
        return "Color " + hexColor;
    }
}
