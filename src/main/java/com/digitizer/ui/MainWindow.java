/*
 * Copyright 2025 Michael Ryan Hunsaker, M.Ed., Ph.D.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitizer.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitizer.core.CalibrationState;
import com.digitizer.core.Dataset;
import com.digitizer.core.FileUtils;
import com.digitizer.io.CsvExporter;
import com.digitizer.io.JsonExporter;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Main window for the Graph Digitizer application.
 * <p>
 * Responsible for creating and arranging the high-level UI: menu bar, toolbar,
 * canvas area, dataset control panel and status bar. This class wires user
 * interactions (file open, calibration flow, export) to core functionality
 * and coordinates cross-component communication (e.g., updating the
 * {@link StatusBar}).
 *
 * Note: The availability of the Auto Trace feature is controlled by a runtime
 * feature flag. Use the Actions -> "Enable Auto Trace" menu toggle to enable
 * or disable the Auto Trace button and menu item while the feature is being
 * refined.
 */
public class MainWindow {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    /**
     * The JavaFX primary stage passed from the application. It is used to
     * resolve native dialogs and to host the root scene.
     */
    private final Stage primaryStage;
    /**
     * Shared calibration state. The canvas, exporter and control panels use this
     * object to convert between pixel and data coordinates.
     */
    private final CalibrationState calibration;
    private final List<Dataset> datasets;
    private final int maxDatasets;
    private final String[] defaultColors;

    private CanvasPanel canvasPanel;
    private ControlPanel controlPanel;
    private StatusBar statusBar;
    private UndoManager undoManager;
    // Scroll pane and left scrollbar are fields so we can update metrics and wire zoom/fit
    private javafx.scene.control.ScrollPane scrollPane;
    private javafx.scene.control.ScrollBar leftScroll;
    private Slider zoomSlider;
    private AccessibilityPreferences accessibilityPrefs;
    /** Runtime feature flag controlling Auto Trace availability. */
    private boolean autoTraceEnabled = false;
    /** Toolbar button for Auto Trace (held as a field so it can be toggled). */
    private Button autoTraceBtn;
    /** Menu item for Auto Trace (held as a field so it can be toggled). */
    private MenuItem autoTraceItem;

    /**
     * Constructs a new MainWindow.
     *
     * @param primaryStage   the JavaFX Stage
     * @param calibration    the calibration state
     * @param datasets       the datasets to manage
     * @param maxDatasets    maximum number of datasets allowed
     * @param defaultColors  default color palette
     */
    public MainWindow(
        Stage primaryStage,
        CalibrationState calibration,
        List<Dataset> datasets,
        int maxDatasets,
        String[] defaultColors
    ) {
        this.primaryStage = primaryStage;
        this.calibration = calibration;
        this.datasets = datasets;
        this.maxDatasets = maxDatasets;
        this.defaultColors = defaultColors;
    }

    /**
     * Initializes the main window and displays it.
     * <p>
     * Creates {@link CanvasPanel}, {@link ControlPanel}, and {@link StatusBar} and
     * configures menus, accessibility, keyboard shortcuts and persistent
     * themes. This method must be called from the JavaFX application thread.
     */
    public void initialize() {
        // Initialize accessibility preferences
        accessibilityPrefs = new AccessibilityPreferences();
        // Apply any persisted per-dataset color overrides from preferences
        String[] persistedDatasetColors = accessibilityPrefs.getDatasetColors();
        if (persistedDatasetColors != null && persistedDatasetColors.length > 0) {
            for (int i = 0; i < Math.min(datasets.size(), persistedDatasetColors.length); i++) {
                String hex = persistedDatasetColors[i];
                if (hex != null && !hex.trim().isEmpty()) {
                    datasets.get(i).setHexColor(hex.trim());
                }
            }
        }
        // Apply any persisted per-dataset visibility overrides
        String[] persistedVis = accessibilityPrefs.getDatasetVisibilities();
        if (persistedVis != null && persistedVis.length > 0) {
            for (int i = 0; i < Math.min(datasets.size(), persistedVis.length); i++) {
                String v = persistedVis[i];
                if (v != null && !v.trim().isEmpty()) {
                    try {
                        boolean visible = Boolean.parseBoolean(v.trim());
                        datasets.get(i).setVisible(visible);
                    } catch (Exception ignore) {}
                }
            }
        }

        // Create UI components
        // Create undo manager and wire to canvas/control panels
        this.undoManager = new UndoManager(datasets, accessibilityPrefs);
        canvasPanel = new CanvasPanel(calibration, datasets, this.undoManager);
        this.undoManager.setCanvasPanel(canvasPanel);
        controlPanel = new ControlPanel(calibration, datasets, canvasPanel, accessibilityPrefs, this.undoManager);
        // Increase control panel width to avoid wrapping of dataset row controls
        // Allow the panel to expand with the window by removing a small fixed max
        controlPanel.setPrefWidth(700);
        controlPanel.setMinWidth(700);
        controlPanel.setMaxWidth(Double.MAX_VALUE);
        statusBar = new StatusBar();

        // Detect OS text scaling
        double osScaling = AccessibilityHelper.getOSTextScaling();
        if (osScaling > 1.0) {
            logger.info("Detected OS text scaling: {}% ({}x)", (int) (osScaling * 100), osScaling);
        }

        // Create root layout
        BorderPane root = new BorderPane();

        // Top: Menu bar and Toolbar
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(createMenuBar(), createToolbar());
        root.setTop(topContainer);

        // Center: Canvas wrapped in a ScrollPane so we can show the image at its natural resolution
        this.scrollPane = new javafx.scene.control.ScrollPane(canvasPanel);
        this.scrollPane.setPannable(true);
        this.scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // we'll provide a left-side scrollbar that syncs to vertical scroll
        this.scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        // Left-side scrollbar synchronized with the ScrollPane's vvalue (pixel mode)
        this.leftScroll = new javafx.scene.control.ScrollBar();
        this.leftScroll.setOrientation(javafx.geometry.Orientation.VERTICAL);
        this.leftScroll.setMin(0.0);
        this.leftScroll.setMax(0.0);
        this.leftScroll.setUnitIncrement(20.0);

        // Sync ScrollPane -> leftScroll (convert vvalue 0..1 to pixel offset)
        this.scrollPane.vvalueProperty().addListener((obs, oldV, newV) -> {
            try {
                Platform.runLater(() -> {
                    double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                    double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                    double max = Math.max(0.0, contentHeight - viewportHeight);
                    if (max <= 0) {
                        leftScroll.setValue(0);
                    } else {
                        leftScroll.setValue(newV.doubleValue() * max);
                    }
                });
            } catch (Exception ignore) {}
        });

        // Sync leftScroll -> ScrollPane (convert pixel offset to vvalue 0..1)
        this.leftScroll.valueProperty().addListener((obs, oldV, newV) -> {
            try {
                Platform.runLater(() -> {
                    double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                    double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                    double max = Math.max(0.0, contentHeight - viewportHeight);
                    if (max <= 0) {
                        this.scrollPane.setVvalue(0);
                    } else {
                        this.scrollPane.setVvalue(newV.doubleValue() / max);
                    }
                });
            } catch (Exception ignore) {}
        });

        // When content size or viewport changes, update scrollbar metrics
        this.scrollPane.viewportBoundsProperty().addListener((obs, oldB, newB) -> updateScrollMetrics());
        canvasPanel.boundsInParentProperty().addListener((obs, oldB, newB) -> updateScrollMetrics());

        root.setLeft(this.leftScroll);
        root.setCenter(this.scrollPane);

        // Right: Control panel (use the instantiated ControlPanel so added UI is visible)
        root.setRight(controlPanel);

        // Bottom: Status bar
        root.setBottom(statusBar);

        // Create and set scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Register scene with theme manager
        ThemeManager.setScene(scene);

        // Add keyboard shortcuts for zoom and dataset selection (Ctrl+1..6)
        scene.setOnKeyPressed(event -> {
            // Intercept Ctrl+Z / Ctrl+Y unless the focus is in a text input control
            if (event.isControlDown() && (event.getCode() == KeyCode.Z || event.getCode() == KeyCode.Y)) {
                javafx.scene.Node focus = scene.getFocusOwner();
                if (focus instanceof TextInputControl) {
                    // Let the text control handle undo/redo (do not consume)
                    return;
                }
                if (event.getCode() == KeyCode.Z) {
                    if (this.undoManager != null) this.undoManager.undo();
                    AccessibilityHelper.announceAction("Undo");
                } else if (event.getCode() == KeyCode.Y) {
                    if (this.undoManager != null) this.undoManager.redo();
                    AccessibilityHelper.announceAction("Redo");
                }
                event.consume();
                return;
            }

            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case EQUALS:
                    case PLUS:
                        // Zoom in
                        if (zoomSlider != null) {
                            double newZoom = Math.min(zoomSlider.getMax(), zoomSlider.getValue() + 0.1);
                            zoomSlider.setValue(newZoom);
                            String msg = String.format("Zoomed in to %.0f%%", newZoom * 100);
                            statusBar.setStatus(msg);
                            AccessibilityHelper.announceAction(msg);
                        }
                        event.consume();
                        break;
                    case MINUS:
                        // Zoom out
                        if (zoomSlider != null) {
                            double newZoom = Math.max(zoomSlider.getMin(), zoomSlider.getValue() - 0.1);
                            zoomSlider.setValue(newZoom);
                            String msg = String.format("Zoomed out to %.0f%%", newZoom * 100);
                            statusBar.setStatus(msg);
                            AccessibilityHelper.announceAction(msg);
                        }
                        event.consume();
                        break;
                    case DIGIT0:
                    case NUMPAD0:
                        // Reset zoom to 100%
                        if (zoomSlider != null) {
                            zoomSlider.setValue(1.0);
                            String msg = "Zoom reset to 100%";
                            statusBar.setStatus(msg);
                            AccessibilityHelper.announceAction(msg);
                        }
                        event.consume();
                        break;
                    case DIGIT1:
                    case NUMPAD1:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(0);
                            AccessibilityHelper.announceAction("Selected dataset 1");
                        }
                        event.consume();
                        break;
                    case DIGIT2:
                    case NUMPAD2:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(1);
                            AccessibilityHelper.announceAction("Selected dataset 2");
                        }
                        event.consume();
                        break;
                    case DIGIT3:
                    case NUMPAD3:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(2);
                            AccessibilityHelper.announceAction("Selected dataset 3");
                        }
                        event.consume();
                        break;
                    case DIGIT4:
                    case NUMPAD4:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(3);
                            AccessibilityHelper.announceAction("Selected dataset 4");
                        }
                        event.consume();
                        break;
                    case DIGIT5:
                    case NUMPAD5:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(4);
                            AccessibilityHelper.announceAction("Selected dataset 5");
                        }
                        event.consume();
                        break;
                    case DIGIT6:
                    case NUMPAD6:
                        if (controlPanel != null) {
                            controlPanel.selectDataset(5);
                            AccessibilityHelper.announceAction("Selected dataset 6");
                        }
                        event.consume();
                        break;
                    case RIGHT:
                        // Next dataset
                        if (controlPanel != null) {
                            int cur = controlPanel.getSelectedDatasetIndex();
                            int next = Math.min(cur + 1, Math.max(cur, 5));
                            controlPanel.selectDataset(next);
                            AccessibilityHelper.announceAction("Selected dataset " + (next + 1));
                        }
                        event.consume();
                        break;
                    case LEFT:
                        // Previous dataset
                        if (controlPanel != null) {
                            int cur = controlPanel.getSelectedDatasetIndex();
                            int prev = Math.max(0, cur - 1);
                            controlPanel.selectDataset(prev);
                            AccessibilityHelper.announceAction("Selected dataset " + (prev + 1));
                        }
                        event.consume();
                        break;
                    case V:
                        // Toggle visibility of selected dataset only on Ctrl+Shift+V
                        if (event.isShiftDown() && controlPanel != null) {
                            int idx = controlPanel.getSelectedDatasetIndex();
                            controlPanel.toggleVisibility(idx);
                            event.consume();
                        }
                        break;
                }
            }
        });

        // Ensure the stage is shown maximized so controls and large images are visible
        primaryStage.setTitle("Graph Digitizer");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);

        // Now that scene is set, apply accessibility settings
        applyAccessibilitySettings();
        // Apply saved palette (if any)
        String[] savedPal = accessibilityPrefs.getPaletteColors();
        if (savedPal != null && savedPal.length > 0) {
            applyColorPalette(savedPal, accessibilityPrefs.getPaletteName());
        }

        // Show and maximize (maximize after show for better cross-platform compatibility)
        primaryStage.show();
        primaryStage.setMaximized(true);

        // Show startup help dialog if the user has it enabled in preferences
        try {
            if (this.accessibilityPrefs != null && this.accessibilityPrefs.isShowStartupHelp()) {
                Platform.runLater(() -> showStartupHelpDialog(false));
            }
        } catch (Exception ignore) {}

        logger.info("Main window initialized and shown");
    }

    /**
     * Builds the application's menu bar containing File (Save CSV, Save JSON, About, Exit)
     * and Themes menus. Items are wired with keyboard accelerators and accessibility
     * announcements.
     *
     * @return configured {@link MenuBar}
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        // File menu (Open/Save/Exit/About)
        Menu fileMenu = new Menu("File");

        MenuItem saveCsvItem = new MenuItem("Save CSV");
        saveCsvItem.setOnAction(e -> handleSaveCsv());
        // Match toolbar hint: Ctrl+E for export CSV
        saveCsvItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));

        MenuItem openJsonItem = new MenuItem("Open JSON");
        openJsonItem.setOnAction(e -> handleOpenJson());
        // Use Ctrl+Shift+O for opening project JSON to avoid clashing with Load Image
        openJsonItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem saveJsonItem = new MenuItem("Save JSON");
        saveJsonItem.setOnAction(e -> handleSaveJson());
        // Match toolbar hint: Ctrl+S for saving project JSON
        saveJsonItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        fileMenu.getItems().addAll(openJsonItem, saveCsvItem, saveJsonItem, new SeparatorMenuItem(), aboutItem, exitItem);

        // Themes menu
        Menu themesMenu = new Menu("Themes");
        for (String themeName : ThemeManager.getAvailableThemes()) {
            MenuItem themeItem = new MenuItem(themeName);
            themeItem.setOnAction(e -> {
                ThemeManager.applyTheme(themeName);
                String message = "Theme changed to: " + themeName;
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Theme changed to: {}", themeName);
            });
            themesMenu.getItems().add(themeItem);
        }

        // Accessibility menu (contains color-blind palette choices)
        Menu accessibilityMenu = createAccessibilityMenu();

        // Help menu: build items now but add to menubar at the end so it appears far-right
        Menu helpMenu = new Menu("Help");
        MenuItem usageItem = new MenuItem("Usage Instructions");
        usageItem.setOnAction(e -> showStartupHelpDialog(true));

        MenuItem reportGithub = new MenuItem("Report a Bug - GitHub");
        reportGithub.setOnAction(e -> {
            try {
                openUrl("https://github.com/mrhunsaker/Graph_Digitizer_Java_Implementation/issues");
            } catch (Exception ex) {
                logger.warn("Could not open browser: {}", ex.getMessage());
            }
        });

        MenuItem reportEmail = new MenuItem("Report a Bug - Email");
        reportEmail.setOnAction(e -> {
            try {
                String subject = "Graph Digitizer - Bug Report";
                openEmail("hunsakerconsulting@gmail.com", subject);
            } catch (Exception ex) {
                logger.warn("Could not open email client: {}", ex.getMessage());
            }
        });

        MenuItem featureGithub = new MenuItem("Feature Request - GitHub");
        featureGithub.setOnAction(e ->
            openUrl("https://github.com/mrhunsaker/Graph_Digitizer_Java_Implementation/discussions")
        );

        MenuItem featureEmail = new MenuItem("Feature Request - Email");
        featureEmail.setOnAction(e -> openEmail("hunsakerconsulting@gmail.com", "Graph Digitizer - Feature Request"));

        helpMenu
            .getItems()
            .addAll(
                usageItem,
                new SeparatorMenuItem(),
                reportGithub,
                reportEmail,
                new SeparatorMenuItem(),
                featureGithub,
                featureEmail
            );

        // Actions menu mirrors toolbar buttons and provides accelerators
        Menu actionsMenu = new Menu("Actions");

        MenuItem loadImageItem = new MenuItem("Load Image");
        loadImageItem.setOnAction(e -> handleLoadImage());
        loadImageItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));

        MenuItem calibrateItem = new MenuItem("Calibrate");
        calibrateItem.setOnAction(e -> handleCalibrate());
        calibrateItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));

        // Auto Trace menu item (enabled/disabled by runtime feature flag)
        this.autoTraceItem = new MenuItem("Auto Trace");
        this.autoTraceItem.setOnAction(e -> handleAutoTrace());
        this.autoTraceItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        this.autoTraceItem.setDisable(!autoTraceEnabled);

        // Toggle to enable/disable Auto Trace at runtime
        javafx.scene.control.CheckMenuItem enableAutoTraceToggle = new javafx.scene.control.CheckMenuItem("Enable Auto Trace");
        enableAutoTraceToggle.setSelected(this.autoTraceEnabled);
        enableAutoTraceToggle.setOnAction(ev -> toggleAutoTrace(enableAutoTraceToggle.isSelected()));

        MenuItem fitItem = new MenuItem("Fit to Viewport");
        fitItem.setOnAction(ev -> {
            try {
                Bounds vp = this.scrollPane.getViewportBounds();
                canvasPanel.fitToViewport(vp.getWidth(), vp.getHeight());
                this.zoomSlider.setValue(canvasPanel.getZoom());
                Platform.runLater(() -> {
                    try {
                        double contentWidth = canvasPanel.getBoundsInParent().getWidth();
                        double viewportWidth = this.scrollPane.getViewportBounds().getWidth();
                        double maxW = Math.max(0.0, contentWidth - viewportWidth);
                        if (maxW <= 0) this.scrollPane.setHvalue(0);
                        else this.scrollPane.setHvalue(0.5);
                        double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                        double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                        double maxH = Math.max(0.0, contentHeight - viewportHeight);
                        if (maxH <= 0) this.scrollPane.setVvalue(0);
                        else this.scrollPane.setVvalue(0.5);
                    } catch (Exception ignore) {}
                });
            } catch (Exception ignore) {}
        });
        // No standard accelerator for Fit; allow Ctrl+Shift+F
        fitItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem oneToOneItem = new MenuItem("100% Zoom");
        oneToOneItem.setOnAction(ev -> {
            canvasPanel.setZoom(1.0);
            this.zoomSlider.setValue(1.0);
        });
        oneToOneItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN));

        actionsMenu
            .getItems()
            .addAll(
                loadImageItem,
                calibrateItem,
                enableAutoTraceToggle,
                this.autoTraceItem,
                new SeparatorMenuItem(),
                fitItem,
                oneToOneItem
            );

        menuBar.getMenus().addAll(fileMenu, actionsMenu, themesMenu, accessibilityMenu);

        // Edit menu (Undo/Redo)
        Menu editMenu = new Menu("Edit");
        MenuItem hideAllItem = new MenuItem("Hide All");
        hideAllItem.setOnAction(e -> {
            try {
                if (this.undoManager != null) {
                    UndoManager.CompositeAction comp = new UndoManager.CompositeAction("Hide All");
                    for (Dataset d : this.datasets) comp.add(new UndoManager.ToggleVisibilityAction(d, d.isVisible(), false));
                    this.undoManager.push(comp);
                } else {
                    for (Dataset d : this.datasets) d.setVisible(false);
                    if (this.accessibilityPrefs != null) {
                        String[] visArr = new String[this.datasets.size()];
                        for (int i = 0; i < this.datasets.size(); i++) visArr[i] = String.valueOf(
                            this.datasets.get(i).isVisible()
                        );
                        this.accessibilityPrefs.setDatasetVisibilities(visArr);
                    }
                    if (this.controlPanel != null) this.controlPanel.refreshDatasetInfoDisplay();
                    if (this.canvasPanel != null) this.canvasPanel.redraw();
                }
                AccessibilityHelper.announceAction("All datasets hidden");
            } catch (Exception ignore) {}
        });
        hideAllItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem showAllItem = new MenuItem("Show All");
        showAllItem.setOnAction(e -> {
            try {
                if (this.undoManager != null) {
                    UndoManager.CompositeAction comp = new UndoManager.CompositeAction("Show All");
                    for (Dataset d : this.datasets) comp.add(new UndoManager.ToggleVisibilityAction(d, d.isVisible(), true));
                    this.undoManager.push(comp);
                } else {
                    for (Dataset d : this.datasets) d.setVisible(true);
                    if (this.accessibilityPrefs != null) {
                        String[] visArr = new String[this.datasets.size()];
                        for (int i = 0; i < this.datasets.size(); i++) visArr[i] = String.valueOf(
                            this.datasets.get(i).isVisible()
                        );
                        this.accessibilityPrefs.setDatasetVisibilities(visArr);
                    }
                    if (this.controlPanel != null) this.controlPanel.refreshDatasetInfoDisplay();
                    if (this.canvasPanel != null) this.canvasPanel.redraw();
                }
                AccessibilityHelper.announceAction("All datasets shown");
            } catch (Exception ignore) {}
        });
        showAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setOnAction(e -> {
            try {
                if (this.undoManager != null) this.undoManager.undo();
                AccessibilityHelper.announceAction("Undo");
            } catch (Exception ignore) {}
        });
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setOnAction(e -> {
            try {
                if (this.undoManager != null) this.undoManager.redo();
                AccessibilityHelper.announceAction("Redo");
            } catch (Exception ignore) {}
        });
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().addAll(hideAllItem, showAllItem, new SeparatorMenuItem(), undoItem, redoItem);
        // Initialize Undo/Redo menu state and listen for changes
        try {
            if (this.undoManager != null) {
                // helper to update menu labels and enabled state
                Runnable updateEditMenu = () -> {
                    try {
                        boolean canU = this.undoManager.canUndo();
                        boolean canR = this.undoManager.canRedo();
                        String udesc = this.undoManager.peekUndoDescription();
                        String rdesc = this.undoManager.peekRedoDescription();
                        undoItem.setDisable(!canU);
                        redoItem.setDisable(!canR);
                        undoItem.setText(canU && udesc != null && !udesc.isEmpty() ? "Undo: " + udesc : "Undo");
                        redoItem.setText(canR && rdesc != null && !rdesc.isEmpty() ? "Redo: " + rdesc : "Redo");
                        // Hide/Show availability
                        boolean anyVisible = false;
                        boolean anyHidden = false;
                        for (Dataset d : this.datasets) {
                            if (d.isVisible()) anyVisible = true;
                            else anyHidden = true;
                            if (anyVisible && anyHidden) break;
                        }
                        hideAllItem.setDisable(!anyVisible);
                        showAllItem.setDisable(!anyHidden);
                    } catch (Exception ignore) {}
                };

                // initial update
                Platform.runLater(updateEditMenu);
                this.undoManager.addChangeListener(() -> Platform.runLater(updateEditMenu));
            } else {
                // if no undo manager, compute hide/show enablement from datasets
                Platform.runLater(() -> {
                    boolean anyVisible = false;
                    boolean anyHidden = false;
                    for (Dataset d : this.datasets) {
                        if (d.isVisible()) anyVisible = true;
                        else anyHidden = true;
                        if (anyVisible && anyHidden) break;
                    }
                    hideAllItem.setDisable(!anyVisible);
                    showAllItem.setDisable(!anyHidden);
                });
            }
        } catch (Exception ignore) {}
        menuBar.getMenus().add(editMenu);
        // Add Help last so it appears at the far right of the menu bar
        menuBar.getMenus().add(helpMenu);

        return menuBar;
    }

    /**
     * Creates the Accessibility menu with font size, point size, focus, and contrast options.
     *
     * @return configured Accessibility {@link Menu}
     */
    private Menu createAccessibilityMenu() {
        Menu accessibilityMenu = new Menu("Accessibility");

        // Font Size submenu
        Menu fontSizeMenu = new Menu("Font Size");
        for (AccessibilityPreferences.FontSize size : AccessibilityPreferences.FontSize.values()) {
            MenuItem item = new MenuItem(size.name().replace("_", " "));
            item.setOnAction(e -> {
                accessibilityPrefs.setFontSize(size);
                applyAccessibilitySettings();
                String message = "Font size changed to " + size.name().replace("_", " ");
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info(message);
            });
            fontSizeMenu.getItems().add(item);
        }

        // Point Size submenu
        Menu pointSizeMenu = new Menu("Point Size");
        for (AccessibilityPreferences.PointSize size : AccessibilityPreferences.PointSize.values()) {
            MenuItem item = new MenuItem(size.name().replace("_", " ") + " (" + (int) size.size + "px)");
            item.setOnAction(e -> {
                accessibilityPrefs.setPointSize(size);
                canvasPanel.setPointSize(size.size);
                canvasPanel.redraw();
                String message = "Point size changed to " + size.name().replace("_", " ");
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info(message);
            });
            pointSizeMenu.getItems().add(item);
        }

        // Shape Variation toggle
        MenuItem shapeVariationItem = new MenuItem("Toggle Shape Variation");
        shapeVariationItem.setOnAction(e -> {
            boolean enabled = !accessibilityPrefs.isUseShapeVariation();
            accessibilityPrefs.setUseShapeVariation(enabled);
            canvasPanel.setUseShapeVariation(enabled);
            canvasPanel.redraw();
            String message = "Shape variation " + (enabled ? "enabled" : "disabled");
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            logger.info(message);
        });

        // High Contrast Mode toggle
        MenuItem highContrastItem = new MenuItem("Toggle High Contrast Mode");
        highContrastItem.setOnAction(e -> {
            boolean enabled = !accessibilityPrefs.isHighContrastMode();
            accessibilityPrefs.setHighContrastMode(enabled);
            if (enabled) {
                ThemeManager.applyTheme("High Contrast Black");
            }
            String message = "High contrast mode " + (enabled ? "enabled" : "disabled");
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            logger.info(message);
        });

        // Focus Border Width submenu
        Menu focusBorderMenu = new Menu("Focus Border Width");
        for (int width = 2; width <= 5; width++) {
            final int borderWidth = width;
            MenuItem item = new MenuItem(width + "px");
            item.setOnAction(e -> {
                accessibilityPrefs.setFocusBorderWidth(borderWidth);
                applyAccessibilitySettings();
                String message = "Focus border width changed to " + borderWidth + "px";
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info(message);
            });
            focusBorderMenu.getItems().add(item);
        }

        accessibilityMenu
            .getItems()
            .addAll(
                fontSizeMenu,
                pointSizeMenu,
                new SeparatorMenuItem(),
                shapeVariationItem,
                highContrastItem,
                new SeparatorMenuItem(),
                focusBorderMenu
            );

        // Color-blind friendly palettes submenu (radio items with persistent selection)
        Menu palettesSub = new Menu("Color-blind Palettes");
        javafx.scene.control.ToggleGroup paletteToggle = new javafx.scene.control.ToggleGroup();

        javafx.scene.control.RadioMenuItem okabe = new javafx.scene.control.RadioMenuItem("Okabe-Ito (friendly)");
        okabe.setToggleGroup(paletteToggle);
        String[] okabePal = new String[] { "#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#CC79A7" };
        okabe.setOnAction(e -> {
            accessibilityPrefs.setPalette("Okabe-Ito", okabePal);
            applyColorPalette(okabePal, "Okabe-Ito");
        });

        javafx.scene.control.RadioMenuItem brewer = new javafx.scene.control.RadioMenuItem("ColorBrewer (colorblind)");
        brewer.setToggleGroup(paletteToggle);
        String[] brewerPal = new String[] { "#377eb8", "#ff7f00", "#4daf4a", "#f781bf", "#a65628", "#984ea3" };
        brewer.setOnAction(e -> {
            accessibilityPrefs.setPalette("ColorBrewer", brewerPal);
            applyColorPalette(brewerPal, "ColorBrewer");
        });

        javafx.scene.control.RadioMenuItem resetPal = new javafx.scene.control.RadioMenuItem("Default Palette");
        resetPal.setToggleGroup(paletteToggle);
        resetPal.setOnAction(e -> {
            accessibilityPrefs.setPalette("", null);
            applyColorPalette(this.defaultColors, "Default");
        });

        palettesSub.getItems().addAll(okabe, brewer, new SeparatorMenuItem(), resetPal);
        accessibilityMenu.getItems().add(new SeparatorMenuItem());
        accessibilityMenu.getItems().add(palettesSub);

        // Initialize selection based on persisted preference
        String saved = accessibilityPrefs.getPaletteName();
        if (saved != null && !saved.isEmpty()) {
            if (saved.equals("Okabe-Ito")) {
                okabe.setSelected(true);
            } else if (saved.equals("ColorBrewer")) {
                brewer.setSelected(true);
            }
        } else {
            resetPal.setSelected(true);
        }

        return accessibilityMenu;
    }

    /**
     * Applies current accessibility preferences to the UI.
     */
    private void applyAccessibilitySettings() {
        if (canvasPanel == null) return;

        // Apply font sizes
        AccessibilityPreferences.FontSize fontSize = accessibilityPrefs.getFontSize();
        String fontStyle = String.format("-fx-font-size: %.0fpx;", fontSize.body);
        controlPanel.setStyle(fontStyle);
        statusBar.setStyle(fontStyle);

        // Apply focus border width
        double borderWidth = accessibilityPrefs.getFocusBorderWidth();
        String focusStyle = String.format(
            "-fx-focus-color: #0096FF; -fx-faint-focus-color: #0096FF22; " + "-fx-focus-border-width: %.1fpx;",
            borderWidth
        );
        primaryStage.getScene().getRoot().setStyle(focusStyle);

        // Apply point size and shape variation
        canvasPanel.setPointSize(accessibilityPrefs.getPointSize().size);
        canvasPanel.setUseShapeVariation(accessibilityPrefs.isUseShapeVariation());
        canvasPanel.redraw();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("About Graph Digitizer");
        alert.setHeaderText("Graph Digitizer");
        String content =
            "Version " +
            GraphDigitizerApp.APP_VERSION +
            "\n\n" +
            "Author: Michael Ryan Hunsaker, M.Ed., Ph.D.\n" +
            "Licensed under the Apache 2.0 License.";
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Constructs the top toolbar (Load Image, Calibrate, Auto Trace, Save JSON, Save CSV, Zoom controls)
     * with accessibility metadata and keyboard-friendly layout.
     *
     * @return toolbar container
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        // Load Image button
        Button loadImageBtn = new Button("Load Image");
        AccessibilityHelper.setButtonAccessibility(
            loadImageBtn,
            "Load Image",
            "Load a PNG or JPEG image for digitization",
            "Ctrl+O"
        );
        loadImageBtn.setOnAction(e -> handleLoadImage());

        // Calibrate button
        Button calibrateBtn = new Button("Calibrate");
        AccessibilityHelper.setButtonAccessibility(
            calibrateBtn,
            "Calibrate",
            "Enter calibration mode to set axis reference points by clicking 4 locations",
            "Ctrl+L"
        );
        calibrateBtn.setOnAction(e -> handleCalibrate());

        // Auto Trace button (enabled/disabled by runtime feature flag)
        this.autoTraceBtn = new Button("Auto Trace");
        AccessibilityHelper.setButtonAccessibility(
            this.autoTraceBtn,
            "Auto Trace",
            "Automatically detect and trace the data curve using color matching",
            "Ctrl+T"
        );
        this.autoTraceBtn.setOnAction(e -> handleAutoTrace());
        this.autoTraceBtn.setDisable(!autoTraceEnabled);

        // Save JSON button
        Button saveJsonBtn = new Button("Save JSON");
        AccessibilityHelper.setButtonAccessibility(
            saveJsonBtn,
            "Save JSON",
            "Export all data and calibration to a JSON file for later editing",
            "Ctrl+S"
        );
        saveJsonBtn.setOnAction(e -> handleSaveJson());

        // Save CSV button
        Button saveCsvBtn = new Button("Save CSV");
        AccessibilityHelper.setButtonAccessibility(
            saveCsvBtn,
            "Save CSV",
            "Export data points to a CSV file for use in spreadsheets and other tools",
            "Ctrl+E"
        );
        saveCsvBtn.setOnAction(e -> handleSaveCsv());

        Button clearDataBtn = new Button("Clear Data");
        AccessibilityHelper.setButtonAccessibility(
            clearDataBtn,
            "Clear Data",
            "Clear all datasets, calibration and X-axis snap values (start fresh)",
            "Ctrl+K"
        );
        clearDataBtn.setOnAction(e -> {
            // Confirm with the user before clearing data to avoid accidental loss
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION
            );
            confirm.initOwner(primaryStage);
            confirm.setTitle("Confirm Clear Data");
            confirm.setHeaderText("Clear all data and reset datasets?");
            confirm.setContentText(
                "This will remove all data points, calibration, snap X values, and reset dataset names/colors. This action cannot be undone."
            );
            confirm
                .showAndWait()
                .ifPresent(bt -> {
                    if (bt == javafx.scene.control.ButtonType.OK) {
                        clearAllData();
                    } else {
                        AccessibilityHelper.announceAction("Clear data cancelled");
                    }
                });
        });

        toolbar.getChildren().addAll(loadImageBtn, calibrateBtn, this.autoTraceBtn, saveJsonBtn, saveCsvBtn, clearDataBtn);

        // --- Zoom controls ---
        Label zoomLabel = new Label("Zoom:");
        Button fitBtn = new Button("Fit");
        Button oneBtn = new Button("100%");
        this.zoomSlider = new Slider(0.25, 3.0, 1.0);
        this.zoomSlider.setPrefWidth(160);
        this.zoomSlider.valueProperty().addListener((obs, oldV, newV) -> {
            try {
                canvasPanel.setZoom(newV.doubleValue());
            } catch (Exception ignore) {}
        });
        fitBtn.setOnAction(e -> {
            Bounds vp = this.scrollPane.getViewportBounds();
            canvasPanel.fitToViewport(vp.getWidth(), vp.getHeight());
            this.zoomSlider.setValue(canvasPanel.getZoom());
            // After canvasPanel resizes to the new zoom, center horizontally
            Platform.runLater(() -> {
                try {
                    double contentWidth = canvasPanel.getBoundsInParent().getWidth();
                    double viewportWidth = this.scrollPane.getViewportBounds().getWidth();
                    double maxW = Math.max(0.0, contentWidth - viewportWidth);
                    if (maxW <= 0) this.scrollPane.setHvalue(0);
                    else this.scrollPane.setHvalue(0.5); // center horizontally
                    // Center vertically as well
                    double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                    double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                    double maxH = Math.max(0.0, contentHeight - viewportHeight);
                    if (maxH <= 0) this.scrollPane.setVvalue(0);
                    else this.scrollPane.setVvalue(0.5); // center vertically
                } catch (Exception ignore) {}
            });
        });
        oneBtn.setOnAction(e -> {
            canvasPanel.setZoom(1.0);
            this.zoomSlider.setValue(1.0);
        });

        HBox zoomBox = new HBox(6, zoomLabel, fitBtn, oneBtn, this.zoomSlider);
        zoomBox.setPadding(new Insets(0, 0, 0, 20));
        toolbar.getChildren().add(zoomBox);

        return toolbar;
    }

    /**
     * Creates the right control panel with accessible form controls.
     * All input fields are labeled for screen readers and have helpful tooltips.
     *
     * @return a VBox containing accessible controls
     */
    // createRightPanel removed — ControlPanel instance is used directly

    /**
     * Handles the "Load Image" action. Prompts the user for a file and then
     * delegates to the {@link CanvasPanel#loadImage(java.io.File)} method.
     * Provides user feedback through {@link StatusBar} and {@link AccessibilityHelper}.
     */
    private void handleLoadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                canvasPanel.loadImage(file);
                // When a new image is loaded, clear any previous data and calibration
                clearAllData();
                String message = "Loaded image: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Loaded image: {}", file.getAbsolutePath());
                // Center horizontally after load so the image appears centered
                Platform.runLater(() -> {
                    try {
                        double contentWidth = canvasPanel.getBoundsInParent().getWidth();
                        double viewportWidth = this.scrollPane.getViewportBounds().getWidth();
                        double maxW = Math.max(0.0, contentWidth - viewportWidth);
                        if (maxW <= 0) this.scrollPane.setHvalue(0);
                        else this.scrollPane.setHvalue(0.5);
                        // Also center vertically after load
                        double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                        double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                        double maxH = Math.max(0.0, contentHeight - viewportHeight);
                        if (maxH <= 0) this.scrollPane.setVvalue(0);
                        else this.scrollPane.setVvalue(0.5);
                    } catch (Exception ignore) {}
                });
            } catch (Exception e) {
                String message = "Error loading image: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error loading image", e);
            }
        }
    }

    /**
     * Clears all datasets (points), resets calibration and snap X values,
     * and clears plot metadata (title and axis labels). Leaves dataset
     * names and colors intact so the user can continue reusing the series.
     */
    private void clearAllData() {
        try {
            // Clear points from each dataset and reset visibility/axis assignment
            for (int i = 0; i < this.datasets.size(); i++) {
                Dataset d = this.datasets.get(i);
                d.clearPoints();
                d.setVisible(true);
                d.setUseSecondaryYAxis(false);
                // Reset name to default (Dataset 1..N)
                try {
                    d.setName("Dataset " + (i + 1));
                } catch (Exception ignore) {}
                // Reset color to default palette if available
                if (this.defaultColors != null && this.defaultColors.length > 0) {
                    String hex = this.defaultColors[i % this.defaultColors.length];
                    try {
                        d.setHexColor(hex);
                    } catch (Exception ignore) {}
                }
            }

            // Reset calibration anchors and numeric ranges to defaults
            if (this.calibration != null) {
                this.calibration.reset();
                this.calibration.setDataXMin(0.0);
                this.calibration.setDataXMax(1.0);
                this.calibration.setDataYMin(0.0);
                this.calibration.setDataYMax(1.0);
                this.calibration.setDataY2Min(null);
                this.calibration.setDataY2Max(null);
                this.calibration.setY2Log(null);
                this.calibration.setXLog(false);
                this.calibration.setYLog(false);
            }

            // Clear snap X values
            if (this.canvasPanel != null && this.canvasPanel.getSnapper() != null) {
                this.canvasPanel.getSnapper().clearSnapXValues();
                this.canvasPanel.setSnapLinesVisible(false);
            }

            // Clear titles/labels in control panel
            if (this.controlPanel != null) {
                this.controlPanel.setTitle("");
                this.controlPanel.setXLabel("");
                this.controlPanel.setYLabel("");
                this.controlPanel.refreshDatasetInfoDisplay();
            }

            // Persist reset dataset colors and visibilities to accessibility prefs
            if (this.accessibilityPrefs != null) {
                String[] hexes = new String[this.datasets.size()];
                String[] visArr = new String[this.datasets.size()];
                for (int i = 0; i < this.datasets.size(); i++) {
                    hexes[i] = this.datasets.get(i).getHexColor();
                    visArr[i] = String.valueOf(this.datasets.get(i).isVisible());
                }
                this.accessibilityPrefs.setDatasetColors(hexes);
                this.accessibilityPrefs.setDatasetVisibilities(visArr);
            }

            // Clear undo history
            if (this.undoManager != null) this.undoManager.clear();

            // Redraw canvas to reflect cleared state
            if (this.canvasPanel != null) this.canvasPanel.redraw();

            String msg = "Cleared all datasets, calibration and X-axis items";
            if (this.statusBar != null) this.statusBar.setStatus(msg);
            AccessibilityHelper.announceAction(msg);
            logger.info(msg);
        } catch (Exception e) {
            logger.error("Error clearing data", e);
        }
    }

    /**
     * Puts the UI into calibration mode and tells the canvas to accept the
     * four calibration anchors. The user is guided with accessible hints.
     */
    private void handleCalibrate() {
        String message = "Calibration mode: Click 4 points (X-left, X-right, Y-bottom, Y-top)";
        statusBar.setStatus(message);
        AccessibilityHelper.announceModeChange(
            "Calibration Mode",
            "Click in the image canvas to mark 4 calibration points: " +
                "1. Left X value, 2. Right X value, 3. Bottom Y value, 4. Top Y value"
        );
        canvasPanel.enterCalibrationMode();
        logger.info("Entered calibration mode");
    }

    /**
     * Runs automatic curve extraction by delegating to the canvas. Requires
     * a valid calibration to be in place; otherwise an error is announced.
     */
    private void handleAutoTrace() {
        if (!calibration.isCalibrated()) {
            String message = "Error: Image must be calibrated before auto-trace";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction("Error - Image calibration required");
            return;
        }

        try {
            canvasPanel.performAutoTrace();
            String message = "Auto-trace complete";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            logger.info("Auto-trace performed");
        } catch (Exception e) {
            String message = "Error: " + e.getMessage();
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction("Error during auto-trace - " + e.getMessage());
            logger.error("Error during auto-trace", e);
        }
    }

    /**
     * Presents a file chooser and serializes the current project to JSON
     * using {@link JsonExporter}. The method validates required fields like
     * the project title and reports status and accessibility announcements.
     */
    private void handleSaveJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project as JSON");
        // Require a title and use it to build the default filename
        String title = controlPanel.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String message = "Please enter a title before saving";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            try {
                Alert alert = new Alert(AlertType.WARNING);
                alert.initOwner(primaryStage);
                alert.setTitle("Missing Title");
                alert.setHeaderText("A project title is required");
                alert.setContentText("Please enter a title before saving the project.");
                alert.showAndWait();
                // After the dialog is dismissed, move focus to the Title field so the user can type immediately
                try {
                    if (controlPanel != null) controlPanel.focusTitleField();
                } catch (Exception ignore) {}
            } catch (Exception ignore) {}
            return;
        }
        String safe = FileUtils.sanitizeFilename(title.trim());
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fileChooser.setInitialFileName(safe + "_" + ts + ".json");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                JsonExporter.exportToJson(
                    file.getAbsolutePath(),
                    title.trim(),
                    controlPanel.getXLabel(),
                    controlPanel.getYLabel(),
                    controlPanel.getY2Label(),
                    calibration,
                    datasets
                );
                String message = "Saved JSON to: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Saved JSON to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error saving JSON: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error saving JSON", e);
            }
        }
    }

    /**
     * Presents a file chooser and writes the points of all datasets to a CSV
     * file using {@link CsvExporter}. The CSV is saved in "wide" format where
     * the first column is X and remaining columns correspond to datasets.
     */
    private void handleSaveCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project as CSV");
        // Require a title and use it to build the default filename
        String title = controlPanel.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String message = "Please enter a title before saving";
            statusBar.setStatus(message);
            AccessibilityHelper.announceAction(message);
            try {
                Alert alert = new Alert(AlertType.WARNING);
                alert.initOwner(primaryStage);
                alert.setTitle("Missing Title");
                alert.setHeaderText("A project title is required");
                alert.setContentText("Please enter a title before saving the project.");
                alert.showAndWait();
                try {
                    if (controlPanel != null) controlPanel.focusTitleField();
                } catch (Exception ignore) {}
            } catch (Exception ignore) {}
            return;
        }
        String safe = FileUtils.sanitizeFilename(title.trim());
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fileChooser.setInitialFileName(safe + "_" + ts + ".csv");
        fileChooser
            .getExtensionFilters()
            .addAll(new FileChooser.ExtensionFilter("CSV Files", "*.csv"), new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                CsvExporter.exportToCsv(file.getAbsolutePath(), datasets);
                String message = "Saved CSV to: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Saved CSV to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error saving CSV: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error saving CSV", e);
            }
        }
    }

    /**
     * Presents a file chooser to open a previously saved project JSON and
     * import its datasets, labels and calibration state.
     */
    private void handleOpenJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project JSON");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                com.digitizer.io.ProjectJson pj = JsonExporter.importFromJson(file.getAbsolutePath());
                java.util.List<Dataset> loaded = JsonExporter.convertJsonToDatasets(pj, calibration);
                // Update existing datasets list in-place so references remain valid
                this.datasets.clear();
                this.datasets.addAll(loaded);
                if (controlPanel != null) {
                    controlPanel.setTitle(pj.title);
                    controlPanel.setXLabel(pj.xlabel);
                    controlPanel.setYLabel(pj.ylabel);
                    controlPanel.setY2Label(pj.y2label);
                    controlPanel.refreshDatasetInfoDisplay();
                }
                if (canvasPanel != null) canvasPanel.redraw();
                String message = "Loaded JSON project: " + file.getName();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction(message);
                logger.info("Loaded JSON project from: {}", file.getAbsolutePath());
            } catch (Exception e) {
                String message = "Error loading JSON: " + e.getMessage();
                statusBar.setStatus(message);
                AccessibilityHelper.announceAction("Error - " + message);
                logger.error("Error loading JSON", e);
            }
        }
    }

    /**
     * Gets the status bar for displaying messages.
     *
     * @return the StatusBar component
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Gets the canvas panel.
     *
     * @return the CanvasPanel component
     */
    public CanvasPanel getCanvasPanel() {
        return canvasPanel;
    }

    /**
     * Recompute left scrollbar metrics based on content and viewport sizes.
     */
    private void updateScrollMetrics() {
        Platform.runLater(() -> {
            try {
                double contentHeight = canvasPanel.getBoundsInParent().getHeight();
                double viewportHeight = this.scrollPane.getViewportBounds().getHeight();
                double max = Math.max(0.0, contentHeight - viewportHeight);
                this.leftScroll.setMax(max);
                this.leftScroll.setVisibleAmount(viewportHeight);
                this.leftScroll.setBlockIncrement(Math.max(1.0, viewportHeight));
                this.leftScroll.setUnitIncrement(Math.max(1.0, viewportHeight / 20.0));
                double v = this.scrollPane.getVvalue();
                if (max <= 0) this.leftScroll.setValue(0);
                else this.leftScroll.setValue(v * max);
            } catch (Exception ignore) {}
        });
    }

    /**
     * Toggle Auto Trace availability at runtime. Enables or disables the toolbar
     * button and the corresponding menu item to prevent users from invoking
     * Auto Trace while the feature is disabled.
     *
     * @param enabled true to enable Auto Trace, false to disable
     */
    private void toggleAutoTrace(boolean enabled) {
        this.autoTraceEnabled = enabled;
        try {
            if (this.autoTraceBtn != null) this.autoTraceBtn.setDisable(!enabled);
            if (this.autoTraceItem != null) this.autoTraceItem.setDisable(!enabled);
            String msg = "Auto Trace " + (enabled ? "enabled" : "disabled");
            if (this.statusBar != null) this.statusBar.setStatus(msg);
            AccessibilityHelper.announceAction(msg);
            logger.info("Auto Trace toggled: {}", enabled);
        } catch (Exception ignore) {}
    }

    /**
     * Applies a color palette to the current datasets, updating their hex colors
     * and refreshing UI elements.
     *
     * @param palette array of hex color strings
     * @param name friendly name of the palette
     */
    private void applyColorPalette(String[] palette, String name) {
        if (palette == null || palette.length == 0) return;
        for (int i = 0; i < datasets.size(); i++) {
            String hex = palette[i % palette.length];
            datasets.get(i).setHexColor(hex);
        }
        if (canvasPanel != null) canvasPanel.redraw();
        if (controlPanel != null) controlPanel.refreshDatasetInfoDisplay();
        String msg = "Color palette applied: " + name;
        if (statusBar != null) statusBar.setStatus(msg);
        AccessibilityHelper.announceAction(
            msg + ". Use the Color Picker in the control panel to adjust individual series colors. Changes are remembered."
        );
        // Persist per-dataset colors so app-level overrides survive restarts
        if (accessibilityPrefs != null) {
            String[] hexes = new String[this.datasets.size()];
            for (int i = 0; i < this.datasets.size(); i++) hexes[i] = this.datasets.get(i).getHexColor();
            accessibilityPrefs.setDatasetColors(hexes);
        }
        logger.info("Applied color palette: {}", name);
    }

    /**
     * Show the startup/help dialog. If invokedFromMenu is true, the dialog
     * was requested via the Help menu; otherwise it may be shown automatically
     * at application startup. The checkbox controls the persisted preference
     * to show the dialog on startup.
     */
    private void showStartupHelpDialog(boolean invokedFromMenu) {
        try {
            String instructions =
                "Supported image formats: PNG, JPG, JPEG\n\n" +
                "1) Load an image:\n" +
                "   - Menu: File -> Load Image (or press Ctrl+O)\n" +
                "   - Choose a supported PNG or JPEG file from disk.\n\n" +
                "2) Calibrate the image:\n" +
                "   - Click 'Calibrate' (Ctrl+L).\n" +
                "   - Click exactly four points in this order: \n" +
                "       a) Left X reference (enter its numeric X value)\n" +
                "       b) Right X reference (enter its numeric X value)\n" +
                "       c) Bottom Y reference (enter its numeric Y value)\n" +
                "       d) Top Y reference (enter its numeric Y value)\n\n" +
                "3) (Optional) Set Snap X values:\n" +
                "   - In the Control panel enter comma-separated values (e.g. 1.0,2.5,3.5)\n" +
                "     or a range using start:step:end (e.g. 0:0.5:5). Click Apply.\n\n" +
                "4) Digitize points:\n" +
                "   - Select a dataset on the right and click points on the canvas to add them.\n" +
                "   - Use Auto Trace (if enabled) after calibration for automated extraction.\n\n" +
                "5) Save results:\n" +
                "   - Save JSON (Ctrl+S) to keep calibration and datasets.\n" +
                "   - Save CSV (Ctrl+E) to export numeric point data for spreadsheets.\n\n" +
                "6) Start over if needed:\n" +
                "   - Use Clear Data (toolbar or Ctrl+K) to reset datasets, calibration and snaps.\n\n" +
                "Tip: If you do not want to see this dialog at startup, check 'Do not show this message again'.";

            javafx.scene.control.TextArea ta = new javafx.scene.control.TextArea(instructions);
            ta.setWrapText(true);
            ta.setEditable(false);
            ta.setPrefRowCount(14);
            ta.setPrefColumnCount(60);

            CheckBox dontShow = new CheckBox("Do not show this message again");
            // Checkbox selected means 'do not show', so reflect current preference
            dontShow.setSelected(!accessibilityPrefs.isShowStartupHelp());

            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, ta, dontShow);
            box.setPadding(new Insets(10));

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Getting Started - Graph Digitizer");
            alert.setHeaderText("Quick Start: Exact order to use this app");
            // Use dialog pane content to include checkbox and wrapped text
            alert.getDialogPane().setContent(box);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            alert.showAndWait();

            // Persist the inverted checkbox value into the preference
            boolean doNotShow = dontShow.isSelected();
            accessibilityPrefs.setShowStartupHelp(!doNotShow);
        } catch (Exception e) {
            logger.warn("Failed to show startup help dialog: {}", e.getMessage());
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback: attempt runtime exec for common platforms
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", url });
            }
        } catch (Exception e) {
            logger.warn("Unable to open URL {}: {}", url, e.getMessage());
        }
    }

    private void openEmail(String toAddress, String subject) {
        try {
            String encSub = URLEncoder.encode(subject, "UTF-8");
            String mailto = String.format("mailto:%s?subject=%s", toAddress, encSub);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(new URI(mailto));
            } else {
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", mailto });
            }
        } catch (UnsupportedEncodingException uee) {
            logger.warn("Encoding not supported for email subject: {}", uee.getMessage());
        } catch (Exception e) {
            logger.warn("Unable to open mail client: {}", e.getMessage());
        }
    }
}
