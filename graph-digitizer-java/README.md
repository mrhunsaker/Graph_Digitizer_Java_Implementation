# Graph Digitizer (Java 21 Edition)

A modern Java 21 / JavaFX implementation of the Graph Digitizer tool for extracting numeric data points from raster images of graphs.

## Version

**1.0-beta** (Java Edition)

## 📚 Documentation

**1.0** (Java Edition)

- **[Quick Reference Guide](QUICK_REFERENCE.md)** - Commands, shortcuts, and common tasks
- **[Developer Guide](DEVELOPER.md)** - Architecture, patterns, and extension guide
- **[Project Summary](PROJECT_SUMMARY.md)** - Complete project overview
- **[Index](INDEX.md)** - Comprehensive documentation index
java -jar target/graph_digitizer_1.0.jar

### Accessibility Documentation

- **[Accessibility Overview](ACCESSIBILITY.md)** - Complete accessibility features guide
- **[Quick Start](ACCESSIBILITY_QUICK_START.md)** - Get started with screen readers
- **[Implementation Guide](ACCESSIBILITY_IMPLEMENTATION.md)** - Technical implementation details
- **[Implementation Complete](ACCESSIBILITY_IMPLEMENTATION_COMPLETE.md)** - ✅ All audit fixes applied
- **[Low Vision Audit](ACCESSIBILITY_AUDIT_LOW_VISION.md)** - Comprehensive accessibility audit results
- **[Summary](ACCESSIBILITY_SUMMARY.md)** - Features and capabilities overview
- **[Checklist](ACCESSIBILITY_CHECKLIST.md)** - Verification and testing checklist

### Themes Documentation

java -jar target/graph_digitizer_1.0.jar
- **[Quick Reference](THEMES_QUICK_REFERENCE.md)** - Theme selection guide
- **[Implementation](THEMES_IMPLEMENTATION_SUMMARY.md)** - Technical details

### Packaging & Distribution

- **[Packaging Guide](packaging/README.md)** - AppImage, DEB, RPM, and native installers

## Quick Start

### Prerequisites

- Java 21 or later
jpackage --type dmg --name GraphDigitizer --main-jar graph_digitizer_1.0.jar \

Note: This project uses JavaFX 21 and therefore either the user's JDK should
contain JavaFX modules (e.g., a Liberica Full JDK) or the build will need
to download platform-specific JavaFX artifacts (handled by Maven profiles in
the `pom.xml`).

### Installation

1. Clone or download the repository
2. Navigate to the project directory
3. Build the project:

```bash
mvn clean package

```text

### Running the Application

Using Maven:

```bash
mvn javafx:run

```text

Or run the JAR directly:

```bash
java -jar target/graph_digitizer_1.0-beta.jar

```text

If you want to distribute a "clickable" application that does not require the
end user to install Java, see "Packaging & Distribution" below.

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

## Example Session & Assets

The `docs/README_Assets` folder contains a screenshot from a sample digitization session and the raw data files produced during that session. The assets were moved into the documentation directory so they are versioned with the project.

- **Screenshot (inline):**

  ![Session Screenshot](docs/README_Assets/Screenshot 2025-11-19 073537.png)

- **Session data (raw files):**

  - CSV: [Sample_Graph_20251119-073453.csv](docs/README_Assets/Sample_Graph_20251119-073453.csv)
  - JSON: [Sample_Graph_20251119-073453.json](docs/README_Assets/Sample_Graph_20251119-073453.json)

Inline examples (small excerpts):

CSV excerpt:

```csv
x,Linear,InverseLinear,zigzag,nil,Mountain,Dataset 6
1,0.13245033112582782,-0.033112582781456956,-0.033112582781456956,0,0.16556291390728478,
2,0.9602649006622516,15.132450331125828,0.8940397350993378,0.9271523178807948,1.0596026490066226,
3,1.9867549668874174,14.072847682119205,2.185430463576159,0.9271523178807948,1.0264900662251657,
```

JSON excerpt:

```json
{
  "title": "Sample Graph",
  "xlabel": "X values (0-15)",
  "ylabel": "Y values (0-15)",
  "x_min": 0.0,
  "x_max": 15.0
}
```

Additional example files demonstrating common cases:

- Log-scaled X axis example (CSV/JSON):
  - [graph_digitizer_example_log.csv](docs/README_Assets/graph_digitizer_example_log.csv)
  - [graph_digitizer_example_log.json](docs/README_Assets/graph_digitizer_example_log.json)

- Missing values example (CSV/JSON):
  - [graph_digitizer_example_missing.csv](docs/README_Assets/graph_digitizer_example_missing.csv)
  - [graph_digitizer_example_missing.json](docs/README_Assets/graph_digitizer_example_missing.json)


## Project Structure

```text
├── pom.xml                           # Maven configuration
├── src/
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

Note: Auto Trace is guarded by a runtime feature flag. If the Auto Trace controls are disabled, enable them via the menu: **Actions -> Enable Auto Trace**. The toggle updates the toolbar button and menu item immediately without restarting the app. This allows you to safely keep the unfinished Auto Trace implementation hidden until you're ready to use it.

### 5. Save Your Work

- **Save JSON**: Full project with metadata and all datasets
- **Save CSV**: Tabular format suitable for spreadsheets and further analysis

## File Formats

### JSON Format

```json
{
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

```csv
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

### Generating HTML API docs (Javadoc)

Use Maven's javadoc plugin to generate API documentation based on the
Javadoc comments added throughout the codebase. This creates an "apidocs"
folder with HTML files under `target/site`.

```bash
mvn javadoc:javadoc

# Open the docs in your browser

start target/site/apidocs/index.html # Windows
open target/site/apidocs/index.html # macOS
xdg-open target/site/apidocs/index.html # Linux

```

## Packaging & Distribution (Click-able apps and Fat JARs)

This project is a JavaFX desktop application and shipping a cross-platform
installer or a single binary is commonly done using `jlink` + `jpackage`.
Below are recommended options for creating a clickable, distributable
application on macOS, Linux, and Windows.

### Option A: Fat JAR (Quick 1-file package; Java must be installed)

1. Add the Maven Shade plugin to your `pom.xml` to build a "fat" JAR bundling
   all libraries (not native JavaFX OS libs). Example in `pom.xml`:

```xml
<!-- snippet: put inside <build><plugins> -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.4.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>com.digitizer.ui.GraphDigitizerApp</mainClass>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>

```

1. Then build the artifact:

```bash
mvn clean package

# Resulting file in target/ will be a shaded jar, try:

java -jar target/graph_digitizer_1.0-beta.jar

```

Notes:

- A standard fat JAR will still require the end user to have a compatible
  Java runtime installed. For JavaFX, you still need the JavaFX runtime
  for the target OS if not bundled.
- For a fully self-contained native app (recommended), use Option B.

### Option B: Self-contained native binaries (recommended for end-users)

This approach uses `jlink` to create a custom runtime image and `jpackage`
to generate an OS-specific package (DMG, PKG, DEB, RPM, EXE, MSI).
Maven can call both of these tools via plugin configuration or use the
`jlink` and `jpackage` CLIs directly with a JDK that supports them.

You will need a JDK 21 distribution (for example Azul, Adoptium or Liberica)
installed that contains `jlink` and `jpackage` tools.

Basic flow (CLI approach):

1. Create runtime image with required modules (JavaFX modules and your app):

```bash

# Example for macOS/Linux/Windows - adjust module list and paths

jlink --module-path $JAVA_HOME/jmods:target/lib --add-modules java.base,java.desktop,java.logging,javafx.controls,javafx.graphics --output custom-runtime

```

1. Build an app image with jpackage (Windows example creating an exe):

```bash
jpackage --type exe \
  --input target/ \
  --name GraphDigitizer \
  --main-jar graph_digitizer_1.0-beta.jar \
  --main-class com.digitizer.ui.GraphDigitizerApp \
  --runtime-image custom-runtime \
  --icon build/icons/graphdigitizer.ico

```

1. For macOS, use `--type dmg` or `--type pkg` and `--icon` as .icns.
1. For Linux, use `--type deb` or `--type rpm`, or create an AppImage.

Tip: Use `maven-jlink-plugin` and the `jpackage` Maven plugin to integrate
this into your Maven lifecycle so OS-specific packages are reproducible.

The repository includes a Maven profile named `native` that automates
runtime image creation and packaging using `maven-jlink-plugin` and
`org.panteleyev:jpackage-maven-plugin`. Example usages:

```bash

# On Windows (EXE installer)

mvn -Pnative -Djpackage.type=exe package

# On macOS (DMG)

mvn -Pnative -Djpackage.type=dmg package

# On Linux (DEB)

mvn -Pnative -Djpackage.type=deb package

```

Notes:

- Set `-Dicon=path/to/icon` to provide a custom icon for the installer.
- Run packaging on the target OS (Windows packages on Windows, macOS on macOS) for best results.

#### Automated MSI via Maven

The `native` profile now automates `jpackage` as part of `mvn -Pnative package`. On Windows, if
you want an MSI installer the steps are:

1. Install the WiX Toolset (v3.11 or v4+) and ensure `candle.exe`/`light.exe` (or equivalent WiX binaries) are on your `PATH`.

2. Recommended quick command (PowerShell) — full automated flow using the `native` Maven profile:

```powershell
# Ensure JAVA_HOME is set and points to a JDK that includes `jpackage` (Java 21+ or Semeru/Adoptium with tools)
echo $Env:JAVA_HOME
& "$Env:JAVA_HOME\bin\java" -version

# Optional: remove any previous app-image to avoid conflicts
if (Test-Path 'target\jpackage\GraphDigitizer') { Remove-Item -Recurse -Force 'target\jpackage\GraphDigitizer' }

# Build MSI with the native profile (runs jpackage via the POM)
mvn -Pnative -DskipTests -Djpackage.type=msi -Dicon.win=build/icons/scatter-plot-256.ico package
```

3. Manual two-step `jpackage` (if you need to debug or run `jpackage` yourself):

```powershell
# Create the app-image (bundles the runtime libs and application)
& "$Env:JAVA_HOME\bin\jpackage.exe" --type app-image \
  --input target/jpackage-input \
  --main-jar graph-digitizer.jar \
  --main-class com.digitizer.ui.GraphDigitizerApp \
  --name GraphDigitizer \
  --dest target/jpackage \
  --icon build/icons/scatter-plot-256.ico \
  --app-version 1.1 \
  --java-options "--module-path $APPDIR\\lib" \
  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.swing"

# Build the MSI from the app-image
& "$Env:JAVA_HOME\bin\jpackage.exe" --type msi \
  --app-image target/jpackage/GraphDigitizer \
  --dest target/jpackage-msi \
  --name GraphDigitizer \
  --icon build/icons/scatter-plot-256.ico \
  --app-version 1.1 \
  --win-dir-chooser --win-menu --win-shortcut
```

4. Verify the produced MSI (automated smoke test):

```powershell
# Run the included verification script (extracts the MSI, runs the bundled EXE briefly, and performs a silent install)
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-msi.ps1 -TimeoutSeconds 30 -DoInstall

# Logs are written to: target/jpackage-msi/verify-msi.log, exe-run.log, exe-err.log, install.log
```

Where to find the artifacts:

- App image: `target/jpackage/GraphDigitizer`
- MSI installer: `target/jpackage-msi/GraphDigitizer-${project.version}.msi` (example: `target/jpackage-msi/GraphDigitizer-1.1.msi`)

Notes & troubleshooting
- If you see an error about WiX missing, install the WiX Toolset and add its `bin` to your `PATH`.
- If `jpackage` fails with an "application destination directory already exists" error, the `native` profile includes a cleanup exec that removes `target/jpackage/GraphDigitizer` before packaging — run `mvn -Pnative -DskipTests package` again.
- The `pom.xml` copies the shaded JAR to `target/jpackage-input/graph-digitizer.jar` and platform JavaFX jars into `target/jpackage-input/lib` automatically when using the `native` profile.
- If you want to run packaging on CI, run the packaging job on Windows-hosted runners for MSI outputs, and ensure WiX and a matching JDK are installed on the runner.

Advanced notes
- You can override the installer type and icon via Maven properties: `-Djpackage.type=exe|msi|dmg|deb|rpm` and `-Dicon.win=path\to\icon.ico`.
- To debug `jpackage` arguments, run the manual two-step commands above and append `--verbose` to see detailed output.
- For signing the final EXE/MSI in CI, see `scripts/sign-windows.ps1` which accepts a PFX and password (integrate with secure CI secrets).

If you'd like, I can add a small CI job example (GitHub Actions) to build and archive the MSI per-release.

### Advanced Packaging & Signing Resources

For AppImage, DEB/RPM maintainer scripts, desktop integration, icon auto-selection, and cross-platform signing/notarization:

- See `packaging/README.md` for: AppImage recipe (`appimage-builder.yml`), `.zsync` delta update steps, Debian `postinst`/`prerm`, RPM spec template, verification checklist, CI integration notes.
- Windows code signing: `scripts/sign-windows.ps1` (SecureString support; integrate with CI secrets).
- macOS signing & notarization: `scripts/sign-macos.sh` (codesign, submit, staple).
- Icon selection / generation: `scripts/select-icon.ps1` (Windows) and `scripts/create-mac-iconset.sh` (macOS) produce `selected-icon.properties` / `.icns`.

Quick examples:

```bash

# Linux AppImage build (after jpackage app-image)

appimage-builder --recipe packaging/appimage-builder.yml
appimagetool --create-zsync GraphDigitizer-x86_64.AppImage  # optional delta updates

```

```pwsh

# Windows signing (example)

pwsh scripts/sign-windows.ps1 -PfxPath certs/code_signing.pfx -PasswordEnvVar WINDOWS_CERT_PASS -Files (Get-ChildItem target -Filter *.exe).FullName

```

```bash

# macOS signing & notarization (example)

./scripts/sign-macos.sh GraphDigitizer.app "Developer ID Application: Your Company" TEAMID GraphDigitizer.dmg

```

### JavaFX & platform-native libraries

JavaFX requires native libraries for each target OS. When packaging with
`jlink`/`jpackage`, include the JavaFX modules for the specific OS. When
building on CI for multiple OSes, build packages on each target OS
or use cross-compile tooling that provides platform-specific JavaFX
artifacts.

### Example: Creating a macOS App (brief)

1. Install a JDK 21 with `jpackage` (Adoptium OpenJDK or Liberica Full JDK).
2. Build the jar and modules:

```bash
mvn clean package
jlink --module-path $JAVA_HOME/jmods:target/lib --add-modules java.base,java.desktop,javafx.controls,javafx.graphics --output runtime-mac

```

1. Use `jpackage` to make a `.app` bundle and optionally `.dmg`:

```bash
jpackage --type dmg --name GraphDigitizer --main-jar graph-digitizer-1.2.0.jar --main-class com.digitizer.ui.GraphDigitizerApp --runtime-image runtime-mac --icon build/icons/graphdigitizer.icns

```

### Troubleshooting packaging

- Error "No JavaFX runtime found" typically means you haven't included
  the JavaFX modules for your OS; ensure `--module-path` contains
  the JavaFX SDK modules.
- For Windows, use `--type msi` or `--type exe`. For `.msi` you may need
  `WiX` installed on Windows for full packaging.
- Use `--verbose` with `jpackage` for extra messages.

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
- **SLF4J + Log4j2**: Logging (console, rolling file, JSON, async option)
- **LMAX Disruptor**: Enables Log4j2 asynchronous loggers
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
### Coordinate Systems

The application manages three related coordinate systems and clarifies how they interact:

1. **Image Pixel Coordinates**: The image's natural pixel coordinate space (0..width-1, 0..height-1). The
  {@link com.digitizer.core.CoordinateTransformer} maps between numeric data values and these image pixel
  coordinates (this is the coordinate space used by the tracer and by the calibration anchors stored in
  {@link com.digitizer.core.CalibrationState}).

2. **Canvas Coordinates**: Pixel positions in the JavaFX Canvas where the image is rendered. The canvas may
  render the image at a scaled size (`displayScale`) and with offsets (`offsetX`, `offsetY`) so UI drawing
  (snap lines, points, ticks) must convert image-pixel coordinates into canvas coordinates before drawing.

3. **Data Coordinates**: Actual numeric values from the graph axes (e.g., 0.0..100.0). The transformer supports
  linear and logarithmic mappings between data coordinates and image pixels.

Note: Because the UI supports zooming (which changes `displayScale`) and fitting, the conversion helpers
in the UI layer perform image<->canvas conversions. This ensures points and tick marks remain visually in the
same place on the plotted graph when zoom/fitting changes are applied.

### Headless Core

All core logic (coordinate transforms, color operations, file I/O) is intentionally GUI-free. This allows:

- Unit testing without JavaFX/GUI headaches
- Headless/command-line processing
- Embedding in other applications

### Logging

The application uses SLF4J with a Log4j2 backend (`log4j2.xml`). The configuration defines:

- Console output with concise pattern
- Rolling text file (`logs/graph-digitizer.log`) with daily + size rollover (max 14 archives)
- Structured JSON events (`logs/graph-digitizer.json`) newline-delimited for ingestion
- Package logger `com.digitizer` at DEBUG and root at INFO

#### Asynchronous Logging (Optional Performance Tuning)

To enable async logging (reduces contention on the JavaFX UI thread), start the JVM with:

```bash
java -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar target/graph_digitizer_1.0-beta.jar

```

Maven/JavaFX run example:

```bash
mvn -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector javafx:run

```

Ensure the LMAX Disruptor dependency is present (already declared in `pom.xml`).

#### JSON Log Consumption

`graph-digitizer.json` is newline-delimited JSON (NDJSON). Example ingestion with `jq`:

```bash
jq -c '.' logs/graph-digitizer.json

```

#### JSON Log Ingestion Scripts

Two example helper scripts are provided in `scripts/` to parse and filter the structured JSON log:

- PowerShell: `scripts/ingest-json-log.ps1` (filter by level/logger)
- Python: `scripts/ingest_json_log.py` (arguments `--level` / `--logger`)

Examples:

```pwsh
pwsh scripts/ingest-json-log.ps1 -Level ERROR
pwsh scripts/ingest-json-log.ps1 -Logger com.digitizer.ui

```

```bash
python scripts/ingest_json_log.py --level INFO --logger com.digitizer

```

#### MDC (Mapped Diagnostic Context)

The application initializes a session identifier via `LoggingConfig.initializeMdc(...)`. Each log event includes MDC keys if referenced in patterns (add `%X{session}` to `PatternLayout` in `log4j2.xml` if you want it visible in the rolling text file). Current keys:

- `session`: Unique per application run (epoch milliseconds)
- `user`: Reserved for future use (currently unset / null for desktop context)

Add a user id example:

```java
LoggingConfig.initializeMdc(LoggingConfig.generateSessionId(), System.getProperty("user.name"));

```

Update pattern example:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %X{session} %logger - %msg%n"/>

```

#### Environment Checks

At startup `LoggingConfig.runEnvironmentChecks()` logs:

- Java version (`java.version`)
- Whether async logging property is present
- Creates `logs/` directory if missing

#### Archiving Old Logs

Use the helper script to compress and prune older logs:

```pwsh
pwsh scripts/archive-logs.ps1 -Days 14
pwsh scripts/archive-logs.ps1 -DryRun

```

Creates `logs/archive-YYYYMMDD-HHMMSS.zip` and removes archived originals.

#### Customization Tips

- Change retention via `<DefaultRolloverStrategy max="X"/>` in `log4j2.xml`.
- Modify patterns with `<PatternLayout>`; add `%X{key}` for MDC if needed.
- Remove JSON appender if structured logging not required.

#### Migrated From Logback

Previous `logback.xml` has been removed due to a security review; Log4j2 >= 2.23.1 plus async option provides hardened, flexible logging.

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

---

## Licensing and Javadoc

This project is licensed under the Apache License 2.0; the copyright and
license headers are present in all source files. The generated Javadocs
are considered part of developer documentation and can be published as
project site pages (GitHub Pages, GitHub Actions `mvn site` deployments).

If you want a public API site, run:

```bash
mvn site

```

That will build documentation including Javadocs and test reports into
`target/site` suitable for hosting.
