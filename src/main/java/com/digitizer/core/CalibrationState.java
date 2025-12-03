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

import java.util.Objects;

import javafx.geometry.Point2D;

/**
 * Represents the calibration state of the graph digitizer.
 * <p>
 * Calibration consists of four anchor points in pixel/canvas coordinates
 * (left X, right X, bottom Y, top Y) as well as the numeric axis ranges
 * (dataXMin, dataXMax, dataYMin, dataYMax). Both linear and
 * logarithmic axes are supported. The object tracks whether a complete
 * calibration has been applied using {@link #isCalibrated()}.
 */
public class CalibrationState {
    
    // Calibration anchor points in pixel/canvas coordinates
    private Point2D pixelXMin;      // X-left pixel position
    private Point2D pixelXMax;      // X-right pixel position
    private Point2D pixelYMin;      // Y-bottom pixel position
    private Point2D pixelYMax;      // Y-top pixel position

    // Numeric axis ranges
    private double dataXMin;
    private double dataXMax;
    private double dataYMin;
    private double dataYMax;
    // Optional secondary Y axis numeric range and log flag
    private Double dataY2Min = null;
    private Double dataY2Max = null;
    private Boolean y2Log = null;

    // Log scale flags
    private boolean xLog;
    private boolean yLog;

    /**
     * Constructs a new uncalibrated CalibrationState.
     */
    public CalibrationState() {
        this.pixelXMin = null;
        this.pixelXMax = null;
        this.pixelYMin = null;
        this.pixelYMax = null;
        this.dataXMin = 0.0;
        this.dataXMax = 1.0;
        this.dataYMin = 0.0;
        this.dataYMax = 1.0;
        this.xLog = false;
        this.yLog = false;
    }

    // Getters and Setters

    public Point2D getPixelXMin() {
        return pixelXMin;
    }

    /**
     * Sets the pixel coordinate of the left X calibration anchor.
     *
     * @param pixelXMin the pixel Point2D for the left X anchor (may be null)
     */
    public void setPixelXMin(Point2D pixelXMin) {
        this.pixelXMin = pixelXMin;
    }

    public Point2D getPixelXMax() {
        return pixelXMax;
    }

    /**
     * Sets the pixel coordinate of the right X calibration anchor.
     *
     * @param pixelXMax the pixel Point2D for the right X anchor (may be null)
     */
    public void setPixelXMax(Point2D pixelXMax) {
        this.pixelXMax = pixelXMax;
    }

    public Point2D getPixelYMin() {
        return pixelYMin;
    }

    /**
     * Sets the pixel coordinate of the bottom Y calibration anchor.
     *
     * @param pixelYMin the pixel Point2D for the bottom Y anchor (may be null)
     */
    public void setPixelYMin(Point2D pixelYMin) {
        this.pixelYMin = pixelYMin;
    }

    public Point2D getPixelYMax() {
        return pixelYMax;
    }

    /**
     * Sets the pixel coordinate of the top Y calibration anchor.
     *
     * @param pixelYMax the pixel Point2D for the top Y anchor (may be null)
     */
    public void setPixelYMax(Point2D pixelYMax) {
        this.pixelYMax = pixelYMax;
    }

    public double getDataXMin() {
        return dataXMin;
    }

    /**
     * Sets the numeric minimum value for the X axis (data coordinates).
     *
     * @param dataXMin minimum X data value
     */
    public void setDataXMin(double dataXMin) {
        this.dataXMin = dataXMin;
    }

    public double getDataXMax() {
        return dataXMax;
    }

    /**
     * Sets the numeric maximum value for the X axis (data coordinates).
     *
     * @param dataXMax maximum X data value
     */
    public void setDataXMax(double dataXMax) {
        this.dataXMax = dataXMax;
    }

    public double getDataYMin() {
        return dataYMin;
    }

    /**
     * Sets the numeric minimum value for the primary Y axis (data coordinates).
     *
     * @param dataYMin minimum Y data value
     */
    public void setDataYMin(double dataYMin) {
        this.dataYMin = dataYMin;
    }

    public double getDataYMax() {
        return dataYMax;
    }

    /**
     * Sets the numeric maximum value for the primary Y axis (data coordinates).
     *
     * @param dataYMax maximum Y data value
     */
    public void setDataYMax(double dataYMax) {
        this.dataYMax = dataYMax;
    }

    public Double getDataY2Min() {
        return dataY2Min;
    }

    public void setDataY2Min(Double dataY2Min) {
        this.dataY2Min = dataY2Min;
    }

    public Double getDataY2Max() {
        return dataY2Max;
    }

    public void setDataY2Max(Double dataY2Max) {
        this.dataY2Max = dataY2Max;
    }

    public boolean isXLog() {
        return xLog;
    }

    public void setXLog(boolean xLog) {
        this.xLog = xLog;
    }

    public boolean isYLog() {
        return yLog;
    }

    public void setYLog(boolean yLog) {
        this.yLog = yLog;
    }

    public Boolean isY2Log() {
        return y2Log;
    }

    public void setY2Log(Boolean y2Log) {
        this.y2Log = y2Log;
    }

    /**
     * Checks if all four calibration anchors have been set.
     *
     * @return true if all anchors are non-null, false otherwise
     */
    public boolean isCalibrated() {
        return pixelXMin != null && pixelXMax != null && pixelYMin != null && pixelYMax != null;
    }

    /**
     * Resets calibration to uncalibrated state.
     */
    public void reset() {
        this.pixelXMin = null;
        this.pixelXMax = null;
        this.pixelYMin = null;
        this.pixelYMax = null;
    }

    @Override
    public String toString() {
        return String.format(
                "CalibrationState{" +
                        "pixelXMin=%s, pixelXMax=%s, pixelYMin=%s, pixelYMax=%s, " +
                        "dataX=[%.2f, %.2f], dataY=[%.2f, %.2f], " +
                        "xLog=%b, yLog=%b}",
                pixelXMin, pixelXMax, pixelYMin, pixelYMax,
                dataXMin, dataXMax, dataYMin, dataYMax,
                xLog, yLog
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalibrationState that)) return false;
        return Double.compare(that.dataXMin, dataXMin) == 0 &&
               Double.compare(that.dataXMax, dataXMax) == 0 &&
               Double.compare(that.dataYMin, dataYMin) == 0 &&
               Double.compare(that.dataYMax, dataYMax) == 0 &&
               xLog == that.xLog &&
               yLog == that.yLog &&
               Objects.equals(pixelXMin, that.pixelXMin) &&
               Objects.equals(pixelXMax, that.pixelXMax) &&
               Objects.equals(pixelYMin, that.pixelYMin) &&
               Objects.equals(pixelYMax, that.pixelYMax);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pixelXMin, pixelXMax, pixelYMin, pixelYMax,
                          dataXMin, dataXMax, dataYMin, dataYMax, xLog, yLog);
    }
}
