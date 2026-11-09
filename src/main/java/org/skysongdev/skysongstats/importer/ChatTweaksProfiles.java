package org.skysongdev.skysongstats.importer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Reads SkysongChatTweaks' skysongchat_profiles table: one row per character,
// with the injury permission nodes it was given joined by ';'. Only reads.
final class ChatTweaksProfiles {
    static final String TABLE = "skysongchat_profiles";

    // uuid and profile as stored, which may be anything; the import checks them.
    record Row(String uuid, String profile, List<String> nodes) {
        Row {
            nodes = List.copyOf(nodes);
        }
    }

    private ChatTweaksProfiles() {
    }

    static List<Row> read(Connection connection) throws SQLException {
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uuid, profile, permissions FROM " + TABLE + " ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new Row(result.getString("uuid"), result.getString("profile"),
                        nodes(result.getString("permissions"))));
            }
        }
        return rows;
    }

    // SkysongChatTweaks joins the nodes with ';' and saves "" for none.
    static List<String> nodes(String permissions) {
        if (permissions == null) {
            return List.of();
        }
        return Arrays.stream(permissions.split(";"))
                .map(String::trim)
                .filter(node -> !node.isEmpty())
                .toList();
    }
}
