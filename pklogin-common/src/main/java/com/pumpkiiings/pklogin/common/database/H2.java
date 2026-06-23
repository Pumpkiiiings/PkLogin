package com.pumpkiiings.pklogin.common.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class H2 extends AbstractDatabase {

    @NonNull
    private final File file;

    @Override
    protected void configure(HikariConfig config) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new RuntimeException("Failed to create '" + parentFile + "'");
        }
        config.setJdbcUrl("jdbc:h2:" + file.toString() + ";MODE=MySQL");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
    }
}
