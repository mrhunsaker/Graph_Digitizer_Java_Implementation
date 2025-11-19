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

package com.digitizer.io;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Data model for JSON serialization of datasets.
 * Represents a single dataset with its metadata and points.
 */
public class DatasetJson {
    @SerializedName("name")
    public String name;

    @SerializedName("color")
    public String color;

    @SerializedName("points")
    public List<List<Double>> points;
    @SerializedName("visible")
    public boolean visible = true;
    @SerializedName("use_secondary_y")
    public boolean useSecondaryY = false;

    /**
     * Constructs a new DatasetJson with empty points list.
     */
    public DatasetJson() {
        this.points = new ArrayList<>();
    }

    /**
     * Constructs a DatasetJson with all fields.
     *
     * @param name   the dataset name
     * @param color  the hex color string
     * @param points the list of [x, y] coordinate pairs
     */
    public DatasetJson(String name, String color, List<List<Double>> points) {
        this.name = name;
        this.color = color;
        this.points = points != null ? points : new ArrayList<>();
        this.visible = true;
    }

    public DatasetJson(String name, String color, List<List<Double>> points, boolean visible) {
        this.name = name;
        this.color = color;
        this.points = points != null ? points : new ArrayList<>();
        this.visible = visible;
    }

    public DatasetJson(String name, String color, List<List<Double>> points, boolean visible, boolean useSecondaryY) {
        this.name = name;
        this.color = color;
        this.points = points != null ? points : new ArrayList<>();
        this.visible = visible;
        this.useSecondaryY = useSecondaryY;
    }
}
