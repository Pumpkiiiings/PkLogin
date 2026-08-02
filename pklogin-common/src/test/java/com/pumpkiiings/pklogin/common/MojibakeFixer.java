package com.pumpkiiings.pklogin.common;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Repairs language files whose UTF-8 bytes were once decoded with a single-byte
 * code page and written back out as UTF-8.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:fixMojibake}.</p>
 *
 * <p>The damage is undone by encoding the text back through the code page that
 * caused it and decoding those bytes as UTF-8. Which code page that was differs
 * per file — Cyrillic files went through windows-1251, western European ones
 * through windows-1252 — so each candidate is tried and the result is only kept
 * when it decodes cleanly.</p>
 */
public class MojibakeFixer {

    private static final String LANG_DIR =
            "pklogin-common/src/main/resources/com/pumpkiiings/pklogin/config/lang";

    private static final List<String> CANDIDATE_CHARSETS =
            Arrays.asList("windows-1251", "windows-1252", "ISO-8859-1");

    /**
     * What CP1252 puts in 0x80-0x9F, in order. The five slots CP1252 leaves
     * undefined keep their C1 control character so the table stays a straight
     * 32-entry index.
     */
    private static final String CP1252_HIGH =
            "€‚ƒ„…†‡"
          + "ˆ‰Š‹ŒŽ"
          + "‘’“”•–—"
          + "˜™š›œžŸ";

    public static void main(String[] args) throws Exception {
        Path langDir = Paths.get(LANG_DIR);
        if (!Files.isDirectory(langDir)) {
            langDir = Paths.get("src/main/resources/com/pumpkiiings/pklogin/config/lang");
        }
        if (!Files.isDirectory(langDir)) {
            System.err.println("Could not locate the lang directory from " + Paths.get(".").toAbsolutePath());
            System.exit(2);
        }

        try (Stream<Path> paths = Files.list(langDir)) {
            paths.filter(p -> p.toString().endsWith(".yml")).sorted().forEach(MojibakeFixer::fix);
        }
    }

    private static void fix(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            // Repair is attempted on every file, not only ones containing C1
            // control characters: plain "Ã¡"-style damage has no control chars at
            // all, so gating on them left several files untouched. Sequences are
            // only rewritten when they form valid UTF-8, which correct text does
            // not, so undamaged files come through unchanged.
            String repaired = repairRuns(content);

            if (repaired.equals(content)) {
                System.out.println(looksDamaged(content)
                        ? "MANUAL  " + path.getFileName() + " - damaged but not automatically recoverable"
                        : "OK      " + path.getFileName());
                return;
            }

            if (looksDamaged(repaired)) {
                System.out.println("MANUAL  " + path.getFileName()
                        + " - partially recovered, still contains invalid characters");
                return;
            }

            Files.write(path, repaired.getBytes(StandardCharsets.UTF_8));
            System.out.println("FIXED   " + path.getFileName());
        } catch (Exception e) {
            System.out.println("ERROR   " + path.getFileName() + " - " + e.getMessage());
        }
    }

    /**
     * Repairs each damaged stretch of text on its own.
     *
     * <p>Whole-file conversion does not work here: the banner at the top of every
     * language file uses box-drawing characters that no single-byte code page can
     * represent, so one unencodable character would abandon the entire file. Only
     * maximal runs of non-ASCII text are converted, and a run is replaced solely
     * when it round-trips into clean UTF-8.</p>
     */
    private static String repairRuns(String content) {
        StringBuilder out = new StringBuilder(content.length());
        int i = 0;
        while (i < content.length()) {
            int consumed = tryDecodeSequenceAt(content, i, out);
            if (consumed > 0) {
                i += consumed;
            } else {
                out.append(content.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Attempts to read one mojibaked UTF-8 sequence starting at {@code index}.
     *
     * <p>Files are often only partly damaged — a Czech file can hold a correct
     * {@code á} right next to a broken {@code č} — so conversion is attempted per
     * character sequence rather than per run. A sequence is rewritten only when
     * its characters map to a UTF-8 lead byte followed by the right number of
     * continuation bytes and the whole thing decodes strictly.</p>
     *
     * @return the number of input characters consumed, or 0 if nothing matched
     */
    private static int tryDecodeSequenceAt(String content, int index, StringBuilder out) {
        int lead = toByte(content.charAt(index));
        if (lead < 0xC2 || lead > 0xF4) return 0;

        int length = lead < 0xE0 ? 2 : lead < 0xF0 ? 3 : 4;
        if (index + length > content.length()) return 0;

        byte[] bytes = new byte[length];
        bytes[0] = (byte) lead;
        for (int k = 1; k < length; k++) {
            int continuation = toByte(content.charAt(index + k));
            if (continuation < 0x80 || continuation > 0xBF) return 0;
            bytes[k] = (byte) continuation;
        }

        String decoded = decodeUtf8Strict(bytes);
        if (decoded == null || looksDamaged(decoded)) return 0;

        out.append(decoded);
        return length;
    }

    /** @return the CP1252 byte for this character, or -1 if it has none */
    private static int toByte(char c) {
        int index = CP1252_HIGH.indexOf(c);
        if (index >= 0) return 0x80 + index;
        return c <= 0xFF ? c : -1;
    }

    /** @return the run decoded through whichever candidate code page works, else unchanged */
    private static String repairRun(String run) {
        // CP1252 is by far the usual culprit, so it is tried first, through a
        // hybrid table (see toCp1252Bytes) rather than the JDK charset.
        String decoded = decodeUtf8Strict(toCp1252Bytes(run));
        if (decoded != null && !looksDamaged(decoded)) {
            return decoded;
        }

        for (String charsetName : CANDIDATE_CHARSETS) {
            CharsetEncoder encoder = Charset.forName(charsetName).newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            try {
                ByteBuffer bytes = encoder.encode(CharBuffer.wrap(run));
                byte[] raw = new byte[bytes.remaining()];
                bytes.get(raw);

                String candidate = decodeUtf8Strict(raw);
                if (candidate != null && !looksDamaged(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // This code page cannot represent the run; try the next one.
            }
        }
        return run;
    }

    /**
     * Maps text back to the bytes a CP1252 decode would have produced.
     *
     * <p>Neither stock charset can do this on its own: CP1252 leaves 0x81, 0x8D,
     * 0x8F, 0x90 and 0x9D undefined, while ISO-8859-1 has no room for the
     * typographic characters CP1252 puts in the rest of that range. Real-world
     * mojibake contains both, so the two tables are combined here.</p>
     *
     * @return the bytes, or null if some character belongs to neither table
     */
    private static byte[] toCp1252Bytes(String run) {
        byte[] out = new byte[run.length()];
        for (int i = 0; i < run.length(); i++) {
            char c = run.charAt(i);
            int index = CP1252_HIGH.indexOf(c);
            if (index >= 0) {
                out[i] = (byte) (0x80 + index);
            } else if (c <= 0xFF) {
                out[i] = (byte) c;
            } else {
                return null;
            }
        }
        return out;
    }

    /** @return the decoded text, or null if the bytes are not valid UTF-8 */
    private static String decodeUtf8Strict(byte[] bytes) {
        if (bytes == null) return null;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Damage shows up as C1 control characters (U+0080-U+009F) or replacement
     * characters — neither belongs in a language file, and YAML rejects the former
     * outright.
     */
    private static boolean looksDamaged(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '�') return true;
            if (c >= 0x80 && c <= 0x9F) return true;
        }
        return false;
    }
}
