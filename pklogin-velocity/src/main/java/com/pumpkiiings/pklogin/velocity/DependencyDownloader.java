package com.pumpkiiings.pklogin.velocity;

import com.velocitypowered.api.plugin.PluginManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class DependencyDownloader {

    private final Object plugin;
    private final PluginManager pluginManager;
    private final Logger logger;
    private final File dataDirectory;

    private static final Map<String, String> DEPENDENCIES = new HashMap<>();

    static {
        // Formato: Nombre del Archivo -> URL
        DEPENDENCIES.put("sqlite-jdbc-3.42.0.0.jar", "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar");
        DEPENDENCIES.put("h2-2.2.224.jar", "https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar");
        DEPENDENCIES.put("mariadb-java-client-3.1.4.jar", "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar");
        DEPENDENCIES.put("HikariCP-5.1.0.jar", "https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar");
        DEPENDENCIES.put("snakeyaml-2.2.jar", "https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar");
    }

    public DependencyDownloader(Object plugin, PluginManager pluginManager, Logger logger, File dataDirectory) {
        this.plugin = plugin;
        this.pluginManager = pluginManager;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void loadDependencies() {
        File libsFolder = new File(dataDirectory, "libraries");
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
        }

        for (Map.Entry<String, String> entry : DEPENDENCIES.entrySet()) {
            String fileName = entry.getKey();
            String urlString = entry.getValue();
            File jarFile = new File(libsFolder, fileName);

            if (!jarFile.exists()) {
                logger.info("[PkLogin] Downloading missing dependency: " + fileName + " ...");
                try {
                    downloadFile(urlString, jarFile);
                    logger.info("[PkLogin] Successfully downloaded " + fileName);
                } catch (Exception e) {
                    logger.error("[PkLogin] Failed to download " + fileName, e);
                    continue;
                }
            }

            // Inyectar en el ClassPath
            try {
                pluginManager.addToClasspath(plugin, jarFile.toPath());
            } catch (Exception e) {
                logger.error("[PkLogin] Failed to inject " + fileName + " into the classpath.", e);
            }
        }
    }

    private void downloadFile(String urlString, File destination) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "PkLogin-Dependency-Downloader/1.0");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
