package com.pumpkiiings.pklogin.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class MojibakeFixer {
    public static void main(String[] args) throws Exception {
        Path langDir = Paths.get("pklogin-common/src/main/resources/com/pumpkiiings/pklogin/config/lang");
        
        try (Stream<Path> paths = Files.list(langDir)) {
            paths.filter(p -> p.toString().endsWith(".yml")).forEach(p -> {
                try {
                    // Read file as UTF-8. It currently contains "PouÅ¾ij"
                    String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                    
                    // The characters "Å" and "¾" are Unicode chars, but they represent the bytes C5 BE.
                    // If we get the bytes of this string in ISO-8859-1 (which maps 1-to-1 for first 256 chars), 
                    // we recover the original UTF-8 bytes C5 BE!
                    byte[] bytes1 = content.getBytes(java.nio.charset.Charset.forName("windows-1252"));
                    String fixedContent = new String(bytes1, StandardCharsets.UTF_8);
                    
                    if (!fixedContent.contains("\uFFFD")) {
                        Files.write(p, fixedContent.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Fixed " + p.getFileName());
                    } else {
                        System.out.println("Skipped " + p.getFileName() + " (not mojibaked or different encoding)");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
