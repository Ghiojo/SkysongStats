package org.skysongdev.skysongstats.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Saves ancestries in MySQL through the same connection pool as the stat tables:
// one ss_ancestries row per character and one ss_ancestry_adjustments row per
// adjustment. A save replaces both in one transaction, so a character is never
// left with half of its adjustments. Every query is a prepared statement.
public class SqlAncestryRepository implements AncestryRepository {
    private static final String CREATE_ANCESTRIES = """
            CREATE TABLE IF NOT EXISTS ss_ancestries (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              ancestry VARCHAR(64) NOT NULL,
              choices_pending BOOLEAN NOT NULL,
              PRIMARY KEY (uuid, profile)
            )""";
    private static final String CREATE_ADJUSTMENTS = """
            CREATE TABLE IF NOT EXISTS ss_ancestry_adjustments (
              uuid VARCHAR(36) NOT NULL,
              profile VARCHAR(20) NOT NULL,
              position INT NOT NULL,
              stat VARCHAR(64) NOT NULL,
              amount INT NOT NULL,
              source VARCHAR(16) NOT NULL,
              PRIMARY KEY (uuid, profile, position)
            )""";
    private static final String LOAD_ANCESTRIES = "SELECT uuid, profile, ancestry, choices_pending FROM ss_ancestries";
    private static final String LOAD_ADJUSTMENTS = "SELECT uuid, profile, stat, amount, source "
            + "FROM ss_ancestry_adjustments ORDER BY position";
    private static final String SAVE_ANCESTRY = "INSERT INTO ss_ancestries (uuid, profile, ancestry, choices_pending) "
            + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ancestry = VALUES(ancestry), "
            + "choices_pending = VALUES(choices_pending)";
    private static final String SAVE_ADJUSTMENT = "INSERT INTO ss_ancestry_adjustments "
            + "(uuid, profile, position, stat, amount, source) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DELETE_ANCESTRY = "DELETE FROM ss_ancestries WHERE uuid = ? AND profile = ?";
    private static final String DELETE_ADJUSTMENTS = "DELETE FROM ss_ancestry_adjustments WHERE uuid = ? AND profile = ?";

    private final DataSource dataSource;

    public SqlAncestryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_ANCESTRIES);
            statement.execute(CREATE_ADJUSTMENTS);
        } catch (SQLException e) {
            throw new StorageException("Unable to create the ancestry tables", e);
        }
    }

    @Override
    public Map<ProfileKey, StoredAncestry> loadAll() {
        Map<ProfileKey, String> ancestries = new LinkedHashMap<>();
        Map<ProfileKey, Boolean> pending = new LinkedHashMap<>();
        Map<ProfileKey, List<StoredAncestryAdjustment>> adjustments = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(LOAD_ANCESTRIES);
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    ProfileKey key = key(results);
                    ancestries.put(key, results.getString("ancestry"));
                    pending.put(key, results.getBoolean("choices_pending"));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(LOAD_ADJUSTMENTS);
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    adjustments.computeIfAbsent(key(results), k -> new ArrayList<>()).add(new StoredAncestryAdjustment(
                            results.getString("stat"), results.getInt("amount"), results.getString("source")));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load the ancestries", e);
        }
        Map<ProfileKey, StoredAncestry> loaded = new LinkedHashMap<>();
        ancestries.forEach((key, ancestry) -> loaded.put(key,
                new StoredAncestry(ancestry, pending.get(key), adjustments.getOrDefault(key, List.of()))));
        return loaded;
    }

    @Override
    public void save(ProfileKey character, StoredAncestry ancestry) {
        inTransaction("save the ancestry of " + character, connection -> {
            delete(connection, DELETE_ADJUSTMENTS, character);
            try (PreparedStatement statement = connection.prepareStatement(SAVE_ANCESTRY)) {
                bindCharacter(statement, character);
                statement.setString(3, ancestry.ancestry());
                statement.setBoolean(4, ancestry.choicesPending());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(SAVE_ADJUSTMENT)) {
                int position = 0;
                for (StoredAncestryAdjustment adjustment : ancestry.adjustments()) {
                    bindCharacter(statement, character);
                    statement.setInt(3, position++);
                    statement.setString(4, adjustment.stat());
                    statement.setInt(5, adjustment.amount());
                    statement.setString(6, adjustment.source());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void delete(ProfileKey character) {
        inTransaction("delete the ancestry of " + character, connection -> {
            delete(connection, DELETE_ADJUSTMENTS, character);
            delete(connection, DELETE_ANCESTRY, character);
        });
    }

    private interface Work {
        void run(Connection connection) throws SQLException;
    }

    private void inTransaction(String what, Work work) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to " + what, e);
        }
    }

    private static void delete(Connection connection, String sql, ProfileKey character) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCharacter(statement, character);
            statement.executeUpdate();
        }
    }

    private static void bindCharacter(PreparedStatement statement, ProfileKey character) throws SQLException {
        statement.setString(1, character.player().toString());
        statement.setString(2, character.profile());
    }

    private static ProfileKey key(ResultSet results) throws SQLException {
        return new ProfileKey(UUID.fromString(results.getString("uuid")), results.getString("profile"));
    }
}
