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

# Create minimal JRE with jlink (NO JavaFX modules - they will be bundled separately)
echo "Creating custom JRE with jlink (without JavaFX)..."
JLINK_MODULES="java.base,java.desktop,java.logging,java.xml,java.naming,java.sql,java.prefs,java.scripting,jdk.unsupported,jdk.crypto.ec"

echo "Modules: $JLINK_MODULES"

jlink \
    --add-modules "$JLINK_MODULES" \
    --output "$APPDIR/usr/lib/jre" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2

echo "Custom JRE created successfully"

# Copy shaded JAR to AppDir
cp "$JAR_FILE" "$APPDIR/usr/lib/$APP_NAME.jar"
echo "Copied JAR to AppDir"

# Copy JavaFX JARs from Maven dependencies
echo "Copying JavaFX dependencies..."
mkdir -p "$APPDIR/usr/lib/javafx"

# Get the local Maven repository path
MAVEN_REPO="${HOME}/.m2/repository"

# JavaFX version from your pom.xml
JAVAFX_VERSION="21.0.2"

# Detect platform
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    JAVAFX_PLATFORM="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    JAVAFX_PLATFORM="mac"
else
    JAVAFX_PLATFORM="linux"  # default to linux
fi

echo "Detected platform: $JAVAFX_PLATFORM"

# Copy JavaFX JARs - try both platform-specific and non-platform JARs
for module in base graphics controls fxml swing media web; do
    # Try platform-specific JAR first
    JAVAFX_JAR="$MAVEN_REPO/org/openjfx/javafx-$module/$JAVAFX_VERSION/javafx-$module-$JAVAFX_VERSION-$JAVAFX_PLATFORM.jar"
    if [ -f "$JAVAFX_JAR" ]; then
        echo "  ✓ Copying javafx-$module (platform-specific)"
        cp "$JAVAFX_JAR" "$APPDIR/usr/lib/javafx/"
    else
        # Try non-platform JAR as fallback
        JAVAFX_JAR_GENERIC="$MAVEN_REPO/org/openjfx/javafx-$module/$JAVAFX_VERSION/javafx-$module-$JAVAFX_VERSION.jar"
        if [ -f "$JAVAFX_JAR_GENERIC" ]; then
            echo "  ✓ Copying javafx-$module (generic)"
            cp "$JAVAFX_JAR_GENERIC" "$APPDIR/usr/lib/javafx/"
        else
            echo "  ✗ WARNING: javafx-$module not found"
        fi
    fi
done

# Verify JavaFX JARs were copied
JAVAFX_COUNT=$(find "$APPDIR/usr/lib/javafx" -name "*.jar" 2>/dev/null | wc -l)
echo "Copied $JAVAFX_COUNT JavaFX JAR files"

if [ "$JAVAFX_COUNT" -lt 3 ]; then
    echo "ERROR: Not enough JavaFX JARs found. Expected at least 3 (base, graphics, controls)"
    echo "Checking Maven repository at: $MAVEN_REPO"
    echo "Looking for JavaFX version: $JAVAFX_VERSION"
    echo ""
    echo "Contents of javafx directory:"
    ls -la "$APPDIR/usr/lib/javafx/" 2>/dev/null || echo "  Directory is empty or does not exist"
    echo ""
    echo "Attempting to find JavaFX in Maven repo:"
    find "$MAVEN_REPO/org/openjfx" -name "*$JAVAFX_VERSION*.jar" 2>/dev/null | head -10 || echo "  No JavaFX JARs found"
    exit 1
fi

# Create launcher script with JavaFX module path
cat > "$APPDIR/usr/bin/$APP_NAME" << 'LAUNCHER_EOF'
#!/bin/bash
APPDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"
JAVAFX_LIBS="$APPDIR/usr/lib/javafx"

# Build module path from JavaFX JARs
MODULE_PATH=""
for jar in "$JAVAFX_LIBS"/*.jar; do
    if [ -z "$MODULE_PATH" ]; then
        MODULE_PATH="$jar"
    else
        MODULE_PATH="$MODULE_PATH:$jar"
    fi
done

exec "$APPDIR/usr/lib/jre/bin/java" \
    --module-path "$MODULE_PATH" \
    --add-modules javafx.controls,javafx.fxml,javafx.swing \
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
METADATA_EOF

echo "Created desktop integration files"

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
    zsyncmake "$OUTPUT_FILE" -o "$OUTPUT_FILE.zsync"
fi

echo ""
echo "✓ Done!"
