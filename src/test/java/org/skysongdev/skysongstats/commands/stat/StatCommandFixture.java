package org.skysongdev.skysongstats.commands.stat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.definition.StatsConfigLoader;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StoredModifier;
import org.skysongdev.skysongstats.storage.StoredStats;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What the stat command tests share: a MockBukkit server, the default stats.yml,
 * a StatService that saves nothing, and a message sink that records every key
 * sent. Alice has a character; Bob is online without one.
 */
final class StatCommandFixture {
    record Sent(String to, String key, Map<String, String> placeholders) {
    }

    // Records messages instead of sending them. Only the keys in "known" count as
    // present in messages.yml.
    static final class RecordingSink implements MessageSink {
        final List<Sent> sent = new ArrayList<>();
        final Set<String> known = new HashSet<>();

        @Override
        public void send(CommandSender to, String key, Map<String, String> placeholders) {
            sent.add(new Sent(to.getName(), key, placeholders));
        }

        @Override
        public boolean has(String key) {
            return known.contains(key);
        }

        List<String> keys() {
            return sent.stream().map(Sent::key).toList();
        }
    }

    private static final StatRepository NO_STORAGE = new StatRepository() {
        @Override
        public void createTables() {
        }

        @Override
        public Map<ProfileKey, StoredStats> loadAll() {
            return Map.of();
        }

        @Override
        public void saveValue(ProfileKey character, String statId, int value) {
        }

        @Override
        public void saveModifier(ProfileKey character, StoredModifier modifier) {
        }

        @Override
        public void deleteModifier(String modifierId) {
        }

        @Override
        public void deleteCharacter(ProfileKey character) {
        }
    };

    final ServerMock server;
    final Plugin plugin;
    final StatService stats;
    final RecordingSink messages = new RecordingSink();
    final StatActionRunner runner;
    final PlayerMock alice;
    final PlayerMock bob;

    StatCommandFixture() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        stats = new StatService(defaultStats(), NO_STORAGE, change -> { });
        alice = server.addPlayer("Alice");
        bob = server.addPlayer("Bob");
        stats.createCharacter(character(alice));
        runner = new StatActionRunner(stats,
                player -> player.equals(bob) ? Optional.empty() : Optional.of(character(player)),
                Bukkit::getPlayerExact, messages);
    }

    static StatDefinitions defaultStats() {
        try (InputStream in = StatCommandFixture.class.getClassLoader().getResourceAsStream("stats.yml");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return StatsConfigLoader.load(YamlConfiguration.loadConfiguration(reader), name -> true).stats();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static ProfileKey character(Player player) {
        return new ProfileKey(player.getUniqueId(), "main");
    }

    void allow(Player player, String... permissions) {
        for (String permission : permissions) {
            player.addAttachment(plugin, permission, true);
        }
    }

    int value(Player player, String stat) {
        return stats.value(character(player), stat).orElseThrow();
    }

    List<String> onlineNames() {
        return server.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    void close() {
        MockBukkit.unmock();
    }
}
