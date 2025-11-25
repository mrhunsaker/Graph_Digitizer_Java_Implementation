# Test Resources

This directory contains test assets and example files for the Graph Digitizer Java implementation.

## Structure

```

src/test/resources/
├── images/                 # Test image files for manual and automated testing
│   ├── BarGraph1.png       # Bar chart example
│   ├── LineChart1.png      # Line chart example
│   ├── LineChart2.png      # Additional line chart
│   ├── LineChart3.webp     # WebP format test
│   ├── LineGraph.png       # Simple line graph
│   ├── LineGraph.bmp       # BMP format test
│   ├── LineGraph.tiff      # TIFF format test
│   ├── LineGraph_fullquality.jpeg  # High-quality JPEG
│   └── LineGraph_midquality.jpeg   # Mid-quality JPEG
├── assets/                 # Documentation and reference files
│   └── sample_README.md    # Original Julia GraphDigitizer README
└── demo_autotrace.jl       # Julia auto-trace demo script (reference)

```

## Usage in Tests

### Java Tests

Test images are available at runtime via classpath:

```java
String imagePath = Objects.requireNonNull(
    getClass().getClassLoader().getResource("images/LineGraph.png")
).getPath();

```

### Manual Testing

You can use these images with the GUI:


1. Run `mvn javafx:run` to launch the application

2. Click "Load Image" and navigate to `src/test/resources/images/`

3. Select any test image to load

## Image Formats Tested

The test set includes multiple formats to ensure the application handles:


- **PNG** (most common): BarGraph1.png, LineChart1.png, LineChart2.png, LineGraph.png

- **JPEG** (compressed): LineGraph_fullquality.jpeg, LineGraph_midquality.jpeg

- **WebP** (modern format): LineChart3.webp

- **BMP** (legacy): LineGraph.bmp

- **TIFF** (archive/scan): LineGraph.tiff

## Reference Files


- **sample_README.md**: Original Julia GraphDigitizer documentation (reference for feature parity)

- **demo_autotrace.jl**: Julia auto-trace implementation example (reference for algorithm)

## Adding New Test Images

When adding new test images:


1. Place images in the `images/` subdirectory

2. Ensure they are different graph types or edge cases (e.g., high DPI, unusual aspect ratios)

3. Update this README with a brief description

4. Consider file size for repository storage (use compression when appropriate)

## License

Test images and reference files inherit the project's Apache License 2.0.
