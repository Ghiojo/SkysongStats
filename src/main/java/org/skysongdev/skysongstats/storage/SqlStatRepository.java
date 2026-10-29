package org.skysongdev.skysongstats.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// Saves stats in MySQL, one row per value, through a connection pool. Every
// method borrows a connection and gives it back, and every query is a prepared
// statement.
public class SqlStatRepository implements StatRepository {
    private static final String CREATE_VALUES = """
            CREATE TABLE IF NOT EXISTS ss_stat_values (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              stat_id VARCHAR(64) NOT NULL,
              stat_value INT NOT NULL,
              PRIMARY KEY (uuid, profile, stat_id)
            )""";
    // seq only keeps modifiers in the order they were added; id is what the plugin uses.
    private static final String CREATE_MODIFIERS = """
            CREATE TABLE IF NOT EXISTS ss_modifiers (
              seq BIGINT AUTO_INCREMENT PRIMARY KEY,
              id VARCHAR(36) NOT NULL UNIQUE,
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              target VARCHAR(64) NOT NULL,
              amount INT NOT NULL
            )""";

    private static final String SAVE_VALUE =
            "INSERT INTO ss_stat_values (uuid, profile, stat_id, stat_value) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE stat_value = VALUES(stat_value)";
    private static final String SAVE_MODIFIER =
            "INSERT INTO ss_modifiers (id, uuid, profile, target, amount) VALUES (?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE target = VALUES(target), amount = VALUES(amount)";
    private static final String DELETE_MODIFIER = "DELETE FROM ss_modifiers WHERE id = ?";
    private static final String DELETE_CHARACTER_VALUES = "DELETE FROM ss_stat_values WHERE uuid = ? AND profile = ?";
    private static final String DELETE_CHARACTER_MODIFIERS = "DELETE FROM ss_modifiers WHERE uuid = ? AND profile = ?";

    private final DataSource dataSource;

    public SqlStatRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_VALUES);
            statement.execute(CREATE_MODIFIERS);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the stat tables", e);
        }
    }

    @Override
    public Map<ProfileKey, StoredStats> loadAll() {
        Map<ProfileKey, Map<String, Integer>> values = new LinkedHashMap<>();
        Map<ProfileKey, List<StoredModifier>> modifiers = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            loadValues(connection, values);
            loadModifiers(connection, modifiers);
        } catch (SQLException e) {
            throw new StorageException("Unable to load stats", e);
        }
        Set<ProfileKey> keys = new LinkedHashSet<>(values.keySet());
        keys.addAll(modifiers.keySet());
        Map<ProfileKey, StoredStats> characters = new LinkedHashMap<>();
        for (ProfileKey key : keys) {
            characters.put(key, new StoredStats(values.getOrDefault(key, Map.of()), modifiers.getOrDefault(key, List.of())));
        }
        return characters;
    }

    private static void loadValues(Connection connection, Map<ProfileKey, Map<String, Integer>> values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uuid, profile, stat_id, stat_value FROM ss_stat_values");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                ProfileKey key = new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
                values.computeIfAbsent(key, k -> new HashMap<>()).put(results.getString("stat_id"), results.getInt("stat_value"));
            }
        }
    }

    private static void loadModifiers(Connection connection, Map<ProfileKey, List<StoredModifier>> modifiers) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, uuid, profile, target, amount FROM ss_modifiers ORDER BY seq");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                ProfileKey key = new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
                modifiers.computeIfAbsent(key, k -> new ArrayList<>()).add(new StoredModifier(
                        results.getString("id"), results.getString("target"), results.getInt("amount")));
            }
        }
    }

    @Override
    public void saveValue(ProfileKey character, String statId, int value) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_VALUE)) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.setString(3, statId);
            statement.setInt(4, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save " + statId + " for " + character, e);
        }
    }

    @Override
    public void saveModifier(ProfileKey character, StoredModifier modifier) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_MODIFIER)) {
            statement.setString(1, modifier.id());
            statement.setString(2, character.player().toString());
            statement.setString(3, character.profile());
            statement.setString(4, modifier.target());
            statement.setInt(5, modifier.amount());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save modifier " + modifier.id() + " for " + character, e);
        }
    }

    @Override
    public void deleteModifier(String modifierId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_MODIFIER)) {
            statement.setString(1, modifierId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to delete modifier " + modifierId, e);
        }
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement values = connection.prepareStatement(DELETE_CHARACTER_VALUES);
                 PreparedStatement modifiers = connection.prepareStatement(DELETE_CHARACTER_MODIFIERS)) {
                deleteRows(values, character);
                deleteRows(modifiers, character);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to delete the stats of " + character, e);
        }
    }

    private static void deleteRows(PreparedStatement statement, ProfileKey character) throws SQLException {
        statement.setString(1, character.player().toString());
        statement.setString(2, character.profile());
        statement.executeUpdate();
    }
}
