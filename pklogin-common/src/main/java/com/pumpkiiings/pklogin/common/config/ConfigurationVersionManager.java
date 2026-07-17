package com.pumpkiiings.pklogin.common.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ConfigurationVersionManager {

    private final Logger logger;
    private final File configFile;
    private final InputStream defaultResource;
    private final List<ConfigurationMigration> migrations;
    
    private YamlDocument document;

    public ConfigurationVersionManager(File configFile, InputStream defaultResource) {
        this.logger = Logger.getLogger("PkLogin-Config");
        this.configFile = configFile;
        this.defaultResource = defaultResource;
        this.migrations = new ArrayList<>();
    }

    public void registerMigration(ConfigurationMigration migration) {
        this.migrations.add(migration);
    }

    public YamlDocument loadAndMigrate(String defaultVersionStr) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            // If the file doesn't exist, just load default from resource
            if (!configFile.exists()) {
                this.document = YamlDocument.create(configFile, defaultResource,
                        GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(false).build(),
                        DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
                return this.document;
            }

            // Load existing file
            this.document = YamlDocument.create(configFile, defaultResource,
                    GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(false).build(),
                    DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);

            String currentVersion = document.getString("version");
            if (currentVersion == null && document.contains("file-version")) {
                currentVersion = document.getString("file-version");
            }
            if (currentVersion == null) {
                currentVersion = "1.0";
            }

            // Sort or just find paths
            // For a simple path finding, we just look for a chain of migrations
            List<ConfigurationMigration> migrationsToRun = new ArrayList<>();
            String processingVersion = currentVersion;

            while (!processingVersion.equals(defaultVersionStr)) {
                ConfigurationMigration nextMigration = findMigration(processingVersion);
                if (nextMigration == null) {
                    break; // No more migrations, or we reached the end
                }
                migrationsToRun.add(nextMigration);
                processingVersion = nextMigration.toVersion();
            }

            if (!migrationsToRun.isEmpty()) {
                logger.info("Outdated configuration found for " + configFile.getName() + " (" + currentVersion + "). Migrating to " + processingVersion + "...");
                
                // Backup before migrating
                backupFile();

                boolean success = true;
                for (ConfigurationMigration migration : migrationsToRun) {
                    try {
                        migration.migrate(document);
                        logger.info("- Successfully applied migration: " + migration.fromVersion() + " -> " + migration.toVersion());
                    } catch (Exception e) {
                        logger.severe("- Failed to apply migration " + migration.fromVersion() + " -> " + migration.toVersion() + " for " + configFile.getName());
                        e.printStackTrace();
                        success = false;
                        break;
                    }
                }

                if (success) {
                    // Also use BoostedYAML's built-in updater to add any missing default keys just in case
                    document.update();
                    document.set("version", processingVersion);
                    document.save();
                    logger.info("Successfully migrated " + configFile.getName() + " to version " + processingVersion + ".");
                } else {
                    logger.severe("Migration aborted for " + configFile.getName() + ". Please check the errors above.");
                    // Reload original without saving to avoid corruption
                    this.document = YamlDocument.create(configFile, defaultResource,
                            GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(false).build(),
                            DumperSettings.DEFAULT, UpdaterSettings.DEFAULT);
                }
            } else if (!currentVersion.equals(defaultVersionStr)) {
                // If there are no explicit migrations, we still want to add missing keys automatically using BoostedYAML
                if (document.update()) {
                    document.set("version", defaultVersionStr);
                    document.save();
                    logger.info("Automatically added missing keys to " + configFile.getName() + " and updated version to " + defaultVersionStr + ".");
                }
            }

            return this.document;
        } catch (IOException e) {
            logger.severe("Failed to load or migrate configuration: " + configFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    private ConfigurationMigration findMigration(String fromVersion) {
        for (ConfigurationMigration migration : migrations) {
            if (migration.fromVersion().equals(fromVersion)) {
                return migration;
            }
        }
        return null;
    }

    private void backupFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backup = new File(configFile.getParentFile(), configFile.getName() + ".bak-" + timestamp);
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created backup for " + configFile.getName() + " at " + backup.getName());
        } catch (IOException e) {
            logger.warning("Failed to create backup for " + configFile.getName());
            e.printStackTrace();
        }
    }
}
