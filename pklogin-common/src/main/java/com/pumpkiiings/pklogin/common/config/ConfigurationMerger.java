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

package com.pumpkiiings.pklogin.common.config;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies keys that exist in the bundled defaults but not in the server's file.
 *
 * <p>This deliberately replaces boosted-yaml's {@code YamlDocument#update()}.
 * That method reshapes the document to match the defaults, which means it
 * <em>deletes</em> any key the server owner added themselves — a custom section,
 * a note to self, an option carried over from another fork. Upgrading the plugin
 * should never cost someone their configuration, so the merge here only ever
 * adds.</p>
 *
 * <p>Three rules, in order of priority:</p>
 * <ol>
 *   <li>A key the file already has keeps its value, whatever the default says.</li>
 *   <li>A key only the defaults have is added.</li>
 *   <li>A key only the file has is left alone.</li>
 * </ol>
 */
public final class ConfigurationMerger {

    private ConfigurationMerger() {}

    /** What a merge did, for logging and for tests to assert on. */
    public static final class Result {

        private final List<String> addedRoutes;

        private Result(List<String> addedRoutes) {
            this.addedRoutes = addedRoutes;
        }

        /** Routes that were absent from the file and have now been added. */
        public List<String> getAddedRoutes() {
            return addedRoutes;
        }

        public int getAddedCount() {
            return addedRoutes.size();
        }

        public boolean changedAnything() {
            return !addedRoutes.isEmpty();
        }
    }

    /**
     * Adds every default key the target is missing.
     *
     * <p>Both documents must be standalone — loaded without the other attached as
     * a fallback. Otherwise {@code contains} would report defaults as present and
     * nothing would ever be added.</p>
     *
     * @param target   the server's document, modified in place
     * @param defaults the bundled document to take missing keys from
     * @return which routes were added
     */
    public static Result addMissing(YamlDocument target, YamlDocument defaults) {
        List<String> added = new ArrayList<>();

        for (String route : defaults.getRoutesAsStrings(true)) {
            // Sections come into existence on their own when a leaf below them is
            // set, and copying one wholesale would drag in its children even where
            // the file has deliberately removed some.
            if (defaults.isSection(route)) {
                continue;
            }
            if (target.contains(route)) {
                continue;
            }
            target.set(route, defaults.get(route));
            added.add(route);
        }

        return new Result(added);
    }
}
