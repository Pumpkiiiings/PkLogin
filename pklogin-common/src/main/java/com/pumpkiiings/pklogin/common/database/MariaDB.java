package com.pumpkiiings.pklogin.common.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MariaDB extends AbstractDatabase {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    @Override
    protected void configure(HikariConfig config) {
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
}
