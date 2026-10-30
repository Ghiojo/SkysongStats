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

// Saves skill XP in MySQL, one row per skill, through the same connection pool as
// the stat tables. Every query is a prepared statement.
public class SqlSkillRepository implements SkillRepository {
    private static final String CREATE_XP = """
            CREATE TABLE IF NOT EXISTS ss_skill_xp (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              skill_id VARCHAR(64) NOT NULL,
              xp INT NOT NULL,
              PRIMARY KEY (uuid, profile, skill_id)
            )""";
    private static final String LOAD_XP = "SELECT uuid, profile, skill_id, xp FROM ss_skill_xp";
    private static final String SAVE_XP = "INSERT INTO ss_skill_xp (uuid, profile, skill_id, xp) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE xp = VALUES(xp)";
    private static final String DELETE_CHARACTER = "DELETE FROM ss_skill_xp WHERE uuid = ? AND profile = ?";

    private final DataSource dataSource;

    public SqlSkillRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_XP);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the skill table", e);
        }
    }

    @Override
    public Map<ProfileKey, Map<String, Integer>> loadAll() {
        Map<ProfileKey, Map<String, Integer>> characters = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOAD_XP);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                ProfileKey key = new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
                characters.computeIfAbsent(key, k -> new HashMap<>()).put(results.getString("skill_id"), results.getInt("xp"));
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load skills", e);
        }
        return characters;
    }

    @Override
    public void saveXp(ProfileKey character, String skillId, int xp) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_XP)) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.setString(3, skillId);
            statement.setInt(4, xp);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to save " + skillId + " XP for " + character, e);
        }
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_CHARACTER)) {
            statement.setString(1, character.player().toString());
            statement.setString(2, character.profile());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to delete the skills of " + character, e);
        }
    }
}
