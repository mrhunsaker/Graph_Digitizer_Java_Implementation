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

package com.digitizer.core;

import javafx.geometry.Point2D;

/**
 * Handles coordinate transformations between data space and canvas (pixel)
 * space.
 * <p>
 * The transformer converts between the application's numeric data units and
 * image pixel positions (the image's natural pixel coordinate system).
 * Note: the {@link CanvasPanel} renders the image into the JavaFX Canvas
 * at a scaled size and with offsets; therefore the {@code CoordinateTransformer}
 * works in image (natural) pixel coordinates, and callers in the UI layer
 * should convert between image pixels and canvas coordinates when drawing
 * or handling mouse events. The transformer respects
 * {@link com.digitizer.core.CalibrationState} (anchor points and log flags)
 * and provides helper methods used by the {@link com.digitizer.ui.CanvasPanel}
 * and {@link com.digitizer.image.AutoTracer}.
 */
public class CoordinateTransformer {

    private final CalibrationState calibration;

    /**
     * Constructs a new CoordinateTransformer with the given calibration state.
     *
     * @param calibration the calibration state containing anchor points and ranges
     */
    public CoordinateTransformer(CalibrationState calibration) {
        this.calibration = calibration;
    }

    /**
    * Transforms data coordinates to image-pixel coordinates.
     *
     * @param dataX the x value in data space
     * @param dataY the y value in data space
     * @return a Point2D in canvas space, or (0, 0) if not calibrated
     */
    public Point2D dataToCanvas(double dataX, double dataY) {
        return dataToCanvas(dataX, dataY, false);
    }

    /**
    * Transforms data coordinates to image-pixel coordinates selecting primary or secondary Y axis.
     *
     * @param dataX the x value in data space
     * @param dataY the y value in data space
     * @param useSecondaryY whether to use the secondary Y axis ranges/log flag
     * @return a Point2D in canvas space, or (0, 0) if not calibrated
     */
    public Point2D dataToCanvas(double dataX, double dataY, boolean useSecondaryY) {
        if (!calibration.isCalibrated()) {
            return new Point2D(0, 0);
        }

        // X transformation
        double xPixel1 = calibration.getPixelXMin().getX();
        double xPixel2 = calibration.getPixelXMax().getX();
        double t = calculateFraction(dataX, calibration.getDataXMin(), calibration.getDataXMax(),
                                    calibration.isXLog());
        double canvasX = xPixel1 + t * (xPixel2 - xPixel1);

        // Y transformation (primary or secondary numeric range)
        double yPixel1 = calibration.getPixelYMin().getY();
        double yPixel2 = calibration.getPixelYMax().getY();
        double minY = calibration.getDataYMin();
        double maxY = calibration.getDataYMax();
        boolean yLog = calibration.isYLog();
        if (useSecondaryY && calibration.getDataY2Min() != null && calibration.getDataY2Max() != null) {
            minY = calibration.getDataY2Min();
            maxY = calibration.getDataY2Max();
            if (calibration.isY2Log() != null) yLog = calibration.isY2Log();
        }

        double u = calculateFraction(dataY, minY, maxY, yLog);
        double canvasY = yPixel1 + u * (yPixel2 - yPixel1);

        return new Point2D(canvasX, canvasY);
    }

    /**
    * Transforms image-pixel coordinates to data coordinates.
     *
     * @param canvasX the x value in canvas space
     * @param canvasY the y value in canvas space
     * @return a Point2D in data space, or (0, 0) if not calibrated
     */
    public Point2D canvasToData(double canvasX, double canvasY) {
        return canvasToData(canvasX, canvasY, false);
    }

    /**
    * Transforms image-pixel coordinates to data coordinates selecting primary or secondary Y axis.
     *
     * @param canvasX the x value in canvas space
     * @param canvasY the y value in canvas space
     * @param useSecondaryY whether to interpret the Y value against the secondary Y axis
     * @return a Point2D in data space, or (0, 0) if not calibrated
     */
    public Point2D canvasToData(double canvasX, double canvasY, boolean useSecondaryY) {
        if (!calibration.isCalibrated()) {
            return new Point2D(0, 0);
        }

        // X inverse transformation
        double xPixel1 = calibration.getPixelXMin().getX();
        double xPixel2 = calibration.getPixelXMax().getX();
        double denomX = xPixel2 - xPixel1;
        double t = (denomX == 0) ? 0 : (canvasX - xPixel1) / denomX;
        double dataX = invertFraction(t, calibration.getDataXMin(), calibration.getDataXMax(),
                                     calibration.isXLog());

        // Y inverse transformation (select primary or secondary ranges)
        double yPixel1 = calibration.getPixelYMin().getY();
        double yPixel2 = calibration.getPixelYMax().getY();
        double denomY = yPixel2 - yPixel1;
        double u = (denomY == 0) ? 0 : (canvasY - yPixel1) / denomY;

        double minY = calibration.getDataYMin();
        double maxY = calibration.getDataYMax();
        boolean yLog = calibration.isYLog();
        if (useSecondaryY && calibration.getDataY2Min() != null && calibration.getDataY2Max() != null) {
            minY = calibration.getDataY2Min();
            maxY = calibration.getDataY2Max();
            if (calibration.isY2Log() != null) yLog = calibration.isY2Log();
        }
        double dataY = invertFraction(u, minY, maxY, yLog);

        return new Point2D(dataX, dataY);
    }

    /**
     * Calculates the fractional position of a data value within its range.
     *
     * @param value the data value
     * @param min   the minimum value of the range
     * @param max   the maximum value of the range
     * @param isLog whether the scale is logarithmic (base 10)
     * @return a value typically in [0, 1] representing the position within the range
     */
    private double calculateFraction(double value, double min, double max, boolean isLog) {
        if (isLog) {
            if (value <= 0 || min <= 0) {
                return 0.0;
            }
            double num = Math.log10(value) - Math.log10(min);
            double den = Math.log10(max) - Math.log10(min);
            return (den == 0) ? 0.0 : num / den;
        } else {
            double den = max - min;
            return (den == 0) ? 0.0 : (value - min) / den;
        }
    }

    /**
     * Inverts the fraction calculation to get a data value from a fraction.
     *
     * @param fraction the fractional position (typically in [0, 1])
     * @param min      the minimum value of the range
     * @param max      the maximum value of the range
     * @param isLog    whether the scale is logarithmic (base 10)
     * @return the corresponding data value
     */
    private double invertFraction(double fraction, double min, double max, boolean isLog) {
        if (isLog) {
            double logMin = Math.log10(min);
            double logMax = Math.log10(max);
            double logValue = logMin + fraction * (logMax - logMin);
            return Math.pow(10, logValue);
        } else {
            return min + fraction * (max - min);
        }
    }
}
