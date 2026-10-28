package org.skysongdev.skysongstats.service.downtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores downtime in the three tables the plugin already has:
 * {@code skysong_downtime}, {@code skysong_character_downtime} and
 * {@code skysong_downtime_preferences}. The tables and their columns are
 * unchanged; {@link org.skysongdev.skysongstats.database.Database} still
 * creates them.
 *
 * The first two tables have no unique key, so a save updates the existing
 * row and inserts one only when there is none.
 */
public class SqlDowntimeRepository implements DowntimeRepository {

    /** Gives the open connection, like {@code Database::getConnection}. */
    @FunctionalInterface
    public interface ConnectionSource {
        Connection get() throws SQLException;
    }

    private static final Logger LOGGER = Logger.getLogger("SkysongStats");

    private final ConnectionSource connections;

    public SqlDowntimeRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public DowntimeSnapshot loadAll() {
        Map<UUID, Integer> globals = new HashMap<>();
        Map<CharacterKey, Integer> characters = new HashMap<>();
        Map<UUID, Boolean> spendsGlobal = new HashMap<>();
        try {
            Connection connection = connections.get();
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, current_downtime FROM skysong_downtime");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    UUID player = parse(results.getString("uuid"));
                    if (player != null) {
                        globals.put(player, results.getInt("current_downtime"));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, profile, current_downtime FROM skysong_character_downtime");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    UUID player = parse(results.getString("uuid"));
                    String profile = results.getString("profile");
                    if (player != null && profile != null) {
                        characters.put(new CharacterKey(player, profile), results.getInt("current_downtime"));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, isGlobal FROM skysong_downtime_preferences");
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    UUID player = parse(results.getString("uuid"));
                    if (player != null) {
                        spendsGlobal.put(player, results.getBoolean("isGlobal"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to load downtime", e);
        }
        return new DowntimeSnapshot(globals, characters, spendsGlobal);
    }

    @Override
    public void saveGlobal(UUID player, int value) {
        try {
            writeGlobal(connections.get(), player, value);
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to save global downtime for " + player, e);
        }
    }

    @Override
    public void saveCharacter(CharacterKey key, int value) {
        try {
            writeCharacter(connections.get(), key, value);
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to save character downtime for " + key, e);
        }
    }

    @Override
    public void deleteCharacter(CharacterKey key) {
        try (PreparedStatement statement = connections.get().prepareStatement("DELETE FROM skysong_character_downtime WHERE uuid = ? AND profile = ?")) {
            statement.setString(1, key.player().toString());
            statement.setString(2, key.profile());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to delete character downtime for " + key, e);
        }
    }

    @Override
    public void savePreference(UUID player, boolean spendsGlobal) {
        try {
            Connection connection = connections.get();
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("UPDATE skysong_downtime_preferences SET isGlobal = ? WHERE uuid = ?")) {
                statement.setBoolean(1, spendsGlobal);
                statement.setString(2, player.toString());
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO skysong_downtime_preferences(uuid, isGlobal) VALUES (?, ?)")) {
                    statement.setString(1, player.toString());
                    statement.setBoolean(2, spendsGlobal);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to save downtime preference for " + player, e);
        }
    }

    /** Saves every value in one transaction, so a failure leaves the tables as they were. */
    @Override
    public void saveAll(Map<UUID, Integer> globals, Map<CharacterKey, Integer> characters) {
        try {
            Connection connection = connections.get();
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (Map.Entry<UUID, Integer> entry : globals.entrySet()) {
                    writeGlobal(connection, entry.getKey(), entry.getValue());
                }
                for (Map.Entry<CharacterKey, Integer> entry : characters.entrySet()) {
                    writeCharacter(connection, entry.getKey(), entry.getValue());
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DowntimeStorageException("Unable to save downtime reset", e);
        }
    }

    private static void writeGlobal(Connection connection, UUID player, int value) throws SQLException {
        int updated;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE skysong_downtime SET current_downtime = ? WHERE uuid = ?")) {
            statement.setInt(1, value);
            statement.setString(2, player.toString());
            updated = statement.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO skysong_downtime(uuid, current_downtime) VALUES (?, ?)")) {
                statement.setString(1, player.toString());
                statement.setInt(2, value);
                statement.executeUpdate();
            }
        }
    }

    private static void writeCharacter(Connection connection, CharacterKey key, int value) throws SQLException {
        int updated;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE skysong_character_downtime SET current_downtime = ? WHERE uuid = ? AND profile = ?")) {
            statement.setInt(1, value);
            statement.setString(2, key.player().toString());
            statement.setString(3, key.profile());
            updated = statement.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO skysong_character_downtime(uuid, profile, current_downtime) VALUES (?, ?, ?)")) {
                statement.setString(1, key.player().toString());
                statement.setString(2, key.profile());
                statement.setInt(3, value);
                statement.executeUpdate();
            }
        }
    }

    /** Rows with a malformed UUID are skipped with a warning instead of stopping the whole load. */
    private static UUID parse(String uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "[SkysongStats] Skipping downtime row with invalid UUID: {0}", uuid);
            return null;
        }
    }
}
