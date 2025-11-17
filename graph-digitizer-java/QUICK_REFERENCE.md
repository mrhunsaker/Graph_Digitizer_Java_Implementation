# Quick Reference

## Build Commands

```bash
# Build the project
mvn clean package

# Run during development
mvn javafx:run

# Run tests
mvn test

# Create executable JAR
mvn clean package
# Output: target/graph-digitizer-1.2.0.jar

# Generate Javadoc
mvn javadoc:javadoc
# Output: target/site/apidocs/

# Run specific test
mvn test -Dtest=FileUtilsTest

# Display dependency tree
mvn dependency:tree
```

## Running the Application

### Development
```bash
mvn javafx:run
```

### Production
```bash
java -jar target/graph-digitizer-1.2.0.jar
```

## Project Structure Quick Lookup

| Task | Location |
|------|----------|
| Change UI layout | `src/main/java/com/digitizer/ui/MainWindow.java` |
| Add new export format | `src/main/java/com/digitizer/io/MyExporter.java` |
| Add color utilities | `src/main/java/com/digitizer/core/ColorUtils.java` |
| Write tests | `src/test/java/com/digitizer/[package]/MyTest.java` |
| Configure logging | `src/main/resources/logback.xml` |
| Manage dependencies | `pom.xml` |
| User documentation | `README.md` |
| Developer guide | `DEVELOPER.md` |

## Key Classes

### Core (No GUI)
- `Point` - Immutable coordinate record
- `Dataset` - Collection of points with metadata
- `CalibrationState` - Calibration data
- `CoordinateTransformer` - Linear/log transforms
- `ColorUtils` - Color operations
- `FileUtils` - File utilities

### Image Processing
- `ImageLoader` - Load PNG/JPEG files
- `AutoTracer` - Color-based curve extraction

### File I/O
- `JsonExporter` - JSON format
- `CsvExporter` - CSV format
- `ProjectJson`, `DatasetJson` - POJO models

### UI (JavaFX)
- `GraphDigitizerApp` - Application entry point
- `MainWindow` - Main window + orchestration
- `CanvasPanel` - Image canvas
- `ControlPanel` - Control widgets
- `StatusBar` - Status display

## Common Tasks

### Adding a New Button
1. In `MainWindow.createToolbar()`:
```java
Button newBtn = new Button("New Feature");
newBtn.setOnAction(e -> handleNewFeature());
toolbar.getChildren().add(newBtn);
```

2. Implement handler:
```java
private void handleNewFeature() {
    // Implementation
    statusBar.setStatus("Feature complete");
}
```

### Adding a Test
1. Create test class in `src/test/java/com/digitizer/[package]/`:
```java
public class MyClassTest {
    @Test
    public void testSomething() {
        // Test code
        assertEquals(expected, actual);
    }
}
```

2. Run: `mvn test`

### Accessing Application Data
- Current image: `canvasPanel.currentImage`
- Calibration: `calibration` field (passed to components)
- Datasets: `datasets` list (mutable)
- Active dataset: `datasets.get(activeIndex)`

## Maven Plugin Reference

| Plugin | Purpose | Command |
|--------|---------|---------|
| javafx-maven-plugin | Run JavaFX apps | `mvn javafx:run` |
| maven-compiler-plugin | Compile Java 21 code | `mvn compile` |
| maven-surefire-plugin | Run tests | `mvn test` |
| maven-shade-plugin | Create fat JAR | `mvn package` |
| maven-jar-plugin | Create standard JAR | `mvn jar:jar` |
| maven-javadoc-plugin | Generate docs | `mvn javadoc:javadoc` |

## Logging Examples

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
    
    public void doSomething() {
        logger.info("Starting operation");
        logger.warn("Warning: something is {}", state);
        logger.error("Error occurred", exception);
        logger.debug("Debug info: {}", details);
    }
}
```

## Dependency Version Info

- Java: 21+
- JavaFX: 21.0.2
- GSON: 2.10.1
- Apache Commons CSV: 1.10.0
- SLF4J: 2.0.9
- Logback: 1.4.11
- JUnit: 4.13.2 and 5.9.3

Update in `pom.xml` under `<properties>` and `<dependencies>`.

## File Format Examples

### JSON Export
```json
{
  "title": "My Graph",
  "xlabel": "Time",
  "ylabel": "Value",
  "x_min": 0.0,
  "x_max": 100.0,
  "y_min": -1.0,
  "y_max": 1.0,
  "x_log": false,
  "y_log": false,
  "datasets": [
    {
      "name": "Data1",
      "color": "#0072B2",
      "points": [[0.0, 0.1], [1.0, 0.15]]
    }
  ]
}
```

### CSV Export
```
x,Dataset_1,Dataset_2
0.0,0.1,-0.05
1.0,0.15,0.2
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Build fails | Run `mvn clean install` |
| Tests won't run | Ensure test class in `src/test/java` |
| GUI won't start | Check Java 21 is installed (`java -version`) |
| Image won't load | Verify PNG/JPEG format and file permissions |
| Import issues | Run `mvn clean` then rebuild |

## File Locations

- Source code: `src/main/java/com/digitizer/`
- Test code: `src/test/java/com/digitizer/`
- Resources: `src/main/resources/`
- Build output: `target/`
- Javadoc: `target/site/apidocs/`
- Logs: `logs/`

## Documentation

| Document | Purpose |
|----------|---------|
| `README.md` | User guide and features |
| `DEVELOPER.md` | Architecture and development |
| `CHANGELOG.md` | Version history |
| `PROJECT_SUMMARY.md` | Complete project overview |
| `pom.xml` | Build configuration |

## Performance Tips

- Use `mvn clean package` for final builds
- Run `mvn test` before pushing changes
- Profile auto-trace with large images using Java profiler
- Canvas redraw is O(points) - optimize if > 10K points
