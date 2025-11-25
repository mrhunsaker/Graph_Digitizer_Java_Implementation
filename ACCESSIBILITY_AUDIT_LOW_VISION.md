# Accessibility Audit Report: Low Vision & Screen Reader Support

**Graph Digitizer Java Application**  
**Date:** November 18, 2025  
**Focus Areas:** Low Vision, High Contrast, Tab Navigation, Screen Reader Accessibility

---

## Executive Summary

**Overall Assessment:** GOOD with opportunities for enhancement

The Graph Digitizer application demonstrates solid accessibility foundations with comprehensive screen reader support, keyboard navigation, and theme options. However, several areas require attention to fully support low vision professionals who rely on high contrast, magnification, and assistive technologies.

### Audit Scope


- Low vision user requirements (magnification, contrast, font sizes)

- High contrast mode compatibility

- Tab order and keyboard navigation

- Screen reader announcements and ARIA-like properties

- Visual focus indicators

- Color dependency and alternatives

---

## Critical Findings (Priority 1 - Immediate Action Required)

### 1. Canvas Accessibility - CRITICAL ISSUE ⚠️

**Problem:** The main `CanvasPanel` uses JavaFX Canvas which renders pixels directly, making it completely inaccessible to screen readers.

**File:** `CanvasPanel.java`  
**Lines:** 129-136

```java
canvas = new Canvas(800, 600);
canvas.setAccessibleText("Image Canvas");
canvas.setAccessibleRoleDescription("Canvas for image display and point selection");
canvas.setAccessibleHelp(
    "Click to place calibration points or data points. " +
    "Use arrow keys to adjust calibration points. " +
    "Press Enter to confirm calibration."
);

```

**Impact:**


- Screen readers cannot announce canvas content (image, points, calibration markers)

- Low vision users cannot inspect individual data points

- No alternative text representation of visual data

- Calibration points are visual-only (red circles)

**Recommended Fixes:**


1. **Add audio feedback for point placement:**

```java
private void handleMouseClick(MouseEvent event) {
    // ... existing code ...
    if (!calibrationMode) {
        // After adding point:
        AccessibilityHelper.announceCoordinates(
            "Point " + (active.getPoints().size()) + " added",
            newPoint.x(), newPoint.y()
        );
        // Consider: Audible click/beep for confirmation
    }
}

```


1. **Provide alternative point list view:**

   - Add a hidden accessible list control that mirrors canvas points

   - Allow keyboard navigation through points

   - Provide "read all points" command


2. **Add keyboard navigation for calibration:**

```java
// Currently missing - add arrow key support to move calibration points
case UP: case DOWN: case LEFT: case RIGHT:
    if (calibrationMode && !calibrationPoints.isEmpty()) {
        Point2D last = calibrationPoints.get(calibrationPoints.size() - 1);
        double dx = 0, dy = 0;
        if (event.getCode() == KeyCode.LEFT) dx = -1;
        if (event.getCode() == KeyCode.RIGHT) dx = 1;
        if (event.getCode() == KeyCode.UP) dy = -1;
        if (event.getCode() == KeyCode.DOWN) dy = 1;
        calibrationPoints.set(calibrationPoints.size() - 1, 
            new Point2D(last.getX() + dx, last.getY() + dy));
        AccessibilityHelper.announceCoordinates(
            "Calibration point adjusted", last.getX() + dx, last.getY() + dy);
        redraw();
    }
    break;

```

---

### 2. Focus Indicators - HIGH PRIORITY ⚠️

**Problem:** No visible focus indicators on controls, critical for keyboard-only and low vision users.

**Files:** All UI components  
**Current State:** JavaFX default focus indicators may be insufficient

**Impact:**


- Keyboard users cannot see which control has focus

- Low vision users lose track during tab navigation

- Fails WCAG 2.4.7 (Focus Visible)

**Recommended Fixes:**


1. **Add custom focus styles globally:**

```java
// In MainWindow.initialize() or ThemeManager
scene.getRoot().setStyle(scene.getRoot().getStyle() + 
    "; -fx-focus-color: #0066CC; " +
    "-fx-faint-focus-color: #0066CC22; " +
    "-fx-focus-border-width: 3px;");

```


1. **Add focus event handlers for announcements:**

```java
// In ControlPanel and other components
titleField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
    if (isNowFocused) {
        AccessibilityHelper.announceHelp("Title Field", 
            titleField.getAccessibleHelp());
    }
});

```


1. **Ensure focus indicators work in all themes:**

   - Test with high contrast themes

   - Verify 3:1 contrast ratio against background

   - Consider thicker borders (3-4px) for low vision users

---

### 3. High Contrast Mode Support - HIGH PRIORITY ⚠️

**Problem:** Themes use inline CSS but don't respect Windows High Contrast Mode or platform accessibility settings.

**Files:** `ThemeManager.java`, theme definitions  
**Lines:** 46-59

**Impact:**


- Users who enable OS-level high contrast lose all theme styling

- Critical UI elements may become invisible

- Color-coded datasets become indistinguishable

**Recommended Fixes:**


1. **Detect OS high contrast mode:**

```java
public static boolean isHighContrastMode() {
    // Windows: Check registry or system property
    String hcMode = System.getProperty("sun.desktop.highContrast");
    return "true".equalsIgnoreCase(hcMode);
}

```


1. **Add dedicated high contrast themes:**

```java
THEMES.put("High Contrast Black", 
    "-fx-base: #000000; " +
    "-fx-control-inner-background: #000000; " +
    "-fx-text-fill: #FFFFFF; " +
    "-fx-accent: #FFFF00; " +  // Yellow accent for maximum contrast
    "-fx-selection-bar: #00FF00;");

THEMES.put("High Contrast White", 
    "-fx-base: #FFFFFF; " +
    "-fx-control-inner-background: #FFFFFF; " +
    "-fx-text-fill: #000000; " +
    "-fx-accent: #0000FF; " +
    "-fx-selection-bar: #FF0000;");

```


1. **Add contrast ratio validation:**

```java
/**

 * Calculates WCAG contrast ratio between two colors.

 * Minimum: 4.5:1 for normal text, 3:1 for large text.
 */
public static double getContrastRatio(Color fg, Color bg) {
    double l1 = getLuminance(fg);
    double l2 = getLuminance(bg);
    double lighter = Math.max(l1, l2);
    double darker = Math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
}

```

---

## High Priority Findings (Priority 2 - Plan for Next Release)

### 4. Tab Order & Keyboard Navigation

**Current State:** PARTIALLY IMPLEMENTED ✓

**Strengths:**


- Controls are keyboard accessible

- Menu items have accelerators (Ctrl+S, Ctrl+O, etc.)

- Canvas has focus traversable enabled

**Gaps:**


1. **No explicit tab order management:**

```java
// Should add in ControlPanel constructor:
titleField.setFocusTraversable(true);
xlabelField.setFocusTraversable(true);
ylabelField.setFocusTraversable(true);

// Set tab order explicitly:
Platform.runLater(() -> {
    titleField.requestFocus(); // Start here
});

```


1. **Missing keyboard shortcuts for common actions:**

   - Zoom in/out (Ctrl+Plus/Minus)

   - Pan canvas (Arrow keys when not in calibration mode)

   - Next/Previous dataset (Tab/Shift+Tab in data view)

**Recommended Additions:**

```java
// In MainWindow.createToolbar()
scene.setOnKeyPressed(event -> {
    if (event.isControlDown()) {
        switch (event.getCode()) {
            case PLUS, EQUALS -> {
                double newZoom = Math.min(3.0, zoomSlider.getValue() + 0.25);
                zoomSlider.setValue(newZoom);
                AccessibilityHelper.announceAction("Zoom: " + 
                    Math.round(newZoom * 100) + "%");
            }
            case MINUS, UNDERSCORE -> {
                double newZoom = Math.max(0.25, zoomSlider.getValue() - 0.25);
                zoomSlider.setValue(newZoom);
                AccessibilityHelper.announceAction("Zoom: " + 
                    Math.round(newZoom * 100) + "%");
            }
            case DIGIT0 -> {
                zoomSlider.setValue(1.0);
                AccessibilityHelper.announceAction("Zoom reset to 100%");
            }
        }
    }
});

```

---

### 5. Color Dependency & Alternatives

**Problem:** Data points and datasets rely heavily on color for identification.

**Current Implementation:**


- Dataset colors: Blue, Orange, Green, Pink, Yellow, Light Blue

- Color names announced via `getColorName()` method ✓

- But visual distinction still required

**Gaps for Low Vision Users:**


1. **No pattern/texture alternatives:**

   - All points rendered as solid circles

   - No shape variation (circle, square, triangle, diamond)

   - No pattern fills


2. **Small point size:**

   - Points rendered at 6px diameter

   - Difficult to see at high zoom

   - No option to increase point size

**Recommended Fixes:**


1. **Add shape variation per dataset:**

```java
// In CanvasPanel.redraw()
for (int i = 0; i < datasets.size(); i++) {
    Dataset dataset = datasets.get(i);
    gc.setFill(dataset.getColor());
    gc.setStroke(Color.BLACK);
    gc.setLineWidth(1);
    
    for (Point point : dataset.getPoints()) {
        Point2D canvasPoint = transformer.dataToCanvas(point.x(), point.y());
        double x = canvasPoint.getX();
        double y = canvasPoint.getY();
        double size = 6; // Make configurable
        
        // Different shapes per dataset
        switch (i % 4) {
            case 0 -> gc.fillOval(x - size/2, y - size/2, size, size);
            case 1 -> gc.fillRect(x - size/2, y - size/2, size, size);
            case 2 -> { // Triangle
                gc.fillPolygon(
                    new double[]{x, x - size/2, x + size/2},
                    new double[]{y - size/2, y + size/2, y + size/2},
                    3);
            }
            case 3 -> { // Diamond
                gc.fillPolygon(
                    new double[]{x, x + size/2, x, x - size/2},
                    new double[]{y - size/2, y, y + size/2, y},
                    4);
            }
        }
        // Add outline for contrast
        gc.strokeOval(x - size/2, y - size/2, size, size);
    }
}

```


1. **Add point size control:**

```java
// In ControlPanel or preferences
Slider pointSizeSlider = new Slider(4, 16, 6);
pointSizeSlider.setShowTickLabels(true);
pointSizeSlider.setShowTickMarks(true);
pointSizeSlider.setMajorTickUnit(4);
AccessibilityHelper.setLabelAccessibility(
    new Label("Point Size:"),
    "Point Size", "Control");

```

---

### 6. Screen Reader Announcements

**Current State:** GOOD with room for improvement ✓

**Strengths:**


- Comprehensive `AccessibilityHelper` class

- Announcements via logging (picked up by screen readers)

- Context-appropriate announcements (mode changes, progress, coordinates)

**Gaps:**


1. **Missing live region announcements:**

   - Status bar updates not announced

   - Point count changes not announced

   - Calibration progress not live-updated


2. **Verbose announcements:**

   - Full color hex codes announced (e.g., "#0072B2")

   - Could be simplified to just color name

**Recommended Fixes:**


1. **Add ARIA-like live regions:**

```java
// In StatusBar.java
public void setStatus(String message) {
    statusLabel.setText(message);
    // Make it a live region
    statusLabel.setAccessibleText(message);
    AccessibilityHelper.announceStatus(message);
    
    // Also show temporarily highlighted for low vision users
    statusLabel.setStyle("-fx-background-color: yellow; " +
                         "-fx-padding: 4px;");
    PauseTransition pause = new PauseTransition(Duration.seconds(2));
    pause.setOnFinished(e -> statusLabel.setStyle(""));
    pause.play();
}

```


1. **Simplify color announcements:**

```java
// In ControlPanel.getColorName()
private String getColorName(String hexColor) {
    // Don't announce hex code to screen readers
    return switch (hexColor.toUpperCase()) {
        case "#0072B2" -> "Blue";
        case "#E69F00" -> "Orange";
        case "#009E73" -> "Green";
        case "#CC79A7" -> "Pink";
        case "#F0E442" -> "Yellow";
        case "#56B4E9" -> "Light Blue";
        default -> "Custom color"; // Not full hex
    };
}

```


1. **Add progress announcements:**

```java
// In CanvasPanel.performAutoTrace()
public void performAutoTrace() {
    // ... existing code ...
    
    AccessibilityHelper.announceProgress("Auto-trace",
        tracedPoints.size(), tracedPoints.size());
    AccessibilityHelper.announceAction(
        "Auto-trace complete. " + tracedPoints.size() + 
        " points added to " + activeDataset.getName());
}

```

---

## Medium Priority Findings (Priority 3 - Future Enhancements)

### 7. Text Sizing & Magnification

**Current State:** Uses default JavaFX font sizes

**Gaps:**


- No user control over font size

- Small labels (14px for section headings)

- May be difficult to read for low vision users

**Recommended:**


1. **Add font size preference:**

```java
public enum FontSize {
    SMALL(10, 12, 14),
    NORMAL(12, 14, 16),
    LARGE(14, 16, 18),
    EXTRA_LARGE(16, 18, 20);
    
    final double body, label, heading;
    
    FontSize(double body, double label, double heading) {
        this.body = body;
        this.label = label;
        this.heading = heading;
    }
}

public static void applyFontSize(Scene scene, FontSize size) {
    scene.getRoot().setStyle(scene.getRoot().getStyle() +
        "; -fx-font-size: " + size.body + "px;");
}

```


1. **Respect OS text scaling:**

```java
// Check for Windows text scaling setting
double scale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
if (scale > 1.0) {
    scene.getRoot().setStyle(scene.getRoot().getStyle() +
        "; -fx-font-size: " + (12 * scale) + "px;");
}

```

---

### 8. Zoom & Magnification

**Current State:** GOOD ✓


- Zoom slider (0.25x to 3x)

- Fit and 100% buttons

- Node-level scaling

**Gaps:**


- No keyboard shortcuts (covered in #4)

- Zoom doesn't affect point rendering size

- No magnifier lens for precise placement

**Recommended:**


- Points should maintain visual size at all zoom levels

- Add circular magnifier window (like Julia version)

---

### 9. Tooltips & Timing

**Current State:** Tooltips added to most controls ✓

**Gaps:**


- Default tooltip delay (1 second) may be too short

- No extended display time for screen reader users

- Small font size

**Recommended:**

```java
// Set global tooltip behavior
Tooltip.install(control, new Tooltip(text) {{
    setShowDelay(Duration.millis(500));  // Faster appearance
    setHideDelay(Duration.seconds(10));   // Stay longer
    setShowDuration(Duration.seconds(30)); // Allow reading
    setStyle("-fx-font-size: 14px;");     // Larger text
}});

```

---

## Positive Findings ✓

### Strengths


1. **Comprehensive Screen Reader Support:**

   - All controls have accessible names

   - Role descriptions provided

   - Help text available

   - Coordinate and progress announcements


2. **Keyboard Accessibility:**

   - All functionality available without mouse

   - Keyboard accelerators for common actions

   - Enter/Escape for mode changes


3. **Multiple Themes:**

   - 14 themes including light and dark options

   - Themes persist across sessions

   - Good variety for personal preference


4. **Semantic Structure:**

   - Logical grouping of controls

   - Section headings for screen readers

   - Clear separation of concern


5. **Error Feedback:**

   - Validation errors announced

   - Clear error messages

   - Multiple feedback channels (visual + audio)

---

## WCAG 2.1 Compliance Summary

 | Criterion | Level | Status | Notes |
 | ----------- | ------- | -------- | ------- |
 | 1.1.1 Non-text Content | A | ⚠️ Partial | Canvas content not accessible |
 | 1.3.1 Info and Relationships | A | ✓ Pass | Good semantic structure |
 | 1.3.2 Meaningful Sequence | A | ✓ Pass | Logical tab order |
 | 1.4.1 Use of Color | A | ⚠️ Partial | Color used but names provided |
 | 1.4.3 Contrast (Minimum) | AA | ⚠️ Needs Testing | Themes not validated |
 | 1.4.11 Non-text Contrast | AA | ⚠️ Needs Testing | Focus indicators may be weak |
 | 2.1.1 Keyboard | A | ✓ Pass | All functionality available |
 | 2.1.2 No Keyboard Trap | A | ✓ Pass | No traps detected |
 | 2.4.3 Focus Order | A | ✓ Pass | Logical order |
 | 2.4.7 Focus Visible | AA | ❌ Fail | Insufficient focus indicators |
 | 3.2.4 Consistent Identification | AA | ✓ Pass | Consistent naming |
 | 4.1.2 Name, Role, Value | A | ✓ Pass | Accessibility properties set |

**Overall:** Level A mostly achieved, Level AA requires work

---

## Recommended Action Plan

### Immediate (Sprint 1)


1. Add keyboard navigation for calibration points

2. Enhance focus indicators (3px borders, high contrast)

3. Add high contrast themes

4. Implement zoom keyboard shortcuts

### Short-term (Sprint 2-3)


5. Add shape variations for datasets

2. Implement point size control

3. Add live region announcements

4. Validate contrast ratios for all themes

### Long-term (Future Releases)


9. Alternative canvas representation (accessible data table)

2. Magnifier lens feature

3. Font size preferences

4. OS-level accessibility setting integration

---

## Testing Recommendations

### Required Testing Platforms


1. **Windows Narrator** - Built-in screen reader

2. **NVDA** - Popular free screen reader

3. **JAWS** - Professional screen reader

4. **Windows Magnifier** - Test at 200%, 400%

5. **Windows High Contrast Mode** - All themes

### Test Scenarios


1. **Screen Reader User:**

   - Navigate entire UI with keyboard only

   - Load image, calibrate, add points

   - Save data

   - Verify all actions announced


2. **Low Vision User:**

   - Enable high contrast mode

   - Increase zoom to 400%

   - Verify all controls visible

   - Test with various themes


3. **Keyboard-Only User:**

   - Complete full workflow without mouse

   - Verify focus always visible

   - Test all keyboard shortcuts


4. **Color Blind User:**

   - Distinguish all datasets

   - Complete calibration

   - Verify shape/pattern alternatives work

---

## Conclusion

The Graph Digitizer application has a solid accessibility foundation with excellent screen reader support and keyboard navigation. The primary challenges are:


1. **Canvas accessibility** - The pixel-based canvas needs alternative representations

2. **High contrast support** - Themes need validation and high-contrast variants

3. **Visual feedback** - Focus indicators and color alternatives need enhancement

With the recommended fixes, the application can achieve WCAG 2.1 Level AA compliance and serve low vision professionals effectively.

**Estimated Effort:** 3-5 developer days for Priority 1-2 items

---

**Auditor Notes:** This audit was conducted via code review. User testing with actual assistive technology users is strongly recommended before production release.
