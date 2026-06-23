package com.pumpkiiings.pklogin.common.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class SQLite extends AbstractDatabase {

    @NonNull
    private final File file;

    @Override
    protected void configure(HikariConfig config) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new RuntimeException("Failed to create '" + parentFile + "'");
        }
        config.setJdbcUrl("jdbc:sqlite:" + file.toString());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite doesn't handle concurrency well
    }
}
