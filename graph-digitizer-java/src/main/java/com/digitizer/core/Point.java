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

/**
 * Represents a single data point with x and y coordinates.
 * <p>
 * Points are modeled as an immutable {@code record} to simplify threading and
 * ensure value semantics for equality and hashing. Use {@link #distanceTo(Point)}
 * when comparing proximity between points.
 */
public record Point(double x, double y) {

    /**
     * Creates a new Point with the specified coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public Point {
        // Record constructor validation if needed can be added here
    }

    /**
     * Returns a string representation of the point.
     *
     * @return formatted string "Point(x, y)"
     */
    @Override
    public String toString() {
        return String.format("Point(%.6f, %.6f)", x, y);
    }

    /**
     * Calculates the Euclidean distance to another point.
     *
     * @param other the other point
     * @return the distance between this point and the other point
     */
    public double distanceTo(Point other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
