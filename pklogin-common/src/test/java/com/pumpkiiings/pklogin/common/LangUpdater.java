package com.pumpkiiings.pklogin.common;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.FileInputStream;

public class LangUpdater {

    public static void main(String[] args) {
        try {
            File langDir = new File("src/main/resources/com/pumpkiiings/pklogin/config/lang");
            File enFile = new File(langDir, "messages_en.yml");

            if (!enFile.exists()) {
                System.out.println("Could not find messages_en.yml at " + enFile.getAbsolutePath());
                return;
            }

            File[] files = langDir.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.getName().startsWith("messages_") && file.getName().endsWith(".yml") && !file.getName().equals("messages_en.yml")) {
                    System.out.println("Updating " + file.getName() + "...");
                    
                    try {
                        YamlDocument doc = YamlDocument.create(
                                file, 
                                new FileInputStream(enFile),
                                GeneralSettings.DEFAULT, 
                                LoaderSettings.builder().setAutoUpdate(false).build(),
                                DumperSettings.DEFAULT, 
                                UpdaterSettings.DEFAULT
                        );
                        
                        doc.update();
                        try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
                            doc.save(writer);
                        }
                        System.out.println("Saved " + file.getName());
                    } catch (Exception ex) {
                        System.out.println("Failed to update " + file.getName() + ": " + ex.getMessage());
                    }
                }
            }
            System.out.println("Done updating all language files!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
