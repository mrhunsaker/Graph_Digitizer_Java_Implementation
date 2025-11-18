# Changelog

All notable changes to the Graph Digitizer project will be documented in this file.

## [1.2.0] - Java Edition Release

### Overview

Complete rewrite of Graph Digitizer from Julia to Java 21 with JavaFX GUI.

### Added

- **Java 21 implementation** with full Maven build system
- **JavaFX 21** graphical interface replacing GTK
- **Core package** with zero GUI dependencies:
  - `Point` record type for immutable coordinates
  - `Dataset` class for managing point collections
  - `CalibrationState` for calibration management
  - `CoordinateTransformer` for linear/logarithmic coordinate transforms
  - `ColorUtils` for color operations and hex parsing
  - `FileUtils` for filename sanitization and defaults
- **Image package** for image I/O and processing:
  - `ImageLoader` for PNG/JPEG loading
  - `AutoTracer` for color-based automatic curve extraction
- **IO package** for multiple export formats:
  - `JsonExporter` for full-fidelity JSON with metadata
  - `CsvExporter` for wide-format CSV export
  - GSON-based JSON serialization
  - Apache Commons CSV integration
- **UI package** with JavaFX components:
  - `GraphDigitizerApp` as application entry point
  - `MainWindow` for main window orchestration
  - `CanvasPanel` for image display and interaction
  - `ControlPanel` for calibration/dataset controls
  - `StatusBar` for status messages
- **Testing infrastructure**:
  - JUnit 4 and JUnit 5 support
  - Unit tests for core utilities
  - Test structure mirroring source code
- **Build system**:
  - Maven pom.xml with all dependencies configured
  - Maven plugins for JavaFX, assembly, shade
  - Logback logging configuration
- **Documentation**:
  - Comprehensive README with usage instructions
  - Developer guide with architecture overview
  - Javadoc comments on all public APIs
  - CHANGELOG (this file)
  - Apache License 2.0

### Changed

- **Architecture**: Single-file Julia monolith → multi-module Java structure
- **UI Framework**: GTK (Gtk.jl) → JavaFX (cross-platform, native look-and-feel)
- **Dependency Management**: Julia package manager → Maven
- **Build Process**: Julia REPL → Maven command-line builds
- **Logging**: Julia console output → SLF4J with Logback

### Architecture Improvements

1. **Separation of Concerns**:
   - Core logic isolated from GUI for testability
   - File I/O separated into dedicated package
   - Image processing in separate module

2. **Type Safety**:
   - Replaced dynamic Julia types with Java's type system
   - Immutable `Point` record type for coordinates
   - Enum-style color definitions

3. **Testing**:
   - Core classes testable without GUI framework
   - Unit test coverage for utilities and algorithms
   - Test structure mirrors source structure

4. **Extensibility**:
   - Plugin-friendly design for export formats
   - Clear interfaces for adding new functionality
   - Modular package structure

### Performance Considerations

- JavaFX canvas rendering faster than Cairo for most use cases
- Compiled bytecode execution vs. interpreted Julia
- Native image loading through Java ImageIO
- Optimized color distance calculations for auto-trace

### Known Limitations (vs. Julia Version)

- Snap X values feature not yet implemented (planned for 1.2.1)
- Precision zoom/magnifier overlay not yet implemented (planned for 1.3.0)
- Rectangular magnifier preview not yet implemented (planned for 1.3.0)
- Project files (.gdz with embedded images) not implemented (planned for 2.0.0)

### Migration Notes

- **Coordinate System**: Identical linear and logarithmic transformations to Julia version
- **Color Matching**: Same Euclidean RGB distance algorithm as auto-trace
- **File Formats**: 100% compatible JSON and CSV with Julia version
- **License**: Maintained Apache 2.0 license from original

## Development Notes

### Why Java 21?

- Modern language features (records, pattern matching)
- Stable LTS foundation for future development
- Excellent IDE support in VS Code, IntelliJ, Eclipse
- Large ecosystem of libraries and frameworks
- Strong backward compatibility across versions

### Why JavaFX?

- Cross-platform GUI (Windows, macOS, Linux)
- Part of OpenJDK ecosystem
- Modern CSS styling capabilities
- Canvas API for custom drawing
- Good performance for 2D graphics

### Why Maven?

- Industry-standard build tool for Java
- Easy dependency management
- Large plugin ecosystem
- Simple, declarative pom.xml configuration
- Works well with CI/CD systems

## Future Roadmap

### 1.2.1 (Q1 2025)

- [ ] Implement snap X values feature
- [ ] Add snapping guide lines visualization
- [ ] Keyboard shortcuts customization UI

### 1.3.0 (Q2 2025)

- [ ] Precision zoom with circular magnifier
- [ ] Rectangular magnifier preview
- [ ] Undo/redo stack
- [ ] Recent files menu

### 2.0.0 (TBD)

- [ ] Project file format (.gdz) with embedded images
- [ ] Plugin system for custom exporters
- [ ] Batch processing CLI tool
- [ ] Python script integration hooks

## Contributors

- **Michael Ryan Hunsaker** - Original Julia implementation, Java port
- **Community Contributors** - (Welcome!)

## Installation & Usage

See [README.md](README.md) for installation and quick start.

See [DEVELOPER.md](DEVELOPER.md) for development guide.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
