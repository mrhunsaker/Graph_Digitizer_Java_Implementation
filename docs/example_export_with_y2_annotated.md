# Example Export (with secondary Y-axis)

This annotated example shows a full JSON export produced by Graph Digitizer that includes a secondary (right-hand) Y axis label and dataset-level `use_secondary_y` flags.

File: `docs/example_export_with_y2.json`

```json
{
  "title": "River Monitoring - Sample Export",
  "xlabel": "Date (days)",
  "ylabel": "Zebra Mussels (count)",
  "y2label": "Chlorophyll (mg/L)",
  "x_min": 0.0,
  "x_max": 30.0,
  "y_min": 0.0,
  "y_max": 500.0,
  "x_log": false,
  "y_log": false,
  "y2_min": 0.0,
  "y2_max": 50.0,
  "y2_log": false,
  "datasets": [
    {
      "name": "Zebra Mussels",
      "color": "#0072B2",
      "visible": true,
      "use_secondary_y": false,
      "points": [
        [0.0, 12.0],
        [5.0, 45.0],
        [10.0, 120.0]
      ]
    },
    {
      "name": "Chlorophyll",
      "color": "#E69F00",
      "visible": true,
      "use_secondary_y": true,
      "points": [
        [0.0, 2.2],
        [5.0, 3.1],
        [10.0, 4.6]
      ]
    }
  ]
}
```

---

Field-by-field explanation

- `title` (string)
  - Human-friendly project title set in the Control panel. Used by default when building the export filename.

- `xlabel` / `ylabel` (string)
  - Labels for the primary X and Y axes.

- `y2label` (string, optional)
  - Label for the secondary (right-hand) Y axis. This field is present when the user has entered a secondary Y-axis title in the Control panel.

- `x_min`, `x_max`, `y_min`, `y_max` (number)
  - Numeric range for the primary axes derived from the calibration anchors or manual inputs.

- `x_log`, `y_log` (boolean)
  - Whether the corresponding axis uses logarithmic scaling.

- `y2_min`, `y2_max`, `y2_log` (number/boolean, optional)
  - Numeric range and optional log flag for the secondary Y axis. When absent (`null`) it indicates no secondary axis calibration was provided.

- `datasets` (array)
  - Array of dataset objects. The exporter omits any dataset that contains no points (so empty/unused series are not included in `datasets` nor as CSV columns).

Each dataset object contains:

- `name` (string)
  - Dataset title as shown in the UI.

- `color` (string)
  - Hex color code used for the series (helps downstream rendering tools preserve visual mapping).

- `visible` (boolean)
  - Whether the series is marked visible in the UI at the time of export.

- `use_secondary_y` (boolean)
  - New in this format: if `true`, the dataset is plotted against the secondary (right-hand) Y axis and consumers should map its Y values using the `y2_min`/`y2_max` range (and `y2_log` if present).

- `points` (array of [x, y])
  - The list of numeric points for the series. Each point is an array with two numbers: `[x, y]`.

Notes & best practices

- Omitted empty datasets: any series with zero points is not written into `datasets` to avoid cluttering exports with empty columns when exporting CSV.

- CSV compatibility: when exported to CSV, only datasets with points are included as columns. The CSV writer collates unique X values across datasets and leaves blank cells where a dataset has no value for a particular X.

- Backwards compatibility: older readers that ignore `y2label` and `use_secondary_y` will still be able to parse points and primary axis metadata; readers that understand these fields can render dual-axis charts or remap secondary-series values appropriately.

If you'd like, I can also add a small example CSV that demonstrates how these two datasets appear in the wide-format CSV exported by the app.

## CSV wide-format example

Below is a matching CSV export for the two example datasets shown above (wide format). The CSV writer omits datasets with no points and aligns Y values by X.

```csv
x,Zebra_Mussels,Chlorophyll
0,12.0,2.2
5,45.0,3.1
10,120.0,4.6
15,230.0,6.8
20,340.0,8.2
25,410.0,9.5
```

Note: If some datasets do not have a value for a given X, the corresponding cell is left blank.
