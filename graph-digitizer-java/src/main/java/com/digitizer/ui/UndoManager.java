package com.digitizer.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.digitizer.core.Dataset;
import com.digitizer.core.Point;

/**
 * Simple undo/redo manager for dataset visibility and point edits.
 */
public class UndoManager {

    public interface UndoableAction {
        void undo();
        void redo();
        String getDescription();
    }

    private final Deque<UndoableAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoableAction> redoStack = new ArrayDeque<>();
    private final List<Dataset> datasets;
    private final AccessibilityPreferences prefs;
    private CanvasPanel canvasPanel; // set after construction
    private final java.util.List<Runnable> changeListeners = new java.util.ArrayList<>();

    public UndoManager(List<Dataset> datasets, AccessibilityPreferences prefs) {
        this.datasets = datasets;
        this.prefs = prefs;
    }

    public void setCanvasPanel(CanvasPanel cp) {
        this.canvasPanel = cp;
    }

    public void push(UndoableAction action) {
        if (action == null) return;
        action.redo();
        undoStack.push(action);
        redoStack.clear();
        persistVisibilities();
        if (canvasPanel != null) canvasPanel.redraw();
        notifyChangeListeners();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /**
     * Returns a short description of the next undoable action, or empty string.
     */
    public String peekUndoDescription() {
        if (undoStack.isEmpty()) return "";
        try { return undoStack.peek().getDescription(); } catch (Exception e) { return ""; }
    }

    /**
     * Returns a short description of the next redoable action, or empty string.
     */
    public String peekRedoDescription() {
        if (redoStack.isEmpty()) return "";
        try { return redoStack.peek().getDescription(); } catch (Exception e) { return ""; }
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        UndoableAction a = undoStack.pop();
        a.undo();
        redoStack.push(a);
        persistVisibilities();
        if (canvasPanel != null) canvasPanel.redraw();
        notifyChangeListeners();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        UndoableAction a = redoStack.pop();
        a.redo();
        undoStack.push(a);
        persistVisibilities();
        if (canvasPanel != null) canvasPanel.redraw();
        notifyChangeListeners();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyChangeListeners();
    }

    public void addChangeListener(Runnable r) {
        if (r == null) return;
        this.changeListeners.add(r);
    }

    private void notifyChangeListeners() {
        for (Runnable r : this.changeListeners) {
            try { r.run(); } catch (Exception ignore) {}
        }
    }

    private void persistVisibilities() {
        if (prefs == null || datasets == null) return;
        String[] vis = new String[datasets.size()];
        for (int i = 0; i < datasets.size(); i++) vis[i] = String.valueOf(datasets.get(i).isVisible());
        prefs.setDatasetVisibilities(vis);
    }

    // Concrete actions
    public static class ToggleVisibilityAction implements UndoableAction {
        private final Dataset dataset;
        private final boolean before;
        private final boolean after;

        public ToggleVisibilityAction(Dataset dataset, boolean before, boolean after) {
            this.dataset = dataset;
            this.before = before;
            this.after = after;
        }

        @Override
        public void undo() { dataset.setVisible(before); }

        @Override
        public void redo() { dataset.setVisible(after); }

        @Override
        public String getDescription() { return "Toggle visibility"; }
    }

    public static class AddPointAction implements UndoableAction {
        private final Dataset dataset;
        private final Point point;

        public AddPointAction(Dataset dataset, Point point) {
            this.dataset = dataset; this.point = point;
        }

        @Override
        public void undo() { int idx = dataset.getPoints().lastIndexOf(point); if (idx >= 0) dataset.removePoint(idx); }

        @Override
        public void redo() { dataset.getPoints().add(point); }

        @Override
        public String getDescription() { return "Add point"; }
    }

    public static class RemovePointAction implements UndoableAction {
        private final Dataset dataset;
        private final Point point;
        private final int index;

        public RemovePointAction(Dataset dataset, Point point, int index) {
            this.dataset = dataset; this.point = point; this.index = index;
        }

        @Override
        public void undo() { dataset.getPoints().add(index, point); }

        @Override
        public void redo() { dataset.removePoint(index); }

        @Override
        public String getDescription() { return "Remove point"; }
    }

    public static class MovePointAction implements UndoableAction {
        private final Dataset dataset;
        private final int index;
        private final Point beforePoint;
        private final Point afterPoint;

        public MovePointAction(Dataset dataset, int index, Point beforePoint, Point afterPoint) {
            this.dataset = dataset; this.index = index; this.beforePoint = beforePoint; this.afterPoint = afterPoint;
        }

        @Override
        public void undo() { dataset.getPoints().set(index, beforePoint); }

        @Override
        public void redo() { dataset.getPoints().set(index, afterPoint); }

        @Override
        public String getDescription() { return "Move point"; }
    }

    public static class CompositeAction implements UndoableAction {
        private final List<UndoableAction> actions = new ArrayList<>();
        private final String desc;

        public CompositeAction(String desc) { this.desc = desc; }

        public void add(UndoableAction a) { actions.add(a); }

        @Override
        public void undo() {
            // undo in reverse order
            for (int i = actions.size() - 1; i >= 0; i--) actions.get(i).undo();
        }

        @Override
        public void redo() {
            for (UndoableAction a : actions) a.redo();
        }

        @Override
        public String getDescription() { return desc; }
    }
}
