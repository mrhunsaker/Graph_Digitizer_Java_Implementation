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

package com.digitizer.image;

import java.util.ArrayList;
import java.util.List;

import com.digitizer.core.ColorUtils;
import com.digitizer.core.CoordinateTransformer;
import com.digitizer.core.Dataset;
import com.digitizer.core.Point;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Performs auto-trace of curves in images using color matching.
 * <p>
 * This class performs a per-column scan and selects the pixel whose color
 * has the smallest RGB distance to the target dataset color. The resulting
 * pixels are converted into {@link com.digitizer.core.Point} coordinates
 * using a provided {@link com.digitizer.core.CoordinateTransformer}.
 *
 * <p>The algorithm is intentionally simple and robust; it is not a full
 * image processing pipeline — for most cases it returns a plausible
 * one-point-per-column representation of the curve that can then be
 * post-processed or smoothed by callers.
 *
 * <p><strong>Complexity:</strong> For an image of width W (scanned columns) and height H
 * the trace performs O(W * H) color distance computations and uses O(W) additional
 * memory for the output point list. This is acceptable for typical plot images; large
 * images can be sub-sampled (adjust start/end pixel X) if needed.
 */
public class AutoTracer {

    private final Image image;
    private final CoordinateTransformer transformer;
    private final int startPixelX;
    private final int endPixelX;

    /**
     * Constructs an AutoTracer for the given image and coordinate transformation.
     *
     * @param image       the image to trace
     * @param transformer the coordinate transformer for pixel-to-data conversion
     * @param startPixelX the starting x pixel coordinate (inclusive)
     * @param endPixelX   the ending x pixel coordinate (inclusive)
     */
    public AutoTracer(Image image, CoordinateTransformer transformer, int startPixelX, int endPixelX) {
        this.image = image;
        this.transformer = transformer;
        this.startPixelX = Math.max(0, startPixelX);
        this.endPixelX = Math.min((int) image.getWidth() - 1, endPixelX);
    }

    /**
     * Performs auto-tracing of a curve using color matching.
     * Scans column-by-column and selects the pixel with the minimum RGB distance to the target color.
     *
     * @param targetDataset the dataset whose color is used for matching
     * @return a list of traced points in data coordinates
     */
    public List<Point> traceDataset(Dataset targetDataset) {
        List<Point> tracedPoints = new ArrayList<>();
        PixelReader reader = image.getPixelReader();

        if (reader == null) {
            return tracedPoints;
        }

        Color targetColor = targetDataset.getColor();
        int imageHeight = (int) image.getHeight();

        // Scan columns from startPixelX to endPixelX
        for (int pixelX = startPixelX; pixelX <= endPixelX; pixelX++) {
            double bestDistance = Double.MAX_VALUE;
            int bestPixelY = -1;

            // Find the pixel in this column closest to the target color
            for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
                Color pixelColor = reader.getColor(pixelX, pixelY);
                double distance = ColorUtils.colorDistance(targetColor, pixelColor);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestPixelY = pixelY;
                }
            }

            // Convert the best pixel to data coordinates
            if (bestPixelY != -1) {
                Point2D dataCoords = transformer.canvasToData(pixelX, bestPixelY, targetDataset.isUseSecondaryYAxis());
                tracedPoints.add(new Point(dataCoords.getX(), dataCoords.getY()));
            }
        }

        return tracedPoints;
    }

    /**
     * Gets the number of columns to be scanned during auto-trace.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return Math.max(0, endPixelX - startPixelX + 1);
    }
}
