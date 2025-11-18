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
import java.util.List;
import java.util.Objects;

import javafx.scene.paint.Color;

/**
 * Represents a dataset with a name, color and a mutable collection of
 * {@link com.digitizer.core.Point}s.
 * <p>
 * Each dataset contains a user-visible name and a CSS hex color string.
 * The color is cached as a {@link javafx.scene.paint.Color} for efficient
 * rendering on the {@link com.digitizer.ui.CanvasPanel}.  While datasets
 * may be modified at runtime, callers should generally avoid changing
 * internal lists directly unless intentionally editing the dataset state.
 */
public class Dataset {
    private String name;
    private String hexColor;
    private Color color;
    private List<Point> points;
    // Whether this dataset is visible on the canvas (default true)
    private boolean visible = true;

    /**
     * Constructs a new Dataset with the given name and hex color.
     *
     * @param name      the dataset name (user-visible label)
     * @param hexColor  the color as a hex string (e.g., "#0072B2")
     */
    public Dataset(String name, String hexColor) {
        this.name = Objects.requireNonNull(name, "Dataset name cannot be null");
        this.hexColor = Objects.requireNonNull(hexColor, "Hex color cannot be null");
        this.color = ColorUtils.hexToColor(hexColor);
        this.points = new ArrayList<>();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Copy constructor for creating a new instance with the same data.
     *
     * @param other the dataset to copy
     */
    public Dataset(Dataset other) {
        this.name = other.name;
        this.hexColor = other.hexColor;
        this.color = other.color;
        this.points = new ArrayList<>(other.points);
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Dataset name cannot be null");
    }

    public String getHexColor() {
        return hexColor;
    }

    public void setHexColor(String hexColor) {
        this.hexColor = Objects.requireNonNull(hexColor, "Hex color cannot be null");
        this.color = ColorUtils.hexToColor(hexColor);
    }

    public Color getColor() {
        return color;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = Objects.requireNonNull(points, "Points list cannot be null");
    }

    /**
     * Adds a point to this dataset.
     *
     * @param point the point to add
     */
    public void addPoint(Point point) {
        this.points.add(Objects.requireNonNull(point, "Point cannot be null"));
    }

    /**
     * Removes a point from this dataset.
     *
     * @param point the point to remove
     * @return true if the point was removed, false if it was not in the list
     */
    public boolean removePoint(Point point) {
        return this.points.remove(point);
    }

    /**
     * Removes a point at the specified index.
     *
     * @param index the index of the point to remove
     * @return the removed point
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Point removePoint(int index) {
        return this.points.remove(index);
    }

    /**
     * Clears all points from this dataset.
     */
    public void clearPoints() {
        this.points.clear();
    }

    /**
     * Gets the number of points in this dataset.
     *
     * @return the point count
     */
    public int getPointCount() {
        return this.points.size();
    }

    @Override
    public String toString() {
        return String.format("Dataset{name='%s', color='%s', points=%d}", name, hexColor, points.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dataset dataset)) return false;
        return name.equals(dataset.name) &&
               hexColor.equals(dataset.hexColor) &&
               points.equals(dataset.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, hexColor, points);
    }
}
