package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.settings.Messages;
import dev.dejvokep.boostedyaml.YamlDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that every bundled language file parses and defines every message key.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:checkLangs}. A missing key does not
 * fail the build loudly at runtime — the player just sees "Missing message" — so
 * it is worth catching here.</p>
 */
public final class LanguageFilesTest {

    private static final String LANG_DIR =
            "pklogin-common/src/main/resources/com/pumpkiiings/pklogin/config/lang";

    public static void main(String[] args) throws Exception {
        File dir = new File(LANG_DIR);
        if (!dir.isDirectory()) {
            dir = new File("src/main/resources/com/pumpkiiings/pklogin/config/lang");
        }
        if (!dir.isDirectory()) {
            System.err.println("Could not locate the lang directory from " + new File(".").getAbsolutePath());
            System.exit(2);
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            System.err.println("No language files found in " + dir.getAbsolutePath());
            System.exit(2);
        }

        int failed = 0;
        for (File file : files) {
            List<String> problems = new ArrayList<>();
            YamlDocument document;
            try {
                document = YamlDocument.create(file);
            } catch (Exception e) {
                System.out.println(file.getName() + ": FAILED TO PARSE - " + e.getMessage());
                failed++;
                continue;
            }

            for (Messages message : Messages.values()) {
                String key = message.getKey();
                if (key.startsWith("Messages.Title")) {
                    // Titles are compound: they need a title and a subtitle.
                    if (document.get(key + ".title") == null || document.get(key + ".subtitle") == null) {
                        problems.add(key);
                    }
                } else if (document.get(key) == null) {
                    problems.add(key);
                }
            }

            if (problems.isEmpty()) {
                System.out.println(file.getName() + ": OK (" + Messages.values().length + " keys)");
            } else {
                System.out.println(file.getName() + ": MISSING " + problems.size() + " key(s)");
                for (String problem : problems) {
                    System.out.println("    - " + problem);
                }
                failed++;
            }
        }

        if (failed > 0) {
            System.err.println(failed + " language file(s) need attention.");
            System.exit(1);
        }
        System.out.println("All language files are complete.");
    }
}
