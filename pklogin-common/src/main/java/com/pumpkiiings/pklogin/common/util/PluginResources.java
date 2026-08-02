/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.common.util;

import java.io.InputStream;

/**
 * Single source of truth for the bundled resource paths.
 *
 * <p>These must match the directory layout under
 * {@code pklogin-common/src/main/resources}. Keeping them here means a rename
 * of the resource folder breaks in exactly one place instead of silently
 * returning {@code null} from every {@code getResourceAsStream} call site.</p>
 */
public final class PluginResources {

    /** Root of the bundled configuration resources, with a trailing slash. */
    public static final String BASE = "com/pumpkiiings/pklogin/config/";

    public static final String CONFIG = BASE + "config.yml";
    public static final String LANG_DIR = BASE + "lang/";
    public static final String TWO_FACTOR_DIR = BASE + "2fa/";

    public static final String DEFAULT_LANGUAGE_FILE = "messages_en.yml";

    private PluginResources() {}

    /**
     * Opens a bundled resource.
     *
     * @param path the absolute resource path (no leading slash)
     * @return the stream, or {@code null} if the resource is missing
     */
    public static InputStream open(String path) {
        return PluginResources.class.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Opens a bundled language file, falling back to English when the requested
     * one is not shipped with the plugin.
     */
    public static InputStream openLanguage(String fileName) {
        InputStream stream = open(LANG_DIR + fileName);
        return stream != null ? stream : open(LANG_DIR + DEFAULT_LANGUAGE_FILE);
    }

    /** Opens the bundled English language file, used as the blueprint for updates. */
    public static InputStream openDefaultLanguage() {
        return open(LANG_DIR + DEFAULT_LANGUAGE_FILE);
    }

    /** Opens the bundled default {@code config.yml}. */
    public static InputStream openConfig() {
        return open(CONFIG);
    }

    /** Opens a bundled file from the {@code 2fa} folder. */
    public static InputStream openTwoFactor(String fileName) {
        return open(TWO_FACTOR_DIR + fileName);
    }
}
