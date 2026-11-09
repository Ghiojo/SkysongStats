package org.skysongdev.skysongstats.importer;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

// /ssimport chattweaks: copies SkysongChatTweaks' conditions, injuries and who
// knew which injury into SkysongStats. It only reads SkysongChatTweaks' files and
// database. In order:
//   1. reads <chattweaks folder>/config.yml;
//   2. adds the conditions and injuries conditions.yml and injuries.yml don't have
//      yet, saves both files and reloads them (a file with mistakes stops here);
//   3. reads skysongchat_profiles off the main thread;
//   4. back on the main thread, teaches every character the injuries its nodes
//      stand for, and reports. The full list goes to the console.
// Only one import runs at a time. Running it again is safe: nothing is added twice.
public final class ChatTweaksImporter {
    private static final String KEYS = "import.chattweaks.";

    @FunctionalInterface
    public interface Connector {
        Connection open(DatabaseSettings settings) throws SQLException;
    }

    // Where the import writes: the two definition files, how to reload them (the
    // files that had mistakes come back), the injuries service, and which
    // characters SkysongStats has.
    public record Targets(File conditionsFile, File injuriesFile, Supplier<List<String>> reload,
                          AfflictionService afflictions, Predicate<ProfileKey> characterExists) {
        public Targets {
            Objects.requireNonNull(conditionsFile, "conditionsFile");
            Objects.requireNonNull(injuriesFile, "injuriesFile");
            Objects.requireNonNull(reload, "reload");
            Objects.requireNonNull(afflictions, "afflictions");
            Objects.requireNonNull(characterExists, "characterExists");
        }
    }

    // async reads the database; main does everything else.
    public record Threads(Executor async, Executor main) {
        public Threads {
            Objects.requireNonNull(async, "async");
            Objects.requireNonNull(main, "main");
        }
    }

    private final Supplier<File> folder;
    private final Targets targets;
    private final Connector connector;
    private final Threads threads;
    private final Supplier<Messages> messages;
    private final Logger logger;
    private final DefinitionMerger merger;
    private final AtomicBoolean running = new AtomicBoolean();

    // folder is SkysongChatTweaks' plugin folder, read again on every run.
    public ChatTweaksImporter(Supplier<File> folder, Targets targets, Connector connector, Threads threads,
                              Supplier<Messages> messages, Logger logger, Predicate<String> isItemMaterial) {
        this.folder = Objects.requireNonNull(folder, "folder");
        this.targets = Objects.requireNonNull(targets, "targets");
        this.connector = Objects.requireNonNull(connector, "connector");
        this.threads = Objects.requireNonNull(threads, "threads");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.merger = new DefinitionMerger(isItemMaterial);
    }

    public boolean isRunning() {
        return running.get();
    }

    // The report reaches the sender when the database has been read.
    public void start(CommandSender sender) {
        if (!running.compareAndSet(false, true)) {
            send(sender, "already-running", Map.of());
            return;
        }
        boolean waitingForDatabase = false;
        try {
            waitingForDatabase = begin(sender);
        } finally {
            if (!waitingForDatabase) {
                running.set(false);
            }
        }
    }

    // True when the database read was started, which ends the run later.
    private boolean begin(CommandSender sender) {
        File file = new File(folder.get(), ChatTweaksConfig.FILE_NAME);
        Map<String, String> path = Map.of("path", file.getPath());
        if (!file.isFile()) {
            send(sender, "not-found", path);
            return false;
        }
        ChatTweaksConfig config;
        try {
            config = ChatTweaksConfig.read(load(file));
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.WARNING, "Could not read " + file, e);
            send(sender, "read-failed", path);
            return false;
        }
        send(sender, "started", path);
        Definitions definitions = importDefinitions(sender, config);
        if (definitions == null) {
            return false;
        }
        return config.database().map(settings -> {
            readDatabase(sender, definitions, config, settings);
            return true;
        }).orElseGet(() -> {
            definitions.report(this, sender);
            send(sender, "no-database", path);
            return false;
        });
    }

    // What step 2 added and skipped, or null when it stopped the import (the
    // sender has been told why).
    private Definitions importDefinitions(CommandSender sender, ChatTweaksConfig config) {
        DefinitionMerger.Result conditions;
        DefinitionMerger.Result injuries;
        try {
            conditions = merge(targets.conditionsFile(), root -> merger.mergeConditions(root, config.conditions()));
            injuries = merge(targets.injuriesFile(), root -> merger.mergeInjuries(root, config.injuries()));
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.WARNING, "/ssimport chattweaks could not update the definition files", e);
            send(sender, "save-failed", Map.of());
            return null;
        }
        logDefinitions("condition", conditions);
        logDefinitions("injury", injuries);
        List<String> notReloaded = targets.reload().get();
        if (!notReloaded.isEmpty()) {
            send(sender, "not-reloaded", Map.of("files", String.join(", ", notReloaded)));
            return null;
        }
        return new Definitions(conditions, injuries);
    }

    // Saves the file only when something was added, so a second run leaves it as it is.
    private static DefinitionMerger.Result merge(File file, Function<YamlConfiguration, DefinitionMerger.Result> merge)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration root = file.isFile() ? load(file) : new YamlConfiguration();
        DefinitionMerger.Result result = merge.apply(root);
        if (!result.added().isEmpty()) {
            root.save(file);
        }
        return result;
    }

    private void readDatabase(CommandSender sender, Definitions definitions, ChatTweaksConfig config,
                              DatabaseSettings settings) {
        threads.async().execute(() -> {
            Runnable next = () -> databaseFailed(sender, definitions);
            try {
                next = readRows(sender, definitions, config, settings);
            } finally {
                threads.main().execute(next);
            }
        });
    }

    // Off the main thread: what to run on it next.
    private Runnable readRows(CommandSender sender, Definitions definitions, ChatTweaksConfig config,
                              DatabaseSettings settings) {
        try (Connection connection = connector.open(settings)) {
            List<ChatTweaksProfiles.Row> rows = ChatTweaksProfiles.read(connection);
            return () -> finish(sender, definitions, config, rows);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Could not read " + ChatTweaksProfiles.TABLE + " from " + settings, e);
            return () -> databaseFailed(sender, definitions);
        }
    }

    private void databaseFailed(CommandSender sender, Definitions definitions) {
        try {
            definitions.report(this, sender);
            send(sender, "database-failed", Map.of());
        } finally {
            running.set(false);
        }
    }

    private void finish(CommandSender sender, Definitions definitions, ChatTweaksConfig config,
                        List<ChatTweaksProfiles.Row> rows) {
        try {
            KnownInjuryImport.Result known = KnownInjuryImport.run(rows, injuriesByNode(config.injuries()),
                    targets.characterExists(), targets.afflictions());
            known.console().forEach(line -> logger.info("[/ssimport chattweaks] " + line));
            definitions.report(this, sender);
            send(sender, "report-known", Map.of(
                    "characters", String.valueOf(known.characters()),
                    "learnt", String.valueOf(known.learnt()),
                    "already_known", String.valueOf(known.alreadyKnown()),
                    "unknown_profiles", String.valueOf(known.unknownProfiles()),
                    "unreadable_rows", String.valueOf(known.unreadableRows()),
                    "unmatched_nodes", String.valueOf(known.unmatchedNodes())));
        } finally {
            running.set(false);
        }
    }

    // Permission node, lowercase as LuckPerms keeps them -> the injuries listing it.
    static Map<String, List<String>> injuriesByNode(List<ChatTweaksConfig.Injury> injuries) {
        Map<String, List<String>> byNode = new LinkedHashMap<>();
        for (ChatTweaksConfig.Injury injury : injuries) {
            for (String node : injury.permissions()) {
                List<String> holders = byNode.computeIfAbsent(node.toLowerCase(Locale.ROOT), key -> new ArrayList<>());
                if (!holders.contains(injury.id())) {
                    holders.add(injury.id());
                }
            }
        }
        return byNode;
    }

    private void logDefinitions(String kind, DefinitionMerger.Result result) {
        result.added().forEach(id -> logger.info("[/ssimport chattweaks] Added " + kind + " " + id));
        result.skipped().forEach((id, why) ->
                logger.info("[/ssimport chattweaks] Skipped " + kind + " " + id + ": " + why));
    }

    private static YamlConfiguration load(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        return yaml;
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        messages.get().send(sender, KEYS + key, placeholders);
    }

    private record Definitions(DefinitionMerger.Result conditions, DefinitionMerger.Result injuries) {
        void report(ChatTweaksImporter importer, CommandSender sender) {
            importer.send(sender, "report-definitions", Map.of(
                    "conditions_added", String.valueOf(conditions.added().size()),
                    "conditions_skipped", String.valueOf(conditions.skipped().size()),
                    "injuries_added", String.valueOf(injuries.added().size()),
                    "injuries_skipped", String.valueOf(injuries.skipped().size())));
        }
    }
}
