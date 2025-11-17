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
import com.digitizer.core.CoordinateTransformer;
import com.digitizer.core.Dataset;
import com.digitizer.core.Point;
import com.digitizer.core.Snapper;
import com.digitizer.image.AutoTracer;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Canvas panel for displaying the image and data points.
 * Handles mouse interactions for calibration and point placement.
 */
public class CanvasPanel extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(CanvasPanel.class);

    private final Canvas canvas;
    private final CalibrationState calibration;
    private final List<Dataset> datasets;

    // Snapper for optional snap-to-X behavior (inactive until values configured)
    private final Snapper snapper = new Snapper();

    // Snap line visual properties (configurable)
    private javafx.scene.paint.Color snapLineColor = javafx.scene.paint.Color.LIGHTGRAY;
    private String snapLineStyle = "Dotted"; // options: Dotted, Dashed, Solid
    private boolean snapLinesVisible = true;

    private Image currentImage;
    private boolean calibrationMode = false;
    private List<Point2D> calibrationPoints = new ArrayList<>();

    private double displayScale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    // Node-level zoom (applied as scaleX/scaleY). 1.0 = 100%.
    private double zoom = 1.0;

    /**
     * Set the node-level zoom (scale) for the canvas. This uses node scaling so the
     * canvas content does not need to be redrawn at a different resolution.
     *
     * @param z scale factor (1.0 = 100%)
     */
    public void setZoom(double z) {
        if (z <= 0) return;
        this.zoom = z;
        javafx.application.Platform.runLater(() -> {
            canvas.setScaleX(z);
            canvas.setScaleY(z);
        });
    }

    /**
     * Current zoom factor.
     */
    public double getZoom() {
        return this.zoom;
    }

    /**
     * Compute and apply a zoom that fits the canvas into the provided viewport size.
     * If image/canvas is smaller than viewport this may return a value > 1.0.
     */
    public void fitToViewport(double viewportWidth, double viewportHeight) {
        if (canvas == null) return;
        double cw = canvas.getWidth();
        double ch = canvas.getHeight();
        if (cw <= 0 || ch <= 0) return;
        double sx = viewportWidth / cw;
        double sy = viewportHeight / ch;
        double s = Math.min(sx, sy);
        if (s <= 0) s = 1.0;
        setZoom(s);
    }

    /**
     * Constructs a new CanvasPanel with accessibility features.
     *
     * @param calibration the calibration state
     * @param datasets    the datasets
     */
    public CanvasPanel(CalibrationState calibration, List<Dataset> datasets) {
        this.calibration = calibration;
        this.datasets = datasets;

        // Create a canvas; size will be updated to image natural resolution when an image is loaded
        canvas = new Canvas(800, 600);
        canvas.setStyle("-fx-border-color: #cccccc;");
        
        // Accessibility setup for canvas
        canvas.setAccessibleText("Image Canvas");
        canvas.setAccessibleRoleDescription("Canvas for image display and point selection");
        canvas.setAccessibleHelp(
            "Click to place calibration points or data points. " +
            "Use arrow keys to adjust calibration points. " +
            "Press Enter to confirm calibration."
        );

        this.getChildren().add(canvas);

        setupMouseHandlers();
        setupKeyboardHandlers();
        redraw();
    }

    /**
     * Exposes the Snapper so other UI components (e.g. ControlPanel) can configure
     * the snap X values and tolerance.
     *
     * @return the Snapper instance used by this canvas
     */
    public Snapper getSnapper() {
        return snapper;
    }

    /** Set whether snap lines are visible (does not clear snap list). */
    public void setSnapLinesVisible(boolean visible) {
        this.snapLinesVisible = visible;
        javafx.application.Platform.runLater(this::redraw);
    }

    public boolean isSnapLinesVisible() {
        return this.snapLinesVisible;
    }

    public void setSnapLineColor(javafx.scene.paint.Color color) {
        if (color != null) {
            this.snapLineColor = color;
            javafx.application.Platform.runLater(this::redraw);
        }
    }

    public javafx.scene.paint.Color getSnapLineColor() {
        return this.snapLineColor;
    }

    public void setSnapLineStyle(String style) {
        if (style != null) {
            this.snapLineStyle = style;
            javafx.application.Platform.runLater(this::redraw);
        }
    }

    public String getSnapLineStyle() {
        return this.snapLineStyle;
    }

    /**
     * Loads an image from a file.
     *
     * @param file the image file
     */
    public void loadImage(java.io.File file) throws Exception {
        this.currentImage = new Image(new java.io.FileInputStream(file));
        logger.info("Loaded image: {}x{}", (int) currentImage.getWidth(), (int) currentImage.getHeight());

        // Resize the canvas to the image's natural resolution so the full image is available
        double w = currentImage.getWidth();
        double h = currentImage.getHeight();
        if (w > 0 && h > 0) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            // Also ensure this pane reports preferred size so parent ScrollPane can show full image
            this.setPrefWidth(w);
            this.setPrefHeight(h);
            displayScale = 1.0;
            // reset node scale to 100% when loading a new image
            setZoom(1.0);
        }

        redraw();
    }

    /**
     * Enters calibration mode for recording anchor points.
     */
    public void enterCalibrationMode() {
        this.calibrationMode = true;
        this.calibrationPoints.clear();
        logger.info("Entered calibration mode");
        redraw();
    }

    /**
     * Performs auto-trace on the active dataset.
     */
    public void performAutoTrace() {
        if (currentImage == null || datasets.isEmpty()) {
            throw new IllegalStateException("Image must be loaded first");
        }

        if (!calibration.isCalibrated()) {
            throw new IllegalStateException("Calibration must be applied first");
        }

        CoordinateTransformer transformer = new CoordinateTransformer(calibration);
        int startX = (int) Math.min(
                calibration.getPixelXMin().getX(),
                calibration.getPixelXMax().getX()
        );
        int endX = (int) Math.max(
                calibration.getPixelXMin().getX(),
                calibration.getPixelXMax().getX()
        );

        AutoTracer tracer = new AutoTracer(currentImage, transformer, startX, endX);
        Dataset activeDataset = datasets.get(0);
        List<Point> tracedPoints = tracer.traceDataset(activeDataset);

        activeDataset.clearPoints();
        activeDataset.getPoints().addAll(tracedPoints);

        logger.info("Auto-traced {} points", tracedPoints.size());
        redraw();
    }

    /**
     * Redraws the canvas.
     */
    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw image
        if (currentImage != null) {
            gc.drawImage(currentImage, offsetX, offsetY, 
                    currentImage.getWidth() * displayScale, 
                    currentImage.getHeight() * displayScale);
        }

        // Draw calibration points
        if (calibrationMode) {
            for (Point2D pt : calibrationPoints) {
                gc.setFill(Color.RED);
                gc.fillOval(pt.getX() - 5, pt.getY() - 5, 10, 10);
                gc.setStroke(Color.RED);
                gc.strokeOval(pt.getX() - 5, pt.getY() - 5, 10, 10);
            }
        }

        // Draw vertical snap lines for each configured snap X value (if calibrated and visible)
        if (calibration.isCalibrated() && !snapper.getSnapXValues().isEmpty() && snapLinesVisible) {
            gc.save();
            gc.setStroke(snapLineColor);
            gc.setLineWidth(1.0);
            // choose dash pattern based on style
            switch (snapLineStyle) {
                case "Dashed" -> gc.setLineDashes(12, 6);
                case "Solid" -> gc.setLineDashes(0);
                default -> gc.setLineDashes(6, 6); // Dotted
            }
            CoordinateTransformer transformer = new CoordinateTransformer(calibration);
            double dataYMin = calibration.getDataYMin();
            double dataYMax = calibration.getDataYMax();
            for (Double sx : snapper.getSnapXValues()) {
                try {
                    Point2D top = transformer.dataToCanvas(sx, dataYMax);
                    Point2D bottom = transformer.dataToCanvas(sx, dataYMin);
                    gc.strokeLine(top.getX(), top.getY(), bottom.getX(), bottom.getY());
                } catch (Exception e) {
                    // If transformation fails for any reason, skip this snap line
                    logger.debug("Skipping snap line for x={} due to: {}", sx, e.getMessage());
                }
            }
            gc.setLineDashes(0);
            gc.restore();
        }

        // Draw dataset points
        if (calibration.isCalibrated()) {
            for (Dataset dataset : datasets) {
                gc.setFill(dataset.getColor());
                for (Point point : dataset.getPoints()) {
                    Point2D canvasPoint = new CoordinateTransformer(calibration)
                            .dataToCanvas(point.x(), point.y());
                    gc.fillOval(canvasPoint.getX() - 3, canvasPoint.getY() - 3, 6, 6);
                }
            }
        }
    }

    private void setupMouseHandlers() {
        canvas.setOnMouseClicked(this::handleMouseClick);
    }

    private void setupKeyboardHandlers() {
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (calibrationMode && calibrationPoints.size() > 0) {
                        // Do not auto-apply on Enter; notify user of progress
                        AccessibilityHelper.announceProgress("Calibration points", calibrationPoints.size(), 4);
                    }
                    break;
                case ESCAPE:
                    if (calibrationMode) {
                        calibrationMode = false;
                        calibrationPoints.clear();
                        AccessibilityHelper.announceAction("Calibration mode cancelled");
                        redraw();
                    }
                    break;
                case DELETE:
                case BACK_SPACE:
                    if (calibrationMode && calibrationPoints.size() > 0) {
                        calibrationPoints.remove(calibrationPoints.size() - 1);
                        AccessibilityHelper.announceProgress("Calibration points", 
                            calibrationPoints.size(), 4);
                        redraw();
                    }
                    break;
                default:
                    // Other keys ignored
                    break;
            }
            event.consume();
        });
    }

    private void handleMouseClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (calibrationMode) {
            if (calibrationPoints.size() < 4) {
                calibrationPoints.add(new Point2D(x, y));
                String pointName = "";
                switch (calibrationPoints.size()) {
                    case 1 -> pointName = "Left X value";
                    case 2 -> pointName = "Right X value";
                    case 3 -> pointName = "Bottom Y value";
                    case 4 -> pointName = "Top Y value";
                }
                
                logger.info("Calibration point {}: ({}, {})", calibrationPoints.size(), x, y);
                AccessibilityHelper.announceCoordinates("Calibration point " + 
                    calibrationPoints.size() + " (" + pointName + ")", x, y);
                AccessibilityHelper.announceProgress("Calibration points", 
                    calibrationPoints.size(), 4);
                if (calibrationPoints.size() == 4) {
                    // Do not auto-apply calibration. Let the user confirm via the ControlPanel "Apply Calibration" button.
                    AccessibilityHelper.announceAction("4 calibration points set. Press Apply Calibration to confirm.");
                }
            }
            redraw();
        }
        else {
            CoordinateTransformer transformer = new CoordinateTransformer(calibration);
            javafx.geometry.Point2D dataPt = transformer.canvasToData(x, y);
            Point newPoint = new Point(dataPt.getX(), dataPt.getY());

            boolean snapped = false;
            // Apply snapping only if snap values exist and calibration is set
            if (!snapper.getSnapXValues().isEmpty() && calibration.isCalibrated()) {
                Point snappedPoint = snapper.snapPoint(newPoint);
                if (snappedPoint.x() != newPoint.x()) {
                    newPoint = snappedPoint;
                    snapped = true;
                }
            }

            Dataset active = datasets.get(0);
            active.getPoints().add(newPoint);
            logger.info("Added point {} to dataset {} (snapped={})", newPoint, active.getName(), snapped);
            AccessibilityHelper.announceAction(String.format("Point added: X=%.6f, Y=%.6f%s", newPoint.x(), newPoint.y(), snapped ? " (snapped)" : ""));
            redraw();
        }
    }

    /**
     * Applies the collected calibration anchor points and numeric ranges.
     * This method is intended to be invoked by a UI control (e.g., ControlPanel Apply button)
     * so calibration isn't applied automatically.
     *
     * @param dataXMin numeric X minimum provided by the user
     * @param dataXMax numeric X maximum provided by the user
     * @param dataYMin numeric Y minimum provided by the user
     * @param dataYMax numeric Y maximum provided by the user
     * @param xLog whether X axis should be logarithmic
     * @param yLog whether Y axis should be logarithmic
     * @return true if calibration applied, false if insufficient anchor points
     */
    public boolean confirmCalibration(double dataXMin, double dataXMax,
                                      double dataYMin, double dataYMax,
                                      boolean xLog, boolean yLog) {
        if (calibrationPoints.size() < 4) {
            AccessibilityHelper.announceAction("Insufficient calibration points. Please set 4 anchor points first.");
            return false;
        }

        calibration.setPixelXMin(calibrationPoints.get(0));
        calibration.setPixelXMax(calibrationPoints.get(1));
        calibration.setPixelYMin(calibrationPoints.get(2));
        calibration.setPixelYMax(calibrationPoints.get(3));

        calibration.setDataXMin(dataXMin);
        calibration.setDataXMax(dataXMax);
        calibration.setDataYMin(dataYMin);
        calibration.setDataYMax(dataYMax);
        calibration.setXLog(xLog);
        calibration.setYLog(yLog);

        calibrationMode = false;
        logger.info("Calibration applied: {}", calibration);
        redraw();
        AccessibilityHelper.announceAction("Calibration applied");
        return true;
    }
    
}
