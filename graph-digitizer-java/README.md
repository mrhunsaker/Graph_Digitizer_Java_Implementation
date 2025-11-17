# Graph Digitizer (Java 21 Edition)

A modern Java 21 / JavaFX implementation of the Graph Digitizer tool for extracting numeric data points from raster images of graphs.

## Version

**1.2.0** (Java Edition)

## Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.8.0 or later

### Installation

1. Clone or download the repository
2. Navigate to the project directory
3. Build the project:

```bash
mvn clean package
```

### Running the Application

Using Maven:

```bash
mvn javafx:run
```

Or run the JAR directly:

```bash
java -jar target/graph-digitizer-1.2.0.jar
```

## Features

- **Load PNG/JPEG Images**: Load raster images of graphs for digitization
- **Non-blocking Calibration**: Record four clicks to establish coordinate mapping
- **Manual Point Editing**: Left-click to add points, right-click or Delete to remove
- **Precision Placement**: Zoom and magnifier tools for pixel-level accuracy (planned)
- **Auto-trace**: Automatically extract curve points using color matching
- **Multiple Datasets**: Support for up to 6 color-coded datasets
- **Linear & Log Scales**: Support for both linear and logarithmic axes
- **Export Formats**: Save to JSON (full metadata) or CSV (tabular data)
- **Responsive UI**: Modern JavaFX interface with intuitive controls

## Project Structure

```
graph-digitizer-java/
├── pom.xml                           # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/digitizer/
│   │   │   ├── core/                 # Core business logic (no GUI dependencies)
│   │   │   │   ├── Point.java
│   │   │   │   ├── Dataset.java
│   │   │   │   ├── CalibrationState.java
│   │   │   │   ├── CoordinateTransformer.java
│   │   │   │   ├── ColorUtils.java
│   │   │   │   └── FileUtils.java
│   │   │   ├── image/                # Image processing
│   │   │   │   ├── ImageLoader.java
│   │   │   │   └── AutoTracer.java
│   │   │   ├── io/                   # File I/O
│   │   │   │   ├── ProjectJson.java
│   │   │   │   ├── DatasetJson.java
│   │   │   │   ├── JsonExporter.java
│   │   │   │   └── CsvExporter.java
│   │   │   └── ui/                   # User Interface
│   │   │       ├── GraphDigitizerApp.java
│   │   │       ├── MainWindow.java
│   │   │       ├── CanvasPanel.java
│   │   │       ├── ControlPanel.java
│   │   │       └── StatusBar.java
│   │   └── resources/
│   │       ├── fxml/                 # JavaFX FXML files (future)
│   │       └── css/                  # Stylesheets (future)
│   └── test/
│       └── java/com/digitizer/       # Unit tests
│           ├── core/
│           └── io/
└── target/                           # Build output
```

## Architecture

### Core Package (`com.digitizer.core`)

Pure business logic with no GUI dependencies:

- **Point**: Immutable record representing a single data point (x, y)
- **Dataset**: Mutable collection of points with metadata (name, color)
- **CalibrationState**: Manages calibration anchors and numeric axis ranges
- **CoordinateTransformer**: Transforms between data and canvas coordinates (supports log scales)
- **ColorUtils**: Color parsing, distance calculations, and blending
- **FileUtils**: Filename sanitization, defaults, and file operations

### Image Package (`com.digitizer.image`)

Image loading and automatic curve extraction:

- **ImageLoader**: Loads PNG/JPEG images from files
- **AutoTracer**: Column-by-column color matching for curve extraction

### IO Package (`com.digitizer.io`)

File import/export with support for multiple formats:

- **ProjectJson** / **DatasetJson**: POJO models for JSON serialization
- **JsonExporter**: Full-fidelity JSON format with metadata and log flags
- **CsvExporter**: Wide-format CSV export for spreadsheet compatibility

### UI Package (`com.digitizer.ui`)

JavaFX-based graphical interface:

- **GraphDigitizerApp**: Application entry point and lifecycle management
- **MainWindow**: Main window orchestration and menu bar
- **CanvasPanel**: Image display, point visualization, and user interaction
- **ControlPanel**: Dataset and calibration controls
- **StatusBar**: Status message display

## How to Use

### 1. Load an Image

Click "Load Image" button and select a PNG or JPEG file containing the graph.

### 2. Calibrate

1. Click "Calibrate" button
2. Click four points on the image in order:
   - X-left: leftmost known x-axis position
   - X-right: rightmost known x-axis position
   - Y-bottom: bottom known y-axis position
   - Y-top: top known y-axis position
3. Enter numeric axis ranges (X min/max, Y min/max)
4. Toggle "X Log" / "Y Log" if axes are logarithmic
5. Click "Apply Calibration"

### 3. Add/Edit Points

- **Left-click**: Add a point at that location
- **Drag**: Move an existing point
- **Right-click**: Delete a point (or press Delete/Backspace)

### 4. Auto-trace (Optional)

Select a dataset and click "Auto Trace". The algorithm scans columns and selects pixels matching the dataset color.

### 5. Save Your Work

- **Save JSON**: Full project with metadata and all datasets
- **Save CSV**: Tabular format suitable for spreadsheets and further analysis

## File Formats

### JSON Format

```json
{
  "title": "My Plot",
  "xlabel": "Time (s)",
  "ylabel": "Amplitude",
  "x_min": 0.0,
  "x_max": 100.0,
  "y_min": -1.0,
  "y_max": 1.0,
  "x_log": false,
  "y_log": false,
  "datasets": [
    {
      "name": "Dataset 1",
      "color": "#0072B2",
      "points": [[0.0, 0.1], [1.0, 0.15], ...]
    }
  ]
}
```

### CSV Format

Wide-format with x values in the first column:

```
x,Dataset_1,Dataset_2
0.0,0.1,-0.05
1.0,0.15,
2.5,,0.2
```

## Building and Extending

### Building with Maven

```bash
# Clean and package
mvn clean package

# Create executable JAR with dependencies
mvn package

# Run tests
mvn test

# Run the application
mvn javafx:run
```

### Project Layout Best Practices

1. **Core Business Logic**: Place non-GUI code in the `core` package. These classes can be tested and reused without JavaFX dependencies.

2. **Modularity**: Each package has a specific responsibility:
   - `core`: Data models and algorithms
   - `image`: Image I/O and processing
   - `io`: File format handlers
   - `ui`: User interface only

3. **Testing**: Unit tests in `src/test/java` mirror the source structure. Test core logic independently of UI.

4. **Dependencies**: 
   - Core classes import only from Java standard library
   - Image/IO classes import from core and external libraries
   - UI classes import from core, image, and io packages

### Adding New Features

Example: Adding a new export format

1. Create a new exporter class in `com.digitizer.io` package
2. Implement export logic
3. Add unit tests in `src/test/java/com/digitizer/io/`
4. Call the exporter from `MainWindow` in the appropriate event handler

### Extending the UI

The UI is designed to be easily extensible. To add new controls:

1. Create a new panel class extending `javafx.scene.layout.Region` or `VBox`
2. Initialize it in `MainWindow.initialize()`
3. Add it to the appropriate layout container

## Dependencies

- **JavaFX 21.0.2**: Modern UI framework
- **GSON 2.10.1**: JSON serialization
- **Apache Commons CSV 1.10.0**: CSV parsing and writing
- **SLF4J + Logback**: Logging framework
- **JUnit 4 & JUnit 5**: Testing frameworks

## Testing

Run all tests:

```bash
mvn test
```

Run specific test:

```bash
mvn test -Dtest=FileUtilsTest
```

## Development Notes

### Coordinate Systems

The application manages two coordinate systems:

1. **Canvas Coordinates**: Pixel positions in the JavaFX Canvas
2. **Data Coordinates**: Actual numeric values from the graph axes

The `CoordinateTransformer` class handles conversion between these systems, including support for logarithmic scales.

### Headless Core

All core logic (coordinate transforms, color operations, file I/O) is intentionally GUI-free. This allows:

- Unit testing without JavaFX/GUI headaches
- Headless/command-line processing
- Embedding in other applications

### Logging

The application uses SLF4J with Logback for logging. Configure logging in `logback.xml` if needed.

## Troubleshooting

### Application won't start

Ensure Java 21 is installed and JAVA_HOME is set:

```bash
java -version
```

### Image won't load

Verify the file is:
- A valid PNG or JPEG file
- Readable by the current user
- Not too large (tested up to 4K resolution)

### Build fails

Try:

```bash
mvn clean install
mvn javafx:run
```

Ensure Maven has internet access to download dependencies.

## Future Improvements

- [ ] FXML-based UI layouts for better separation of concerns
- [ ] Undo/redo stack
- [ ] Project file format (.gdz) combining image + metadata
- [ ] Keyboard shortcuts customization
- [ ] Snap X values and guide lines (from Julia version)
- [ ] Batch processing from command line
- [ ] Plugin system for custom export formats
- [ ] Precision zoom with circular magnifier overlay

## Contributing

Contributions are welcome! Please:

1. Write tests for new features
2. Follow the existing code style
3. Update documentation
4. Test on multiple platforms if possible

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.

## Contact

For issues, questions, or suggestions, please open an issue on the repository.
