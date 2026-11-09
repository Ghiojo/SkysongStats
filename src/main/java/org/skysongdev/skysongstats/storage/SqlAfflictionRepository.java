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

// Saves injuries, directly given conditions and known injuries in MySQL, one row
// each, through the same connection pool as the stat tables. Every query is a
// prepared statement.
public class SqlAfflictionRepository implements AfflictionRepository {
    private static final String CREATE_INJURIES = """
            CREATE TABLE IF NOT EXISTS ss_injuries (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              injury_id VARCHAR(64) NOT NULL,
              awarded_at BIGINT NOT NULL,
              PRIMARY KEY (uuid, profile, injury_id)
            )""";
    private static final String CREATE_CONDITIONS = """
            CREATE TABLE IF NOT EXISTS ss_conditions (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              condition_id VARCHAR(64) NOT NULL,
              level INT NOT NULL,
              awarded_at BIGINT NOT NULL,
              PRIMARY KEY (uuid, profile, condition_id)
            )""";
    private static final String CREATE_KNOWN = """
            CREATE TABLE IF NOT EXISTS ss_known_injuries (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              injury_id VARCHAR(64) NOT NULL,
              learnt_at BIGINT NOT NULL,
              learnt_by VARCHAR(16) NOT NULL,
              PRIMARY KEY (uuid, profile, injury_id)
            )""";
    private static final String LOAD_INJURIES = "SELECT uuid, profile, injury_id, awarded_at FROM ss_injuries";
    private static final String LOAD_CONDITIONS = "SELECT uuid, profile, condition_id, level, awarded_at FROM ss_conditions";
    private static final String LOAD_KNOWN = "SELECT uuid, profile, injury_id, learnt_at, learnt_by FROM ss_known_injuries";
    private static final String SAVE_INJURY = "INSERT INTO ss_injuries (uuid, profile, injury_id, awarded_at) "
            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE awarded_at = VALUES(awarded_at)";
    private static final String SAVE_CONDITION = "INSERT INTO ss_conditions (uuid, profile, condition_id, level, awarded_at) "
            + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = VALUES(level), awarded_at = VALUES(awarded_at)";
    private static final String SAVE_KNOWN = "INSERT INTO ss_known_injuries (uuid, profile, injury_id, learnt_at, learnt_by) "
            + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE learnt_at = VALUES(learnt_at), learnt_by = VALUES(learnt_by)";
    private static final String DELETE_INJURY = "DELETE FROM ss_injuries WHERE uuid = ? AND profile = ? AND injury_id = ?";
    private static final String DELETE_CONDITION = "DELETE FROM ss_conditions WHERE uuid = ? AND profile = ? AND condition_id = ?";
    private static final String DELETE_KNOWN = "DELETE FROM ss_known_injuries WHERE uuid = ? AND profile = ? AND injury_id = ?";
    private static final String DELETE_CHARACTER_INJURIES = "DELETE FROM ss_injuries WHERE uuid = ? AND profile = ?";
    private static final String DELETE_CHARACTER_CONDITIONS = "DELETE FROM ss_conditions WHERE uuid = ? AND profile = ?";
    private static final String DELETE_CHARACTER_KNOWN = "DELETE FROM ss_known_injuries WHERE uuid = ? AND profile = ?";

    // Opens the statement of one write; each method passes its own constant SQL.
    @FunctionalInterface
    private interface Prepare {
        PreparedStatement on(Connection connection) throws SQLException;
    }

    // Fills in the statement's values.
    @FunctionalInterface
    private interface Bind {
        void values(PreparedStatement statement) throws SQLException;
    }

    // Reads one row into the characters being loaded.
    @FunctionalInterface
    private interface Row {
        void read(ResultSet results, Map<ProfileKey, Builder> characters) throws SQLException;
    }

    private final DataSource dataSource;

    public SqlAfflictionRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_INJURIES);
            statement.execute(CREATE_CONDITIONS);
            statement.execute(CREATE_KNOWN);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the injury and condition tables", e);
        }
    }

    @Override
    public Map<ProfileKey, StoredAfflictions> loadAll() {
        Map<ProfileKey, Builder> characters = new LinkedHashMap<>();
        load(connection -> connection.prepareStatement(LOAD_INJURIES), characters, (results, loaded) ->
                builder(results, loaded).injuries.put(results.getString("injury_id"), results.getLong("awarded_at")));
        load(connection -> connection.prepareStatement(LOAD_CONDITIONS), characters, (results, loaded) ->
                builder(results, loaded).conditions.put(results.getString("condition_id"),
                        new StoredCondition(Math.max(1, results.getInt("level")), results.getLong("awarded_at"))));
        // A learnt_by this version doesn't know (written by a newer one) reads as a discover.
        load(connection -> connection.prepareStatement(LOAD_KNOWN), characters, (results, loaded) ->
                builder(results, loaded).known.put(results.getString("injury_id"), new StoredKnownInjury(
                        results.getLong("learnt_at"),
                        LearnSource.fromColumn(results.getString("learnt_by")).orElse(LearnSource.DISCOVER))));
        Map<ProfileKey, StoredAfflictions> stored = new LinkedHashMap<>();
        characters.forEach((key, builder) -> stored.put(key, builder.build()));
        return stored;
    }

    @Override
    public void saveInjury(ProfileKey character, String injuryId, long awardedAt) {
        write("save injury " + injuryId + " of " + character, connection -> connection.prepareStatement(SAVE_INJURY),
                statement -> {
                    bindRow(statement, character, injuryId);
                    statement.setLong(4, awardedAt);
                });
    }

    @Override
    public void deleteInjury(ProfileKey character, String injuryId) {
        write("delete injury " + injuryId + " of " + character, connection -> connection.prepareStatement(DELETE_INJURY),
                statement -> bindRow(statement, character, injuryId));
    }

    @Override
    public void saveCondition(ProfileKey character, String conditionId, StoredCondition condition) {
        write("save condition " + conditionId + " of " + character,
                connection -> connection.prepareStatement(SAVE_CONDITION), statement -> {
                    bindRow(statement, character, conditionId);
                    statement.setInt(4, condition.level());
                    statement.setLong(5, condition.awardedAt());
                });
    }

    @Override
    public void deleteCondition(ProfileKey character, String conditionId) {
        write("delete condition " + conditionId + " of " + character,
                connection -> connection.prepareStatement(DELETE_CONDITION),
                statement -> bindRow(statement, character, conditionId));
    }

    @Override
    public void saveKnownInjury(ProfileKey character, String injuryId, StoredKnownInjury known) {
        write("save known injury " + injuryId + " of " + character,
                connection -> connection.prepareStatement(SAVE_KNOWN), statement -> {
                    bindRow(statement, character, injuryId);
                    statement.setLong(4, known.learntAt());
                    statement.setString(5, known.learntBy().column());
                });
    }

    @Override
    public void deleteKnownInjury(ProfileKey character, String injuryId) {
        write("delete known injury " + injuryId + " of " + character,
                connection -> connection.prepareStatement(DELETE_KNOWN),
                statement -> bindRow(statement, character, injuryId));
    }

    @Override
    public void deleteCharacter(ProfileKey character) {
        String what = "delete the injuries and conditions of " + character;
        write(what, connection -> connection.prepareStatement(DELETE_CHARACTER_INJURIES),
                statement -> bindCharacter(statement, character));
        write(what, connection -> connection.prepareStatement(DELETE_CHARACTER_CONDITIONS),
                statement -> bindCharacter(statement, character));
        write(what, connection -> connection.prepareStatement(DELETE_CHARACTER_KNOWN),
                statement -> bindCharacter(statement, character));
    }

    private void load(Prepare prepare, Map<ProfileKey, Builder> characters, Row row) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepare.on(connection);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                row.read(results, characters);
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load injuries and conditions", e);
        }
    }

    private void write(String what, Prepare prepare, Bind bind) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = prepare.on(connection)) {
            bind.values(statement);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to " + what, e);
        }
    }

    private static void bindCharacter(PreparedStatement statement, ProfileKey character) throws SQLException {
        statement.setString(1, character.player().toString());
        statement.setString(2, character.profile());
    }

    private static void bindRow(PreparedStatement statement, ProfileKey character, String id) throws SQLException {
        bindCharacter(statement, character);
        statement.setString(3, id);
    }

    private static Builder builder(ResultSet results, Map<ProfileKey, Builder> characters) throws SQLException {
        ProfileKey key = new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
        return characters.computeIfAbsent(key, k -> new Builder());
    }

    // One character's rows while they are being read.
    private static final class Builder {
        private final Map<String, Long> injuries = new LinkedHashMap<>();
        private final Map<String, StoredCondition> conditions = new LinkedHashMap<>();
        private final Map<String, StoredKnownInjury> known = new LinkedHashMap<>();

        private StoredAfflictions build() {
            return new StoredAfflictions(injuries, conditions, known);
        }
    }
}
