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

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Root data model for JSON file format.
 * Represents the complete application state: title, labels, axes, and datasets.
 */
public class ProjectJson {
    @SerializedName("title")
    public String title;

    @SerializedName("xlabel")
    public String xlabel;

    @SerializedName("ylabel")
    public String ylabel;

    @SerializedName("x_min")
    public double xMin;

    @SerializedName("x_max")
    public double xMax;

    @SerializedName("y_min")
    public double yMin;

    @SerializedName("y_max")
    public double yMax;

    @SerializedName("x_log")
    public boolean xLog;

    @SerializedName("y_log")
    public boolean yLog;

    @SerializedName("datasets")
    public List<DatasetJson> datasets;

    /**
     * Constructs a new ProjectJson with default values.
     */
    public ProjectJson() {
        this.title = "";
        this.xlabel = "";
        this.ylabel = "";
        this.xMin = 0.0;
        this.xMax = 1.0;
        this.yMin = 0.0;
        this.yMax = 1.0;
        this.xLog = false;
        this.yLog = false;
        this.datasets = new ArrayList<>();
    }

    /**
     * Constructs a new ProjectJson with all fields.
     *
     * @param title    the project title
     * @param xlabel   the x-axis label
     * @param ylabel   the y-axis label
     * @param xMin     minimum x value
     * @param xMax     maximum x value
     * @param yMin     minimum y value
     * @param yMax     maximum y value
     * @param xLog     whether x-axis is logarithmic
     * @param yLog     whether y-axis is logarithmic
     * @param datasets the list of datasets
     */
    public ProjectJson(String title, String xlabel, String ylabel,
                      double xMin, double xMax, double yMin, double yMax,
                      boolean xLog, boolean yLog, List<DatasetJson> datasets) {
        this.title = title;
        this.xlabel = xlabel;
        this.ylabel = ylabel;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.xLog = xLog;
        this.yLog = yLog;
        this.datasets = datasets != null ? datasets : new ArrayList<>();
    }
}
