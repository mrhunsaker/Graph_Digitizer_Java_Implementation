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

package com.digitizer.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for file operations including filename sanitization and
 * default save location discovery.
 * <p>
 * This class provides helper methods used by the UI export logic to build
 * safe filenames for saving and to determine the preferred location for
 * exported files. Use {@link #sanitizeFilename(String)} when creating
 * user-generated filenames to avoid platform-specific issues.
 */
public final class FileUtils {

    private FileUtils() {
        // Utility class, should not be instantiated
    }

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /**
     * Gets the user's Downloads directory if it exists, otherwise returns the system temp directory.
     *
     * @return the preferred downloads directory path
     */
    public static String getPreferredDownloadsDir() {
        try {
            String userHome = System.getProperty("user.home");
            Path downloadsPath = Paths.get(userHome, "Downloads");
            if (Files.exists(downloadsPath) && Files.isDirectory(downloadsPath)) {
                return downloadsPath.toString();
            }
        } catch (Exception e) {
            // Fall through to use temp directory
        }

        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Sanitizes a string to be safe for use as a filename.
     * Replaces non-alphanumeric characters (except . - _) with underscores,
     * collapses multiple underscores, and trims leading/trailing underscores/dots.
     *
     * @param input the input string
     * @return the sanitized filename
     */
    public static String sanitizeFilename(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String sanitized = input.trim();
        
        // Replace disallowed characters with underscore
        sanitized = sanitized.replaceAll("[^A-Za-z0-9_.-]", "_");
        
        // Collapse consecutive underscores
        sanitized = sanitized.replaceAll("_+", "_");
        
        // Remove underscores immediately before dots
        sanitized = sanitized.replaceAll("_+\\.", ".");
        
        // Collapse consecutive dots
        sanitized = sanitized.replaceAll("\\.+", ".");
        
        // Trim leading/trailing underscores or dots
        sanitized = sanitized.replaceAll("^[_.]+|[_.]+$", "");

        return sanitized.isEmpty() ? "" : sanitized;
    }

    /**
     * Generates a default filename for saving using the title or a timestamp.
     *
     * @param title     the title text (may be empty or null)
     * @param extension the file extension without a leading dot (e.g., "json")
     * @return the full path to the default filename
     */
    public static String getDefaultFilename(String title, String extension) {
        String baseName = sanitizeFilename(title);
        if (baseName.isEmpty()) {
            baseName = "graphdigitizer_export_" + LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        }

        String dir = getPreferredDownloadsDir();
        return new File(dir, baseName + "." + extension).getAbsolutePath();
    }

    /**
     * Ensures a filename has the specified extension.
     * If it doesn't already end with the extension, it will be appended.
     *
     * @param filename  the filename
     * @param extension the desired extension without a leading dot
     * @return the filename with the ensured extension
     */
    public static String ensureExtension(String filename, String extension) {
        String dotExt = "." + extension.toLowerCase();
        if (!filename.toLowerCase().endsWith(dotExt)) {
            filename += dotExt;
        }
        return filename;
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param filename the filename
     * @return the extension without the leading dot, or empty string if no extension
     */
    public static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Convenience method that returns the current system timestamp formatted
     * for use as a filename suffix. This can be used when generating
     * default filenames for exports.
     *
     * @return formatted timestamp like "2025-11-18_143020"
     */
    public static String getTimestampForFilename() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
}
