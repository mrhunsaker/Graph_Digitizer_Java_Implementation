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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for FileUtils class.
 */
public class FileUtilsTest {

    @Test
    public void testSanitizeFilename() {
        String result = FileUtils.sanitizeFilename("My Graph Title");
        assertEquals("My_Graph_Title", result);
    }

    @Test
    public void testSanitizeFilenameEmpty() {
        String result = FileUtils.sanitizeFilename("");
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultFilename() {
        String filename = FileUtils.getDefaultFilename("test", "json");
        assertTrue(filename.endsWith(".json"));
    }

    @Test
    public void testEnsureExtension() {
        String result = FileUtils.ensureExtension("myfile", "json");
        assertTrue(result.endsWith(".json"));
    }

    @Test
    public void testGetExtension() {
        String ext = FileUtils.getExtension("myfile.json");
        assertEquals("json", ext);
    }
}
