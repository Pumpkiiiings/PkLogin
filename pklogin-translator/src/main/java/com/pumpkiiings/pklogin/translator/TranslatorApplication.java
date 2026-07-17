package com.pumpkiiings.pklogin.translator;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class TranslatorApplication {

    public static void main(String[] args) {
        boolean dryRun = false;
        if (args.length > 0 && args[0].equals("--dry-run")) {
            dryRun = true;
            System.out.println("====== RUNNING IN DRY-RUN MODE ======");
        }

        try {
            File configFile = new File("translator-config.yml");
            TranslatorConfig config = TranslatorConfig.load(configFile);

            TranslationCache cache = new TranslationCache(new File("cache.json"));
            cache.load();

            TranslationProvider provider;
            if (config.getProvider().equalsIgnoreCase("gemini")) {
                provider = new GeminiProvider(config.getApiKey(), config.getModel());
            } else {
                throw new IllegalArgumentException("Unknown provider: " + config.getProvider());
            }

            String sourceLang = config.getSourceLanguage();
            String sourceFilePath = config.getFiles().get(sourceLang);
            if (sourceFilePath == null) {
                throw new IllegalArgumentException("Source language file not defined in config!");
            }

            File sourceFile = new File(sourceFilePath);
            if (!sourceFile.exists()) {
                throw new IllegalArgumentException("Source language file not found: " + sourceFile.getAbsolutePath());
            }

            for (Map.Entry<String, String> entry : config.getFiles().entrySet()) {
                String targetLang = entry.getKey();
                if (targetLang.equals(sourceLang)) continue;

                File targetFile = new File(entry.getValue());
                System.out.println("\nProcessing language: " + targetLang);
                
                if (!targetFile.exists()) {
                    System.out.println("  -> Target file does not exist, skipping or could be created. (Skipping for now)");
                    continue;
                }

                try {
                    YamlDocument sourceDoc = YamlDocument.create(sourceFile);
                    YamlDocument targetDoc = YamlDocument.create(targetFile);
                    
                    int newTranslations = 0;
                    Set<Route> routes = sourceDoc.getRoutes(true);

                    for (Route route : routes) {
                        if (sourceDoc.isSection(route) || sourceDoc.isList(route)) continue;
                        
                        String originalText = sourceDoc.getString(route);
                        if (originalText == null || originalText.trim().isEmpty()) continue;
                        
                        String targetText = targetDoc.getString(route);
                        boolean isMissing = (targetText == null);
                        boolean isIdentical = (!isMissing && targetText.equals(originalText));
                        
                        // We translate if it's missing OR if it's identical to English (meaning it was copied over without translation)
                        if (isMissing || isIdentical) {
                            System.out.println("  -> Needs translation: " + route.join('.'));
                            
                            // Mask placeholders
                            PlaceholderMasker.MaskResult maskResult = PlaceholderMasker.mask(originalText);
                            String maskedText = maskResult.getMaskedText();
                            
                            String translatedMaskedText = cache.getTranslation(config.getProvider(), config.getModel(), sourceLang, targetLang, maskedText);
                            
                            if (translatedMaskedText == null) {
                                if (!dryRun) {
                                    System.out.println("     Translating via API...");
                                    try {
                                        translatedMaskedText = provider.translate(maskedText, sourceLang, targetLang);
                                        cache.putTranslation(config.getProvider(), config.getModel(), sourceLang, targetLang, maskedText, translatedMaskedText);
                                        cache.save();
                                    } catch (Exception e) {
                                        System.err.println("     FAILED to translate: " + e.getMessage());
                                    } finally {
                                        // Wait 4.1 seconds to respect Gemini's 15 requests per minute limit ALWAYS
                                        try { Thread.sleep(4100); } catch (InterruptedException ie) {}
                                    }
                                    
                                    if (translatedMaskedText == null) {
                                        continue; // Skip this key since it failed
                                    }
                                } else {
                                    System.out.println("     (Dry Run) Would translate: " + originalText);
                                    translatedMaskedText = "[DRY-RUN] " + maskedText;
                                }
                            } else {
                                System.out.println("     Loaded from cache.");
                            }
                            
                            String finalText = maskResult.unmask(translatedMaskedText);
                            sourceDoc.set(route, finalText);
                            newTranslations++;
                        }
                    }

                    if (newTranslations > 0) {
                        if (!dryRun) {
                            System.out.println("  -> Saving " + newTranslations + " new translations to " + targetFile.getName());
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(baos, java.nio.charset.StandardCharsets.UTF_8)) {
                                sourceDoc.save(writer);
                            }
                            java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(baos.toByteArray());
                            
                            try {
                                YamlDocument updatableTarget = YamlDocument.create(
                                        targetFile, 
                                        in,
                                        GeneralSettings.DEFAULT, 
                                        LoaderSettings.builder().setAutoUpdate(false).build(),
                                        DumperSettings.DEFAULT, 
                                        UpdaterSettings.DEFAULT
                                );
                                
                                updatableTarget.update();
                                updatableTarget.save();
                                System.out.println("  ✔ " + targetLang + ": +" + newTranslations + " nuevas traducciones");
                            } catch (Exception e) {
                                System.err.println("  ✖ Failed to save valid YAML for " + targetLang + ": " + e.getMessage());
                            }
                        } else {
                            System.out.println("  ✔ " + targetLang + ": +" + newTranslations + " nuevas traducciones (DRY-RUN)");
                        }
                    } else {
                        System.out.println("  ✔ " + targetLang + ": sin cambios");
                    }
                } catch (Exception e) {
                    System.err.println("  ✖ Failed to process " + targetLang + ": " + e.getMessage());
                }
            }

            System.out.println("\nFinished translation process.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
