#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"
APP_NAME="GraphDigitizer"
BUILD_DIR="target/generated_builds/appimage"
APPDIR="$BUILD_DIR/$APP_NAME.AppDir"

echo "=== Building AppImage for $APP_NAME version $VERSION ==="

# Clean and create build directory
rm -rf "$BUILD_DIR"
mkdir -p "$APPDIR/usr/bin"
mkdir -p "$APPDIR/usr/lib/jvm"
mkdir -p "$APPDIR/usr/share/applications"
mkdir -p "$APPDIR/usr/share/icons/hicolor/256x256/apps"

# Find the shaded JAR
JAR_FILE=$(find target -name "graph-digitizer.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find graph-digitizer.jar"
    exit 1
fi

echo "Found JAR: $JAR_FILE"

# Copy shaded JAR to AppDir (this JAR already contains ALL dependencies including JavaFX)
cp "$JAR_FILE" "$APPDIR/usr/lib/$APP_NAME.jar"
echo "Copied shaded JAR to AppDir"

# Copy the current Java runtime (simpler than jlink, includes everything)
echo "Copying Java runtime from JAVA_HOME..."
if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME is not set"
    exit 1
fi

echo "Using Java from: $JAVA_HOME"
cp -r "$JAVA_HOME" "$APPDIR/usr/lib/jvm/java"

# Remove unnecessary files to reduce size
echo "Removing unnecessary files from JRE..."
rm -rf "$APPDIR/usr/lib/jvm/java/demo" 2>/dev/null || true
rm -rf "$APPDIR/usr/lib/jvm/java/sample" 2>/dev/null || true
rm -rf "$APPDIR/usr/lib/jvm/java/man" 2>/dev/null || true
rm -rf "$APPDIR/usr/lib/jvm/java/src.zip" 2>/dev/null || true
rm -rf "$APPDIR/usr/lib/jvm/java/lib/src.zip" 2>/dev/null || true

echo "Java runtime copied successfully"

# Create launcher script
cat > "$APPDIR/usr/bin/$APP_NAME" << 'LAUNCHER_EOF'
#!/bin/bash
APPDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"
exec "$APPDIR/usr/lib/jvm/java/bin/java" \
    -jar "$APPDIR/usr/lib/GraphDigitizer.jar" \
    "$@"
LAUNCHER_EOF

chmod +x "$APPDIR/usr/bin/$APP_NAME"
echo "Created launcher script"

# Create AppRun symlink
ln -sf "usr/bin/$APP_NAME" "$APPDIR/AppRun"

# Copy icon (try multiple locations)
ICON_SOURCE=""
for icon_path in \
    "build/icons/scatter-plot-256.png" \
    "icons/scatter-plot-256.png" \
    "../icons/scatter-plot-256.png" \
    "src/main/resources/icons/scatter-plot-256.png"; do
    if [ -f "$icon_path" ]; then
        ICON_SOURCE="$icon_path"
        break
    fi
done

if [ -n "$ICON_SOURCE" ]; then
    echo "Using icon: $ICON_SOURCE"
    cp "$ICON_SOURCE" "$APPDIR/usr/share/icons/hicolor/256x256/apps/$APP_NAME.png"
    cp "$ICON_SOURCE" "$APPDIR/$APP_NAME.png"
else
    echo "WARNING: No icon found, AppImage will have no icon"
    # Create a simple placeholder icon
    echo "Creating placeholder icon..."
    cat > "$APPDIR/$APP_NAME.png" << 'ICON_EOF'
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==
ICON_EOF
fi

# Create desktop file
cat > "$APPDIR/usr/share/applications/$APP_NAME.desktop" << DESKTOP_EOF
[Desktop Entry]
Type=Application
Name=Graph Digitizer
GenericName=Graph Data Extraction Tool
Comment=Extract numeric data points from graph images
Exec=$APP_NAME %F
Icon=$APP_NAME
Categories=Science;Education;DataVisualization;
Terminal=false
StartupWMClass=GraphDigitizer
MimeType=image/png;image/jpeg;image/tiff;image/bmp;
DESKTOP_EOF

# Symlink desktop file to root
ln -sf "usr/share/applications/$APP_NAME.desktop" "$APPDIR/$APP_NAME.desktop"

# Create AppStream metadata
mkdir -p "$APPDIR/usr/share/metainfo"
cat > "$APPDIR/usr/share/metainfo/$APP_NAME.appdata.xml" << METADATA_EOF
<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>com.digitizer.GraphDigitizer</id>
  <n>Graph Digitizer</n>
  <summary>Extract numeric data from graph images</summary>
  <description>
    <p>
      An interactive GUI tool for extracting numeric data points from raster images of graphs.
      Supports various image formats and exports to CSV/JSON.
    </p>
  </description>
  <launchable type="desktop-id">$APP_NAME.desktop</launchable>
  <provides>
    <binary>$APP_NAME</binary>
  </provides>
  <releases>
    <release version="$VERSION" date="$(date +%Y-%m-%d)"/>
  </releases>
</component>
METADATA_EOF

echo "Created desktop integration files"

# Show AppDir structure
echo ""
echo "AppDir structure:"
du -sh "$APPDIR"
du -sh "$APPDIR/usr/lib/jvm/java" 2>/dev/null || echo "  JVM: size unknown"
du -sh "$APPDIR/usr/lib/$APP_NAME.jar"

# Build the AppImage
echo ""
echo "Building AppImage with appimagetool..."
OUTPUT_FILE="$BUILD_DIR/$APP_NAME-$VERSION-x86_64.AppImage"

# Set ARCH for appimagetool
export ARCH=x86_64

appimagetool "$APPDIR" "$OUTPUT_FILE"

echo ""
echo "=== AppImage created successfully ==="
echo "Location: $OUTPUT_FILE"
echo "Size: $(du -h "$OUTPUT_FILE" | cut -f1)"

# Make it executable
chmod +x "$OUTPUT_FILE"

# Optional: Create zsync file for delta updates
if command -v zsyncmake &> /dev/null; then
    echo "Creating zsync file for updates..."
    zsyncmake "$OUTPUT_FILE" -o "$OUTPUT_FILE.zsync" 2>/dev/null || echo "  (zsync creation skipped)"
fi

echo ""
echo "✓ Done!"
