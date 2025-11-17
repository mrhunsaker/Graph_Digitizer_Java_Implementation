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
import java.util.Objects;

/**
 * Represents the calibration state of the graph digitizer.
 * This includes the four calibration anchor points and the numeric axis ranges.
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

    public void setPixelXMin(Point2D pixelXMin) {
        this.pixelXMin = pixelXMin;
    }

    public Point2D getPixelXMax() {
        return pixelXMax;
    }

    public void setPixelXMax(Point2D pixelXMax) {
        this.pixelXMax = pixelXMax;
    }

    public Point2D getPixelYMin() {
        return pixelYMin;
    }

    public void setPixelYMin(Point2D pixelYMin) {
        this.pixelYMin = pixelYMin;
    }

    public Point2D getPixelYMax() {
        return pixelYMax;
    }

    public void setPixelYMax(Point2D pixelYMax) {
        this.pixelYMax = pixelYMax;
    }

    public double getDataXMin() {
        return dataXMin;
    }

    public void setDataXMin(double dataXMin) {
        this.dataXMin = dataXMin;
    }

    public double getDataXMax() {
        return dataXMax;
    }

    public void setDataXMax(double dataXMax) {
        this.dataXMax = dataXMax;
    }

    public double getDataYMin() {
        return dataYMin;
    }

    public void setDataYMin(double dataYMin) {
        this.dataYMin = dataYMin;
    }

    public double getDataYMax() {
        return dataYMax;
    }

    public void setDataYMax(double dataYMax) {
        this.dataYMax = dataYMax;
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
