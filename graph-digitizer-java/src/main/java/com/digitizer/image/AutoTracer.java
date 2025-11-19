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

    /**
     * Performs a seed-based trace starting from a user-provided seed pixel.
     * The algorithm follows the line left and right from the seed, constraining
     * the vertical search to a small window around the previous hit. This helps
     * follow dashed or interrupted lines and avoid jumping to other same-color
     * regions.
     *
     * @param targetDataset dataset providing the target color and secondary Y flag
     * @param seedPixelX seed X coordinate in image pixels
     * @param seedPixelY seed Y coordinate in image pixels
     * @param windowHalfHeight vertical search window half-height in pixels (e.g. 8)
     * @param tolerance maximum RGB distance to accept a match (0..~1.732)
     * @param maxGap maximum consecutive columns to tolerate with no match before stopping
     * @return traced points in data coordinates (one per found column)
     */
    public List<com.digitizer.core.Point> traceFromSeed(Dataset targetDataset,
                                                       int seedPixelX, int seedPixelY,
                                                       int windowHalfHeight, double tolerance,
                                                       int maxGap) {
        List<com.digitizer.core.Point> tracedPoints = new ArrayList<>();
        PixelReader reader = image.getPixelReader();
        if (reader == null) return tracedPoints;

        Color targetColor = targetDataset.getColor();
        int imageHeight = (int) image.getHeight();

        // Helper to scan in one horizontal direction (dir = -1 left, +1 right)
        java.util.List<int[]> collect = new java.util.ArrayList<>();

        // Include the seed as the central point
        collect.add(new int[]{seedPixelX, seedPixelY});

        for (int dir : new int[]{-1, 1}) {
            int consecutiveMisses = 0;
            int prevY = seedPixelY;
            int x = seedPixelX + dir;
            while (x >= startPixelX && x <= endPixelX) {
                int yMin = Math.max(0, prevY - windowHalfHeight);
                int yMax = Math.min(imageHeight - 1, prevY + windowHalfHeight);
                double bestDist = Double.MAX_VALUE;
                int bestY = -1;
                for (int y = yMin; y <= yMax; y++) {
                    Color pixelColor = reader.getColor(x, y);
                    double dist = ColorUtils.colorDistance(targetColor, pixelColor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestY = y;
                    }
                }

                if (bestY != -1 && bestDist <= tolerance) {
                    // Accept and continue
                    if (dir < 0) {
                        // inserting at beginning for left side to keep increasing X order later
                        collect.add(0, new int[]{x, bestY});
                    } else {
                        collect.add(new int[]{x, bestY});
                    }
                    prevY = bestY;
                    consecutiveMisses = 0;
                    x += dir;
                } else {
                    consecutiveMisses++;
                    if (consecutiveMisses > maxGap) break;
                    x += dir;
                }
            }
        }

        // Convert collected pixel positions into data coordinates using transformer
        for (int[] px : collect) {
            int pxX = px[0];
            int pxY = px[1];
            javafx.geometry.Point2D data = transformer.canvasToData(pxX, pxY, targetDataset.isUseSecondaryYAxis());
            tracedPoints.add(new com.digitizer.core.Point(data.getX(), data.getY()));
        }

        return tracedPoints;
    }

    /**
     * Trace from a seed using an explicit target color and optional horizontal lookahead.
     * This variant uses the sampled seed color (recommended for dashed lines) rather than
     * the dataset's stored color.
     *
     * @param targetColor the color to match
     * @param useSecondaryYAxis whether to convert Y using secondary axis
     * @param seedPixelX seed X in image pixels
     * @param seedPixelY seed Y in image pixels
     * @param windowHalfHeight vertical search half-window
     * @param tolerance RGB distance tolerance to accept a match
     * @param maxGap maximum consecutive missed columns allowed
     * @param lookahead maximum horizontal columns to search ahead to bridge dashes
     * @return traced points in data coordinates
     */
    public List<com.digitizer.core.Point> traceFromSeedColor(Color targetColor, boolean useSecondaryYAxis,
                                                            int seedPixelX, int seedPixelY,
                                                            int windowHalfHeight, double tolerance,
                                                            int maxGap, int lookahead) {
        List<com.digitizer.core.Point> tracedPoints = new ArrayList<>();
        PixelReader reader = image.getPixelReader();
        if (reader == null) return tracedPoints;

        int imageHeight = (int) image.getHeight();

        java.util.List<int[]> collect = new java.util.ArrayList<>();
        collect.add(new int[]{seedPixelX, seedPixelY});

        for (int dir : new int[]{-1, 1}) {
            int consecutiveMisses = 0;
            int prevY = seedPixelY;
            int x = seedPixelX + dir;
            while (x >= startPixelX && x <= endPixelX) {
                int yMin = Math.max(0, prevY - windowHalfHeight);
                int yMax = Math.min(imageHeight - 1, prevY + windowHalfHeight);
                double bestDist = Double.MAX_VALUE;
                int bestY = -1;
                for (int y = yMin; y <= yMax; y++) {
                    Color pixelColor = reader.getColor(x, y);
                    double dist = ColorUtils.colorDistance(targetColor, pixelColor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestY = y;
                    }
                }

                if (bestY != -1 && bestDist <= tolerance) {
                    if (dir < 0) collect.add(0, new int[]{x, bestY}); else collect.add(new int[]{x, bestY});
                    prevY = bestY;
                    consecutiveMisses = 0;
                    x += dir;
                } else {
                    // Try short horizontal lookahead to bridge dashed lines
                    boolean foundAhead = false;
                    for (int h = 1; h <= lookahead && !foundAhead; h++) {
                        int xa = x + dir * h;
                        if (xa < startPixelX || xa > endPixelX) break;
                        int bestYa = -1;
                        double bestDa = Double.MAX_VALUE;
                        int yaMin = Math.max(0, prevY - windowHalfHeight);
                        int yaMax = Math.min(imageHeight - 1, prevY + windowHalfHeight);
                        for (int y = yaMin; y <= yaMax; y++) {
                            Color pc = reader.getColor(xa, y);
                            double d = ColorUtils.colorDistance(targetColor, pc);
                            if (d < bestDa) {
                                bestDa = d;
                                bestYa = y;
                            }
                        }
                        if (bestYa != -1 && bestDa <= tolerance) {
                            // Accept ahead match and fill gap by adding this point at xa
                            if (dir < 0) collect.add(0, new int[]{xa, bestYa}); else collect.add(new int[]{xa, bestYa});
                            prevY = bestYa;
                            x = xa + dir; // continue after the found column
                            foundAhead = true;
                            consecutiveMisses = 0;
                            break;
                        }
                    }

                    if (!foundAhead) {
                        consecutiveMisses++;
                        if (consecutiveMisses > maxGap) break;
                        x += dir;
                    }
                }
            }
        }

        for (int[] px : collect) {
            int pxX = px[0];
            int pxY = px[1];
            javafx.geometry.Point2D data = transformer.canvasToData(pxX, pxY, useSecondaryYAxis);
            tracedPoints.add(new com.digitizer.core.Point(data.getX(), data.getY()));
        }

        return tracedPoints;
    }
}
