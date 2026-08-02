package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.config.ConfigurationMigration;
import com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager;
import com.pumpkiiings.pklogin.common.config.MigrationContext;
import com.pumpkiiings.pklogin.common.config.MigrationLogger;
import com.pumpkiiings.pklogin.common.config.migrations.ConfigMigrations;
import dev.dejvokep.boostedyaml.YamlDocument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Proves the migration system's data-safety guarantees.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:checkMigrations}. Every case here
 * corresponds to a promise made to server owners: their edits survive upgrades,
 * new options appear, and a failed migration leaves the file as it was.</p>
 */
public final class MigrationTest {

    private static int failures = 0;
    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        keepsCustomisedValues();
        addsNewKeys();
        keepsUnknownUserKeys();
        runsWholeChainInOrder();
        readsLegacyVersionStamp();
        relocatesRenamedKey();
        abortsAndRestoresOnFailure();
        refusesToTouchNewerFile();
        isIdempotent();

        System.out.println();
        System.out.println(checks + " check(s), " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
        System.out.println("Migration system behaves as promised.");
    }

    // ---------------------------------------------------------------- cases

    /** A value the owner changed must survive, even though the default differs. */
    private static void keepsCustomisedValues() throws Exception {
        File file = write("version-config: 1\nsettings:\n  enabled: false\n");
        YamlDocument result = manager(file,
                "version-config: 2\nsettings:\n  enabled: true\n  debug: false\n", 2)
                .register(noop(1))
                .load();

        check("customised value survives", Boolean.FALSE.equals(result.get("settings.enabled")));
        check("new key added alongside", Boolean.FALSE.equals(result.get("settings.debug")));
        check("version stamped", 2 == result.getInt("version-config"));
    }

    /** Keys and whole sections introduced by a new version get added. */
    private static void addsNewKeys() throws Exception {
        File file = write("version-config: 1\nsettings:\n  enabled: true\n");
        YamlDocument result = manager(file,
                "version-config: 2\nsettings:\n  enabled: true\nbrand-new:\n  nested:\n    value: 7\n", 2)
                .register(noop(1))
                .load();

        check("new nested section created", Integer.valueOf(7).equals(result.get("brand-new.nested.value")));
    }

    /** Anything the owner added that the plugin does not know about stays. */
    private static void keepsUnknownUserKeys() throws Exception {
        File file = write("version-config: 1\n"
                + "settings:\n  enabled: true\n  my-own-note: 'do not delete'\n"
                + "my-section:\n  foo: 42\n");
        YamlDocument result = manager(file,
                "version-config: 2\nsettings:\n  enabled: true\n  debug: false\n", 2)
                .register(noop(1))
                .load();

        check("unknown leaf kept", "do not delete".equals(result.get("settings.my-own-note")));
        check("unknown section kept", Integer.valueOf(42).equals(result.get("my-section.foo")));
        check("new key still added", Boolean.FALSE.equals(result.get("settings.debug")));
    }

    /** A file several versions behind runs every step, in order. */
    private static void runsWholeChainInOrder() throws Exception {
        List<Integer> order = new ArrayList<>();
        File file = write("version-config: 1\nvalue: 'original'\n");

        YamlDocument result = manager(file, "version-config: 4\nvalue: 'default'\n", 4)
                .register(recording(1, order))
                .register(recording(2, order))
                .register(recording(3, order))
                .load();

        check("all three steps ran", order.size() == 3);
        check("steps ran in order", order.equals(java.util.Arrays.asList(1, 2, 3)));
        check("value untouched by chain", "original".equals(result.get("value")));
        check("ends at target version", 4 == result.getInt("version-config"));
    }

    /** Installations stamped by the old string scheme are understood. */
    private static void readsLegacyVersionStamp() throws Exception {
        File file = write("version: '1.1'\nSecurity:\n  time-to-login: 99\n");

        YamlDocument result = ConfigMigrations.applyTo(
                new ConfigurationVersionManager(file,
                        stream("version-config: 3\nSecurity:\n  time-to-login: 45\n  hash-algorithm: BCRYPT\n"),
                        ConfigurationVersionManager.VERSION_KEY_CONFIG, 3, quiet()))
                .load();

        check("legacy '1.1' mapped and upgraded", 3 == result.getInt("version-config"));
        check("legacy 'version' key removed", !result.contains("version"));
        check("custom value survived legacy upgrade", Integer.valueOf(99).equals(result.get("Security.time-to-login")));
    }

    /** A key that moved keeps the owner's value at its new route. */
    private static void relocatesRenamedKey() throws Exception {
        File file = write("version: '1.1'\nsecurity:\n  hash-algorithm: ARGON2\n");

        YamlDocument result = ConfigMigrations.applyTo(
                new ConfigurationVersionManager(file,
                        stream("version-config: 3\nSecurity:\n  hash-algorithm: BCRYPT\n"),
                        ConfigurationVersionManager.VERSION_KEY_CONFIG, 3, quiet()))
                .load();

        check("renamed key carries the owner's value", "ARGON2".equals(result.get("Security.hash-algorithm")));
        check("old route removed", !result.contains("security.hash-algorithm"));
    }

    /** A step that throws must leave the file exactly as it was. */
    private static void abortsAndRestoresOnFailure() throws Exception {
        String original = "version-config: 1\nsettings:\n  enabled: false\n";
        File file = write(original);

        YamlDocument result = manager(file, "version-config: 2\nsettings:\n  enabled: true\n", 2)
                .register(new ConfigurationMigration() {
                    public int fromVersion() { return 1; }
                    public void migrate(MigrationContext context) {
                        context.getDocument().set("settings.enabled", true);
                        throw new IllegalStateException("boom");
                    }
                })
                .load();

        String onDisk = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        check("file on disk unchanged after failure", onDisk.equals(original));
        check("still reports old version", 1 == result.getInt("version-config"));
        check("half-applied change not persisted", Boolean.FALSE.equals(result.get("settings.enabled")));
    }

    /** A config written by a newer build must not be downgraded. */
    private static void refusesToTouchNewerFile() throws Exception {
        String original = "version-config: 9\nsettings:\n  from-the-future: true\n";
        File file = write(original);

        manager(file, "version-config: 2\nsettings:\n  enabled: true\n", 2)
                .register(noop(1))
                .load();

        String onDisk = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        check("newer file left untouched", onDisk.equals(original));
    }

    /** Loading twice must not keep rewriting the file. */
    private static void isIdempotent() throws Exception {
        File file = write("version-config: 1\nsettings:\n  enabled: false\n");
        String defaults = "version-config: 2\nsettings:\n  enabled: true\n  debug: false\n";

        manager(file, defaults, 2).register(noop(1)).load();
        String afterFirst = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        manager(file, defaults, 2).register(noop(1)).load();
        String afterSecond = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        check("second load changes nothing", afterFirst.equals(afterSecond));
    }

    // -------------------------------------------------------------- helpers

    private static ConfigurationVersionManager manager(File file, String defaults, int target) {
        return new ConfigurationVersionManager(file, stream(defaults),
                ConfigurationVersionManager.VERSION_KEY_CONFIG, target, quiet());
    }

    private static ConfigurationMigration noop(int from) {
        return new ConfigurationMigration() {
            public int fromVersion() { return from; }
            public void migrate(MigrationContext context) { }
        };
    }

    private static ConfigurationMigration recording(int from, List<Integer> order) {
        return new ConfigurationMigration() {
            public int fromVersion() { return from; }
            public void migrate(MigrationContext context) { order.add(from); }
        };
    }

    private static ByteArrayInputStream stream(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static File write(String yaml) throws Exception {
        File file = File.createTempFile("pklogin-migration", ".yml");
        file.deleteOnExit();
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        // Backups land beside the file; clean them up when the JVM exits.
        File dir = file.getParentFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File[] backups = dir.listFiles((d, n) -> n.startsWith(file.getName() + ".bak-"));
            if (backups != null) for (File backup : backups) backup.delete();
        }));
        return file;
    }

    /** Silent logger; these runs are expected to log warnings and stack traces. */
    private static MigrationLogger quiet() {
        return new MigrationLogger() {
            public void info(String message) { }
            public void warn(String message) { }
            public void error(String message, Throwable cause) { }
        };
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (ok) {
            System.out.println("  PASS  " + what);
        } else {
            failures++;
            System.out.println("  FAIL  " + what);
        }
    }
}
