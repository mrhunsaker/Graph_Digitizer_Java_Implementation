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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Canvas panel for displaying the image and data points and handling
 * pointer-based interactions (calibration, point placement, and auto-trace).
 *
 * <p>The {@link CanvasPanel} owns the JavaFX {@link javafx.scene.canvas.Canvas}
 * that renders images and points. Important coordinate-system notes:
 * <ul>
 *   <li>The {@link com.digitizer.core.CoordinateTransformer} maps between numeric
 *   data values and the image's natural pixel coordinates (image pixels).</li>
 *   <li>The {@code CanvasPanel} renders the image at a scaled size using
 *   {@code displayScale} and an (optional) {@code offsetX}/{@code offsetY}.
 *   Therefore the UI must convert between image-pixel coordinates and
 *   canvas coordinates when drawing or interpreting mouse events. Helper
 *   methods {@link #imageToCanvas(Point2D)} and {@link #canvasToImage(Point2D)}
 *   perform that conversion.</li>
 *   <li>Calibration anchors are stored in image-pixel coordinates in
 *   {@link com.digitizer.core.CalibrationState}. This makes transforms
 *   independent of the current zoom or viewport offsets.</li>
 * </ul>
 *
 * <p>This class exposes operations used by {@link ControlPanel} and
 * {@link MainWindow} including:
 * <ul>
 *   <li>{@link #loadImage(java.io.File)}</li>
 *   <li>{@link #enterCalibrationMode()}</li>
 *   <li>{@link #confirmCalibration(double, double, double, double, boolean, boolean, Double, Double, Boolean)}</li>
 *   <li>{@link #performAutoTrace()}</li>
 * </ul>
 */
public class CanvasPanel extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(CanvasPanel.class);

    private final Canvas canvas;
    private final CalibrationState calibration;
    private final List<Dataset> datasets;

    // Snapper for optional snap-to-X behavior (inactive until values configured)
    private final Snapper snapper = new Snapper();

    // Snap line visual properties (configurable)
    /** Color used to render vertical snap guide lines. */
    private javafx.scene.paint.Color snapLineColor = javafx.scene.paint.Color.LIGHTGRAY;
    /** The style used for snap lines - "Dotted", "Dashed" or "Solid". */
    private String snapLineStyle = "Dotted"; // options: Dotted, Dashed, Solid
    private boolean snapLinesVisible = true;

    // Accessibility properties
    private double pointSize = 6.0;
    private boolean useShapeVariation = true;
    private int selectedCalibrationPoint = -1;
    // Selected dataset/point for keyboard-based editing when not in calibration mode
    private int selectedDatasetIndex = -1;
    private int selectedPointIndex = -1;
    // Active dataset index for new points / auto-trace (driven by ControlPanel selector)
    private int activeDatasetIndex = 0;

    private Image currentImage;
    /** Whether the panel is in calibration mode (recording anchor points). */
    private boolean calibrationMode = false;
    private List<Point2D> calibrationPoints = new ArrayList<>();

    // Auto-trace seed capture mode
    private boolean autoTraceSeedMode = false;
    private int autoTraceExpectedSeeds = 0;
    private List<Point2D> autoTraceSeeds = new ArrayList<>();
    // Default seed-based trace parameters
    private int seedWindowHalfHeight = 8;
    private double seedTolerance = 0.25; // RGB euclidean distance threshold
    private int seedMaxGap = 3;

    /** Display scale applied to image rendering (pixel multiplier). */
    private double displayScale = 1.0;
    /** Current horizontal offset in pixels for panning/centering. */
    private double offsetX = 0;
    /** Current vertical offset in pixels for panning/centering. */
    private double offsetY = 0;
    // Node-level zoom (applied as scaleX/scaleY). 1.0 = 100%.
    private double zoom = 1.0;

    /**
     * Convert a point expressed in image (natural) pixels to canvas coordinates
     * taking into account current offset and displayScale.
     */
    private Point2D imageToCanvas(Point2D img) {
        double cx = offsetX + img.getX() * displayScale;
        double cy = offsetY + img.getY() * displayScale;
        return new Point2D(cx, cy);
    }

    private Point2D imageToCanvas(double imgX, double imgY) {
        return imageToCanvas(new Point2D(imgX, imgY));
    }

    /**
     * Convert a point expressed in canvas coordinates to image (natural) pixels.
     */
    private Point2D canvasToImage(Point2D canvasPt) {
        double ix = (canvasPt.getX() - offsetX) / displayScale;
        double iy = (canvasPt.getY() - offsetY) / displayScale;
        return new Point2D(ix, iy);
    }

    private Point2D canvasToImage(double canvasX, double canvasY) {
        return canvasToImage(new Point2D(canvasX, canvasY));
    }

    /**
     * Set the node-level zoom (scale) for the canvas. This uses node scaling so the
     * canvas content does not need to be redrawn at a different resolution.
     *
     * @param z scale factor (1.0 = 100%)
     */
    public void setZoom(double z) {
        if (z <= 0) return;
        this.zoom = z;
        // Resize the canvas to reflect the requested zoom so parent ScrollPane
        // sees the correct content size and can provide scrolling to all pixels.
        javafx.application.Platform.runLater(() -> {
            try {
                if (currentImage != null) {
                    double w = currentImage.getWidth() * this.zoom;
                    double h = currentImage.getHeight() * this.zoom;
                    canvas.setWidth(Math.max(1.0, w));
                    canvas.setHeight(Math.max(1.0, h));
                    // Report preferred size so parent ScrollPane shows full content
                    this.setPrefWidth(canvas.getWidth());
                    this.setPrefHeight(canvas.getHeight());
                    // Update drawing scale used in redraw
                    this.displayScale = this.zoom;
                } else {
                    // No image loaded yet; just set node transforms to identity
                    canvas.setWidth(canvas.getWidth() * this.zoom);
                    canvas.setHeight(canvas.getHeight() * this.zoom);
                    this.setPrefWidth(canvas.getWidth());
                    this.setPrefHeight(canvas.getHeight());
                    this.displayScale = this.zoom;
                }
                // Ensure node-level scaling is not used (avoid double-scaling)
                canvas.setScaleX(1.0);
                canvas.setScaleY(1.0);
                redraw();
            } catch (Exception ignore) { }
        });
    }

    /**
     * Sets which dataset index is considered "active" for point additions and auto-trace.
     * This is intended to be driven by the dataset selector in {@link ControlPanel}.
     * @param idx 0-based dataset index
     */
    public void setActiveDatasetIndex(int idx) {
        if (idx < 0) idx = 0;
        if (datasets != null && idx >= datasets.size()) idx = Math.max(0, datasets.size() - 1);
        this.activeDatasetIndex = idx;
        javafx.application.Platform.runLater(this::redraw);
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
        // Compute zoom such that the image (natural resolution) fits into the
        // provided viewport. Use the image's natural size (currentImage) if available
        // so that we compute scale relative to the image pixels.
        if (currentImage != null) {
            double iw = currentImage.getWidth();
            double ih = currentImage.getHeight();
            if (iw <= 0 || ih <= 0) return;
            double sx = viewportWidth / iw;
            double sy = viewportHeight / ih;
            double s = Math.min(sx, sy);
            if (s <= 0) s = 1.0;
            setZoom(s);
        } else {
            double sx = viewportWidth / cw;
            double sy = viewportHeight / ch;
            double s = Math.min(sx, sy);
            if (s <= 0) s = 1.0;
            setZoom(s);
        }
    }

    /**
     * Constructs a new CanvasPanel with accessibility features.
     *
     * @param calibration the calibration state
     * @param datasets    the datasets
     */
    private final UndoManager undoManager;

    public CanvasPanel(CalibrationState calibration, List<Dataset> datasets, UndoManager undoManager) {
        this.calibration = calibration;
        this.datasets = datasets;
        this.undoManager = undoManager;

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
     * Sets the point size for rendering data points.
     *
     * @param size the point size in pixels
     */
    public void setPointSize(double size) {
        this.pointSize = Math.max(2.0, Math.min(20.0, size));
    }

    /**
     * Gets the current point size.
     *
     * @return the point size in pixels
     */
    public double getPointSize() {
        return this.pointSize;
    }

    /**
     * Sets whether to use shape variation for different datasets.
     *
     * @param useShapes true to use different shapes per dataset
     */
    public void setUseShapeVariation(boolean useShapes) {
        this.useShapeVariation = useShapes;
    }

    /**
     * Gets whether shape variation is enabled.
     *
     * @return true if using shape variation
     */
    public boolean isUseShapeVariation() {
        return this.useShapeVariation;
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
            // Set canvas to image natural size. setZoom will later resize the
            // canvas when a zoom is applied.
            canvas.setWidth(w);
            canvas.setHeight(h);
            this.setPrefWidth(w);
            this.setPrefHeight(h);
            displayScale = 1.0;
            // Ensure node-level scaling is cleared
            canvas.setScaleX(1.0);
            canvas.setScaleY(1.0);
            // Default zoom to 1.0 (100%) and resize canvas accordingly
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

        // Present a simple dialog to collect seed-based tracing options and number of seeds
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Auto Trace Options");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label lblCount = new Label("Number of lines to trace:");
        Spinner<Integer> spCount = new Spinner<>();
        spCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, datasets.size()), 1));

        Label lblWindow = new Label("Vertical window half-height (px):");
        Spinner<Integer> spWindow = new Spinner<>();
        spWindow.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, seedWindowHalfHeight));

        Label lblTol = new Label("Color tolerance (0.05..1.5):");
        TextField tfTol = new TextField(String.valueOf(seedTolerance));

        Label lblGap = new Label("Max gap (columns):");
        Spinner<Integer> spGap = new Spinner<>();
        spGap.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, seedMaxGap));

        grid.add(lblCount, 0, 0);
        grid.add(spCount, 1, 0);
        grid.add(lblWindow, 0, 1);
        grid.add(spWindow, 1, 1);
        grid.add(lblTol, 0, 2);
        grid.add(tfTol, 1, 2);
        grid.add(lblGap, 0, 3);
        grid.add(spGap, 1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int count = spCount.getValue();
                    int window = spWindow.getValue();
                    double tol = Double.parseDouble(tfTol.getText());
                    int gap = spGap.getValue();
                    // Enter seed-capture mode
                    this.autoTraceSeedMode = true;
                    this.autoTraceExpectedSeeds = Math.max(1, count);
                    this.autoTraceSeeds.clear();
                    this.seedWindowHalfHeight = Math.max(1, window);
                    this.seedTolerance = Math.max(0.0, tol);
                    this.seedMaxGap = Math.max(0, gap);
                    AccessibilityHelper.announceAction("Auto-trace: click " + this.autoTraceExpectedSeeds + " seed points on the image");
                } catch (Exception e) {
                    AccessibilityHelper.announceAction("Invalid auto-trace options");
                }
            }
        });
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
            // Draw the image scaled by displayScale which reflects the current zoom.
            gc.drawImage(currentImage, 0, 0,
                currentImage.getWidth(), currentImage.getHeight(),
                offsetX, offsetY,
                currentImage.getWidth() * displayScale,
                currentImage.getHeight() * displayScale);
        }

        // Draw calibration points with zoom compensation
        if (calibrationMode) {
            double effectiveCalibSize = 10.0 / zoom;
            double halfCalibSize = effectiveCalibSize / 2.0;
            for (int idx = 0; idx < calibrationPoints.size(); idx++) {
                Point2D pt = calibrationPoints.get(idx);
                // Highlight selected calibration point
                if (idx == selectedCalibrationPoint) {
                    gc.setFill(Color.YELLOW);
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(3.0);
                } else {
                    gc.setFill(Color.RED);
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(2.0);
                }
                gc.fillOval(pt.getX() - halfCalibSize, pt.getY() - halfCalibSize, 
                           effectiveCalibSize, effectiveCalibSize);
                gc.strokeOval(pt.getX() - halfCalibSize, pt.getY() - halfCalibSize, 
                             effectiveCalibSize, effectiveCalibSize);
            }
        }

        // Draw auto-trace seeds (if any) - show captured seeds or live capture markers
        if (!autoTraceSeeds.isEmpty() || autoTraceSeedMode) {
            double seedSize = 8.0 / zoom;
            double half = seedSize / 2.0;
            gc.save();
            gc.setLineWidth(2.0);
            gc.setStroke(Color.BLUE);
            gc.setFill(Color.color(0.2, 0.4, 1.0, 0.6));
            for (int i = 0; i < autoTraceSeeds.size(); i++) {
                Point2D img = autoTraceSeeds.get(i);
                Point2D canvasPt = imageToCanvas(img);
                double cx = canvasPt.getX();
                double cy = canvasPt.getY();
                gc.fillOval(cx - half, cy - half, seedSize, seedSize);
                gc.strokeOval(cx - half, cy - half, seedSize, seedSize);
                gc.setFill(Color.WHITE);
                gc.fillText(String.valueOf(i + 1), cx - 3, cy + 4);
                gc.setFill(Color.color(0.2, 0.4, 1.0, 0.6));
            }
            gc.restore();
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
                    // transformer returns image-pixel coordinates; convert to canvas coords
                    Point2D topImg = transformer.dataToCanvas(sx, dataYMax);
                    Point2D bottomImg = transformer.dataToCanvas(sx, dataYMin);
                    Point2D top = imageToCanvas(topImg);
                    Point2D bottom = imageToCanvas(bottomImg);
                    gc.strokeLine(top.getX(), top.getY(), bottom.getX(), bottom.getY());
                } catch (Exception e) {
                    // If transformation fails for any reason, skip this snap line
                    logger.debug("Skipping snap line for x={} due to: {}", sx, e.getMessage());
                }
            }
            gc.setLineDashes(0);
            gc.restore();
        }

        // Draw dataset points with shape variation support
        if (calibration.isCalibrated()) {
            for (int i = 0; i < datasets.size(); i++) {
                Dataset dataset = datasets.get(i);
                if (!dataset.isVisible()) continue; // skip invisible datasets
                gc.setFill(dataset.getColor());
                gc.setStroke(dataset.getColor());
                gc.setLineWidth(2.0);
                
                    for (Point point : dataset.getPoints()) {
                        // Respect dataset-level axis selection (primary or secondary Y)
                        boolean useSecondary = dataset.isUseSecondaryYAxis();
                            Point2D imgPoint = new CoordinateTransformer(calibration)
                                .dataToCanvas(point.x(), point.y(), useSecondary);
                            // Convert image-pixel point to canvas (screen) coordinates
                            Point2D canvasPoint = imageToCanvas(imgPoint);
                            double x = canvasPoint.getX();
                            double y = canvasPoint.getY();
                    // Maintain visual point size at all zoom levels by compensating for zoom
                    double effectivePointSize = pointSize / zoom;
                    double halfSize = effectivePointSize / 2.0;
                    
                    if (useShapeVariation) {
                        int shapeIndex = i % 4;
                        if (shapeIndex == 0) {
                            gc.fillOval(x - halfSize, y - halfSize, effectivePointSize, effectivePointSize);
                            gc.strokeOval(x - halfSize, y - halfSize, effectivePointSize, effectivePointSize);
                        } else if (shapeIndex == 1) {
                            gc.fillRect(x - halfSize, y - halfSize, effectivePointSize, effectivePointSize);
                            gc.strokeRect(x - halfSize, y - halfSize, effectivePointSize, effectivePointSize);
                        } else if (shapeIndex == 2) {
                            gc.fillPolygon(
                                new double[]{x, x - halfSize, x + halfSize},
                                new double[]{y - halfSize, y + halfSize, y + halfSize},
                                3
                            );
                            gc.strokePolygon(
                                new double[]{x, x - halfSize, x + halfSize},
                                new double[]{y - halfSize, y + halfSize, y + halfSize},
                                3
                            );
                        } else {
                            gc.fillPolygon(
                                new double[]{x, x - halfSize, x, x + halfSize},
                                new double[]{y - halfSize, y, y + halfSize, y},
                                4
                            );
                            gc.strokePolygon(
                                new double[]{x, x - halfSize, x, x + halfSize},
                                new double[]{y - halfSize, y, y + halfSize, y},
                                4
                            );
                        }
                    } else {
                        // Default circles for all datasets
                        gc.fillOval(x - halfSize, y - halfSize, pointSize, pointSize);
                        gc.strokeOval(x - halfSize, y - halfSize, pointSize, pointSize);
                    }
                }
            }
        }

        // Draw secondary Y axis (right-hand) if secondary numeric range is configured
        if (calibration.isCalibrated() && calibration.getDataY2Min() != null && calibration.getDataY2Max() != null) {
            CoordinateTransformer transformer = new CoordinateTransformer(calibration);
            double minY = calibration.getDataY2Min();
            double maxY = calibration.getDataY2Max();
            boolean y2Log = Boolean.TRUE.equals(calibration.isY2Log());

            // Number of ticks to render
            int ticks = 5;
            // Use the right pixel anchor X by transforming dataXMax
            double dataXForAxis = calibration.getDataXMax();
            // Use stroke and font for axis
            gc.save();
            gc.setStroke(Color.BLACK);
            gc.setFill(Color.BLACK);
            gc.setLineWidth(1.0);
            gc.setFont(javafx.scene.text.Font.font(11));

            for (int i = 0; i < ticks; i++) {
                double frac = (ticks == 1) ? 0.0 : ((double) i / (ticks - 1));
                double tickValue;
                if (y2Log) {
                    double logMin = Math.log10(minY);
                    double logMax = Math.log10(maxY);
                    double logV = logMin + frac * (logMax - logMin);
                    tickValue = Math.pow(10, logV);
                } else {
                    tickValue = minY + frac * (maxY - minY);
                }

                    Point2D tickImg = transformer.dataToCanvas(dataXForAxis, tickValue, true);
                    Point2D tickPt = imageToCanvas(tickImg);
                    double tickX = tickPt.getX();
                    double tickY = tickPt.getY();
                // Draw small tick to the right of axis anchor
                double tickStartX = tickX + 4;
                double tickEndX = tickX + 12;
                // Clamp within canvas bounds
                tickStartX = Math.min(tickStartX, canvas.getWidth() - 2);
                tickEndX = Math.min(tickEndX, canvas.getWidth() - 2);
                gc.strokeLine(tickStartX, tickY, tickEndX, tickY);
                // Draw label right of tick
                String label = formatNumberForLabel(tickValue);
                double textX = tickEndX + 4;
                double textY = tickY + 4; // baseline adjustment
                gc.fillText(label, Math.min(textX, canvas.getWidth() - 30), textY);
            }

            gc.restore();
        }
    }

    private String formatNumberForLabel(double v) {
        // Choose compact formatting: integer if close to int, else 3 significant digits
        if (Math.abs(v - Math.round(v)) < 1e-6) return String.format("%.0f", v);
        if (Math.abs(v) >= 1e4 || Math.abs(v) < 1e-3) return String.format("%.3e", v);
        return String.format("%.3f", v);
    }

    private void setupMouseHandlers() {
        canvas.setOnMouseClicked(this::handleMouseClick);
    }

    private void setupKeyboardHandlers() {
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (calibrationMode && !calibrationPoints.isEmpty()) {
                        AccessibilityHelper.announceProgress("Calibration points", calibrationPoints.size(), 4);
                    }
                }
                case ESCAPE -> {
                    if (calibrationMode) {
                        calibrationMode = false;
                        calibrationPoints.clear();
                        selectedCalibrationPoint = -1;
                        AccessibilityHelper.announceAction("Calibration mode cancelled");
                        redraw();
                    }
                }
                case DELETE, BACK_SPACE -> {
                    if (calibrationMode && !calibrationPoints.isEmpty()) {
                        calibrationPoints.remove(calibrationPoints.size() - 1);
                        if (selectedCalibrationPoint >= calibrationPoints.size()) {
                            selectedCalibrationPoint = calibrationPoints.size() - 1;
                        }
                        AccessibilityHelper.announceProgress("Calibration points", calibrationPoints.size(), 4);
                        redraw();
                    } else if (!calibrationMode && selectedDatasetIndex >= 0 && selectedPointIndex >= 0) {
                        Dataset ds = datasets.get(selectedDatasetIndex);
                        ds.removePoint(selectedPointIndex);
                        AccessibilityHelper.announceAction("Removed point " + (selectedPointIndex + 1) + " from " + ds.getName());
                        if (selectedPointIndex >= ds.getPoints().size()) {
                            selectedPointIndex = ds.getPoints().size() - 1;
                            if (selectedPointIndex < 0) {
                                selectedDatasetIndex = -1;
                                selectedPointIndex = -1;
                            }
                        }
                        redraw();
                    }
                }
                case TAB -> {
                    if (calibrationMode && !calibrationPoints.isEmpty()) {
                        if (event.isShiftDown()) {
                            selectedCalibrationPoint--;
                            if (selectedCalibrationPoint < 0) {
                                selectedCalibrationPoint = calibrationPoints.size() - 1;
                            }
                        } else {
                            selectedCalibrationPoint++;
                            if (selectedCalibrationPoint >= calibrationPoints.size()) {
                                selectedCalibrationPoint = 0;
                            }
                        }
                        AccessibilityHelper.announceAction("Selected calibration point " + (selectedCalibrationPoint + 1) + " of " + calibrationPoints.size());
                        redraw();
                        event.consume();
                        return;
                    }
                    if (!calibrationMode) {
                        if (datasets.isEmpty()) break;
                        int total = 0;
                        for (Dataset d : datasets) total += d.getPoints().size();
                        if (total == 0) break;
                        int flatIndex = -1;
                        if (selectedDatasetIndex >= 0 && selectedPointIndex >= 0) {
                            int idx = 0;
                            for (int i = 0; i < datasets.size(); i++) {
                                List<com.digitizer.core.Point> pts = datasets.get(i).getPoints();
                                if (i < selectedDatasetIndex) idx += pts.size();
                                else if (i == selectedDatasetIndex) {
                                    idx += selectedPointIndex;
                                    break;
                                }
                            }
                            flatIndex = idx;
                        }
                        if (event.isShiftDown()) flatIndex = (flatIndex <= 0) ? total - 1 : flatIndex - 1;
                        else flatIndex = (flatIndex + 1) % total;

                        int cursor = 0;
                        outer: for (int di = 0; di < datasets.size(); di++) {
                            List<com.digitizer.core.Point> pts = datasets.get(di).getPoints();
                            for (int pi = 0; pi < pts.size(); pi++) {
                                if (cursor == flatIndex) {
                                    selectedDatasetIndex = di;
                                    selectedPointIndex = pi;
                                    break outer;
                                }
                                cursor++;
                            }
                        }
                        if (selectedDatasetIndex >= 0 && selectedPointIndex >= 0) {
                            com.digitizer.core.Point p = datasets.get(selectedDatasetIndex).getPoints().get(selectedPointIndex);
                            AccessibilityHelper.announceAction("Selected point " + (selectedPointIndex + 1) + " in " + datasets.get(selectedDatasetIndex).getName() + " at X=" + String.format("%.4f", p.x()) + " Y=" + String.format("%.4f", p.y()));
                            redraw();
                        }
                    }
                }
                case LEFT, RIGHT, UP, DOWN -> {
                    if (calibrationMode && selectedCalibrationPoint >= 0 && selectedCalibrationPoint < calibrationPoints.size()) {
                        Point2D current = calibrationPoints.get(selectedCalibrationPoint);
                        double dx = 0, dy = 0;
                        double step = event.isControlDown() ? 10 : 1;
                        switch (event.getCode()) {
                            case LEFT -> dx = -step;
                            case RIGHT -> dx = step;
                            case UP -> dy = -step;
                            case DOWN -> dy = step;
                            default -> {}
                        }
                        Point2D newPoint = new Point2D(current.getX() + dx, current.getY() + dy);
                        calibrationPoints.set(selectedCalibrationPoint, newPoint);
                        AccessibilityHelper.announceAction("Moved calibration point " + (selectedCalibrationPoint + 1) + " to pixel " + (int)newPoint.getX() + ", " + (int)newPoint.getY());
                        redraw();
                        event.consume();
                        return;
                    }

                    if (!calibrationMode && selectedDatasetIndex >= 0 && selectedPointIndex >= 0) {
                        Dataset ds = datasets.get(selectedDatasetIndex);
                        com.digitizer.core.Point old = ds.getPoints().get(selectedPointIndex);
                        CoordinateTransformer transformer = new CoordinateTransformer(calibration);
                        boolean useSecondary = ds.isUseSecondaryYAxis();
                        // transformer.dataToCanvas returns image-pixel coords; convert to canvas coords
                        Point2D imgPt = transformer.dataToCanvas(old.x(), old.y(), useSecondary);
                        Point2D canvasPt = imageToCanvas(imgPt);
                        double stepPx = event.isControlDown() ? 10 : 1;
                        double nx = canvasPt.getX();
                        double ny = canvasPt.getY();
                        switch (event.getCode()) {
                            case LEFT -> nx -= stepPx;
                            case RIGHT -> nx += stepPx;
                            case UP -> ny -= stepPx;
                            case DOWN -> ny += stepPx;
                            default -> {}
                        }
                        // Convert back to image-pixel coordinates before asking transformer to invert
                        Point2D newImg = canvasToImage(nx, ny);
                        Point2D newData = transformer.canvasToData(newImg.getX(), newImg.getY(), useSecondary);
                        com.digitizer.core.Point newPoint = new com.digitizer.core.Point(newData.getX(), newData.getY());
                        if (undoManager != null) {
                            UndoManager.MovePointAction mpa = new UndoManager.MovePointAction(ds, selectedPointIndex, old, newPoint);
                            undoManager.push(mpa);
                        } else {
                            ds.getPoints().set(selectedPointIndex, newPoint);
                            redraw();
                        }
                        AccessibilityHelper.announceAction("Moved point " + (selectedPointIndex + 1) + " in " + ds.getName() + " to X=" + String.format("%.4f", newPoint.x()) + " Y=" + String.format("%.4f", newPoint.y()));
                    }
                }
                default -> {
                    // Other keys ignored
                }
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
            // If we are in auto-trace seed-capture mode, collect seed(s) and return
            if (autoTraceSeedMode) {
                Point2D imgCoords = canvasToImage(x, y);
                autoTraceSeeds.add(imgCoords);
                AccessibilityHelper.announceAction("Seed " + autoTraceSeeds.size() + " captured");
                redraw();

                if (autoTraceSeeds.size() >= autoTraceExpectedSeeds) {
                    // Exit seed capture mode and perform tracing on a background thread
                    List<Point2D> seedsCopy = new ArrayList<>(autoTraceSeeds);
                    int startX = (int) Math.min(
                            calibration.getPixelXMin().getX(),
                            calibration.getPixelXMax().getX()
                    );
                    int endX = (int) Math.max(
                            calibration.getPixelXMin().getX(),
                            calibration.getPixelXMax().getX()
                    );
                    this.autoTraceSeedMode = false;
                    this.autoTraceSeeds.clear();

                    // Run tracing off the FX thread
                    new Thread(() -> {
                        try {
                            CoordinateTransformer transformer = new CoordinateTransformer(calibration);
                            AutoTracer tracer = new AutoTracer(currentImage, transformer, startX, endX);
                            // After seeds captured, ask the user to confirm mapping seed -> dataset
                            javafx.application.Platform.runLater(() -> {
                                Dialog<ButtonType> mapDlg = new Dialog<>();
                                mapDlg.setTitle("Map Seeds to Datasets");
                                GridPane mapGrid = new GridPane();
                                mapGrid.setHgap(10);
                                mapGrid.setVgap(8);

                                List<javafx.scene.control.ComboBox<String>> boxes = new ArrayList<>();
                                for (int si = 0; si < seedsCopy.size(); si++) {
                                    mapGrid.add(new Label("Seed " + (si + 1) + ":"), 0, si);
                                    javafx.scene.control.ComboBox<String> cb = new javafx.scene.control.ComboBox<>();
                                    for (Dataset d : datasets) cb.getItems().add(d.getName());
                                    int def = Math.min(datasets.size() - 1, Math.max(0, activeDatasetIndex + si));
                                    cb.getSelectionModel().select(def);
                                    boxes.add(cb);
                                    mapGrid.add(cb, 1, si);
                                }

                                mapDlg.getDialogPane().setContent(mapGrid);
                                mapDlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                                mapDlg.showAndWait().ifPresent(mapBt -> {
                                    if (mapBt == ButtonType.OK) {
                                        // Run tracing for each mapping on a background thread
                                        new Thread(() -> {
                                            try {
                                                for (int si = 0; si < seedsCopy.size(); si++) {
                                                    Point2D seed = seedsCopy.get(si);
                                                    javafx.scene.control.ComboBox<String> cb = boxes.get(si);
                                                    int dsIdx = Math.max(0, Math.min(datasets.size() - 1, cb.getSelectionModel().getSelectedIndex()));
                                                    Dataset ds = datasets.get(dsIdx);
                                                    // Sample seed color at the seed pixel
                                                    Color seedColor = currentImage.getPixelReader().getColor((int)Math.round(seed.getX()), (int)Math.round(seed.getY()));
                                                    // Trace using seed color with a small lookahead to bridge dashes
                                                    List<com.digitizer.core.Point> traced = tracer.traceFromSeedColor(seedColor, ds.isUseSecondaryYAxis(), (int)Math.round(seed.getX()), (int)Math.round(seed.getY()), seedWindowHalfHeight, seedTolerance, seedMaxGap, 3);
                                                    // Post-process: median smoothing (window 3) and outlier removal (10% Y-range)
                                                    List<com.digitizer.core.Point> post = postProcessTracedPoints(traced, true, 3, 0.10, ds.isUseSecondaryYAxis());
                                                    final int assignIdx = dsIdx;
                                                    javafx.application.Platform.runLater(() -> {
                                                        Dataset target = datasets.get(assignIdx);
                                                        target.clearPoints();
                                                        target.getPoints().addAll(post);
                                                        redraw();
                                                    });
                                                }
                                                javafx.application.Platform.runLater(() -> AccessibilityHelper.announceAction("Auto-trace complete"));
                                            } catch (Exception ex) {
                                                logger.error("Error during seed-based auto-trace", ex);
                                                javafx.application.Platform.runLater(() -> AccessibilityHelper.announceAction("Auto-trace failed: " + ex.getMessage()));
                                            }
                                        }).start();
                                    } else {
                                        AccessibilityHelper.announceAction("Auto-trace cancelled");
                                    }
                                });
                            });
                        } catch (Exception ex) {
                            logger.error("Error during seed-based auto-trace", ex);
                            javafx.application.Platform.runLater(() -> AccessibilityHelper.announceAction("Auto-trace failed: " + ex.getMessage()));
                        }
                    }).start();
                }
                return;
            }
            CoordinateTransformer transformer = new CoordinateTransformer(calibration);
            boolean useSecondary = false;
            if (datasets != null && !datasets.isEmpty()) {
                int idx = Math.max(0, Math.min(activeDatasetIndex, datasets.size() - 1));
                useSecondary = datasets.get(idx).isUseSecondaryYAxis();
            }
            // Convert mouse canvas coords to image-pixel coordinates then to data
            Point2D imgCoords = canvasToImage(x, y);
            javafx.geometry.Point2D dataPt = transformer.canvasToData(imgCoords.getX(), imgCoords.getY(), useSecondary);
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

            int safeIdx = Math.max(0, Math.min(activeDatasetIndex, datasets.size() - 1));
            Dataset active = datasets.get(safeIdx);
            // Use undo manager for point addition
            if (undoManager != null) {
                UndoManager.AddPointAction action = new UndoManager.AddPointAction(active, newPoint);
                undoManager.push(action);
                AccessibilityHelper.announceAction(String.format("Point added: X=%.6f, Y=%.6f%s", newPoint.x(), newPoint.y(), snapped ? " (snapped)" : ""));
            } else {
                active.getPoints().add(newPoint);
                AccessibilityHelper.announceAction(String.format("Point added: X=%.6f, Y=%.6f%s", newPoint.x(), newPoint.y(), snapped ? " (snapped)" : ""));
                redraw();
            }
        }
    }

    /**
     * Applies the collected calibration anchor points and numeric ranges.
     * This method is intended to be invoked by a UI control (e.g., ControlPanel Apply button)
     * so calibration isn't applied automatically.
     *
     * @param dataXMin numeric X minimum provided by the user
     * @param dataXMax numeric X maximum provided by the user
     * @param dataYMin numeric Y minimum provided by the user (primary)
     * @param dataYMax numeric Y maximum provided by the user (primary)
     * @param xLog whether X axis should be logarithmic
     * @param yLog whether primary Y axis should be logarithmic
     * @param dataY2Min optional secondary Y minimum (nullable)
     * @param dataY2Max optional secondary Y maximum (nullable)
     * @param y2Log optional secondary Y log flag (nullable)
     * @return true if calibration applied, false if insufficient anchor points
     */
    public boolean confirmCalibration(double dataXMin, double dataXMax,
                                      double dataYMin, double dataYMax,
                                      boolean xLog, boolean yLog,
                                      Double dataY2Min, Double dataY2Max, Boolean y2Log) {
        if (calibrationPoints.size() < 4) {
            AccessibilityHelper.announceAction("Insufficient calibration points. Please set 4 anchor points first.");
            return false;
        }

        // Convert collected calibrationPoints (canvas coords) into image (natural) pixels
        Point2D p0 = canvasToImage(calibrationPoints.get(0));
        Point2D p1 = canvasToImage(calibrationPoints.get(1));
        Point2D p2 = canvasToImage(calibrationPoints.get(2));
        Point2D p3 = canvasToImage(calibrationPoints.get(3));
        calibration.setPixelXMin(p0);
        calibration.setPixelXMax(p1);
        calibration.setPixelYMin(p2);
        calibration.setPixelYMax(p3);

        calibration.setDataXMin(dataXMin);
        calibration.setDataXMax(dataXMax);
        calibration.setDataYMin(dataYMin);
        calibration.setDataYMax(dataYMax);
        calibration.setXLog(xLog);
        calibration.setYLog(yLog);

        // Optional secondary Y axis values
        if (dataY2Min != null && dataY2Max != null) {
            calibration.setDataY2Min(dataY2Min);
            calibration.setDataY2Max(dataY2Max);
            calibration.setY2Log(y2Log);
        } else {
            // Clear secondary axis when not provided
            calibration.setDataY2Min(null);
            calibration.setDataY2Max(null);
            calibration.setY2Log(null);
        }

        calibrationMode = false;
        logger.info("Calibration applied: {}", calibration);
        redraw();
        AccessibilityHelper.announceAction("Calibration applied");
        return true;
    }
    
    /**
     * Post-process traced points: optional median smoothing and outlier removal.
     * @param pts raw traced points (in data coordinates)
     * @param smooth whether to apply median smoothing
     * @param medianWindow odd window size (3..11)
     * @param outlierFraction remove jumps larger than this fraction of Y-range (0 disables)
     * @param useSecondary whether Y uses the secondary axis (for range calculation)
     * @return processed points
     */
    private List<com.digitizer.core.Point> postProcessTracedPoints(List<com.digitizer.core.Point> pts, boolean smooth, int medianWindow, double outlierFraction, boolean useSecondary) {
        List<com.digitizer.core.Point> result = new ArrayList<>();
        if (pts == null || pts.isEmpty()) return result;

        int n = pts.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = pts.get(i).x();
            ys[i] = pts.get(i).y();
        }

        double[] ys2 = ys.clone();
        if (smooth) {
            int half = Math.max(1, medianWindow / 2);
            for (int i = 0; i < n; i++) {
                int a = Math.max(0, i - half);
                int b = Math.min(n - 1, i + half);
                double[] window = new double[b - a + 1];
                for (int j = a; j <= b; j++) window[j - a] = ys[j];
                java.util.Arrays.sort(window);
                ys2[i] = window[window.length / 2];
            }
        }

        // Outlier removal based on Y-range fraction of chosen axis
        double yMin, yMax;
        if (useSecondary && calibration.getDataY2Min() != null && calibration.getDataY2Max() != null) {
            yMin = calibration.getDataY2Min();
            yMax = calibration.getDataY2Max();
        } else {
            yMin = calibration.getDataYMin();
            yMax = calibration.getDataYMax();
        }
        double yRange = Math.abs(yMax - yMin);
        double maxJump = (outlierFraction > 0 && yRange > 0) ? outlierFraction * yRange : Double.POSITIVE_INFINITY;

        // Build filtered result by removing points with large jumps compared to previous accepted point
        result.add(new com.digitizer.core.Point(xs[0], ys2[0]));
        for (int i = 1; i < n; i++) {
            double jump = Math.abs(ys2[i] - ys2[i - 1]);
            if (jump <= maxJump) {
                result.add(new com.digitizer.core.Point(xs[i], ys2[i]));
            } else {
                // skip this point as outlier
            }
        }

        return result;
    }
    
}
