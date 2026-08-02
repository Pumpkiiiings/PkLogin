package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.common.util.PluginResources;
import dev.dejvokep.boostedyaml.YamlDocument;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks the bundled {@code config.yml} against the {@link Settings} enum.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:checkConfig}. Catches the two
 * mistakes this pairing invites: a setting whose key does not exist in the
 * shipped file (so it silently always uses its default), and a key in the file
 * that nothing reads.</p>
 */
public final class ConfigKeysTest {

    public static void main(String[] args) throws Exception {
        InputStream bundled = PluginResources.openConfig();
        if (bundled == null) {
            System.err.println("Could not open the bundled config.yml — check PluginResources.BASE.");
            System.exit(2);
        }

        File temp = File.createTempFile("pklogin-config", ".yml");
        temp.deleteOnExit();
        Files.copy(bundled, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        YamlDocument config = YamlDocument.create(temp);

        List<String> missing = new ArrayList<>();
        Set<String> known = new HashSet<>();
        boolean duplicates = false;

        for (Settings setting : Settings.values()) {
            String key = setting.getKey();
            if (!known.add(key)) {
                System.out.println("DUPLICATE enum key: " + key);
                duplicates = true;
            }
            if (config.get(key) == null) {
                missing.add(key + "  (default: " + setting.getDef() + ")");
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("Settings with no entry in the bundled config.yml:");
            for (String key : missing) {
                System.out.println("    - " + key);
            }
        }

        // Every leaf in the file should be read by something, apart from the
        // version stamp the migrator maintains.
        List<String> unused = new ArrayList<>();
        for (Object route : config.getRoutesAsStrings(true)) {
            String key = String.valueOf(route);
            // Maintained by the migration system, not read through Settings.
            if (key.equals(com.pumpkiiings.pklogin.common.config
                    .ConfigurationVersionManager.VERSION_KEY_CONFIG)) continue;
            if (config.isSection(key)) continue;
            if (!known.contains(key)) {
                unused.add(key);
            }
        }

        if (!unused.isEmpty()) {
            System.out.println("Keys present in config.yml that no Settings entry reads:");
            for (String key : unused) {
                System.out.println("    - " + key);
            }
        }

        if (!missing.isEmpty() || !unused.isEmpty() || duplicates) {
            System.err.println("Configuration and Settings are out of sync.");
            System.exit(1);
        }
        System.out.println("config.yml and Settings agree on all "
                + Settings.values().length + " keys.");
    }
}
