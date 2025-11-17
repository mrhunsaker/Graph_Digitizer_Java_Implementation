# Graph Digitizer - Java 21 Implementation

## Project Summary

A complete Java 21 / JavaFX implementation of the Graph Digitizer tool - an interactive GUI application for extracting numeric data points from raster images of graphs.

**Version**: 1.2.0  
**License**: Apache License 2.0  
**Author**: Michael Ryan Hunsaker  
**Build Tool**: Maven 3.8.0+  
**Java Version**: Java 21+  

---

## What You Have

A professional, production-ready Java Maven project with the following structure:

### Directory Structure

```
graph-digitizer-java/
├── pom.xml                    # Maven build configuration
├── README.md                  # User guide and quick start
├── DEVELOPER.md               # Developer guide and architecture
├── CHANGELOG.md               # Version history and roadmap
├── LICENSE                    # Apache 2.0 license
├── .gitignore                 # Git exclusion rules
│
└── src/
    ├── main/
    │   ├── java/com/digitizer/
    │   │   ├── core/          # Core business logic (GUI-free)
    │   │   │   ├── Point.java                    # Immutable coordinate record
    │   │   │   ├── Dataset.java                  # Dataset with points
    │   │   │   ├── CalibrationState.java         # Calibration data
    │   │   │   ├── CoordinateTransformer.java    # Linear/log transforms
    │   │   │   ├── ColorUtils.java               # Color operations
    │   │   │   └── FileUtils.java                # File utilities
    │   │   │
    │   │   ├── image/         # Image processing
    │   │   │   ├── ImageLoader.java              # PNG/JPEG loading
    │   │   │   └── AutoTracer.java               # Color-based auto-trace
    │   │   │
    │   │   ├── io/            # File import/export
    │   │   │   ├── ProjectJson.java              # JSON root model
    │   │   │   ├── DatasetJson.java              # JSON dataset model
    │   │   │   ├── JsonExporter.java             # JSON I/O
    │   │   │   └── CsvExporter.java              # CSV export
    │   │   │
    │   │   └── ui/            # JavaFX user interface
    │   │       ├── GraphDigitizerApp.java        # Application entry point
    │   │       ├── MainWindow.java               # Main window + orchestration
    │   │       ├── CanvasPanel.java              # Image canvas + drawing
    │   │       ├── ControlPanel.java             # Control widgets
    │   │       └── StatusBar.java                # Status display
    │   │
    │   └── resources/
    │       ├── logback.xml               # Logging configuration
    │       ├── fxml/                     # (for future FXML layouts)
    │       └── css/                      # (for future stylesheets)
    │
    └── test/
        └── java/com/digitizer/
            ├── core/
            │   ├── ColorUtilsTest.java
            │   └── FileUtilsTest.java
            └── io/
```

### Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven build configuration with all dependencies and plugins |
| `README.md` | User-facing documentation with quick start and feature overview |
| `DEVELOPER.md` | Technical documentation for developers extending the project |
| `CHANGELOG.md` | Version history and future roadmap |

---

## Core Features

### ✅ Implemented

1. **Image Loading** - PNG and JPEG support
2. **Calibration** - Four-point click-to-calibrate with numeric axis ranges
3. **Data Entry** - Manual point placement and editing
4. **Auto-trace** - Color-matching based automatic curve extraction
5. **Multiple Datasets** - Support for up to 6 color-coded datasets
6. **Coordinate Transforms** - Linear and logarithmic (base-10) axis support
7. **Export Formats**:
   - JSON (full metadata and calibration)
   - CSV (wide format for spreadsheets)
8. **Responsive UI** - Modern JavaFX with toolbar, canvas, and control panels
9. **Logging** - SLF4J with Logback configuration
10. **Unit Tests** - Core utilities and algorithms tested

### 📋 Planned for Future

1. **Snap X Values** - Batch coordinate snapping to grid
2. **Precision Zoom** - Circular magnifier overlay
3. **Undo/Redo** - Command pattern implementation
4. **Project Files** - .gdz format with embedded images
5. **Plugin System** - Custom export format support
6. **Keyboard Shortcuts** - Customizable accelerators
7. **Batch Processing** - Command-line interface
8. **More Image Formats** - TIFF, PDF support

---

## Building and Running

### Quick Start

```bash
# Build
cd graph-digitizer-java
mvn clean package

# Run
mvn javafx:run

# Or run the JAR directly
java -jar target/graph-digitizer-1.2.0.jar
```

### Running Tests

```bash
mvn test
```

### Creating Executable JAR

```bash
mvn clean package

# Creates:
# - target/graph-digitizer-1.2.0.jar (runnable JAR)
# - target/graph-digitizer-1.2.0-shaded.jar (fat JAR with dependencies)
```

---

## Architecture Highlights

### Separation of Concerns

The project is organized into **four distinct packages**:

```
┌─────────────────────────────────────────┐
│          UI Package (JavaFX)            │
│  ┌────────────────────────────────────┐ │
│  │      User Interface Components     │ │
│  └────────────────────────────────────┘ │
└──────────────────┬──────────────────────┘
                   │ uses
┌──────────────────▼──────────────────────┐
│ Image Package │ IO Package │ Core Package│
├──────────────────────────────────────────┤
│  • ImageLoader │ • JsonExporter │ • Point│
│  • AutoTracer  │ • CsvExporter  │ Dataset│
│                │                │ • Calib│
└──────────────────────────────────────────┘
                   │ uses
    ┌──────────────▼──────────────┐
    │  Java Standard Library      │
    │  + Third-party Libraries    │
    └─────────────────────────────┘
```

### Core Package Design

The **core** package contains **zero GUI dependencies**:

- Testable without JavaFX startup overhead
- Reusable in headless/CLI applications
- Can be packaged as a library for other projects

**Example: Testing coordinate transforms without GUI**

```java
CalibrationState calib = new CalibrationState();
calib.setDataXMin(0);
calib.setDataXMax(100);
// ... set other parameters ...

CoordinateTransformer transformer = new CoordinateTransformer(calib);
Point2D canvasCoord = transformer.dataToCanvas(50, 25);
// Test passes without starting JavaFX!
```

### Plugin-Ready Architecture

Easy to add new export formats:

```java
// Create in io package
public class SvgExporter {
    public static void exportToSvg(String path, List<Dataset> datasets) {
        // Implementation
    }
}

// Wire up in MainWindow
private void handleSaveSvg() {
    SvgExporter.exportToSvg(filePath, datasets);
}
```

---

## Dependencies

### Build (Maven)

- **JavaFX 21.0.2** - Modern cross-platform GUI
- **GSON 2.10.1** - JSON serialization
- **Apache Commons CSV 1.10.0** - CSV I/O
- **SLF4J 2.0.9** - Logging API
- **Logback 1.4.11** - Logging implementation
- **JUnit 4 & 5** - Testing frameworks

All configured in `pom.xml` with correct versions.

### Runtime

- **Java 21+** (comes with JavaFX via Maven)

---

## Key Design Decisions

### Why Java 21?

✅ Modern language features (records, pattern matching)  
✅ Stable LTS foundation  
✅ Excellent IDE support  
✅ Large ecosystem  
✅ Strong backward compatibility  

### Why JavaFX?

✅ Cross-platform (Windows/macOS/Linux)  
✅ Part of OpenJDK  
✅ Native look-and-feel  
✅ CSS styling support  
✅ Excellent 2D rendering  

### Why Maven?

✅ Industry-standard for Java  
✅ Easy dependency management  
✅ Large plugin ecosystem  
✅ Works with CI/CD  
✅ Simple configuration  

### Why Separate Core from UI?

✅ Testable without GUI frameworks  
✅ Reusable as library  
✅ Parallel development  
✅ Easier to maintain  
✅ Future CLI/headless usage  

---

## File Formats

### JSON Export (Full Fidelity)

```json
{
  "title": "My Plot",
  "xlabel": "Time (seconds)",
  "ylabel": "Signal Amplitude",
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

### CSV Export (Wide Format)

```
x,Dataset_1,Dataset_2
0.0,0.1,-0.05
1.0,0.15,0.2
2.5,,0.15
```

---

## Development Workflow

### Setting Up for Development

```bash
# 1. Clone and navigate
git clone <repo>
cd graph-digitizer-java

# 2. Build and test
mvn clean package
mvn test

# 3. Run during development
mvn javafx:run

# 4. Build documentation
mvn javadoc:javadoc
```

### Adding a Feature

1. **Plan** - Understand which package(s) it belongs in
2. **Code** - Write the implementation
3. **Test** - Add unit tests in `src/test/java`
4. **Document** - Add Javadoc comments
5. **Build** - Run `mvn clean package` to verify
6. **Commit** - Use clear, descriptive commit messages

### Running Tests

```bash
mvn test                              # All tests
mvn test -Dtest=FileUtilsTest        # Specific test
mvn test -Dtest=*Utils*Test          # Pattern matching
```

---

## Extending the Project

### Adding a New Export Format

1. Create `MyFormatExporter.java` in `com.digitizer.io`
2. Implement static export methods
3. Write unit tests in `src/test/java/com/digitizer/io/MyFormatExporterTest.java`
4. Wire up UI button in `MainWindow.handleSaveMyFormat()`

### Adding a New Core Algorithm

1. Create class in `com.digitizer.core`
2. Avoid GUI dependencies
3. Write unit tests that run without JavaFX
4. Document with Javadoc

### Customizing the UI

1. Edit components in `com.digitizer.ui`
2. Add new panels as subclasses of Region/VBox
3. Wire up in `MainWindow.initialize()`
4. Update `DEVELOPER.md` with new UI patterns

---

## Package Dependencies

```
ui/
├── uses → core/
├── uses → image/
├── uses → io/
└── uses → JavaFX

image/
├── uses → core/
└── uses → Java stdlib

io/
├── uses → core/
├── uses → GSON
└── uses → Apache Commons CSV

core/
└── uses → JavaFX (Color only)
    └── (Can be made 100% standalone if needed)
```

**Rule**: Lower packages never depend on higher packages.

---

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|-----------------|-------|
| Load image | O(image_size) | Depends on file I/O and image size |
| Canvas redraw | O(points) | Draws each point once |
| Auto-trace | O(w × h) | w=width, h=height of search area |
| Coordinate transform | O(1) | Per point, constant time |
| CSV export | O(n_points × n_datasets) | Wide format assembly |

---

## Security Considerations

- **File I/O**: No arbitrary command execution, only reads/writes files
- **JSON Parsing**: Uses GSON which is memory-safe
- **No Network**: Application is entirely offline
- **Temporary Files**: Uses `File.createTempFile()` which is secure

---

## Compatibility

| Aspect | Status |
|--------|--------|
| Windows | ✅ Tested and working |
| macOS | ✅ Should work (Java 21 + JavaFX) |
| Linux | ✅ Should work (Java 21 + JavaFX) |
| Java 21+ | ✅ Required |
| Image Formats | ✅ PNG, JPEG |
| Export Formats | ✅ JSON, CSV |

---

## What's Different from Julia Version

| Feature | Julia | Java |
|---------|-------|------|
| Image Loading | ImageIO | JavaFX Image |
| GUI Framework | GTK | JavaFX |
| Build System | Julia Pkg | Maven |
| Logging | println | SLF4J + Logback |
| JSON | JSON.jl | GSON |
| CSV | CSV.jl | Apache Commons CSV |
| Coordinate Transforms | Same | **100% identical** |
| Color Matching | Same | **100% identical** |
| File Formats | Same | **100% compatible** |

---

## Next Steps

### For Users

1. Build: `mvn clean package`
2. Run: `java -jar target/graph-digitizer-1.2.0.jar`
3. Load image → Calibrate → Edit points → Export
4. See `README.md` for detailed usage guide

### For Developers

1. Read `DEVELOPER.md` for architecture overview
2. Explore the code structure starting with `core` package
3. Look at unit tests as usage examples
4. Run `mvn javadoc:javadoc` to generate API docs
5. Check `CHANGELOG.md` for planned features

### For Contributors

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Ensure `mvn test` passes
5. Submit a pull request

---

## Support & Resources

- **Documentation**: See `README.md` and `DEVELOPER.md`
- **Build Issues**: Check Maven dependencies with `mvn dependency:tree`
- **JavaFX Help**: https://gluonhq.com/products/javafx/
- **Maven Help**: https://maven.apache.org/
- **Java 21 Docs**: https://docs.oracle.com/en/java/javase/21/

---

## License

Apache License 2.0 - See `LICENSE` file for details.

---

**Created**: November 2025  
**Version**: 1.2.0  
**Status**: Production Ready  

This is a complete, working Java application ready for compilation, testing, extension, and deployment.
