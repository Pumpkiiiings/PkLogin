import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class EncodingFixer {
    public static void main(String[] args) throws IOException {
        String basePath = ".";
        
        try (Stream<Path> paths = Files.walk(Paths.get(basePath))) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".yml"))
                 .forEach(EncodingFixer::processFile);
        }
        System.out.println("Done processing files.");
    }

    private static void processFile(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            
            boolean changed = false;
            
            // Map of corruptions to proper characters
            String[][] replacements = {
                {"¡", "¡"},
                {"¿", "¿"},
                {"á", "á"},
                {"é", "é"},
                {"Ã\u00AD", "í"}, // í
                {"ó", "ó"},
                {"ú", "ú"},
                {"ñ", "ñ"},
                {"Ñ", "Ñ"},
                {"ç", "ç"},
                {"ã", "ã"},
                {"â", "â"},
                {"ê", "ê"},
                {"õ", "õ"},
                {"Ó", "Ó"},
                {"Ú", "Ú"},
                {"ü", "ü"},
                {"ö", "ö"},
                {"ä", "ä"},
                {"Ã\u008D", "Í"}, // Ã followed by control character
                {"Ã\u0081", "Á"},
                {"Ã\u0089", "É"},
                {"Ã\u0093", "Ó"},
                {"Ã\u009A", "Ú"}
            };

            for (String[] r : replacements) {
                if (content.contains(r[0])) {
                    content = content.replace(r[0], r[1]);
                    changed = true;
                }
            }

            if (changed) {
                Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                System.out.println("Fixed: " + path.toString());
            }

        } catch (IOException e) {
            System.err.println("Error reading " + path + ": " + e.getMessage());
        }
    }
}
