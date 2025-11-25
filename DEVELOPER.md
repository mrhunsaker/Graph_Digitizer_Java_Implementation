# Developer Guide

This guide helps developers understand, extend, and maintain the Graph Digitizer Java 21 implementation.

## Architecture Overview

The application is organized into four main packages:

### Core Package (`com.digitizer.core`)

Pure business logic with **zero GUI dependencies**. This is intentional to allow:


- **Unit testing** without JavaFX complications

- **Reuse** in headless/CLI applications

- **Embedding** in other Java applications

**Key Classes:**


- `Point.java`: Immutable record for (x, y) coordinates

- `Dataset.java`: Mutable collection of points with metadata

- `CalibrationState.java`: Manages calibration anchors and axis ranges

- `CoordinateTransformer.java`: Linear/log coordinate transformation

- `ColorUtils.java`: Color operations (hex parsing, distance, blending)

- `FileUtils.java`: File naming, defaults, sanitization

**Design Pattern**: These classes use standard Java patterns and avoid framework-specific code.

### Image Package (`com.digitizer.image`)

Image I/O and automatic curve extraction algorithms.

**Key Classes:**


- `ImageLoader.java`: Loads PNG/JPEG files as JavaFX Image objects

- `AutoTracer.java`: Column-by-column color-matching algorithm

**Design Pattern**: Uses core classes and JavaFX Image, but not GUI widgets.

### IO Package (`com.digitizer.io`)

File format handlers for JSON and CSV.

**Key Classes:**


- `ProjectJson.java`, `DatasetJson.java`: POJO models annotated with `@SerializedName` for GSON

- `JsonExporter.java`: Full-fidelity JSON with metadata and log flags

- `CsvExporter.java`: Wide-format CSV for spreadsheet compatibility

**Design Pattern**: Converters between application objects and serializable POJOs.

### UI Package (`com.digitizer.ui`)

JavaFX-based user interface.

**Key Classes:**


- `GraphDigitizerApp.java`: Application entry point and lifecycle

- `MainWindow.java`: Main window orchestration

- `CanvasPanel.java`: Canvas drawing, image display, mouse interactions

- `ControlPanel.java`: Calibration and dataset controls (extensible)

- `StatusBar.java`: Status message display

**Design Pattern**: MVC-inspired with separation between data (core) and presentation (ui).

## Development Workflow

### Setting Up for Development

```bash

# Clone the repository

git clone <repo-url>
cd graph-digitizer-java

# Install dependencies

mvn clean install

# Run the application

mvn javafx:run

# Run tests

mvn test

# Build JAR

mvn package

```

### Project Configuration

**Key Files:**


- `pom.xml`: Maven configuration (dependencies, plugins, build settings)

- `src/main/resources/logback.xml`: Logging configuration

- `.gitignore`: Git exclusion rules

**Important Plugin Notes:**


- **javafx-maven-plugin**: Enables `mvn javafx:run`

- **maven-shade-plugin**: Creates fat JAR with all dependencies

- **maven-surefire-plugin**: Runs JUnit tests

### Code Style Guidelines


1. **Naming Conventions**:

   - Classes: `PascalCase` (e.g., `CoordinateTransformer`)

   - Methods: `camelCase` (e.g., `dataToCanvas`)

   - Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_DATASETS`)


2. **Documentation**:

   - Every public class needs a Javadoc comment

   - Every public method needs a Javadoc comment with `@param` and `@return` tags

   - Inline comments for non-obvious logic


3. **Imports**:

   - Organize: Java standard library → Third-party libraries → Application code

   - Avoid wildcard imports


4. **Formatting**:

   - 4-space indentation (standard Java)

   - Lines ≤ 120 characters when practical

   - Blank lines between methods

### Testing

**Philosophy**: Test core logic independent of GUI.

```bash

# Run all tests

mvn test

# Run specific test class

mvn test -Dtest=FileUtilsTest

# Run with coverage (requires plugin)

mvn test jacoco:report

```

**Test Location**: Mirror the source structure


- Source: `src/main/java/com/digitizer/core/FileUtils.java`

- Test: `src/test/java/com/digitizer/core/FileUtilsTest.java`

**Test Naming**: Use descriptive names


- Good: `testSanitizeFilenameWithSpecialChars()`

- Bad: `test1()`

### Adding New Features

**Example: Add a new export format (e.g., SVG)**


1. **Create the exporter** in `com.digitizer.io`:

```java
public class SvgExporter {
    public static void exportToSvg(String filePath, List<Dataset> datasets) 
            throws IOException {
        // Implementation
    }
}

```


1. **Add unit tests** in `src/test/java/com/digitizer/io/`:

```java
public class SvgExporterTest {
    @Test
    public void testExportToSvg() { ... }
}

```


1. **Wire up the UI** in `MainWindow.java`:

```java
private void handleSaveSvg() {
    // File chooser dialog
    // Call SvgExporter.exportToSvg()
    // Update status bar
}

```


1. **Add button to toolbar** in `createToolbar()`.

### Extending the UI

**Example: Add a dataset selector combo box**


1. Create the ComboBox in the control panel:

```java
ComboBox<String> datasetSelector = new ComboBox<>();
for (Dataset ds : datasets) {
    datasetSelector.getItems().add(ds.getName());
}

```


1. Listen for changes:

```java
datasetSelector.setOnAction(e -> {
    int selectedIndex = datasetSelector.getSelectionModel().getSelectedIndex();
    canvasPanel.setActiveDataset(selectedIndex);
});

```


1. Update `CanvasPanel` to track active dataset:

```java
private int activeDatasetIndex = 0;

public void setActiveDataset(int index) {
    this.activeDatasetIndex = index;
    redraw();
}

```

## Key Design Decisions

### Why Records for `Point`?

Java Records (Java 16+) provide immutable, value-like semantics:

```java
public record Point(double x, double y) {
    // Thread-safe, automatically generates equals/hashCode/toString
}

```

Benefits:


- Immutability prevents accidental modifications

- Concise syntax

- Built-in equals/hashCode implementations

### Why Separate Core from UI?

Enables:


- **Unit testing** without GUI startup overhead

- **Batch processing** in CLI or server applications

- **Library reuse** by other projects

- **Parallel development** of core and UI

### Why GSON instead of Jackson or JSON-B?


- Simple to use (minimal boilerplate)

- Small dependency footprint

- Works well with POJOs and annotations

- Good for straightforward data serialization

### Coordinate Transformation

The `CoordinateTransformer` class handles a critical function: converting between pixel coordinates and data coordinates. It supports:


- **Linear axes**: Simple linear interpolation

- **Logarithmic axes**: Base-10 log transformation

Why separate this? Allows:


- Testing without GUI

- Reuse in headless tools

- Easy extension to polar, custom transformations

## Common Tasks

### Running on a Different Port/Host (if added)

Edit `pom.xml` or application configuration.

### Adding Logging

```java
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);

logger.info("Application started");
logger.warn("Warning message");
logger.error("Error occurred", exception);

```

Logging output is configured in `src/main/resources/logback.xml`.

### Packaging for Distribution

```bash

# Create fat JAR with all dependencies

mvn clean package

# The JAR will be in target/

java -jar target/graph-digitizer-1.2.0-shaded.jar

```

### Cross-Platform Considerations

### Packaging Helpers & Runtime Images

For packaging and producing native installers the repository includes helper documentation and scripts:


- `docs/JPACKAGE.md` — detailed guidance for `jlink` / `jpackage`, cross-arch runtime image creation (including Docker examples) and CI recommendations.

- `scripts/README.md` — quick reference for the `scripts/` wrappers (e.g. `generate-msi.ps1`) and notes about output location (`target/generated_builds`).

Read those docs before attempting multi-architecture builds. The Windows MSI generator script accepts per-arch runtime images (or will skip an arch if the runtime is not supplied).


- **File paths**: Use `File` or `Path` classes, not hardcoded separators

- **Newlines**: Java handles `\n` on all platforms

- **Encoding**: Explicitly specify UTF-8 when reading/writing text

- **GTK vs native dialogs**: JavaFX handles platform differences

## Troubleshooting Development Issues

### Build fails with "Missing mandatory Classpath entries"

This is a VS Code linting issue, not a real compile error. Run:

```bash
mvn clean compile

```

If it compiles, the linting is just out of sync.

### JavaFX module not found

Ensure `pom.xml` has the JavaFX dependency and Maven has downloaded it:

```bash
rm -rf ~/.m2/repository/org/openjfx/
mvn clean install

```

### Tests fail with "cannot find symbol"

Ensure test classes are in the correct package structure:


- Source: `src/main/java/com/digitizer/core/`

- Tests: `src/test/java/com/digitizer/core/`

### Application slow on large images

Consider:


- Resizing large images before loading

- Optimizing auto-trace algorithm for large images

- Using progressive image loading

## Performance Considerations


1. **Canvas redrawing**: Done on every mouse move/calibration point - consider throttling if needed

2. **Auto-trace**: O(width × height × colors) - uses direct pixel access for speed

3. **Coordinate transforms**: O(1) for each point - suitable for real-time interaction

## Future Enhancement Ideas


- [ ] Implement zoom with mouse wheel

- [ ] Add undo/redo with command pattern

- [ ] Support for more image formats (TIFF, PDF)

- [ ] Project file format (.gdz) with embedded image

- [ ] Multi-threading for auto-trace on large images

- [ ] Plugin system for custom exporters

- [ ] Keyboard shortcut customization UI

- [ ] Recent files menu

## Contributing Guidelines


1. Fork the repository

2. Create a feature branch: `git checkout -b feature/my-feature`

3. Write tests for new functionality

4. Ensure all tests pass: `mvn test`

5. Commit with clear message: `git commit -m "Add feature X"`

6. Push and create a Pull Request

7. Address code review feedback

**Code Review Checklist:**


- [ ] Tests added for new code

- [ ] No breaking changes to public APIs

- [ ] Documentation updated

- [ ] Code follows style guidelines

- [ ] No unused imports or variables

## Resources


- **Java 21**: [Java 21 docs](https://docs.oracle.com/en/java/javase/21/)

- **JavaFX**: [Gluon JavaFX](https://gluonhq.com/products/javafx/)

- **Maven**: [Maven](https://maven.apache.org/)

- **GSON**: [gson on GitHub](https://github.com/google/gson)

- **Apache Commons CSV**: [Commons CSV](https://commons.apache.org/proper/commons-csv/)

## Getting Help


- Check existing GitHub issues

- Review the main README.md

- Look at similar implementations in the core package

- Ask in code comments if unclear
