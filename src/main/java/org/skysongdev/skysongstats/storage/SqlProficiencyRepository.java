package org.skysongdev.skysongstats.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Saves proficiencies in MySQL through the same connection pool as the stat and
// skill tables: ss_proficiency_tiers holds one row per proficiency, and
// ss_proficiency_focus one row per character that has a focus. Every query is a
// prepared statement.
public class SqlProficiencyRepository implements ProficiencyRepository {
    private static final String CREATE_TIERS = """
            CREATE TABLE IF NOT EXISTS ss_proficiency_tiers (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              proficiency_id VARCHAR(64) NOT NULL,
              tier VARCHAR(64) NOT NULL,
              PRIMARY KEY (uuid, profile, proficiency_id)
            )""";
    private static final String CREATE_FOCUS = """
            CREATE TABLE IF NOT EXISTS ss_proficiency_focus (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              proficiency_id VARCHAR(64) NOT NULL,
              points INT NOT NULL,
              PRIMARY KEY (uuid, profile)
            )""";
    private static final String LOAD_TIERS = "SELECT uuid, profile, proficiency_id, tier FROM ss_proficiency_tiers";
    private static final String LOAD_FOCUS = "SELECT uuid, profile, proficiency_id, points FROM ss_proficiency_focus";
    private static final String SAVE_TIER = "INSERT INTO ss_proficiency_tiers (uuid, profile, proficiency_id, tier) "
            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE tier = VALUES(tier)";
    private static final String SAVE_FOCUS = "INSERT INTO ss_proficiency_focus (uuid, profile, proficiency_id, points) "
            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE proficiency_id = VALUES(proficiency_id), points = VALUES(points)";
    private static final String CLEAR_FOCUS = "DELETE FROM ss_proficiency_focus WHERE uuid = ? AND profile = ?";
    private static final String DELETE_TIERS = "DELETE FROM ss_proficiency_tiers WHERE uuid = ? AND profile = ?";

    private final DataSource dataSource;

    public SqlProficiencyRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TIERS);
            statement.execute(CREATE_FOCUS);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the proficiency tables", e);
        }
    }

    @Override
    public Map<ProfileKey, StoredProficiencies> loadAll() {
        Map<ProfileKey, Map<String, String>> tiers = new LinkedHashMap<>();
        Map<ProfileKey, StoredFocus> focuses = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(LOAD_TIERS);
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    tiers.computeIfAbsent(key(results), k -> new HashMap<>())
                            .put(results.getString("proficiency_id"), results.getString("tier"));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(LOAD_FOCUS);
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    focuses.put(key(results), new StoredFocus(results.getString("proficiency_id"), results.getInt("points")));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load proficiencies", e);
        }
        Map<ProfileKey, StoredProficiencies> characters = new LinkedHashMap<>();
        tiers.forEach((key, saved) -> characters.put(key, new StoredProficiencies(saved, focuses.get(key))));
        focuses.forEach((key, focus) -> characters.putIfAbsent(key, new StoredProficiencies(Map.of(), focus)));
        return characters;
    }

    @Override
    public void saveTier(ProfileKey character, String proficiencyId, String tierId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_TIER)) {
            setCharacter(statement, character);
            statement.setString(3, proficiencyId);
            statement.setString(4, tierId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save the " + proficiencyId + " tier for " + character, e);
        }
    }

    @Override
    public void saveFocus(ProfileKey character, StoredFocus focus) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_FOCUS)) {
            setCharacter(statement, character);
            statement.setString(3, focus.proficiencyId());
            statement.setInt(4, focus.points());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save the proficiency focus for " + character, e);
        }
    }

    @Override
    public void clearFocus(ProfileKey character) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CLEAR_FOCUS)) {
            setCharacter(statement, character);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to clear the proficiency focus for " + character, e);
        }
    }

    // Both tables in one transaction, so a deleted profile never keeps half its rows.
    @Override
    public void deleteCharacter(ProfileKey character) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement tiers = connection.prepareStatement(DELETE_TIERS);
                 PreparedStatement focus = connection.prepareStatement(CLEAR_FOCUS)) {
                setCharacter(tiers, character);
                tiers.executeUpdate();
                setCharacter(focus, character);
                focus.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to delete the proficiencies of " + character, e);
        }
    }

    private static void setCharacter(PreparedStatement statement, ProfileKey character) throws SQLException {
        statement.setString(1, character.player().toString());
        statement.setString(2, character.profile());
    }

    private static ProfileKey key(ResultSet results) throws SQLException {
        return new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
    }
}
