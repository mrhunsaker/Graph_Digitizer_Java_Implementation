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
mkdir -p "$APPDIR/usr/lib"
mkdir -p "$APPDIR/usr/share/applications"
mkdir -p "$APPDIR/usr/share/icons/hicolor/256x256/apps"

# Find the shaded JAR
JAR_FILE=$(find target -name "graph-digitizer.jar" -type f | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Could not find graph-digitizer.jar"
    exit 1
fi

echo "Found JAR: $JAR_FILE"

# Create jlink runtime (minimal JRE with JavaFX modules)
echo "Creating custom JRE with jlink..."
JLINK_MODULES="java.base,java.desktop,java.logging,java.xml,java.naming,java.sql,jdk.unsupported"
JLINK_MODULES="$JLINK_MODULES,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"

jlink \
    --add-modules "$JLINK_MODULES" \
    --output "$APPDIR/usr/lib/jre" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2

# Copy JAR to AppDir
cp "$JAR_FILE" "$APPDIR/usr/lib/$APP_NAME.jar"

# Create launcher script
cat > "$APPDIR/usr/bin/$APP_NAME" << 'EOF'
#!/bin/bash
APPDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"
exec "$APPDIR/usr/lib/jre/bin/java" \
    -jar "$APPDIR/usr/lib/GraphDigitizer.jar" \
    "$@"
EOF

chmod +x "$APPDIR/usr/bin/$APP_NAME"

# Create AppRun symlink
ln -sf "usr/bin/$APP_NAME" "$APPDIR/AppRun"

# Copy icon (try multiple locations)
ICON_SOURCE=""
for icon_path in \
    "build/icons/scatter-plot-256.png" \
    "icons/scatter-plot-256.png" \
    "../icons/scatter-plot-256.png"; do
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
fi

# Create desktop file
cat > "$APPDIR/usr/share/applications/$APP_NAME.desktop" << EOF
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
EOF

# Symlink desktop file to root
ln -sf "usr/share/applications/$APP_NAME.desktop" "$APPDIR/$APP_NAME.desktop"

# Create AppStream metadata (optional but recommended)
mkdir -p "$APPDIR/usr/share/metainfo"
cat > "$APPDIR/usr/share/metainfo/$APP_NAME.appdata.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<component type="desktop-application">
  <id>com.digitizer.GraphDigitizer</id>
  <name>Graph Digitizer</name>
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
EOF

# Build the AppImage
echo "Building AppImage..."
OUTPUT_FILE="$BUILD_DIR/$APP_NAME-$VERSION-x86_64.AppImage"

appimagetool "$APPDIR" "$OUTPUT_FILE"

echo "=== AppImage created successfully ==="
echo "Location: $OUTPUT_FILE"
echo "Size: $(du -h "$OUTPUT_FILE" | cut -f1)"

# Make it executable
chmod +x "$OUTPUT_FILE"

# Optional: Create zsync file for delta updates
if command -v zsyncmake &> /dev/null; then
    echo "Creating zsync file for updates..."
    zsyncmake "$OUTPUT_FILE" -o "$OUTPUT_FILE.zsync"
fi

echo "Done!"
