package org.skysongdev.skysongstats;

import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.skysongdev.skysongstats.api.CharacterId;
import org.skysongdev.skysongstats.api.event.ConditionAwardEvent;
import org.skysongdev.skysongstats.api.event.ConditionRemoveEvent;
import org.skysongdev.skysongstats.api.event.InjuryAwardEvent;
import org.skysongdev.skysongstats.api.event.InjuryDiscoverEvent;
import org.skysongdev.skysongstats.api.event.InjuryRemoveEvent;
import org.skysongdev.skysongstats.apiimpl.AfflictionApiTypes;
import org.skysongdev.skysongstats.character.ActiveCondition;
import org.skysongdev.skysongstats.character.ConditionChange;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.messages.AwardAnnouncers;
import org.skysongdev.skysongstats.storage.LearnSource;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import java.util.Map;
import java.util.Optional;
import org.skysongdev.skysongstats.character.AfflictionChangeListener;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.InjuryAward;
import org.skysongdev.skysongstats.commands.affliction.ConditionCommand;
import org.skysongdev.skysongstats.commands.ancestry.AncestryCommand;
import org.skysongdev.skysongstats.commands.ancestry.AncestryPrompt;
import org.skysongdev.skysongstats.commands.affliction.ConditionMenu;
import org.skysongdev.skysongstats.commands.affliction.InjuryMenu;
import org.skysongdev.skysongstats.menu.AfflictionMenus;
import org.skysongdev.skysongstats.menu.ListMenuListener;
import org.skysongdev.skysongstats.commands.affliction.InjuryCommand;
import org.skysongdev.skysongstats.messages.AfflictionLists;
import org.skysongdev.skysongstats.messages.AfflictionSheetTags;
import org.skysongdev.skysongstats.messages.AncestryPlaceholders;
import org.skysongdev.skysongstats.messages.AncestrySheetTags;
import org.skysongdev.skysongstats.messages.SheetTags;
import org.skysongdev.skysongstats.messages.AfflictionTexts;
import org.skysongdev.skysongstats.storage.AfflictionRepository;
import org.skysongdev.skysongstats.storage.AncestryRepository;
import org.skysongdev.skysongstats.character.AncestryService;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.api.event.ChangeCause;
import org.skysongdev.skysongstats.api.event.DowntimeChangeEvent;
import org.skysongdev.skysongstats.api.event.ProficiencyFocusChangeEvent;
import org.skysongdev.skysongstats.api.event.ProficiencyTierChangeEvent;
import org.skysongdev.skysongstats.api.event.SkillChangeEvent;
import org.skysongdev.skysongstats.api.event.StatChangeEvent;
import org.skysongdev.skysongstats.api.event.SkysongStatsReloadEvent;
import org.skysongdev.skysongstats.apiimpl.ApiTypes;
import org.skysongdev.skysongstats.apiimpl.SkysongStatsApiImpl;
import org.skysongdev.skysongstats.character.ChangeCauses;
import org.skysongdev.skysongstats.character.PluginCharacterService;
import org.skysongdev.skysongstats.character.ProficiencyChangeListener;
import org.skysongdev.skysongstats.character.ProficiencyFocusChange;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.ProficiencyTierChange;
import org.skysongdev.skysongstats.character.SkillChange;
import org.skysongdev.skysongstats.character.SkillService;
import org.skysongdev.skysongstats.character.StatChange;
import org.skysongdev.skysongstats.character.StatChangeListener;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.commands.character.CharacterCommand;
import org.skysongdev.skysongstats.commands.downtime.DowntimeCommand;
import org.skysongdev.skysongstats.commands.misc.ImportCommand;
import org.skysongdev.skysongstats.commands.misc.ReloadCommand;
import org.skysongdev.skysongstats.importer.ChatTweaksImporter;
import org.skysongdev.skysongstats.definition.DefinitionFile;
import org.skysongdev.skysongstats.commands.profile.ProfileCommand;
import org.skysongdev.skysongstats.commands.setup.SetupCommand;
import org.skysongdev.skysongstats.commands.skills.SkillBook;
import org.skysongdev.skysongstats.commands.proficiency.PendingFocusChanges;
import org.skysongdev.skysongstats.commands.proficiency.ProficiencyCommand;
import org.skysongdev.skysongstats.commands.proficiency.ProficiencySheet;
import org.skysongdev.skysongstats.commands.skills.SkillsCommand;
import org.skysongdev.skysongstats.commands.stat.CommandTargets;
import org.skysongdev.skysongstats.commands.stat.StatActionRunner;
import org.skysongdev.skysongstats.commands.stat.StatAliasCommand;
import org.skysongdev.skysongstats.commands.stat.StatCommandRegistry;
import org.skysongdev.skysongstats.commands.stat.StatModifierCommand;
import org.skysongdev.skysongstats.commands.stat.StatPermissionRegistry;
import org.skysongdev.skysongstats.commands.stat.StatSheet;
import org.skysongdev.skysongstats.commands.stat.StatsRootCommand;
import org.skysongdev.skysongstats.database.Database;
import org.skysongdev.skysongstats.database.Pinger;
import org.skysongdev.skysongstats.database.SetupProfile;
import org.skysongdev.skysongstats.definition.ConditionDefinitions;
import org.skysongdev.skysongstats.definition.ConditionsConfigFile;
import org.skysongdev.skysongstats.definition.DefinitionException;
import org.skysongdev.skysongstats.definition.AncestriesConfigFile;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.AncestryDefinitions;
import org.skysongdev.skysongstats.definition.StatAction;
import org.skysongdev.skysongstats.definition.InjuriesConfigFile;
import org.skysongdev.skysongstats.definition.InjuryDefinitions;
import org.skysongdev.skysongstats.definition.ProficienciesConfig;
import org.skysongdev.skysongstats.definition.ProficienciesConfigFile;
import org.skysongdev.skysongstats.definition.SkillDefinitions;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.definition.SkillsConfigFile;
import org.skysongdev.skysongstats.definition.StatsConfig;
import org.skysongdev.skysongstats.definition.StatsConfigFile;
import org.skysongdev.skysongstats.height.HeightScaler;
import org.skysongdev.skysongstats.items.ItemSourceRegistry;
import org.skysongdev.skysongstats.items.ItemTagListener;
import org.skysongdev.skysongstats.items.ItemTagReader;
import org.skysongdev.skysongstats.items.SkysongItemsTagReader;
import org.skysongdev.skysongstats.listeners.*;
import org.skysongdev.skysongstats.messages.ExternalPlaceholders;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.MessagesFile;
import org.skysongdev.skysongstats.messages.PluginTexts;
import org.skysongdev.skysongstats.placeholders.PlaceholderApiPlaceholders;
import org.skysongdev.skysongstats.placeholders.StatsExpansion;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;
import org.skysongdev.skysongstats.service.downtime.DowntimeSettings;
import org.skysongdev.skysongstats.service.downtime.DowntimeStorageException;
import org.skysongdev.skysongstats.service.downtime.SqlDowntimeRepository;
import org.skysongdev.skysongstats.proficiency.ProficiencyPermissionRegistry;
import org.skysongdev.skysongstats.proficiency.ProficiencyTierPermissions;
import org.skysongdev.skysongstats.proficiency.ProficiencyTierSync;
import org.skysongdev.skysongstats.setup.SetupScreenListener;
import org.skysongdev.skysongstats.skill.LuckPermsNodes;
import org.skysongdev.skysongstats.skill.PlayerNodes;
import org.skysongdev.skysongstats.skill.SkillLevelPermissions;
import org.skysongdev.skysongstats.skill.SkillLevelSync;
import org.skysongdev.skysongstats.skill.SkillPermissionRegistry;
import org.skysongdev.skysongstats.storage.CharacterStorage;
import org.skysongdev.skysongstats.storage.DatabaseSettings;
import org.skysongdev.skysongstats.storage.PluginCharacterRepository;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.ProficiencyRepository;
import org.skysongdev.skysongstats.storage.SkillRepository;
import org.skysongdev.skysongstats.storage.StatRepository;
import org.skysongdev.skysongstats.storage.StatStorage;
import org.skysongdev.skysongstats.storage.StorageException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

public class SkysongStats extends JavaPlugin {
    private static SkysongStats plugin;
    public static SkysongStats getPlugin(){
        if(plugin == null){
            plugin = new SkysongStats();
        }
        return plugin;
    }
    private Utils utils;
    public Utils getUtils() { return utils; }
    private Database database;
    private DowntimeService downtimeService;
    private Messages messages;
    private StatsConfig statsConfig;
    // The conditions and injuries of conditions.yml and injuries.yml. Conditions
    // load first: injuries name them.
    private ConditionDefinitions conditionDefinitions;
    private InjuryDefinitions injuryDefinitions;
    // The ancestries of ancestries.yml, picked at the start of /setup. They adjust
    // the /setup stats, so they load after stats.yml.
    private AncestryDefinitions ancestryDefinitions;
    private CharacterStorage storage;
    private StatService statService;
    private SkillService skillService;
    private SkillLevelPermissions skillLevels;
    private SkillLevelSync skillLevelSync;
    private SkillPermissionRegistry skillPermissions;
    private ProficiencyService proficiencyService;
    private ProficiencyTierPermissions proficiencyTiers;
    private ProficiencyTierSync proficiencyTierSync;
    private ProficiencyPermissionRegistry proficiencyPermissions;
    private AfflictionService afflictionService;
    // Characters other plugins own, such as NPCs (API characters()).
    private PluginCharacterService pluginCharacterService;
    private AncestryService ancestryService;
    private AncestryPrompt ancestryPrompt;
    private final AfflictionTexts afflictionTexts = new AfflictionTexts(this::getMessages, this::characterName);
    // A character's injuries and conditions as text, for placeholders and sheets.
    private final AfflictionLists afflictionLists = new AfflictionLists(this::getAfflictionService, afflictionTexts,
            this::getMessages);
    private final AncestryPlaceholders ancestryPlaceholders = new AncestryPlaceholders(this::getAncestryService,
            () -> statsConfig == null ? null : statsConfig.stats(), this::getMessages);
    // Skill level and proficiency tier nodes, in LuckPerms.
    private PlayerNodes playerNodes;

    // messages.yml for the stat commands and the /setup screen.
    private final PluginTexts texts = new PluginTexts(this::getMessages);
    private final PendingFocusChanges pendingFocusChanges = new PendingFocusChanges(System::currentTimeMillis);
    private StatCommandRegistry statCommands;
    private StatPermissionRegistry statPermissions;

    public ProfileCommand profileCommand = new ProfileCommand();
    public SetupCommand setupCommand = new SetupCommand(texts);
    public CharacterCommand characterCommand = new CharacterCommand();
    public DowntimeCommand downtimeCommand;


    public Database getDatabase() { return database; }
    public DowntimeService getDowntimeService() { return downtimeService; }
    public Messages getMessages() { return messages; }
    public StatsConfig getStatsConfig() { return statsConfig; }
    public ConditionDefinitions getConditionDefinitions() { return conditionDefinitions; }
    public InjuryDefinitions getInjuryDefinitions() { return injuryDefinitions; }
    public AncestryDefinitions getAncestryDefinitions() { return ancestryDefinitions; }
    public StatService getStatService() { return statService; }
    public SkillService getSkillService() { return skillService; }
    public ProficiencyService getProficiencyService() { return proficiencyService; }
    public AfflictionService getAfflictionService() { return afflictionService; }
    public PluginCharacterService getPluginCharacterService() { return pluginCharacterService; }
    public AncestryService getAncestryService() { return ancestryService; }
    public AncestryPrompt getAncestryPrompt() { return ancestryPrompt; }
    public AfflictionTexts getAfflictionTexts() { return afflictionTexts; }
    public AfflictionLists getAfflictionLists() { return afflictionLists; }
    public AncestryPlaceholders getAncestryPlaceholders() { return ancestryPlaceholders; }
    // Plugins that say injury award messages themselves (API announceAwards).
    private final AwardAnnouncers awardAnnouncers = new AwardAnnouncers();
    public AwardAnnouncers getAwardAnnouncers() { return awardAnnouncers; }

    // The public API offered to other plugins, null until onEnable finished.
    private SkysongStatsApiImpl api;
    // What the change being made now is caused by, for the change events.
    private final ChangeCauses changeCauses = new ChangeCauses();
    public ChangeCauses getChangeCauses() { return changeCauses; }
    // Other plugins' containers counted with the inventory for item tags (API item sources).
    private ItemSourceRegistry itemSourceRegistry;
    public ItemSourceRegistry getItemSourceRegistry() { return itemSourceRegistry; }
    private ItemTagListener itemTagListener;
    public ItemTagListener getItemTagListener() { return itemTagListener; }
    // Keeps players' Minecraft scale on their character's height.
    private HeightScaler heightScaler;

    @Override
    public void onEnable() {
        plugin = this;
        utils = new Utils(plugin);

        saveDefaultConfig();
        loadMessages();
        try {
            statsConfig = loadStatsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix stats.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            ancestryDefinitions = loadAncestriesConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix ancestries.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SkillsConfig skillsConfig;
        try {
            skillsConfig = loadSkillsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix skills.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ProficienciesConfig proficienciesConfig;
        try {
            proficienciesConfig = loadProficienciesConfig(skillsConfig.skills());
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix proficiencies.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            conditionDefinitions = loadConditionsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix conditions.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            injuryDefinitions = loadInjuriesConfig(conditionDefinitions);
        } catch (DefinitionException e) {
            getLogger().log(Level.SEVERE, "Fix injuries.yml and restart the server. SkysongStats is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!InitializeDatabase()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        InitializeDowntime();
        // The stats come before the commands, listeners and placeholders that use them.
        InitializeStats();
        playerNodes = luckPermsNodes();
        InitializeSkills(skillsConfig);
        InitializeProficiencies(proficienciesConfig);
        InitializeAfflictions();
        InitializePluginCharacters();
        InitializeAncestries();

        InitializeCommands();
        AssignListeners();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new StatsExpansion(this).register();
        }


        super.onEnable();

        Bukkit.getScheduler().runTaskTimer(this, new Pinger(), 0L, 6000L);

        // Last, so other plugins only get the API once everything behind it is running.
        api = new SkysongStatsApiImpl(this);
        api.register(getServer().getServicesManager(), this);

    }

    public void InitializeCommands(){
        Supplier<List<String>> onlinePlayers = () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        CommandTargets targets = new CommandTargets(statService, utils.getProfileManager()::activeCharacter,
                Bukkit::getPlayer, texts);
        StatActionRunner runner = new StatActionRunner(statService, targets, texts);
        StatsRootCommand statsCommand = new StatsRootCommand(statService, runner, targets,
                new StatSheet(statService, texts), texts, onlinePlayers);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("sstats").setExecutor(statsCommand);
        getCommand("modifier").setExecutor(new StatModifierCommand(statService, targets, texts, onlinePlayers));
        // The per-stat names from stats.yml (/heal, /damage, /temphp...).
        statCommands = new StatCommandRegistry(getServer().getCommandMap(), getName().toLowerCase(Locale.ROOT),
                label -> new StatAliasCommand(label, statService, runner, texts, onlinePlayers));
        statPermissions = new StatPermissionRegistry(getServer().getPluginManager());
        registerStatCommands();

        getCommand("profile").setExecutor(profileCommand);
        getCommand("setup").setExecutor(setupCommand);
        getCommand("skills").setExecutor(new SkillsCommand(skillService, targets, texts, new SkillBook(this::getMessages),
                onlinePlayers, getLogger()::info));
        getCommand("proficiency").setExecutor(new ProficiencyCommand(proficiencyService, skillService, targets, texts,
                new ProficiencySheet(this::getMessages), pendingFocusChanges, onlinePlayers, getLogger()::info, changeCauses));
        AfflictionMenus afflictionMenus = new AfflictionMenus(this::getMessages);
        getCommand("injury").setExecutor(new InjuryCommand(afflictionService,
                new InjuryMenu(afflictionService, targets, this::getMessages, afflictionMenus), targets,
                this::getMessages, afflictionTexts, onlinePlayers, getLogger()::info, changeCauses));
        getCommand("condition").setExecutor(new ConditionCommand(afflictionService,
                new ConditionMenu(afflictionService, this::getMessages, afflictionMenus), targets,
                this::getMessages, afflictionTexts, onlinePlayers, getLogger()::info, changeCauses));
        getCommand("ancestry").setExecutor(new AncestryCommand(ancestryService, ancestryPrompt, ancestryLookup(),
                this::getMessages, onlinePlayers, getLogger()::info, changeCauses));
        getCommand("ssimport").setExecutor(new ImportCommand(chatTweaksImporter(), this::getMessages));
        getCommand("ssreload").setExecutor(new ReloadCommand());
        getCommand("schar").setExecutor(characterCommand);
        getCommand("char").setExecutor(characterCommand);
        downtimeCommand = new DowntimeCommand(downtimeService);
        getCommand("downtime").setExecutor(downtimeCommand);
    }

    // /ssimport chattweaks. Its folder comes from config.yml on every run, relative
    // to the server folder unless it is absolute.
    private ChatTweaksImporter chatTweaksImporter(){
        File serverFolder = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        Supplier<File> folder = () -> {
            File configured = new File(getConfig().getString("import.chattweaks-folder", "plugins/SkysongChatTweaks"));
            return configured.isAbsolute() ? configured : new File(serverFolder, configured.getPath());
        };
        ChatTweaksImporter.Targets targets = new ChatTweaksImporter.Targets(
                new File(getDataFolder(), ConditionsConfigFile.FILE_NAME),
                new File(getDataFolder(), InjuriesConfigFile.FILE_NAME),
                this::reloadInjuriesAndConditionsFiles, afflictionService,
                character -> statService.sheet(character).isPresent());
        ChatTweaksImporter.Threads threads = new ChatTweaksImporter.Threads(
                task -> Bukkit.getScheduler().runTaskAsynchronously(this, task),
                task -> Bukkit.getScheduler().runTask(this, task));
        return new ChatTweaksImporter(folder, targets,
                settings -> DriverManager.getConnection(settings.jdbcUrl(), settings.user(), settings.password()),
                threads, this::getMessages, getLogger(), DefinitionFile::isItem);
    }

    public void AssignListeners() {
        getServer().getPluginManager().registerEvents(new PlayerRegisterListener(), this);
        getServer().getPluginManager().registerEvents(new SetupScreenListener(), this);
        getServer().getPluginManager().registerEvents(new ListMenuListener(), this);
        getServer().getPluginManager().registerEvents(skillLevelSync, this);
        getServer().getPluginManager().registerEvents(proficiencyTierSync, this);
        getServer().getPluginManager().registerEvents(pendingFocusChanges, this);
        getServer().getPluginManager().registerEvents(new PlayerShiftRightClickListener(), this);
        getServer().getPluginManager().registerEvents(heightScaler, this);
        getServer().getPluginManager().registerEvents(ancestryPrompt, this);
        itemSourceRegistry = new ItemSourceRegistry(getLogger()::warning);
        itemTagListener = new ItemTagListener(statService, utils.getProfileManager()::activeCharacter, itemTagReader(),
                itemSourceRegistry::items);
        getServer().getPluginManager().registerEvents(itemTagListener, this);
    }

    // Skysong_Items is a hard depend, so its API is there unless Skysong_Items failed
    // to start. Then, as with LuckPerms, the rest of the plugin keeps working and item
    // tags add 0.
    private ItemTagReader itemTagReader(){
        try {
            return SkysongItemsTagReader.fromServer();
        } catch (IllegalStateException e) {
            getLogger().severe("Skysong_Items is not running, item tags in stats.yml will add nothing to stats");
            return (items, source) -> 0;
        }
    }

    public void InitializeDowntime(){
        downtimeService = new DowntimeService(
                new SqlDowntimeRepository(() -> database.getConnection()),
                DowntimeSettings.fromConfig(getConfig().getConfigurationSection("downtime")),
                change -> Bukkit.getPluginManager().callEvent(
                        new DowntimeChangeEvent(ApiTypes.downtimeChange(change), changeCauses.current())));
        try {
            downtimeService.load();
        } catch (DowntimeStorageException e) {
            Bukkit.getLogger().warning("[SkysongStats] Unable to load downtime from the database!");
            e.printStackTrace();
        }
    }

    // Registers the command names and permissions of the current stats.yml, and
    // updates the command lists players see.
    private void registerStatCommands(){
        List<String> taken = statCommands.register(statService.definitions());
        taken.forEach(label -> getLogger().log(Level.WARNING,
                "Another plugin already has /{0}, use /skysongstats:{0} for that stat command", label));
        statPermissions.register(statService.definitions());
        Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
    }

    @Override
    public void onDisable() {
        if (api != null) {
            api.unregister(getServer().getServicesManager());
            api = null;
        }
        if (itemSourceRegistry != null) {
            itemSourceRegistry.clear();
        }
        if (heightScaler != null) {
            Bukkit.getOnlinePlayers().forEach(heightScaler::remove);
        }
        if (statCommands != null) {
            statCommands.unregisterAll();
            statPermissions.unregisterAll();
        }
        if (skillPermissions != null) {
            skillPermissions.unregisterAll();
        }
        if (proficiencyPermissions != null) {
            proficiencyPermissions.unregisterAll();
        }
        if (storage != null) {
            storage.close();
        }
    }

    // Opens the stat tables and loads every character's stats. Profiles made
    // before the stat tables existed get their stats at the stats.yml start values.
    public void InitializeStats(){
        StatRepository repository = storage.repository();
        statService = new StatService(statsConfig.stats(), repository, new StatChangeListener() {
            @Override
            public void onChange(StatChange change) {
                Bukkit.getPluginManager().callEvent(new StatChangeEvent(ApiTypes.statChange(change), changeCauses.current()));
            }

            @Override
            public void onTotalsChanged(ProfileKey character) {
                heightScaler.refresh(character);
            }
        });
        heightScaler = new HeightScaler(statService, utils.getProfileManager()::activeCharacter, Bukkit::getPlayer);
        try {
            repository.createTables();
            statService.load();
        } catch (StorageException e) {
            // Creating sheets now could overwrite the saved stats once MySQL is back.
            getLogger().log(Level.SEVERE, "Unable to load the stats from the database", e);
            return;
        }
        addStatsToOlderProfiles();
    }

    // Every profile has a row in skysong_setup. That table is null when the
    // database could not be read, and then there is nothing to add.
    private void addStatsToOlderProfiles(){
        if (ProfileManager.setupProfiles == null) {
            return;
        }
        for (SetupProfile profile : ProfileManager.setupProfiles) {
            statService.ensureCharacter(new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile()));
        }
    }

    // Loads every character's skill XP from the stat database, and keeps the
    // LuckPerms skill level nodes of active characters up to date.
    public void InitializeSkills(SkillsConfig config){
        SkillRepository repository = storage.skillRepository();
        skillService = new SkillService(config, repository, this::onSkillChange);
        skillLevels = new SkillLevelPermissions(playerNodes, skillService::config, utils.getProfileManager()::isActive);
        skillLevelSync = new SkillLevelSync(skillLevels, skillService, utils.getProfileManager()::activeCharacter);
        skillPermissions = new SkillPermissionRegistry(getServer().getPluginManager());
        skillPermissions.register(config);
        try {
            repository.createTables();
            skillService.load();
        } catch (StorageException e) {
            // Creating characters now could overwrite the saved XP once MySQL is back.
            getLogger().log(Level.SEVERE, "Unable to load the skills from the database", e);
            return;
        }
        addSkillsToOlderProfiles();
    }

    private void onSkillChange(SkillChange change){
        skillLevels.onChange(change);
        Bukkit.getPluginManager().callEvent(new SkillChangeEvent(ApiTypes.skillChange(change), changeCauses.current()));
    }

    // Loads every character's proficiencies from the stat database, and keeps
    // the LuckPerms tier nodes of active characters up to date.
    public void InitializeProficiencies(ProficienciesConfig config){
        ProficiencyRepository repository = storage.proficiencyRepository();
        proficiencyService = new ProficiencyService(config, repository, new ProficiencyChangeListener() {
            @Override
            public void onTierChange(ProficiencyTierChange change) {
                proficiencyTiers.onTierChange(change);
                Bukkit.getPluginManager().callEvent(
                        new ProficiencyTierChangeEvent(ApiTypes.tierChange(change), changeCauses.current()));
            }

            @Override
            public void onFocusChange(ProficiencyFocusChange change) {
                Bukkit.getPluginManager().callEvent(new ProficiencyFocusChangeEvent(ApiTypes.character(change.character()),
                        ApiTypes.focus(change.before()), ApiTypes.focus(change.after()), changeCauses.current()));
            }
        }, getLogger()::warning);
        proficiencyTiers = new ProficiencyTierPermissions(playerNodes, proficiencyService::config,
                utils.getProfileManager()::isActive);
        proficiencyTierSync = new ProficiencyTierSync(proficiencyTiers, proficiencyService,
                utils.getProfileManager()::activeCharacter);
        proficiencyPermissions = new ProficiencyPermissionRegistry(getServer().getPluginManager());
        proficiencyPermissions.register(config);
        try {
            repository.createTables();
            proficiencyService.load();
        } catch (StorageException e) {
            // Creating characters now could overwrite the saved tiers once MySQL is back.
            getLogger().log(Level.SEVERE, "Unable to load the proficiencies from the database", e);
            return;
        }
        addProficienciesToOlderProfiles();
    }

    // Loads every character's injuries, conditions and known injuries from the
    // stat database.
    public void InitializeAfflictions(){
        AfflictionRepository repository = storage.afflictionRepository();
        afflictionService = new AfflictionService(conditionDefinitions, injuryDefinitions, repository,
                afflictionEvents(), System::currentTimeMillis);
        try {
            repository.createTables();
            afflictionService.load();
        } catch (StorageException e) {
            getLogger().log(Level.SEVERE, "Unable to load the injuries and conditions from the database", e);
            return;
        }
        afflictionService.characters().forEach(this::applyEffects);
    }

    // Loads which characters belong to other plugins (NPCs). Their stats, skills
    // and the rest were loaded with everyone else's.
    public void InitializePluginCharacters(){
        PluginCharacterRepository repository = storage.pluginCharacterRepository();
        pluginCharacterService = new PluginCharacterService(repository, new PluginCharacterService.CharacterLifecycle() {
            @Override
            public boolean exists(ProfileKey character) {
                return isCharacter(character);
            }

            @Override
            public void create(ProfileKey character, Map<String, Integer> startingValues) {
                createPluginCharacter(character, startingValues);
            }

            @Override
            public void delete(ProfileKey character) {
                deletePluginCharacter(character);
            }
        });
        try {
            repository.createTables();
            pluginCharacterService.load();
        } catch (StorageException e) {
            getLogger().log(Level.SEVERE, "Unable to load the plugin characters from the database", e);
        }
    }

    // Loads every character's ancestry. Its adjustments are already in the saved
    // base values; the service only moves them when the ancestry changes.
    public void InitializeAncestries(){
        AncestryRepository repository = storage.ancestryRepository();
        ancestryService = new AncestryService(ancestryDefinitions, repository, this::adjustBaseValue,
                this::onAncestryChanged);
        ancestryPrompt = ancestryPrompt();
        try {
            repository.createTables();
            ancestryService.load();
        } catch (StorageException e) {
            getLogger().log(Level.SEVERE, "Unable to load the ancestries from the database", e);
        }
    }

    // Tells players whose character has ancestry choices pending; also what
    // /ancestry choose opens.
    private AncestryPrompt ancestryPrompt(){
        return new AncestryPrompt(ancestryService, () -> statsConfig.stats(), texts, this::getMessages,
                new AncestryPrompt.Players(utils.getProfileManager()::activeCharacter, Bukkit::getPlayer),
                task -> Bukkit.getScheduler().runTask(this, task), changeCauses);
    }

    // The player by name, online first, then anyone who joined before; their
    // character is the profile they play now, when it has stats.
    private AncestryCommand.Lookup ancestryLookup(){
        return new AncestryCommand.Lookup(name -> {
            Player online = Bukkit.getPlayerExact(name);
            return Optional.ofNullable(online != null ? online : Bukkit.getOfflinePlayerIfCached(name));
        }, player -> utils.getProfileManager().activeProfile(player)
                .map(profile -> new ProfileKey(player, profile))
                .filter(character -> statService.sheet(character).isPresent()));
    }

    // The character's ancestry field follows its ancestry: the ancestry's name, or
    // back to "[Not Set]" (free text again) when it has none.
    private void onAncestryChanged(AncestryService.AncestryChange change){
        if (ancestryPrompt != null) {
            ancestryPrompt.onAncestryChanged(change);
        }
        ProfileKey character = change.character();
        CharacterProfile profile = CharacterManager.characterProfiles == null ? null
                : utils.getCharacterManager().findCharacterProfile(character.player().toString(), character.profile());
        if (profile == null) {
            return;
        }
        String name = change.after().map(after -> ancestryDefinitions.get(after.ancestry())
                .map(AncestryDefinition::name).orElse(after.ancestry())).orElse(CharacterProfile.NOT_SET);
        profile.setAncestry(name);
        try {
            database.updateCharacterData(profile);
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Could not save the ancestry field of " + character, e);
        }
    }

    // Adds delta to a stat's base value the way /stats add or reduce would, so bars
    // are capped and the stat events fire. A stat that left stats.yml is skipped.
    private void adjustBaseValue(ProfileKey character, String statId, int delta){
        if (delta == 0 || statService.sheet(character).isEmpty()) {
            return;
        }
        statService.definitions().get(statId).ifPresent(stat -> statService.apply(character, stat,
                delta > 0 ? StatAction.ADD : StatAction.REDUCE, Math.abs(delta)));
    }

    // Whether any part of the plugin knows this id as a character already.
    private boolean isCharacter(ProfileKey character){
        return statService.sheet(character).isPresent() || skillService.hasCharacter(character)
                || ProfileManager.setupProfiles != null && utils.getProfileManager()
                .findSetupProfile(character.player().toString(), character.profile()) != null;
    }

    // A plugin character gets what /profile create gives a profile, except the
    // /setup row and downtime: stats (some at the plugin's values), skills,
    // proficiencies and a character sheet.
    private void createPluginCharacter(ProfileKey character, Map<String, Integer> startingValues){
        statService.createCharacter(character, startingValues);
        skillService.createCharacter(character);
        proficiencyService.createCharacter(character);
        utils.getCharacterManager().addCharacter(
                new CharacterProfile(character.player().toString(), character.profile()));
    }

    private void deletePluginCharacter(ProfileKey character){
        CharacterProfile profile = CharacterManager.characterProfiles == null ? null
                : utils.getCharacterManager().findCharacterProfile(character.player().toString(), character.profile());
        if (profile != null) {
            utils.getCharacterManager().deleteCharacterProfile(profile);
        }
        statService.deleteCharacter(character);
        skillService.deleteCharacter(character);
        proficiencyService.deleteCharacter(character);
        afflictionService.deleteCharacter(character);
        ancestryService.deleteCharacter(character);
    }

    // Every player's profile: the characters with stats that no plugin owns.
    public Set<ProfileKey> playerCharacters(){
        Set<ProfileKey> characters = statService.characters();
        return pluginCharacterService == null ? characters : pluginCharacterService.playerCharacters(characters);
    }

    // What the character's injuries and conditions do to its stats.
    private void applyEffects(ProfileKey character){
        statService.setEffects(character, afflictionService.effects(character));
    }

    // Turns injury and condition changes into stat effects, API events and the
    // award message.
    private AfflictionChangeListener afflictionEvents(){
        return new AfflictionChangeListener() {
            @Override
            public void onInjuryAwarded(InjuryAward award) {
                Optional<Component> message = afflictionTexts.announcement(award.character(), award.injury().injury());
                callEvent(new InjuryAwardEvent(ApiTypes.character(award.character()),
                        AfflictionApiTypes.injury(award.injury()), award.isNew(),
                        message.map(AfflictionTexts::miniMessage).orElse(null), changeCauses.current()));
                message.ifPresent(text -> announceInjury(award.character(), text));
            }

            @Override
            public void onInjuryRemoved(ProfileKey character, InjuryDefinition injury) {
                callEvent(new InjuryRemoveEvent(ApiTypes.character(character), injury.id(), changeCauses.current()));
            }

            @Override
            public void onInjuryLearnt(ProfileKey character, InjuryDefinition injury, LearnSource source) {
                if (source != LearnSource.IMPORT) {
                    callEvent(new InjuryDiscoverEvent(ApiTypes.character(character), injury.id(), changeCauses.current()));
                }
            }

            @Override
            public void onConditionsChanged(ProfileKey character, List<ConditionChange> changes) {
                changes.forEach(change -> conditionEvent(character, change));
            }

            @Override
            public void onAfflictionsChanged(ProfileKey character) {
                applyEffects(character);
            }
        };
    }

    // A condition that stopped, or started or changed level; a change of only what
    // gives it fires nothing.
    private void conditionEvent(ProfileKey character, ConditionChange change){
        CharacterId id = ApiTypes.character(character);
        if (change.stopped()) {
            callEvent(new ConditionRemoveEvent(id, change.before().orElseThrow().id(), changeCauses.current()));
        } else if (change.levelChanged()) {
            callEvent(new ConditionAwardEvent(id, AfflictionApiTypes.condition(change.after().orElseThrow()),
                    change.before().map(ActiveCondition::level).orElse(null), changeCauses.current()));
        }
    }

    private static void callEvent(Event event){
        Bukkit.getPluginManager().callEvent(event);
    }

    // The award message goes to the injured player, when they are playing the
    // injured character and no other plugin announces awards.
    private void announceInjury(ProfileKey character, Component message){
        Player player = Bukkit.getPlayer(character.player());
        if (player == null || awardAnnouncers.anyone() || !utils.getProfileManager().isActive(character)) {
            return;
        }
        messages.send(player, "injury.award.announcement", Map.of(), Map.of("message", message));
    }

    // The name on the character's sheet, or the player's name when it has none.
    private String characterName(ProfileKey character){
        CharacterProfile profile = CharacterManager.characterProfiles == null ? null
                : utils.getCharacterManager().findCharacterProfile(character.player().toString(), character.profile());
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        String playerName = Bukkit.getOfflinePlayer(character.player()).getName();
        return playerName == null ? character.profile() : playerName;
    }

    // LuckPerms is a hard dependency, so it is only missing on a test server.
    private PlayerNodes luckPermsNodes(){
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms == null) {
            getLogger().severe("LuckPerms is not running, skill level and proficiency tier nodes will not be given");
            return (player, grant, revoke) -> { };
        }
        return new LuckPermsNodes(luckPerms, failure -> getLogger().log(Level.WARNING,
                "Unable to update a player's skill level or proficiency tier nodes in LuckPerms", failure));
    }

    private void addSkillsToOlderProfiles(){
        if (ProfileManager.setupProfiles == null) {
            return;
        }
        for (SetupProfile profile : ProfileManager.setupProfiles) {
            skillService.ensureCharacter(new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile()));
        }
    }

    private void addProficienciesToOlderProfiles(){
        if (ProfileManager.setupProfiles == null) {
            return;
        }
        for (SetupProfile profile : ProfileManager.setupProfiles) {
            proficiencyService.ensureCharacter(new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile()));
        }
    }

    /**
     * Re-reads config.yml and applies every setting that can change while the
     * server runs. The database connection is only read on startup.
     */
    // Returns the files that had mistakes and were not reloaded, so /ssreload can
    // tell whoever ran it; the details are in the console.
    public List<String> reloadSettings(){
        List<String> notReloaded = changeCauses.during(ChangeCause.RELOAD, this::reloadEverySetting);
        Bukkit.getPluginManager().callEvent(new SkysongStatsReloadEvent());
        return notReloaded;
    }

    private List<String> reloadEverySetting(){
        reloadConfig();
        downtimeService.setSettings(DowntimeSettings.fromConfig(getConfig().getConfigurationSection("downtime")));
        loadMessages();
        List<String> notReloaded = new ArrayList<>();
        if (!reloadStatsConfig()) {
            notReloaded.add(StatsConfigFile.FILE_NAME);
        }
        if (!reloadAncestriesConfig()) {
            notReloaded.add(AncestriesConfigFile.FILE_NAME);
        } else if (ancestryService != null) {
            ancestryService.setDefinitions(ancestryDefinitions);
        }
        if (!reloadSkillsConfig()) {
            notReloaded.add(SkillsConfigFile.FILE_NAME);
        }
        if (!reloadProficienciesConfig()) {
            notReloaded.add(ProficienciesConfigFile.FILE_NAME);
        }
        notReloaded.addAll(reloadInjuriesAndConditions());
        if (afflictionService != null) {
            afflictionService.setDefinitions(conditionDefinitions, injuryDefinitions);
        }
        return notReloaded;
    }

    // A stats.yml with mistakes is reported and the stats already loaded are kept.
    private boolean reloadStatsConfig(){
        try {
            statsConfig = loadStatsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "stats.yml was not reloaded, keeping the stats from before", e);
            return false;
        }
        if (statService != null) {
            statService.setDefinitions(statsConfig.stats());
            registerStatCommands();
            // A changed or removed scale: block applies at once.
            Bukkit.getOnlinePlayers().forEach(heightScaler::refresh);
        }
        return true;
    }

    // An ancestries.yml with mistakes, or naming a stat the stats now loaded don't
    // have for /setup, is reported and the ancestries already loaded are kept.
    // Characters keep what their ancestry gave them either way.
    private boolean reloadAncestriesConfig(){
        try {
            ancestryDefinitions = loadAncestriesConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "ancestries.yml was not reloaded, keeping the ancestries from before", e);
            return false;
        }
        return true;
    }

    // A skills.yml with mistakes is reported and the skills already loaded are
    // kept. Otherwise everyone online gets the level nodes of the new file.
    private boolean reloadSkillsConfig(){
        SkillsConfig config;
        try {
            config = loadSkillsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "skills.yml was not reloaded, keeping the skills from before", e);
            return false;
        }
        if (skillService != null) {
            skillService.setConfig(config);
            skillPermissions.register(config);
            Bukkit.getOnlinePlayers().forEach(skillLevelSync::sync);
        }
        return true;
    }

    // A proficiencies.yml with mistakes, or naming a skill the skills now loaded
    // don't have, is reported and the proficiencies already loaded are kept.
    // Otherwise everyone online gets the tier nodes of the new file.
    private boolean reloadProficienciesConfig(){
        if (proficiencyService == null) {
            return true;
        }
        ProficienciesConfig config;
        try {
            config = loadProficienciesConfig(skillService.config().skills());
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "proficiencies.yml was not reloaded, keeping the proficiencies from before", e);
            return false;
        }
        proficiencyService.setConfig(config);
        proficiencyPermissions.register(config);
        Bukkit.getOnlinePlayers().forEach(proficiencyTierSync::sync);
        return true;
    }

    // Conditions first, since injuries name them. A conditions.yml with mistakes
    // keeps the conditions from before, and injuries.yml is then checked against
    // those. Either file with mistakes keeps its own definitions from before.
    // Returns the files that were not reloaded.
    private List<String> reloadInjuriesAndConditions(){
        List<String> notReloaded = new ArrayList<>();
        try {
            conditionDefinitions = loadConditionsConfig();
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "conditions.yml was not reloaded, keeping the conditions from before", e);
            notReloaded.add(ConditionsConfigFile.FILE_NAME);
        }
        try {
            injuryDefinitions = loadInjuriesConfig(conditionDefinitions);
        } catch (DefinitionException e) {
            getLogger().log(Level.WARNING, "injuries.yml was not reloaded, keeping the injuries from before", e);
            notReloaded.add(InjuriesConfigFile.FILE_NAME);
        }
        return notReloaded;
    }

    // Reloads only conditions.yml and injuries.yml, as /ssreload would, for
    // /ssimport chattweaks after it added to them. Returns the files that had
    // mistakes and were not reloaded.
    private List<String> reloadInjuriesAndConditionsFiles(){
        List<String> notReloaded = changeCauses.during(ChangeCause.RELOAD, () -> {
            List<String> files = reloadInjuriesAndConditions();
            afflictionService.setDefinitions(conditionDefinitions, injuryDefinitions);
            return files;
        });
        Bukkit.getPluginManager().callEvent(new SkysongStatsReloadEvent());
        return notReloaded;
    }

    private ConditionDefinitions loadConditionsConfig(){
        return ConditionsConfigFile.load(new File(getDataFolder(), ConditionsConfigFile.FILE_NAME),
                bundled(ConditionsConfigFile.FILE_NAME), statsConfig.stats());
    }

    private AncestryDefinitions loadAncestriesConfig(){
        return AncestriesConfigFile.load(new File(getDataFolder(), AncestriesConfigFile.FILE_NAME),
                bundled(AncestriesConfigFile.FILE_NAME), statsConfig.stats());
    }

    private InjuryDefinitions loadInjuriesConfig(ConditionDefinitions conditions){
        return InjuriesConfigFile.load(new File(getDataFolder(), InjuriesConfigFile.FILE_NAME),
                bundled(InjuriesConfigFile.FILE_NAME), statsConfig.stats(), conditions);
    }

    private ProficienciesConfig loadProficienciesConfig(SkillDefinitions skills){
        return ProficienciesConfigFile.load(new File(getDataFolder(), ProficienciesConfigFile.FILE_NAME),
                bundled(ProficienciesConfigFile.FILE_NAME), skills);
    }

    private SkillsConfig loadSkillsConfig(){
        return SkillsConfigFile.load(new File(getDataFolder(), SkillsConfigFile.FILE_NAME), bundled(SkillsConfigFile.FILE_NAME));
    }

    private StatsConfig loadStatsConfig(){
        return StatsConfigFile.load(new File(getDataFolder(), StatsConfigFile.FILE_NAME), bundled(StatsConfigFile.FILE_NAME));
    }

    private void loadMessages(){
        messages = MessagesFile.load(new File(getDataFolder(), MessagesFile.FILE_NAME), bundled(MessagesFile.FILE_NAME), getLogger()::warning,
                externalPlaceholders(), SheetTags.all(new AfflictionSheetTags(afflictionLists, this::sheetCharacter),
                        new AncestrySheetTags(ancestryPlaceholders, this::sheetCharacter)));
    }

    // The character an online player is playing, for the sheet tags; none for
    // offline players.
    private Optional<ProfileKey> sheetCharacter(OfflinePlayer player){
        return Optional.ofNullable(player.getPlayer()).filter(online -> utils != null)
                .flatMap(online -> utils.getProfileManager().activeCharacter(online));
    }

    // Other plugins' %placeholders% on the sheets, when PlaceholderAPI is installed.
    private static ExternalPlaceholders externalPlaceholders(){
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? PlaceholderApiPlaceholders.create()
                : ExternalPlaceholders.NONE;
    }

    private String bundled(String fileName){
        try (InputStream in = getResource(fileName)) {
            if (in == null) {
                throw new IllegalStateException(fileName + " is missing from the plugin jar");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The plugin can't work without MySQL, so this returns false, with one error
    // that says what to fix, when it can't be reached or read.
    public boolean InitializeDatabase(){
        DatabaseSettings settings = DatabaseSettings.fromConfig(getConfig().getConfigurationSection("database"));
        this.database = new Database(
                getConfig().getString("database.host"),
                getConfig().getString("database.port"),
                getConfig().getString("database.user"),
                getConfig().getString("database.password"),
                getConfig().getString("database.database_name"));
        try {
            storage = openDatabase(settings);
            return true;
        } catch (SQLException | RuntimeException e) {
            getLogger().log(Level.SEVERE, "Unable to use the database " + settings
                    + ". SkysongStats needs it, so it is disabled. Check that MySQL is running and the"
                    + " database: section of config.yml, then restart the server.", e);
            return false;
        }
    }

    // Opens the stat and skill tables, and loads the profiles and characters of
    // the older tables. Tests replace it, since they have no MySQL.
    protected CharacterStorage openDatabase(DatabaseSettings settings) throws SQLException {
        CharacterStorage opened = StatStorage.open(settings, getLogger()::warning);
        try {
            database.initializeDatabase();
            database.dumpActiveStatsDatabase();
            database.dumpDatabaseData();
        } catch (SQLException e) {
            opened.close();
            throw e;
        }
        return opened;
    }
}
