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

package com.digitizer.integration;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.CoordinateTransformer;
import com.digitizer.core.Dataset;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for calibration and coordinate transformation workflows.
 */
class CalibrationIntegrationTest {

    private CalibrationState calibration;
    private CoordinateTransformer transformer;

    @BeforeEach
    void setUp() {
        calibration = new CalibrationState();
    }

    @Test
    void testSimpleLinearCalibration() {
        // Set up a simple calibration: pixel (0,0) -> data (0,0), pixel (100,100) -> data (10,10)
        calibration.setPixelXMin(new Point2D(0, 50));   // X-left at pixel x=0
        calibration.setPixelXMax(new Point2D(100, 50)); // X-right at pixel x=100
        calibration.setPixelYMin(new Point2D(50, 100)); // Y-bottom at pixel y=100
        calibration.setPixelYMax(new Point2D(50, 0));   // Y-top at pixel y=0

        calibration.setDataXMin(0.0);
        calibration.setDataXMax(10.0);
        calibration.setDataYMin(0.0);
        calibration.setDataYMax(10.0);
        calibration.setXLog(false);
        calibration.setYLog(false);

        assertTrue(calibration.isCalibrated(), "Calibration should be complete");

        transformer = new CoordinateTransformer(calibration);

        // Test canvas to data
        Point2D dataPoint = transformer.canvasToData(50, 50);
        assertEquals(5.0, dataPoint.getX(), 0.01, "X coordinate should be correctly transformed");
        assertEquals(5.0, dataPoint.getY(), 0.01, "Y coordinate should be correctly transformed");

        // Test data to canvas
        Point2D canvasPoint = transformer.dataToCanvas(5.0, 5.0);
        assertEquals(50.0, canvasPoint.getX(), 0.01, "Canvas X should be correctly transformed");
        assertEquals(50.0, canvasPoint.getY(), 0.01, "Canvas Y should be correctly transformed");
    }

    @Test
    void testCalibrationRoundTrip() {
        // Set up calibration
        calibration.setPixelXMin(new Point2D(10, 10));
        calibration.setPixelXMax(new Point2D(310, 10));
        calibration.setPixelYMin(new Point2D(10, 210));
        calibration.setPixelYMax(new Point2D(10, 10));

        calibration.setDataXMin(0.0);
        calibration.setDataXMax(100.0);
        calibration.setDataYMin(0.0);
        calibration.setDataYMax(50.0);

        transformer = new CoordinateTransformer(calibration);

        // Test round-trip transformation
        double originalDataX = 25.0;
        double originalDataY = 12.5;

        Point2D canvasPoint = transformer.dataToCanvas(originalDataX, originalDataY);
        Point2D dataPoint = transformer.canvasToData(canvasPoint.getX(), canvasPoint.getY());

        assertEquals(originalDataX, dataPoint.getX(), 0.01, "X should survive round-trip");
        assertEquals(originalDataY, dataPoint.getY(), 0.01, "Y should survive round-trip");
    }

    @Test
    void testUncalibratedState() {
        assertFalse(calibration.isCalibrated(), "Newly created calibration should not be calibrated");

        calibration.setPixelXMin(new Point2D(0, 0));
        assertFalse(calibration.isCalibrated(), "Partial calibration should not be complete");

        calibration.setPixelXMax(new Point2D(100, 0));
        calibration.setPixelYMin(new Point2D(0, 100));
        assertFalse(calibration.isCalibrated(), "Three anchors should not be enough");

        calibration.setPixelYMax(new Point2D(0, 0));
        assertTrue(calibration.isCalibrated(), "Four anchors should complete calibration");
    }

    @Test
    void testCalibrationReset() {
        calibration.setPixelXMin(new Point2D(0, 0));
        calibration.setPixelXMax(new Point2D(100, 0));
        calibration.setPixelYMin(new Point2D(0, 100));
        calibration.setPixelYMax(new Point2D(0, 0));

        assertTrue(calibration.isCalibrated());

        calibration.reset();

        assertFalse(calibration.isCalibrated(), "Reset should clear calibration");
        assertNull(calibration.getPixelXMin(), "Anchors should be null after reset");
    }
}
