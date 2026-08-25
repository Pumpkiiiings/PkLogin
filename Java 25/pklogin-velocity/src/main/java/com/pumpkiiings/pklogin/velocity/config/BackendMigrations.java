package com.pumpkiiings.pklogin.velocity.config;

import com.pumpkiiings.pklogin.common.config.ConfigurationMigration;
import com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager;
import com.pumpkiiings.pklogin.common.config.MigrationContext;

/**
 * The upgrade chain for the proxy's {@code backend.yml}.
 *
 * <p>Kept apart from the {@code config.yml} chain because the two files belong to
 * different sides of the network and change for different reasons; each carries
 * its own version counter so neither drags the other along.</p>
 */
public final class BackendMigrations {

    /**
     * Schema version of the bundled {@code backend.yml}.
     *
     * <ul>
     *   <li>1 — the original file, which carried no version stamp at all</li>
     *   <li>2 — the backend check became {@code verify-connection}</li>
     * </ul>
     */
    public static final int CURRENT_VERSION = 2;

    private BackendMigrations() {}

    /** Registers every step of the chain. */
    public static ConfigurationVersionManager applyTo(ConfigurationVersionManager manager) {
        return manager.register(new OneToTwo());
    }

    /**
     * 1 &rarr; 2. Renames the backend check to {@code verify-connection}.
     *
     * <p>The value moves with the name: an owner who turned the check off did so
     * for a reason — a limbo server in the list, most likely — and that reason did
     * not change with the option's name.</p>
     */
    static final class OneToTwo implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 1;
        }

        @Override
        public String description() {
            return "the backend check is now called verify-connection";
        }

        @Override
        public void migrate(MigrationContext context) {
            context.rename("backend.check-ack-message", "backend.verify-connection");
        }
    }
}
