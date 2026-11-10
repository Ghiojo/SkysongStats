package org.skysongdev.skysongstats.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Saves which characters belong to a plugin in MySQL, one row per character,
// through the same connection pool as the stat tables. Every query is a prepared
// statement.
public class SqlPluginCharacterRepository implements PluginCharacterRepository {
    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS ss_plugin_characters (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              owner VARCHAR(64) NOT NULL,
              PRIMARY KEY (uuid, profile)
            )""";
    private static final String LOAD_ALL = "SELECT uuid, profile, owner FROM ss_plugin_characters";
    private static final String SAVE_OWNER = "INSERT INTO ss_plugin_characters (uuid, profile, owner) VALUES (?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE owner = VALUES(owner)";
    private static final String DELETE_CHARACTER = "DELETE FROM ss_plugin_characters WHERE uuid = ? AND profile = ?";

    private final DataSource dataSource;

    public SqlPluginCharacterRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the plugin character table", e);
        }
    }

    @Override
    public Map<ProfileKey, String> loadAll() {
        Map<ProfileKey, String> characters = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOAD_ALL);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                ProfileKey key = new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
                characters.put(key, results.getString("owner"));
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load the plugin characters", e);
        }
        return characters;
    }

    @Override
    public void save(ProfileKey character, String owner) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_OWNER)) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.setString(3, owner);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save the plugin character " + character, e);
        }
    }

    @Override
    public void delete(ProfileKey character) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_CHARACTER)) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to delete the plugin character " + character, e);
        }
    }
}
