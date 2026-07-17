package com.pumpkiiings.pklogin.translator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderMasker {
    
    // Pattern to match: %var%, {var}, ${var}, and <tag> or </tag>
    private static final Pattern MASK_PATTERN = Pattern.compile("(%[^%\\s]+%|\\{\\d+\\}|\\$\\{[^}]+\\}|<[^>]+>)");

    public static class MaskResult {
        private final String maskedText;
        private final Map<String, String> replacements;

        public MaskResult(String maskedText, Map<String, String> replacements) {
            this.maskedText = maskedText;
            this.replacements = replacements;
        }

        public String getMaskedText() {
            return maskedText;
        }

        public String unmask(String translatedText) {
            String result = translatedText;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
            return result;
        }
    }

    public static MaskResult mask(String text) {
        if (text == null) return new MaskResult(null, new HashMap<>());

        Matcher matcher = MASK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        Map<String, String> replacements = new HashMap<>();
        int counter = 1;

        while (matcher.find()) {
            String original = matcher.group(1);
            String placeholder = "[M" + counter + "]";
            replacements.put(placeholder, original);
            matcher.appendReplacement(sb, placeholder);
            counter++;
        }
        matcher.appendTail(sb);

        return new MaskResult(sb.toString(), replacements);
    }
}
