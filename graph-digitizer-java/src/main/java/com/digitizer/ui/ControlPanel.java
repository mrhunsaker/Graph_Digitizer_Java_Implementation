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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Control panel for managing calibration and dataset parameters.
 * Provides an organized interface for dataset selection and management.
 * Tab-accessible with full screen reader support.
 */
public class ControlPanel extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);

    // Exposed fields for title and axis labels so MainWindow can read them when saving
    private TextField titleField;
    private TextField xlabelField;
    private TextField ylabelField;

    /**
     * Constructs a new ControlPanel with accessibility features.
     *
     * @param calibration the calibration state
     * @param datasets    the datasets
     * @param canvasPanel the canvas panel
     */
    public ControlPanel(CalibrationState calibration, List<Dataset> datasets, CanvasPanel canvasPanel) {
        setSpacing(10);
        setPadding(new Insets(10));
        
        // Create section header and dataset selector
        Label datasetLabel = new Label("Series Selection:");
        AccessibilityHelper.setLabelAccessibility(datasetLabel, "Series Selection:", "Section heading");
        datasetLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Selector for Dataset 1..6
        ComboBox<String> datasetSelector = new ComboBox<>(FXCollections.observableArrayList(
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
        Runnable refreshDatasetInfo = () -> {
            datasetInfoBox.getChildren().clear();
            for (Dataset dataset : datasets) {
                Label datasetInfo = new Label(dataset.getName() + " (" + dataset.getPoints().size() + " points)");
                String colorName = getColorName(dataset.getHexColor());
                AccessibilityHelper.setLabelAccessibility(datasetInfo,
                        dataset.getName() + ", " + colorName + ", containing " +
                        dataset.getPoints().size() + " data points",
                        "Dataset");
                AccessibilityHelper.announceColor(dataset.getName(), dataset.getHexColor(), colorName);
                datasetInfoBox.getChildren().add(datasetInfo);
            }
        };

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
            refreshDatasetInfo.run();
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

        Label yMinLabel = new Label("Y Min:");
        AccessibilityHelper.setLabelAccessibility(yMinLabel, "Y Min:", "Form label");
        TextField yMinField = new TextField("0");
        AccessibilityHelper.setTextFieldAccessibility(yMinField, "Minimum Y Value", "0", "Minimum value on the Y axis. Bottom-click on image to set automatically.");

        Label yMaxLabel = new Label("Y Max:");
        AccessibilityHelper.setLabelAccessibility(yMaxLabel, "Y Max:", "Form label");
        TextField yMaxField = new TextField("1");
        AccessibilityHelper.setTextFieldAccessibility(yMaxField, "Maximum Y Value", "1", "Maximum value on the Y axis. Top-click on image to set automatically.");

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

            boolean ok = canvasPanel.confirmCalibration(dxmin, dxmax, dymin, dymax, xlog, ylog);
            if (ok) {
                AccessibilityHelper.announceAction("Calibration applied");
            }
        });

        getChildren().addAll(
                calibLabel,
                new HBox(10, xMinLabel, xMinField),
                new HBox(10, xMaxLabel, xMaxField),
                new HBox(10, yMinLabel, yMinField),
                new HBox(10, yMaxLabel, yMaxField),
                xLogCheckBox,
                yLogCheckBox,
                applyCalibBtn
        );

        // Snap-to-X UI
        getChildren().add(new Separator());
        Label snapLabel = new Label("Snap X values (comma-separated):");
        AccessibilityHelper.setLabelAccessibility(snapLabel, "Snap X values", "Enter comma-separated X values to snap input points to");
        TextField snapField = new TextField();
        snapField.setPromptText("e.g. 0.1, 0.2, 0.5");
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
            for (String p : parts) {
                try {
                    String trimmed = p.trim();
                    if (trimmed.isEmpty()) continue;
                    double v = Double.parseDouble(trimmed);
                    xs.add(v);
                } catch (NumberFormatException nfe) {
                    AccessibilityHelper.announceAction("Invalid number: " + p.trim());
                    logger.warn("Invalid snap value entered: {}", p, nfe);
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
    private String getColorName(String hexColor) {
        return switch (hexColor.toUpperCase()) {
            case "#0072B2" -> "Blue";
            case "#E69F00" -> "Orange";
            case "#009E73" -> "Green";
            case "#CC79A7" -> "Pink";
            case "#F0E442" -> "Yellow";
            case "#56B4E9" -> "Light Blue";
            default -> "Color " + hexColor;
        };
    }
}
