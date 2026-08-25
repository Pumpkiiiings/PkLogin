package com.pumpkiiings.pklogin.universal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;

@Plugin(id = "pklogin", name = "PkLogin", version = "${version}", authors = {"Pumpkiiiings"})
public class PkLoginLoader extends JavaPlugin {

    // Velocity logger (we don't import org.slf4j.Logger globally to avoid Bukkit conflicts in classloader)
    private org.slf4j.Logger velocityLogger;
    private Path velocityDataDir;

    private static final String GITHUB_API_URL = "https://api.github.com/repos/Pumpkiiiings/PkLogin/releases/latest";

    // Required for Bukkit instantiation
    public PkLoginLoader() {
    }

    // Required for Velocity instantiation
    @Inject
    public PkLoginLoader(org.slf4j.Logger logger, @DataDirectory Path dataDirectory) {
        this.velocityLogger = logger;
        this.velocityDataDir = dataDirectory;
    }

    // ===========================
    // BUKKIT / PAPER ENTRY POINT
    // ===========================
    @Override
    public void onEnable() {
        getLogger().info("[PkLogin] Detected Paper/Spigot environment.");

        File pluginsFolder = getDataFolder().getParentFile();
        File updateFolder = new File(pluginsFolder, "update");
        if (!updateFolder.exists()) {
            updateFolder.mkdirs();
        }

        // Search if PkLogin-Paper is already downloaded or in update folder
        if (checkIfPluginExists(pluginsFolder, "PkLogin-Paper") || checkIfPluginExists(updateFolder, "PkLogin-Paper")) {
            getLogger().severe("=========================================================================");
            getLogger().severe("PkLogin-Paper is already downloaded!");
            getLogger().severe("If you just downloaded it, please RESTART your server to apply the update.");
            getLogger().severe("=========================================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("[PkLogin] Fetching latest Paper version from GitHub...");
        
        try {
            String downloadUrl = fetchLatestReleaseAsset("Paper");
            if (downloadUrl == null) {
                getLogger().severe("Could not find a Paper release asset on GitHub.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            File targetFile = new File(updateFolder, "PkLogin-Paper.jar");
            downloadFile(downloadUrl, targetFile);

            getLogger().info("=========================================================================");
            getLogger().info("SUCCESS! PkLogin-Paper has been downloaded to the 'update' folder.");
            getLogger().info("Paper will automatically apply it on the next restart.");
            getLogger().info("Please RESTART YOUR SERVER now to complete the installation.");
            getLogger().info("=========================================================================");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to download PkLogin-Paper", e);
        }

        getServer().getPluginManager().disablePlugin(this);
    }

    // ===========================
    // VELOCITY ENTRY POINT
    // ===========================
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        velocityLogger.info("[PkLogin] Detected Velocity environment.");

        File pluginsFolder = velocityDataDir.toFile().getParentFile();
        
        if (checkIfPluginExists(pluginsFolder, "PkLogin-Velocity")) {
            velocityLogger.error("=========================================================================");
            velocityLogger.error("PkLogin-Velocity is already downloaded!");
            velocityLogger.error("Please manually delete this universal loader jar, and RESTART the proxy.");
            velocityLogger.error("=========================================================================");
            return;
        }

        velocityLogger.info("[PkLogin] Fetching latest Velocity version from GitHub...");

        try {
            String downloadUrl = fetchLatestReleaseAsset("Velocity");
            if (downloadUrl == null) {
                velocityLogger.error("Could not find a Velocity release asset on GitHub.");
                return;
            }

            File targetFile = new File(pluginsFolder, "PkLogin-Velocity-Latest.jar");
            downloadFile(downloadUrl, targetFile);

            velocityLogger.info("=========================================================================");
            velocityLogger.info("SUCCESS! PkLogin-Velocity has been downloaded to your plugins folder.");
            velocityLogger.info("Because you are on Velocity, you MUST manually delete this universal loader jar.");
            velocityLogger.info("After deleting the loader, please RESTART YOUR PROXY.");
            velocityLogger.info("=========================================================================");

        } catch (Exception e) {
            velocityLogger.error("Failed to download PkLogin-Velocity", e);
        }
    }

    // ===========================
    // UTILS
    // ===========================

    private boolean checkIfPluginExists(File folder, String nameContains) {
        if (!folder.exists()) return false;
        File[] files = folder.listFiles();
        if (files == null) return false;
        
        for (File file : files) {
            if (file.getName().toLowerCase().contains(nameContains.toLowerCase()) && file.getName().endsWith(".jar")) {
                return true;
            }
        }
        return false;
    }

    private String fetchLatestReleaseAsset(String platformFilter) throws Exception {
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "PkLogin-Universal-Loader");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("GitHub API returned HTTP " + conn.getResponseCode());
        }

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
            JsonObject release = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray assets = release.getAsJsonArray("assets");
            
            for (JsonElement element : assets) {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                
                if (name.toLowerCase().contains(platformFilter.toLowerCase()) && name.endsWith(".jar")) {
                    return asset.get("browser_download_url").getAsString();
                }
            }
        }
        return null;
    }

    private void downloadFile(String fileUrl, File destination) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "PkLogin-Universal-Loader");
        
        // Handle redirects
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "PkLogin-Universal-Loader");
        }

        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
