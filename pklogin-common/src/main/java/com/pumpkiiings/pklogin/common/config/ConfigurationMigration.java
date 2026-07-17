package com.pumpkiiings.pklogin.common.config;

import dev.dejvokep.boostedyaml.YamlDocument;

public interface ConfigurationMigration {

    /**
     * @return The version from which this migration starts.
     */
    String fromVersion();

    /**
     * @return The version to which this migration upgrades.
     */
    String toVersion();

    /**
     * Performs the actual migration on the provided configuration document.
     * Use YamlDocument's API to add, remove, rename, or modify keys.
     * 
     * @param config The YamlDocument representing the current configuration.
     */
    void migrate(YamlDocument config);
}
