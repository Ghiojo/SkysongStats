package org.skysongdev.skysongstats.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// Builds the HikariCP connection pool. Unlike the single shared connection in
// Database, the pool checks connections before handing them out and replaces
// ones MySQL has dropped, so a database restart doesn't break the plugin until
// the server restarts.
public final class ConnectionPool {
    static final int MAXIMUM_POOL_SIZE = 4;

    private ConnectionPool() {
    }

    public static HikariDataSource create(DatabaseSettings settings) {
        return new HikariDataSource(config(settings));
    }

    static HikariConfig config(DatabaseSettings settings) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("SkysongStats");
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.user());
        config.setPassword(settings.password());
        // Writes go through one background thread and loads happen at startup, so
        // a few connections are plenty.
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        // The plugin can't work without MySQL, so opening the pool fails right
        // away when it can't get a first connection.
        config.setInitializationFailTimeout(1);
        return config;
    }
}
