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

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accessibility utility helper class for configuring JavaFX controls with
 * screen reader support, keyboard shortcuts, and ARIA-like descriptions.
 *
 * This class provides methods to:
 * - Set accessibility labels for controls
 * - Add keyboard shortcuts and tooltips
 * - Manage focus order
 * - Log accessibility announcements for screen readers
 */
public class AccessibilityHelper {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityHelper.class);

    /**
     * Sets an accessible label and tooltip for a button.
     * Logs the action for screen reader announcement.
     *
     * @param button      the button to configure
     * @param text        the button text
     * @param tooltip     the tooltip text describing the button action
     * @param keyboardHint optional keyboard shortcut hint (e.g., "Ctrl+O")
     */
    public static void setButtonAccessibility(Button button, String text, String tooltip, String keyboardHint) {
        button.setText(text);
        
        String fullTooltip = tooltip;
        if (keyboardHint != null && !keyboardHint.isEmpty()) {
            fullTooltip = tooltip + " (" + keyboardHint + ")";
        }
        
        button.setTooltip(new Tooltip(fullTooltip));
        button.setAccessibleText(text);
        button.setAccessibleRoleDescription("Button: " + text);
        button.setAccessibleHelp("Click to " + tooltip.toLowerCase());
        
        logger.debug("Configured accessible button: {}", text);
    }

    /**
     * Sets an accessible label and tooltip for a text field.
     * Provides context for screen reader users.
     *
     * @param field       the text field to configure
     * @param label       the label text
     * @param placeholder placeholder text shown in empty field
     * @param help        help text describing what the field accepts
     */
    public static void setTextFieldAccessibility(TextField field, String label, String placeholder, String help) {
        field.setPromptText(placeholder);
        field.setAccessibleText(label);
        field.setAccessibleRoleDescription("Text Input: " + label);
        field.setAccessibleHelp(help);
        field.setTooltip(new Tooltip(label + " - " + help));
        
        logger.debug("Configured accessible text field: {}", label);
    }

    /**
     * Sets an accessible label and tooltip for a checkbox.
     * Makes the checkbox's state clearly announceable.
     *
     * @param checkBox the checkbox to configure
     * @param label    the label text
     * @param help     help text describing what toggling does
     */
    public static void setCheckBoxAccessibility(CheckBox checkBox, String label, String help) {
        checkBox.setText(label);
        checkBox.setAccessibleText(label);
        checkBox.setAccessibleRoleDescription("Checkbox: " + label);
        checkBox.setAccessibleHelp(help);
        checkBox.setTooltip(new Tooltip(label + " - " + help));
        
        logger.debug("Configured accessible checkbox: {}", label);
    }

    /**
     * Sets an accessible label for a Label control.
     * Used for section headers and descriptive text.
     *
     * @param label the Label to configure
     * @param text  the label text
     * @param role  the semantic role (e.g., "Section heading", "Description")
     */
    public static void setLabelAccessibility(Label label, String text, String role) {
        label.setText(text);
        label.setAccessibleText(text);
        label.setAccessibleRoleDescription(role + ": " + text);
        
        logger.debug("Configured accessible label: {}", text);
    }

    /**
     * Requests focus on a control and announces it to screen readers.
     * Used when programmatically shifting focus (e.g., after file load).
     *
     * @param control    the control to focus
     * @param announcement optional announcement text for screen readers
     */
    public static void requestFocusAccessible(Control control, String announcement) {
        control.requestFocus();
        
        if (announcement != null && !announcement.isEmpty()) {
            logger.info("ACCESSIBILITY ANNOUNCEMENT: {}", announcement);
        }
    }

    /**
     * Announces a status change to screen readers via logging.
     * Screen reader applications can monitor logs or use system announcements.
     *
     * @param message the message to announce
     */
    public static void announceStatus(String message) {
        logger.info("ACCESSIBILITY ANNOUNCEMENT: {}", message);
    }

    /**
     * Announces a dialog action (button pressed, validation error, etc.) to screen readers.
     *
     * @param action the action that occurred (e.g., "File loaded", "Error: Invalid input")
     */
    public static void announceAction(String action) {
        logger.info("ACTION ANNOUNCED: {}", action);
    }

    /**
     * Announces help text for the currently focused control.
     *
     * @param controlName the name of the control
     * @param helpText the help text to announce
     */
    public static void announceHelp(String controlName, String helpText) {
        logger.info("HELP TEXT: {} - {}", controlName, helpText);
    }

    /**
     * Marks a control as requiring input (validation indicator for screen readers).
     *
     * @param control the control
     * @param required whether the field is required
     */
    public static void setRequired(Control control, boolean required) {
        if (required) {
            String help = control.getAccessibleHelp();
            if (help == null || help.isEmpty()) {
                help = "Required field";
            } else {
                help = help + " (Required)";
            }
            control.setAccessibleHelp(help);
        }
    }

    /**
     * Configures keyboard shortcut accessibility info for a control.
     * Helps screen reader users discover keyboard alternatives.
     *
     * @param control the control
     * @param shortcut the keyboard shortcut (e.g., "Ctrl+S for Save")
     */
    public static void setKeyboardShortcut(Control control, String shortcut) {
        String help = control.getAccessibleHelp();
        if (help == null || help.isEmpty()) {
            help = "Keyboard: " + shortcut;
        } else {
            help = help + " | Keyboard: " + shortcut;
        }
        control.setAccessibleHelp(help);
    }

    /**
     * Creates a focused announcement for mode changes (e.g., entering calibration mode).
     * This helps screen reader users understand the current context.
     *
     * @param mode the mode name (e.g., "Calibration Mode")
     * @param instructions the instructions for this mode
     */
    public static void announceModeChange(String mode, String instructions) {
        String message = "Entered " + mode + ". " + instructions;
        logger.info("MODE CHANGED: {}", message);
    }

    /**
     * Announces numerical feedback to screen readers (e.g., point counts, calibration progress).
     *
     * @param label what is being counted (e.g., "Calibration points")
     * @param current the current count
     * @param total the total expected
     */
    public static void announceProgress(String label, int current, int total) {
        String message = label + ": " + current + " of " + total;
        logger.info("PROGRESS: {}", message);
    }

    /**
     * Announces coordinate or value information to screen readers.
     * Used for providing feedback on canvas interactions.
     *
     * @param description what the values represent
     * @param xValue the x value
     * @param yValue the y value
     */
    public static void announceCoordinates(String description, double xValue, double yValue) {
        String message = description + " - X: " + String.format("%.4f", xValue) + 
                         ", Y: " + String.format("%.4f", yValue);
        logger.info("COORDINATES: {}", message);
    }

    /**
     * Announces color information in accessible format.
     * Screen reader users can't distinguish colors, so describe them.
     *
     * @param datasetName the name of the dataset
     * @param colorHex the hex color code (e.g., "#0072B2")
     * @param colorName accessible description (e.g., "Blue")
     */
    public static void announceColor(String datasetName, String colorHex, String colorName) {
        String message = "Dataset: " + datasetName + " uses color " + colorName + " (" + colorHex + ")";
        logger.info("COLOR: {}", message);
    }
}
