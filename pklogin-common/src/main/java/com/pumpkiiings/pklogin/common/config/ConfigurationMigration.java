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

/**
 * One step of a configuration upgrade, from version N to version N+1.
 *
 * <p>A migration describes only what <em>moved or changed meaning</em>. Keys that
 * were simply added between two versions do not need one: the manager merges the
 * bundled defaults after the chain has run, so listing new keys here would just
 * duplicate the shipped file.</p>
 *
 * <p>To add a version, write a class for the step that reaches it, register it,
 * and bump the target version. The manager walks the chain one step at a time, so
 * a server that skipped several releases runs 1&rarr;2, 2&rarr;3, 3&rarr;4 in order.</p>
 *
 * <pre>{@code
 * public final class ConfigMigration3To4 implements ConfigurationMigration {
 *     public int fromVersion() { return 3; }
 *
 *     public void migrate(MigrationContext context) {
 *         context.rename("limbo.blindness-effect", "limbo.effects.blindness");
 *     }
 * }
 * }</pre>
 */
public interface ConfigurationMigration {

    /** The version this step upgrades from. */
    int fromVersion();

    /** The version this step produces. One step at a time unless overridden. */
    default int toVersion() {
        return fromVersion() + 1;
    }

    /**
     * Applies the structural changes for this step.
     *
     * <p>Throwing aborts the whole upgrade and the file is restored, so failing
     * loudly is safer than leaving the document half-converted.</p>
     */
    void migrate(MigrationContext context);

    /** Shown in the startup log; override when the default is not descriptive. */
    default String description() {
        return getClass().getSimpleName();
    }
}
