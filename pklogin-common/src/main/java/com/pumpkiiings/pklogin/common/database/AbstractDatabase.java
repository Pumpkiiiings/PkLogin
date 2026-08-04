package com.pumpkiiings.pklogin.common.database;

import com.pumpkiiings.pklogin.common.settings.Settings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractDatabase implements Database {

    protected HikariDataSource dataSource;

    protected abstract void configure(HikariConfig config);

    @Override
    public void openConnection() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        
        HikariConfig config = new HikariConfig();

        // Pool defaults first, so an engine that has a hard requirement (SQLite
        // needs a single connection) can override them in configure().
        config.setConnectionTimeout(Settings.DATABASE_POOL_CONNECTION_TIMEOUT.asInt());
        config.setMaxLifetime(Settings.DATABASE_POOL_MAX_LIFETIME.asInt());
        config.setMaximumPoolSize(Settings.DATABASE_POOL_MAX_SIZE.asInt());

        configure(config);

        dataSource = new HikariDataSource(config);
    }

    @Override
    public void closeConnection() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            openConnection();
        }
        return dataSource.getConnection();
    }

    @Override
    public void update(String command, Object... args) throws SQLException {
        command = translate(command);
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(command)) {
            for (int i = 0; i < args.length; i++) {
                preparedStatement.setObject(i + 1, args[i]);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Failed to execute update statement: '" + command + "'", e);
        }
    }

    @Override
    public Query query(String command, Object... args) throws SQLException {
        Connection connection = getConnection();
        // The Query class handles its own statement and result set
        // Notice we do NOT close the connection here. The caller must close the query, 
        // which should ideally also close the connection or return it to the pool.
        // We'll need to modify the Query class slightly to hold and close the connection.
        return new Query(connection, translate(command), args);
    }
}
