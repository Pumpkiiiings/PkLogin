package com.pumpkiiings.pklogin.common.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.RequiredArgsConstructor;

/**
 * PostgreSQL backend.
 *
 * <p>Tables are created wherever the connecting role's {@code search_path} points,
 * which is {@code public} unless it has been changed. PkLogin deliberately has no
 * schema setting of its own: the choice belongs to the database, is made once with
 * {@code ALTER ROLE ... SET search_path}, and a second copy in {@code config.yml}
 * could only ever disagree with it.</p>
 */
@RequiredArgsConstructor
public class PostgreSQL extends AbstractDatabase {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    @Override
    protected void configure(HikariConfig config) {
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setDriverClassName("org.postgresql.Driver");
        config.setUsername(username);
        config.setPassword(password);
    }

    /**
     * Rewrites PkLogin's backtick-quoted identifiers as standard SQL ones.
     * PostgreSQL has no backtick syntax at all and no mode that accepts it.
     *
     * <p>Safe as a plain character swap because every identifier the plugin writes
     * is lower case and quoted, so both forms land on the same name, and no SQL
     * here holds a backtick inside a string literal — the one case that would
     * break.</p>
     */
    @Override
    public String translate(String command) {
        return command.replace('`', '"');
    }
}
