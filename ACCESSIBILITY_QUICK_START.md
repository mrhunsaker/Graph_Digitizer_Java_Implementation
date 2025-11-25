# Graph Digitizer - Accessibility Quick Start

**For Screen Reader Users**

---

## 🎯 Fastest Way to Get Started

### Windows (NVDA)

```


1. Download NVDA: [NVDA download](https://www.nvaccess.org/download/)

2. Install and run NVDA

3. Launch Graph Digitizer

4. Press Tab to navigate, Space/Enter to activate

5. Hear all labels and announcements

```

### Windows (Built-in Narrator)

```


1. Press Windows + Ctrl + N to start Narrator

2. Press Tab to navigate

3. Space to activate buttons

4. All UI read aloud

```

### macOS (VoiceOver)

```


1. Press Cmd + F5 to toggle VoiceOver

2. Press VO + Right/Left Arrow to navigate

3. Press VO + Space to activate

4. All labels are announced

```

---

## ⌨️ Essential Keyboard Shortcuts

### Navigation

 | Key | What It Does |
 | ----- | -------------- |
 | Tab | Move to next control |
 | Shift+Tab | Move to previous control |
 | Space | Click focused button |
 | Enter | Click focused button (alternative) |

### Main Functions

 | Shortcut | Action |
 | ---------- | -------- |
 | Ctrl+O | Load Image |
 | Ctrl+L | Start Calibration |
 | Ctrl+T | Auto Trace |
 | Ctrl+S | Save as JSON |
 | Ctrl+E | Save as CSV |

### During Calibration

 | Key | What It Does |
 | ----- | -------------- |
 | Enter | Confirm point or apply calibration |
 | Escape | Cancel calibration mode |
 | Backspace | Remove last calibration point |
 | Delete | Remove last calibration point |

---

## 📍 Tab Order (Navigation Sequence)

Press **Tab** to move through controls in this order:


1. Load Image button

2. Calibrate button

3. Auto Trace button

4. Save JSON button

5. Save CSV button

6. Title field

7. X Label field

8. Y Label field

9. X Min field

10. X Max field

11. Y Min field

12. Y Max field

13. X Log Scale checkbox

14. Y Log Scale checkbox

15. Canvas (image area)

---

## 🔊 What You'll Hear

### When You Start

```

"Graph Digitizer application started. Version 1.2.0"
"Press Tab to navigate through all controls"

```

### When You Load an Image

```

"Load Image" (button name)
[Press Enter]
[File dialog opens - select image]
"Loaded image: filename.png"

```

### During Calibration

```

[Focus canvas and click 4 points]
"Calibration point 1 (Left X value) - X: 125.0000, Y: 87.0000"
"Calibration points: 1 of 4"
[After 4th point]
"4 calibration points complete. Calibration applied."

```

### After Auto-Trace

```

"Auto-trace complete"

```

### When You Save

```

"Saved JSON to: my_data.json"

```

---

## 🎓 Typical Workflow

### Step-by-Step

```


1. Launch application → Hear: "Graph Digitizer started"

2. Press Ctrl+O → File dialog opens

3. Select PNG/JPEG image → Press Enter

4. Hear: "Loaded image: photo.png"

5. Press Ctrl+L → Hear: "Calibration Mode"

6. Tab to Canvas → Press Tab then Space 4 times to place points
   (OR click 4 points on image)

7. After 4 points → Hear: "Calibration applied"

8. Press Ctrl+T → Auto trace runs

9. Hear: "Auto-trace complete"

10. Press Ctrl+S → Save dialog opens

11. Type filename → Press Enter

12. Hear: "Saved JSON to: filename.json"

```

---

## 🔍 Understand the Form Fields

 | Field | What to Enter | Example |
 | ------- | --------------- | --------- |
 | **Title** | Name of plot | "Temperature vs Time" |
 | **X Label** | Horizontal axis label | "Time (minutes)" |
 | **Y Label** | Vertical axis label | "Temperature (°C)" |
 | **X Min** | Lowest X value | "0" or "0.1" |
 | **X Max** | Highest X value | "60" or "100" |
 | **Y Min** | Lowest Y value | "-10" or "20" |
 | **Y Max** | Highest Y value | "100" or "500" |
 | **X Log** | Check to use logarithmic scale for X | Space to toggle |
 | **Y Log** | Check to use logarithmic scale for Y | Space to toggle |

---

## 🆘 Troubleshooting

### "I don't hear anything"


1. Check screen reader is running

2. Try clicking on a button, then Tab

3. Check volume settings

4. Restart screen reader

### "Tab doesn't work"


1. Click in the window first to focus it

2. Try clicking on a button first

3. Restart the application

### "I'm lost in the interface"


1. Press Tab repeatedly to cycle through all controls

2. Shift+Tab to go backwards

3. Listen to what's announced when you reach each control

### "Colors are confusing"


1. All colors are described as text:

   - Blue (#0072B2)

   - Orange (#E69F00)

   - Green (#009E73)

   - Pink (#CC79A7)

   - Yellow (#F0E442)

   - Light Blue (#56B4E9)

2. Your screen reader announces the color name

---

## 💡 Pro Tips


1. **Use Keyboard Shortcuts**: Ctrl+O, Ctrl+S, etc. are faster than tabbing

2. **Listen Carefully**: Screen reader announces everything - field purpose, button action, etc.

3. **Tab is Your Friend**: If you get lost, press Tab to find the next control

4. **Try the Same Workflow Twice**: Once you know the steps, it goes fast

5. **Use Escape**: Escape always cancels calibration mode if you make a mistake

---

## 📞 Getting Help

### Still Having Issues?


1. Open GitHub repository

2. Click "Issues"

3. Click "New Issue"

4. Add label: `accessibility`

5. Describe:

   - What screen reader you use

   - What operating system

   - What action caused the problem

   - What you expected to happen

---

## ✅ What Works Perfectly


- [x] Navigate with Tab key

- [x] Click buttons with Space or Enter

- [x] Use keyboard for all functions

- [x] Hear all status messages

- [x] Tab through checkboxes

- [x] Type in text fields

- [x] Hear button purposes

- [x] Hear field descriptions

- [x] Understand color-coded items

- [x] Cancel modes with Escape

- [x] Get progress updates (e.g., "3 of 4" points)

---

## 🎯 Most Important Keys

**Just remember these three**:


1. **Tab** - Move to next thing

2. **Space/Enter** - Do the action

3. **Escape** - Cancel current mode

Everything else uses these three keys!

---

**Version**: 1.2.0  
**Last Updated**: November 17, 2025  
**Status**: Fully Accessible ✅
