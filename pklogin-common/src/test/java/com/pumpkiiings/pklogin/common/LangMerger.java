package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.settings.Messages;
import dev.dejvokep.boostedyaml.YamlDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies any message key present in {@code messages_en.yml} but missing from a
 * translated file into that file, using the English text as a placeholder.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:mergeLangs}. Existing translations
 * are never overwritten. Editing YAML through the parser rather than by splicing
 * text keeps the output well-formed and preserves each file's encoding.</p>
 */
public final class LangMerger {

    private static final String LANG_DIR =
            "pklogin-common/src/main/resources/com/pumpkiiings/pklogin/config/lang";

    /** Keys under Title are compound; these are the sub-paths each one needs. */
    private static final List<String> TITLE_PARTS = Arrays.asList(
            "title", "subtitle", "delays.start", "delays.duration", "delays.end");

    public static void main(String[] args) throws Exception {
        File dir = new File(LANG_DIR);
        if (!dir.isDirectory()) {
            dir = new File("src/main/resources/com/pumpkiiings/pklogin/config/lang");
        }
        if (!dir.isDirectory()) {
            System.err.println("Could not locate the lang directory from " + new File(".").getAbsolutePath());
            System.exit(2);
        }

        File englishFile = new File(dir, "messages_en.yml");
        YamlDocument english = YamlDocument.create(englishFile);

        File[] files = dir.listFiles((d, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files == null) {
            System.err.println("No language files found.");
            System.exit(2);
        }

        for (File file : files) {
            if (file.getName().equals("messages_en.yml")) continue;

            YamlDocument target;
            try {
                target = YamlDocument.create(file);
            } catch (Exception e) {
                System.out.println(file.getName() + ": FAILED TO PARSE - " + e.getMessage());
                continue;
            }

            List<String> added = new ArrayList<>();
            for (Messages message : Messages.values()) {
                String key = message.getKey();
                if (key.startsWith("Messages.Title")) {
                    for (String part : TITLE_PARTS) {
                        String path = key + "." + part;
                        if (target.get(path) == null && english.get(path) != null) {
                            target.set(path, english.get(path));
                            added.add(path);
                        }
                    }
                } else if (target.get(key) == null && english.get(key) != null) {
                    target.set(key, english.get(key));
                    added.add(key);
                }
            }

            // Stamp the schema version the bundled files ship at.
            target.set(com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager.VERSION_KEY_MESSAGES,
                    com.pumpkiiings.pklogin.common.config.migrations.MessagesMigrations.CURRENT_VERSION);

            target.save();
            System.out.println(file.getName() + ": added " + added.size() + " key(s)");
        }

        System.out.println("Done. Translated placeholders are in English — replace them as translations arrive.");
    }
}
