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

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * Status bar for displaying messages and application status.
 * Provides accessible status announcements via a live region for screen readers.
 */
public class StatusBar extends BorderPane {

    private final Label statusLabel;

    /**
     * Constructs a new StatusBar with accessibility support.
     */
    public StatusBar() {
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(5, 10, 5, 10));
        statusLabel.setTextFill(Color.BLACK);
        
        // Accessibility setup - make status bar a live region for screen readers
        statusLabel.setAccessibleText("Status bar");
        statusLabel.setAccessibleRoleDescription("Live region with status updates");
        statusLabel.setAccessibleHelp("This area announces status messages and application updates");

        this.setLeft(statusLabel);
        this.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0; -fx-padding: 5;");
    }

    /**
     * Sets the status message and announces it for screen readers.
     * This is a live region that updates continuously.
     *
     * @param message the message to display and announce
     */
    public void setStatus(String message) {
        statusLabel.setText(message);
        // Announce to accessibility tools
        AccessibilityHelper.announceStatus(message);
    }

    /**
     * Gets the current status message.
     *
     * @return the status message
     */
    public String getStatus() {
        return statusLabel.getText();
    }
}
