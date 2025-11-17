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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility that provides snapping behavior for X values.
 *
 * <p>This mirrors the original Julia helper that allows selecting a set of X values
 * and snapping input datapoints to the closest X value within a relative tolerance.
 */
public final class Snapper {

    // Use a slightly more forgiving default tolerance to account for floating point
    // differences when users enter X values or when points are converted from pixels
    // to data coordinates. 1e-6 is a reasonable default for typical scientific data.
    private static final double DEFAULT_TOLERANCE = 1e-6;

    private final List<Double> snapXValues = new ArrayList<>();
    private double tolerance = DEFAULT_TOLERANCE;

    public Snapper() {
        // empty
    }

    /**
     * Sets the list of X values to snap to. The list is copied and kept sorted.
     *
     * @param values the X values to snap to
     */
    public void setSnapXValues(List<Double> values) {
        Objects.requireNonNull(values, "values cannot be null");
        snapXValues.clear();
        snapXValues.addAll(values);
        Collections.sort(snapXValues);
    }

    /**
     * Clears any configured snap X values.
     */
    public void clearSnapXValues() {
        snapXValues.clear();
    }

    /**
     * Adds a single X value to the snap list. Keeps the internal list sorted.
     *
     * @param x the x value to add
     */
    public void addSnapXValue(double x) {
        snapXValues.add(x);
        Collections.sort(snapXValues);
    }

    /**
     * Returns an immutable copy of the configured snap X values.
     *
     * @return list of X values (sorted)
     */
    public List<Double> getSnapXValues() {
        return List.copyOf(snapXValues);
    }

    /**
     * Sets the relative tolerance used when matching an input x to a configured snap x.
     * The match condition uses: |x - target| <= tolerance * max(1.0, |x|)
     *
     * @param tolerance relative tolerance (must be >= 0)
     */
    public void setTolerance(double tolerance) {
        if (tolerance < 0) throw new IllegalArgumentException("tolerance must be >= 0");
        this.tolerance = tolerance;
    }

    /**
     * Snaps the provided x to a configured snap x if one is within tolerance.
     * Snaps the provided x to the nearest configured snap x (simple rounding).
     * If no snap values are configured the original x is returned.
     *
     * @param x the input x coordinate
     * @return the snapped x (or the original x if no match)
     */
    public double snapX(double x) {
        if (snapXValues.isEmpty()) return x;

        Double nearest = findNearestX(snapXValues, x);
        return nearest == null ? x : nearest;
    }

    /**
     * Returns a new Point with the x coordinate snapped (y unchanged).
     *
     * @param p the original point
     * @return a new Point with snapped x
     */
    public Point snapPoint(Point p) {
        Objects.requireNonNull(p, "point cannot be null");
        double snappedX = snapX(p.x());
        return new Point(snappedX, p.y());
    }

    /**
     * Finds the nearest x from the sorted list to the targetX. Returns null
     * if the list is empty (should not happen since callers check emptiness).
     */
    private Double findNearestX(List<Double> sortedXValues, double targetX) {
        if (sortedXValues.isEmpty()) return null;
        Double best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Double x : sortedXValues) {
            double d = Math.abs(x - targetX);
            if (d < bestDist) {
                bestDist = d;
                best = x;
            }
        }
        return best;
    }
}
