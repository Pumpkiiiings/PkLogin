package com.pumpkiiings.pklogin.common.config.migrations.config;

import com.pumpkiiings.pklogin.common.config.ConfigurationMigration;
import dev.dejvokep.boostedyaml.YamlDocument;

public class ConfigMigration_1_to_1_1 implements ConfigurationMigration {

    @Override
    public String fromVersion() {
        return "1";
    }

    @Override
    public String toVersion() {
        return "1.1";
    }

    @Override
    public void migrate(YamlDocument config) {
        // Remove the old file-version key if it exists
        if (config.contains("file-version")) {
            config.remove("file-version");
        }
        
        // As a demonstration of explicit migration:
        // If we want to rename or add specific missing keys intentionally
        // But BoostedYAML's updater handles missing keys automatically!
    }
}
