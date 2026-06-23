package com.pumpkiiings.pklogin.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LibraryDownloader {

    private static final String JDA_URL = "https://github.com/DV8FromTheWorld/JDA/releases/download/v5.0.0-beta.24/JDA-5.0.0-beta.24-withDependencies-min.jar";
    private static final String JAVAMAIL_URL = "https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar";

    public static boolean checkAndDownloadForge(File modFolder) {
        if (!modFolder.exists()) {
            modFolder.mkdirs();
        }

        File jdaFile = new File(modFolder, "JDA-5.0.0-beta.24-withDependencies.jar");
        File mailFile = new File(modFolder, "javax.mail-1.6.2.jar");

        boolean downloaded = false;

        if (!jdaFile.exists()) {
            System.out.println("[PkLogin] Downloading JDA library (Discord 2FA)...");
            if (downloadFile(JDA_URL, jdaFile)) {
                downloaded = true;
            }
        }

        if (!mailFile.exists()) {
            System.out.println("[PkLogin] Downloading JavaMail library (Email 2FA)...");
            if (downloadFile(JAVAMAIL_URL, mailFile)) {
                downloaded = true;
            }
        }

        return downloaded;
    }

    private static boolean downloadFile(String urlStr, File target) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "PkLogin");
            
            // Handle redirects if necessary (GitHub releases usually redirect)
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER) {
                
                String newUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(newUrl).openConnection();
            }

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(target)) {

                byte[] buffer = new byte[1024 * 8];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[PkLogin] Failed to download library from " + urlStr);
            return false;
        }
    }
}
