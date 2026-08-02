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
import java.util.function.Function;

/**
 * The document being migrated, plus the operations a migration is allowed to
 * perform on it.
 *
 * <p>Migrations only need to describe what <em>changed shape</em> between two
 * versions — a key that moved, a value whose meaning was redefined. Keys that
 * were merely added are handled afterwards by {@link ConfigurationMerger}, so a
 * migration that only adds things can be empty.</p>
 *
 * <p>Every operation here is deliberately conservative: nothing overwrites a
 * value the server owner set unless the migration explicitly asks for it.</p>
 */
public final class MigrationContext {

    private final YamlDocument document;
    private final List<String> changes = new ArrayList<>();

    MigrationContext(YamlDocument document) {
        this.document = document;
    }

    /** The raw document, for anything the helpers below do not cover. */
    public YamlDocument getDocument() {
        return document;
    }

    /** Human-readable log of what this migration actually did. */
    public List<String> getChanges() {
        return changes;
    }

    public boolean has(String route) {
        return document.contains(route);
    }

    public Object get(String route) {
        return document.get(route);
    }

    /**
     * Moves a value to a new route, keeping whatever the server owner had set.
     *
     * <p>Does nothing if the old route is absent. If the new route already holds
     * a value, the old one is dropped rather than clobbering it — the newer
     * location is assumed to be the one the owner edited more recently.</p>
     *
     * @return true if anything moved
     */
    public boolean rename(String from, String to) {
        if (!document.contains(from)) {
            return false;
        }

        if (document.contains(to)) {
            document.remove(from);
            changes.add("dropped '" + from + "' (superseded by '" + to + "')");
            return true;
        }

        Object value = document.get(from);
        document.set(to, value);
        document.remove(from);
        changes.add("moved '" + from + "' to '" + to + "'");
        return true;
    }

    /**
     * Sets a value only when the route is absent.
     *
     * <p>Prefer letting {@link ConfigurationMerger} add new keys from the bundled
     * defaults; use this when a migration needs a value the defaults cannot
     * express, such as one derived from an existing setting.</p>
     *
     * @return true if the value was written
     */
    public boolean setIfAbsent(String route, Object value) {
        if (document.contains(route)) {
            return false;
        }
        document.set(route, value);
        changes.add("added '" + route + "'");
        return true;
    }

    /**
     * Removes a route.
     *
     * <p>The only way anything is ever deleted. Reserve it for keys that would
     * actively misbehave if left in place; an option that simply stopped being
     * read is harmless and should stay, so the owner can see what happened.</p>
     *
     * @return true if the route existed
     */
    public boolean remove(String route) {
        if (!document.contains(route)) {
            return false;
        }
        document.remove(route);
        changes.add("removed '" + route + "'");
        return true;
    }

    /**
     * Rewrites an existing value in place, for when a setting keeps its route but
     * changes units or accepted values.
     *
     * @param route     the route to rewrite; ignored if absent
     * @param rewriter  receives the current value, returns the replacement
     * @return true if the value changed
     */
    public boolean transform(String route, Function<Object, Object> rewriter) {
        if (!document.contains(route)) {
            return false;
        }
        Object before = document.get(route);
        Object after = rewriter.apply(before);
        if (java.util.Objects.equals(before, after)) {
            return false;
        }
        document.set(route, after);
        changes.add("rewrote '" + route + "' (" + before + " -> " + after + ")");
        return true;
    }
}
