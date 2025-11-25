# Quick Start Guide: Accessibility Features

**Graph Digitizer Java - Accessibility Quick Start**

This guide shows how to use the new accessibility features added to Graph Digitizer.

## New Accessibility Menu

Look for the **Accessibility** menu in the menu bar (between Themes and Window).

### Font Size

Change text size throughout the application:


1. Click **Accessibility → Font Size**

2. Choose: Small / Normal / Large / Extra Large

**Tip**: Select "Extra Large" for low vision use.

### Point Size

Make data points on the canvas larger:


1. Click **Accessibility → Point Size**

2. Choose: Small (4px) / Normal (6px) / Large (8px) / Extra Large (12px)

**Tip**: Use "Extra Large" to see points more clearly.

### Shape Variation

Use different shapes for each dataset (not just colors):


1. Click **Accessibility → Toggle Shape Variation**

2. When enabled: Dataset 0 = circles, Dataset 1 = squares, Dataset 2 = triangles, Dataset 3 = diamonds

**Why**: Helps color-blind users distinguish between datasets.

### High Contrast Mode

Switch to a high contrast theme optimized for low vision:


1. Click **Accessibility → Toggle High Contrast Mode**

2. Automatically applies "High Contrast Black" theme (white text on black background)

**Alternative**: Go to **Themes** menu and select "High Contrast White" (black text on white background)

### Focus Border Width

Make keyboard focus indicators more visible:


1. Click **Accessibility → Focus Border Width**

2. Choose: 2px / 3px (default) / 4px / 5px

**Tip**: Use 4px or 5px if you have trouble seeing where keyboard focus is.

## Keyboard Navigation

### Calibration Mode


1. Click **Calibrate** button (or press **Ctrl+L**)

2. Click on the canvas to place 4 calibration points

3. Use keyboard to adjust points:

   - **Tab**: Select next calibration point

   - **Shift+Tab**: Select previous calibration point

   - **Arrow Keys**: Move selected point by 1 pixel

   - **Ctrl+Arrow Keys**: Move selected point by 10 pixels (fast mode)

   - **Delete/Backspace**: Remove last calibration point

   - **Escape**: Cancel calibration mode

**Screen Reader**: Announcements will tell you which point is selected and where it moved to.

### Zoom Controls

Use keyboard shortcuts to zoom in/out:


- **Ctrl+Plus** or **Ctrl+Equals**: Zoom in by 10%

- **Ctrl+Minus**: Zoom out by 10%

- **Ctrl+0** (zero): Reset zoom to 100%

**Screen Reader**: Each zoom action announces the new zoom level.

## Settings Persistence

All accessibility settings are saved automatically to:

```

~/.graph-digitizer/accessibility.properties

```

Your preferences will be restored when you restart the application.

## Quick Setup for Low Vision Users

Recommended settings for low vision professionals:


1. **Accessibility → Font Size → Extra Large**

2. **Accessibility → Point Size → Extra Large**

3. **Accessibility → High Contrast Mode → Toggle** (enable)

4. **Accessibility → Focus Border Width → 5px**

5. **Accessibility → Shape Variation** (verify enabled - default)

## Quick Setup for Screen Reader Users

Recommended settings for NVDA/JAWS users:


1. Focus the canvas: Click on it or tab to it

2. Enter calibration mode: Click "Calibrate" or **Ctrl+L**

3. Place 4 calibration points with mouse

4. Press **Tab** to select first point

5. Use **Arrow Keys** to fine-tune position

6. Listen for announcements: "Moved calibration point 1 to pixel 450, 320"

## Testing with Windows Tools

### Windows Narrator (Built-in)


1. Press **Windows+Ctrl+Enter** to start Narrator

2. Navigate menus with **Alt** key and **Arrow Keys**

3. Listen for accessibility announcements

### Windows Magnifier (Built-in)


1. Press **Windows+Plus** to start Magnifier

2. Use **Ctrl+Alt+Arrow Keys** to pan around

3. Test with Point Size set to "Extra Large"

### Windows High Contrast Mode


1. Press **Left Alt+Left Shift+Print Screen** to toggle

2. Test with High Contrast Mode in application enabled

3. Verify focus indicators remain visible

## Troubleshooting

**Q: Font size changes but controls look cut off**  
A: Resize the window or maximize it. Extra Large font needs more space.

**Q: Can't see focus indicators**  
A: Go to **Accessibility → Focus Border Width → 5px**. Also try High Contrast Mode.

**Q: Screen reader not announcing calibration point moves**  
A: Ensure canvas has focus (click on it first). Check that screen reader is running.

**Q: Shape variation not working**  
A: It only applies to dataset points after calibration is complete. Toggle it and redraw.

**Q: Zoom shortcuts not working**  
A: Ensure no text field has focus. Click on the canvas or toolbar first.

## Keyboard Reference Card

 | Action | Keyboard Shortcut |
 | -------- | ------------------- |
 | Load Image | Ctrl+O |
 | Calibrate | Ctrl+L |
 | Auto Trace | Ctrl+T |
 | Save JSON | Ctrl+S |
 | Save CSV | Ctrl+Shift+S |
 | Exit | Ctrl+Q |
 | Zoom In | Ctrl+Plus |
 | Zoom Out | Ctrl+Minus |
 | Reset Zoom | Ctrl+0 |
 | Select Next Cal Point | Tab |
 | Select Prev Cal Point | Shift+Tab |
 | Move Point | Arrow Keys |
 | Move Point Fast | Ctrl+Arrow Keys |
 | Delete Point | Delete/Backspace |
 | Cancel Calibration | Escape |

## WCAG Compliance

This application meets:


- **WCAG 2.1 Level A**: Basic accessibility

- **WCAG 2.1 Level AA**: Enhanced accessibility

- **WCAG 2.1 Level AAA**: Enhanced contrast (High Contrast themes)

## Getting Help

For detailed technical documentation, see:


- [ACCESSIBILITY_IMPLEMENTATION_COMPLETE.md](ACCESSIBILITY_IMPLEMENTATION_COMPLETE.md) - Complete implementation details

- [ACCESSIBILITY_AUDIT_LOW_VISION.md](ACCESSIBILITY_AUDIT_LOW_VISION.md) - Original accessibility audit

- [ACCESSIBILITY.md](ACCESSIBILITY.md) - Full accessibility guide

## Feedback

If you encounter accessibility issues, please report them with:


1. Screen reader and version (e.g., NVDA 2024.1)

2. Operating system (e.g., Windows 11)

3. Steps to reproduce the issue

4. Expected vs. actual behavior
