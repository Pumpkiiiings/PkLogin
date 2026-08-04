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
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads a YAML file, brings it up to the current schema version, and never loses
 * anything the server owner wrote.
 *
 * <p>Each managed file carries its own version counter under its own key, so
 * {@code config.yml} and the message files evolve independently:</p>
 *
 * <pre>
 * config.yml      version-config: 3
 * messages_xx.yml version-messages: 2
 * </pre>
 *
 * <p>On load the file's version is compared against the target. Anything older
 * runs the registered migrations in sequence — a server three releases behind
 * runs 1&rarr;2, 2&rarr;3, 3&rarr;4 — and only once the whole chain succeeds is the
 * version stamp advanced and the file written. If any step throws, the backup
 * taken beforehand is restored and the original file is loaded unchanged, so a
 * broken upgrade degrades to "nothing happened" rather than a corrupted file.</p>
 *
 * <p>After the chain, {@link ConfigurationMerger} adds keys the new version
 * introduced. That step is additive only: existing values win over defaults and
 * keys the defaults do not know about are left in place.</p>
 */
public class ConfigurationVersionManager {

    /** Version key for {@code config.yml}. */
    public static final String VERSION_KEY_CONFIG = "version-config";
    /** Version key for the message files. */
    public static final String VERSION_KEY_MESSAGES = "version-messages";
    /** Version key for the proxy's {@code backend.yml}. */
    public static final String VERSION_KEY_BACKEND = "version-backend";

    /** Assumed when a file predates versioning entirely. */
    private static final int UNVERSIONED = 1;

    private final MigrationLogger logger;
    private final File file;
    private final byte[] defaults;
    private final String versionKey;
    private final int targetVersion;

    private final Map<Integer, ConfigurationMigration> migrations = new HashMap<>();
    private final List<String> legacyVersionKeys = new ArrayList<>();
    private Function<String, Integer> legacyVersionMapper;

    /**
     * @param file          the on-disk file to load, create and migrate
     * @param defaultStream the bundled defaults; buffered so it can be replayed
     * @param versionKey    which version counter this file carries
     * @param targetVersion the version the current plugin build expects
     * @throws IllegalArgumentException if the bundled defaults are missing, which
     *                                  means the packaged resource path is wrong
     */
    public ConfigurationVersionManager(File file, InputStream defaultStream,
                                       String versionKey, int targetVersion) {
        this(file, defaultStream, versionKey, targetVersion, MigrationLogger.console());
    }

    public ConfigurationVersionManager(File file, InputStream defaultStream,
                                       String versionKey, int targetVersion, MigrationLogger logger) {
        this.logger = logger;
        this.file = file;
        this.versionKey = versionKey;
        this.targetVersion = targetVersion;

        if (defaultStream == null) {
            throw new IllegalArgumentException(
                    "Missing bundled defaults for '" + file.getName() + "'. "
                            + "This is a packaging error: check the resource path in PluginResources.");
        }
        this.defaults = readFully(defaultStream);
    }

    /** Registers one step of the upgrade chain. */
    public ConfigurationVersionManager register(ConfigurationMigration migration) {
        if (migration.toVersion() <= migration.fromVersion()) {
            throw new IllegalArgumentException("Migration " + migration.description()
                    + " does not move forwards (" + migration.fromVersion()
                    + " -> " + migration.toVersion() + ")");
        }
        ConfigurationMigration existing = migrations.put(migration.fromVersion(), migration);
        if (existing != null) {
            throw new IllegalArgumentException("Two migrations start at version "
                    + migration.fromVersion() + ": " + existing.description()
                    + " and " + migration.description());
        }
        return this;
    }

    /**
     * Teaches the manager how to read version stamps written by older builds.
     *
     * <p>PkLogin used to store a single {@code version: "1.3"} string in both
     * files. Those installations have to keep upgrading cleanly, so the old key
     * is still read and translated into the new counter.</p>
     *
     * @param key    the legacy key to look for
     * @param mapper turns the legacy value into a version number
     */
    public ConfigurationVersionManager withLegacyVersion(String key, Function<String, Integer> mapper) {
        this.legacyVersionKeys.add(key);
        this.legacyVersionMapper = mapper;
        return this;
    }

    /**
     * Loads the file, creating and upgrading it as needed.
     *
     * @return the loaded document, or null if the file could not be read at all
     */
    public YamlDocument load() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (!file.exists()) {
                writeDefaults();
                logger.info("Created " + file.getName() + " at version " + targetVersion + ".");
                return openWithDefaults();
            }

            // Standalone: with the defaults attached, contains() would report every
            // default key as already present and the merge would add nothing.
            YamlDocument document;
            try {
                document = openStandalone(file);
            } catch (Exception malformed) {
                return recoverUnparseableFile(malformed);
            }
            YamlDocument bundled = openBundledDefaults();

            int currentVersion = detectVersion(document);

            if (currentVersion > targetVersion) {
                logger.warn(file.getName() + " reports version " + currentVersion
                        + " but this build expects " + targetVersion
                        + ". It was probably written by a newer PkLogin; leaving it untouched.");
                return openWithDefaults();
            }

            List<ConfigurationMigration> chain = resolveChain(currentVersion);
            if (chain == null) {
                return openWithDefaults();
            }

            boolean needsMigration = !chain.isEmpty();
            File backup = needsMigration ? backupFile() : null;

            if (needsMigration) {
                logger.info("Upgrading " + file.getName() + " from version "
                        + currentVersion + " to " + targetVersion + ".");
                if (!runChain(document, chain, backup)) {
                    return openWithDefaults();
                }
            }

            ConfigurationMerger.Result merge = ConfigurationMerger.addMissing(document, bundled);
            if (merge.changedAnything()) {
                logger.info("Added " + merge.getAddedCount() + " new key(s) to " + file.getName() + ".");
                for (String route : merge.getAddedRoutes()) {
                    logger.info("  + " + route);
                }
            }

            boolean versionChanged = currentVersion != targetVersion;
            if (versionChanged || merge.changedAnything()) {
                // Stamped last: until this point the file on disk is still the old
                // one, so an interrupted run simply retries next start.
                stampVersion(document);
                document.save();
                if (versionChanged) {
                    logger.info(file.getName() + " is now at version " + targetVersion + ".");
                }
            }

            return openWithDefaults();
        } catch (IOException e) {
            logger.error("Failed to load " + file.getName(), e);
            return null;
        }
    }

    /**
     * Last resort for a file YAML cannot parse — a bad hand edit, a truncated
     * write, a broken encoding.
     *
     * <p>Nothing can be preserved from a document that will not load, so the
     * original is kept beside the new one and the administrator is told exactly
     * where to find it rather than having it silently overwritten.</p>
     */
    private YamlDocument recoverUnparseableFile(Exception cause) throws IOException {
        File backup = backupFile();
        logger.error(file.getName() + " is not valid YAML and could not be read.", cause);
        if (backup != null) {
            logger.warn("Your original file was kept as " + backup.getName()
                    + ". Fresh defaults have been written; copy your settings back over once fixed.");
        }
        writeDefaults();
        return openWithDefaults();
    }

    /**
     * Builds the ordered list of steps from {@code currentVersion} to the target.
     *
     * @return the chain, or null if it cannot be completed (caller should abort)
     */
    private List<ConfigurationMigration> resolveChain(int currentVersion) {
        List<ConfigurationMigration> chain = new ArrayList<>();
        int version = currentVersion;

        while (version < targetVersion) {
            ConfigurationMigration step = migrations.get(version);
            if (step == null) {
                logger.warn("No migration registered from version " + version + " for "
                        + file.getName() + ", so it cannot be upgraded to " + targetVersion
                        + ". Leaving the file untouched — please report this.");
                return null;
            }
            chain.add(step);
            version = step.toVersion();
        }

        if (version != targetVersion) {
            logger.warn("Migration chain for " + file.getName() + " overshoots the target ("
                    + version + " instead of " + targetVersion + "). Leaving the file untouched.");
            return null;
        }
        return chain;
    }

    /** @return true if every step applied cleanly */
    private boolean runChain(YamlDocument document, List<ConfigurationMigration> chain, File backup) {
        for (ConfigurationMigration step : chain) {
            try {
                MigrationContext context = new MigrationContext(document);
                step.migrate(context);

                logger.info("  applied " + step.description()
                        + " (" + step.fromVersion() + " -> " + step.toVersion() + ")");
                for (String change : context.getChanges()) {
                    logger.info("    " + change);
                }
            } catch (Exception e) {
                logger.error("Migration " + step.description() + " failed for "
                        + file.getName() + "; restoring the file as it was.", e);
                restore(backup);
                return false;
            }
        }
        return true;
    }

    private void stampVersion(YamlDocument document) {
        document.set(versionKey, targetVersion);
        // The single "version" string these files used to carry would otherwise
        // sit next to the new counter and confuse whoever reads the file.
        for (String legacyKey : legacyVersionKeys) {
            document.remove(legacyKey);
        }
    }

    /**
     * Reads the file's version, falling back through the legacy key.
     *
     * <p>A file with no recognisable stamp is treated as the oldest version, so it
     * goes through the full chain rather than being assumed current.</p>
     */
    private int detectVersion(YamlDocument document) {
        if (document.contains(versionKey)) {
            Object raw = document.get(versionKey);
            if (raw instanceof Number) {
                return ((Number) raw).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(raw).trim());
            } catch (NumberFormatException ignored) {
                logger.warn("Unreadable " + versionKey + " in " + file.getName()
                        + " ('" + raw + "'); treating it as version " + UNVERSIONED + ".");
                return UNVERSIONED;
            }
        }

        if (legacyVersionMapper != null) {
            for (String legacyKey : legacyVersionKeys) {
                if (document.contains(legacyKey)) {
                    Integer mapped = legacyVersionMapper.apply(String.valueOf(document.get(legacyKey)));
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }

        return UNVERSIONED;
    }

    private void writeDefaults() throws IOException {
        Files.write(file.toPath(), defaults);
    }

    private File backupFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backup = new File(file.getParentFile(), file.getName() + ".bak-" + timestamp);
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Backed up " + file.getName() + " to " + backup.getName() + ".");
            return backup;
        } catch (IOException e) {
            logger.error("Could not back up " + file.getName(), e);
            return null;
        }
    }

    private void restore(File backup) {
        if (backup == null || !backup.exists()) {
            // Nothing was written yet, so the file on disk is still the original.
            return;
        }
        try {
            Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Restored " + file.getName() + " from " + backup.getName() + ".");
        } catch (IOException e) {
            logger.error("Could not restore " + file.getName()
                    + " — the backup is at " + backup.getAbsolutePath(), e);
        }
    }

    /** The file on its own, with no defaults to fall back on. */
    private YamlDocument openStandalone(File target) throws IOException {
        return YamlDocument.create(target, GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(false).build(),
                DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
    }

    private YamlDocument openBundledDefaults() throws IOException {
        return YamlDocument.create(new ByteArrayInputStream(defaults), GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(false).build(),
                DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
    }

    /**
     * The file with the bundled defaults attached, which is what the plugin reads
     * at runtime: should a key still be missing, lookups fall back to the shipped
     * value instead of returning null.
     */
    private YamlDocument openWithDefaults() throws IOException {
        return YamlDocument.create(file, new ByteArrayInputStream(defaults),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(false).build(),
                DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
    }

    private static byte[] readFully(InputStream stream) {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled defaults", e);
        }
    }
}
