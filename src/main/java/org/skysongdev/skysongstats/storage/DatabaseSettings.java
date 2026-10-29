package org.skysongdev.skysongstats.storage;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

// The MySQL connection settings from the "database:" section of config.yml.
public record DatabaseSettings(String host, String port, String database, String user, String password) {

    public static DatabaseSettings fromConfig(ConfigurationSection section) {
        Objects.requireNonNull(section, "config.yml has no 'database:' section");
        return new DatabaseSettings(
                section.getString("host", "localhost"),
                section.getString("port", "3306"),
                section.getString("database_name", ""),
                section.getString("user", "root"),
                // No password key means the user has no password (null for the driver).
                section.getString("password"));
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    // Keeps the password out of logs and error messages.
    @Override
    public String toString() {
        return "DatabaseSettings[" + user + "@" + host + ":" + port + "/" + database + "]";
    }
}
