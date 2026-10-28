# SkysongStats: how the plugin works today

This document describes SkysongStats as it is on `master` after the downtime commit (`81c6629`, "Comitting Downtime changes", merged in `2e69b4b`). That commit also reworked `/setup`; both are covered below. It is a map of the existing code, written before any refactor, so that every later change can be checked against what the plugin did before it.

Nothing here proposes changes. Behaviour that looks wrong is recorded in [Observed issues](#observed-issues) at the end, with the file and line it comes from, so it can be discussed before anything is touched.

## Contents

1. [What the plugin does](#what-the-plugin-does)
2. [Build and runtime dependencies](#build-and-runtime-dependencies)
3. [Startup](#startup)
4. [Data model](#data-model)
5. [Database](#database)
6. [In-memory caches (the managers)](#in-memory-caches-the-managers)
7. [Utils: hard-coded stats and skills](#utils-hard-coded-stats-and-skills)
8. [Commands](#commands)
9. [Character setup flow (GUIs)](#character-setup-flow-guis)
10. [Listeners](#listeners)
11. [Custom events](#custom-events)
12. [PlaceholderAPI expansion](#placeholderapi-expansion)
13. [Downtime](#downtime)
14. [Configuration](#configuration)
15. [Permissions](#permissions)
16. [Observed issues](#observed-issues)

## What the plugin does

SkysongStats is a Paper plugin that keeps a tabletop-style character sheet for each player:

- **Stats**: Strength, Dexterity, Constitution, Focus and Speed, plus HP, temporary HP and AC.
- **Afflictions**: Arcrot, Fictis and Potion Sickness, each with a maximum that depends on a stat.
- **Modifiers**: a list of flat bonuses or penalties attached to a stat.
- **Skills**: 18 fixed skills that gain XP and map to five levels (Unskilled to Expert). Levels are mirrored as LuckPerms permission nodes.
- **Character sheet**: name, age, gender, pronouns, ancestry and description.
- **Profiles**: a player can own several named profiles (characters). Each profile has its own stats, skills, setup state and character sheet, and one profile is active at a time.
- **Setup**: a two-screen GUI (`/setup`) where a new profile spends stat points and picks two starting skills.
- **Downtime**: a per-player (global) pool and a per-profile (character) pool of downtime points that staff add and spend, refilled by `/downtime resetall`.

Arcrot and Fictis are not typed in by hand any more: they are counted from tagged items in the player's inventory, using the tags that Skysong-Special-Items writes on items.

## Build and runtime dependencies

`build.gradle`:

| Dependency | Scope | Notes |
|---|---|---|
| `io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT` | compileOnly | The server runs Paper 1.21.11. |
| `me.clip:placeholderapi:2.11.6` | compileOnly | From `https://repo.extendedclip.com/releases/`. Optional at runtime (`softdepend`). |
| `net.luckperms:api:5.3` | implementation | Used by two listeners. Declared as `softdepend` (spelled `Luckperms`). |
| `files("libs/SkysongSpecialItems.jar")` | implementation | A local jar of Skysong-Special-Items. The `libs/` folder is not in the repository, so a fresh clone does not build. |

The `java` plugin alone does not shade `implementation` dependencies into the jar, so LuckPerms and Skysong-Special-Items classes are expected to come from those plugins at runtime.

`plugin.yml` declares `softdepend: [PlaceholderAPI, Luckperms]` and `loadbefore: [CMI, CMILib]`. It does not declare Skysong_Items. The project version is `1.3`.

The repository has no tests. Since PR #2, `build/`, `.gradle/`, `.idea/` and `libs/` are ignored.

## Startup

`SkysongStats.onEnable()` (`SkysongStats.java`):

1. Stores the static `plugin` instance and creates `Utils`, which creates the five managers.
2. `saveDefaultConfig()`.
3. `InitializeDatabase()`: opens one MySQL connection with the credentials from `config.yml`, creates the nine tables if missing, and loads `skysong_active_stats` into memory.
4. `InitializeCommands()`: binds each command in `plugin.yml` to an executor.
5. `AssignListeners()`: registers the eight listeners.
6. Registers the PlaceholderAPI expansion if PlaceholderAPI is enabled.
7. `database.dumpDatabaseData()`: loads every table into the static lists held by the managers (see below). Everything after this point reads from memory and writes through to MySQL.
8. Schedules `Pinger` every 6000 ticks (5 minutes) to run a `SELECT` so the MySQL connection is not closed for inactivity.

`SkysongStats.getPlugin()` returns the static instance. `getSkySongItems()` returns a field that is never assigned, so it is always `null` and unused.

## Data model

All model classes live in `database/` and are plain mutable objects.

### `PlayerStats`

One per (player UUID, profile). Fields:

| Field | Stored in DB | How it is set |
|---|---|---|
| `strength`, `dexterity`, `constitution`, `focus`, `speed` | yes | Setup GUI, `/stats set|add` |
| `hp` | yes | `/heal`, `/damage`, `/reducehp`, `/stats` |
| `temp_hp` | yes | `/temphp`, `/stats` |
| `ac` | yes | `/ac`, `/stats` |
| `arcrot`, `fictis` | yes | Recounted from the inventory by `AfflictionsListener`; also `/stats` |
| `potion` | yes | `/potionsickness`, `/stats` |
| `hp_max`, `arcrot_max`, `fictis_max`, `potion_max` | no | Derived, see below |
| `modifiers` | in `skysong_modifiers` | `/modifier add|del` |

Defaults for a new profile: all stats 0, Speed 6, HP 60/60, Arcrot 0/35, Fictis 0/10, Potion 0/9, AC 0, no modifiers.

Derived maximums, hard-coded in the full constructor and in `setConstitution` / `setFocus`:

- `hp_max = 60 + 10 × Constitution`
- `potion_max = 9 + Constitution`
- `arcrot_max = 35 + 2 × Focus`
- `fictis_max = 10 + Focus`

These use the base stat only; modifiers on Constitution or Focus do not change them.

Methods:

- `setX` / `addX` for each stat. `addX(v)` calls `setX(current + v)`.
- `setHP(v)` clamps to `[0, hp_max + MAXHP modifiers]`.
- `dealDamage(d)` takes damage from temp HP first and carries the rest to HP. It does not clamp HP at 0.
- `addModifier(m)` appends the modifier; if it is a Max HP modifier, current HP is clamped to the new maximum.
- `getXModifiers()` (one per `StaticStats` value) sums the modifiers for that stat.
- `getModifierFromList(i)`, `getModifierIndex(m)` access the modifier list by position.

### `Modifier`

A `(StaticStats stat, int modifier)` pair. It has an unused `parent` field.

### `PlayerSkills`

One per (UUID, profile). Holds an `ArrayList<Integer>` of 18 XP values indexed by `Utils.Skills.getValue()`. Methods: `getSkill`, `setSkill`, `addSkill`.

### `CharacterProfile`

One per (UUID, profile). Strings: `name`, `age`, `gender`, `ancestry`, `pronouns`, `description`, all defaulting to `"[Not Set]"`.

### `SetupProfile`

One per (UUID, profile). Holds:

- `isSetUp`: whether `/setup` was completed (stored in DB).
- Five `int` fields (`strength`, `dexterity`, `constitution`, `focus`, `speed`): the final values chosen in the point GUI. Not stored in DB.
- `skillsSelected` and `skillPoints` (2) for the skill GUI. Not stored in DB.
- Its own instances of the two setup GUIs (`PointAllocGUI`, `SkillChoiceGUI`), created when the object is created.

`finishProfileSetup()` writes the five values into the profile's `PlayerStats` (and saves it), sets the two chosen skills to 16 XP (Novice) in memory, marks the profile as set up and saves that flag.

### `ProfileIndex`

A `(uuid, profile)` pair that says which profile is active for a player.

## Database

`database/Database.java` holds one `java.sql.Connection`, opened lazily by `getConnection()` and reused for the life of the server. All queries run on the server thread.

Tables (created in `initializeDatabase()`):

| Table | Columns | Model |
|---|---|---|
| `skysong_stats` | `id`, `uuid`, `profile`, `strength`, `dexterity`, `focus`, `constitution`, `speed`, `arcrot`, `potion`, `fictis`, `hp`, `temphp`, `ac` | `PlayerStats` |
| `skysong_active_stats` | `uuid` (PK), `profile` | `ProfileIndex` |
| `skysong_modifiers` | `id`, `uuid`, `profile`, `modid`, `stat`, `modifier` | `Modifier`; `modid` is the position in the list, `stat` the `StaticStats` number |
| `skysong_skills` | `id`, `uuid`, `profile`, one `INT` column per skill (`animal_handling` … `transportation_and_exploration`) | `PlayerSkills` |
| `skysong_setup` | `id`, `uuid`, `profile`, `setup` | `SetupProfile.isSetUp` |
| `skysong_characters` | `id`, `uuid`, `profile`, `name`, `age`, `gender`, `ancestry`, `pronouns`, `description` | `CharacterProfile` |
| `skysong_downtime` | `id`, `uuid`, `current_downtime` | `PlayerDowntime` (global) |
| `skysong_character_downtime` | `id`, `uuid`, `profile`, `current_downtime` | `CharacterDowntime` (per profile) |
| `skysong_downtime_preferences` | `uuid` (PK), `isGlobal` | spending preference |

Methods, grouped as in the file:

- **Stats**: `createStatData`, `updateStatData`, `deleteStatData`, `findStats` (unused; builds SQL by string concatenation).
- **Active profile**: `createActiveProfileData`, `updateActiveProfileData`.
- **Modifiers**: `createModifierData(stats, index)`, `updateModifierData` (unused), `deleteModifierData(stats, index)`.
- **Skills**: `createSkillsData`, `updateSkillsData` (write all 18 columns in enum order), `deleteSkillData`.
- **Setup**: `createSetupData` (two overloads), `updateSetupData`, `deleteSetupData`.
- **Character**: `createCharacterData`, `updateCharacterData`, `deleteCharacterData`.
- **Downtime**: `create|update|deletePlayerDowntimeData`, `create|update|deleteCharacterDowntimeData`, `create|updateDowntimePreferencesData`.
- **Loading**: `dumpDatabaseData()` calls `dumpStatsDatabase` (stats plus each profile's modifiers, one extra query per profile), `dumpActiveStatsDatabase`, `dumpSkillsDatabase` (reads each column by the lowercased enum name), `dumpSetupDatabase`, `dumpModifiersDatabase` (adds every modifier row again, see issue 30), `dumpCharacterDatabase`, `dumpDowntimeDatabase` (both downtime tables) and `dumpDowntimePreferencesDatabase`.
- **Misc**: `checkForUUID(uuid)` (is there any stats row for this player), `Ping()`.

## In-memory caches (the managers)

`Utils` creates and exposes five managers. Each keeps its data in a **static** `ArrayList` filled by the database dump, and searches it linearly.

### `StatsManager`

Static `statsProfileList: ArrayList<PlayerStats>`.

- `findStats(uuid, profile)`.
- `addStatProfile(stats)`: inserts in DB, then adds to the list.
- `addModifier(stats, modifier)`: adds to the object and inserts in DB at its list index.
- `updateStats(stats)`: writes the object to DB.
- `deleteStat(stats)`: deletes from DB and the list.
- `deleteModifiers(stats)`: deletes every modifier row of the profile (not called anywhere).

### `ProfileManager`

Static `activeProfiles: ArrayList<ProfileIndex>`, `setupProfiles: ArrayList<SetupProfile>` and `downtimePreferences: Map<String, Boolean>` (player UUID to "spend from global").

- `findActiveStats(uuid)`: the `PlayerStats` of the player's active profile. Almost every command goes through this.
- `findAllProfiles(uuid)`: names of every profile the player owns.
- `setProfile(stats)`: makes that profile active and saves it.
- `getActiveProfileName(uuid)`, `findActiveProfile(uuid)`.
- `createProfile(stats)`: creates the active-profile row for a new player.
- `findActiveSetupProfile(uuid)`, `findSetupProfile(uuid, profile)`.
- `addSetupProfile`, `createSetupProfile(uuid)` (for the "Default" profile), `setSetupProfile` (not called), `deleteSetupProfile`.
- `addDowntimePreferences(uuid, isGlobal)`, `updateDowntimePreferences(uuid, isGlobal)`: change the map and save.

### `SkillManager`

Static `playerSkills: ArrayList<PlayerSkills>`, plus an instance `skillList` of 18 display ids used for tab completion.

- `findSkills(uuid, profile)`, `findActiveSkills(uuid)`.
- `addSkillProfile`, `updateSkillProfile` (replaces the object in the list; does not save), `deleteSkillProfile`.
- `getSkill(name)`: maps a name or alias (`"metal"`, `"t&e"`, …) to a `Utils.Skills` value.
- `addSkillXP(skills, name, xp)` / `setSkillXP(...)`: same alias switch, then saves.
- `getSkillLevel(xp)` and `getSkillLevelFormatted(xp)`: the level thresholds.

| XP | Level |
|---|---|
| 0–15 | Unskilled |
| 16–39 | Novice |
| 40–79 | Competent |
| 80–127 | Journeyman |
| 128+ | Expert |

### `CharacterManager`

Static `characterProfiles: ArrayList<CharacterProfile>`.

- `findCharacterProfile(uuid, profile)`, `findActiveCharacter(uuid)`.
- `addCharacter`, `updateCharacter`, `deleteCharacterProfile`.

### `DowntimeManager`

Static `playerDowntimes: ArrayList<PlayerDowntime>` and `characterDowntimes: ArrayList<CharacterDowntime>`.

- `findPlayerDowntime(uuid)`, `findCharacterDowntime(uuid, profile)`, `findActiveCharacterDowntime(uuid)` (not called; see issue 36).
- `addPlayerDowntime`, `addCharacterDowntime`: insert in DB, then add to the list.
- `updatePlayerDowntime`, `updateCharacterDowntime`: save, then copy the value onto the cached object.
- `deleteCharacterDowntime` (not called).

## Utils: hard-coded stats and skills

`Utils/Utils.java` is the shared toolbox:

- `PLUGIN_TAG` (chat prefix) and a shared `MiniMessage` instance.
- Five `NamespacedKey`s used to mark GUI buttons: `selectable`, `id` (statline), `stat`, `option`, `skill`.
- `enum StaticStats`: `STRENGTH, DEXTERITY, CONSTITUTION, FOCUS, SPEED, MAXHP, FICTIS_MAX, ARCROT_MAX, POTION_MAX, ARCROT, FICTIS`, with `getStatName`. These are the stats a modifier can target. `toInt` / `toStatEnum` convert to and from the number stored in `skysong_modifiers.stat`.
- `enum Skills`: the 18 skills with their list index, `fromInt`, `getSkillName`, `getSkillNameFormatted` (with colour), `getSkillid` (used in permission nodes).
- `isAStat(name)` and `isASkill(name)`: accepted command names and aliases.
- `isPlayerInDatabase(uuid)`.
- Downtime constants: `MAX_CHARACTER_DOWNTIME = 3`, `MAX_GLOBAL_DOWNTIME = 9`, `GLOBAL_DOWNTIME_RECOVERY = 3`.

The 18 skills, grouped as `/skills` shows them:

- Gathering: Animal Handling, Farming, Forestry, Mining, Mist Gathering, Wood Processing
- Crafting: Alchemy, Artificing, Cooking, Craftsman, Metalworking, Tailoring, Witchcraft
- Service: Economical, Entertainment, Martial, Medical, Transportation and Exploration (T&E)

Adding, removing or renaming a stat or a skill today means editing these enums, the alias switches in `Utils` and `SkillManager`, `PlayerStats`, the SQL in `Database`, every `/stats` and `/modifier` switch, the GUIs, the placeholder switch and `plugin.yml`.

## Commands

Commands are registered in `SkysongStats.InitializeCommands()`. Parent commands (`/stats`, `/skills`, `/profile`, `/modifier`, `/schar`) route their first argument to a subcommand class and pass the rest along.

| Command | Executor | What it does |
|---|---|---|
| `/stats` `/sstats` | `StatsCommand` | No args or `view [player]`: prints the stat sheet. `set <stat> <value> [player]` (`skysongstats.stats.set`): sets a stat. `add <stat> <value> [player]` (`skysongstats.stats.add`): adds to a stat. Targeting another player needs `skysongstats.set.other`. |
| `/heal` `/addhp` `<n> [player]` | `AddHP` | Adds HP, clamped to max. Other player: `skysongstats.heal.other`. Works from console with a player. |
| `/damage` `/dhp` `<n> [player]` | `DamageHP` | `dealDamage`: temp HP first, then HP. Other player: `skysongstats.damage.other`. |
| `/reducehp` `/rhp` `<n> [player]` | `ReduceHP` | Removes HP directly, ignoring temp HP, clamped at 0. Other player: `skysongstats.damage.other`. |
| `/temphp <n> [player]` | `TempHp` | Sets temp HP. Other player: `skysongstats.temphp.other`. |
| `/ac <n>` | `ACCommand` | Sets your own AC. |
| `/potionsickness <n> [player]` | `PotionSickness` | Sets Potion Sickness (in memory only, see issues). |
| `/modifier add <stat> <n> [player]` | `AddModifier` | Adds a modifier. Other player: `skysongstats.modifier.add.other`. |
| `/modifier del <#> [player]` | `DelModifier` | Removes the modifier at that 1-based position. Other player: `skysongstats.modifier.del.other`. |
| `/modifier view [page] [player]` | `ViewModifiers` | Lists modifiers, ten per page. Other player: `skysongstats.modifier.view.other`. |
| `/skills` (no args or `view [player]`) | `ViewSkills` | Opens a written book with the three skill groups, levels and XP. |
| `/skills addxp <skill> <xp> [player]` | `AddSkillXP` | `skysongstats.addxp`. Adds XP, saves, fires `SkillUpdateEvent`. |
| `/skills setxp <skill> <xp> [player]` | `SetSkillXP` | Checks `skysongstats.command.setxp`. Sets XP, saves, fires `SkillUpdateEvent`. |
| `/profile create <name>` | `CreateProfile` | Creates stats, setup, skills, character and character-downtime (3) rows for a new profile (name up to 20 characters). |
| `/profile switch <name>` | `SwitchProfile` | Makes that profile active and fires `ProfileUpdateEvent`. |
| `/profile delete|del <name>` | `DeleteProfile` | Deletes a profile other than the active one. |
| `/profile view` | `ProfileCommand` | Shows the active profile's name. |
| `/schar` `/char` (no args or `view [player]`) | `ViewCharacter` | Prints the character sheet, wrapping the description at 60 characters. |
| `/schar set <field> <value…>` | `SetCharacterField` | Sets name (≤100), age, gender, pronouns (≤20), ancestry (≤50) or description. |
| `/setup` | `SetupCommand` | Opens the setup GUI if the active profile is not set up yet. |
| `/ssreload` | `ReloadCommand` | `skysongstats.reload`. Reloads `config.yml`. |
| `/downtime` `/dt` … | `DowntimeCommand` | See [Downtime](#downtime). |

Stat names accepted by `/stats`: `strength`, `dexterity`, `constitution`, `focus`, `speed`, `arcrot`, `potion`, `fictis`, `hp`, `temphp`, `ac`. `/stats add` also accepts `str`, `dex`, `con`, `foc`, `spd`.

Modifier targets offered by `/modifier add`: `strength`, `dexterity`, `constitution`, `focus`, `speed` (and their short forms), `maxhp`, `arcrot`, `fictis`, `arcrot-max`, `fictis-max`, `potion-max`. The last three are accepted since the downtime commit.

`plugin.yml` also declares `statsreload`, `hp`, `arcrot` and `fictis`, which have no executor. `/fictis` and `/arcrot` executors are commented out in `InitializeCommands()`.

## Character setup flow (GUIs)

Each `SetupProfile` owns its two GUIs. Both are `InventoryHolder`s, and their buttons are recognised by the persistent data keys in `Utils`. The statline screen (`StatSetupGUI`, fed by `base_stats` in `config.yml`) was removed in the downtime commit; `base_stats` is still in `config.yml` but nothing reads it.

1. **`/setup`** (`SetupCommand`) checks `isSetUp`, rebuilds `PointAllocGUI` and opens it.
2. **`PointAllocGUI`** (27 slots) shows one button per stat. Left click adds 1, right click removes 1 (`StatPointsGUIListener` calls `addStat`). Every stat starts at 0 except Speed at 6. Rules, as coded in `addStat` (Speed counted from 6):
   - 7 points to spend; a stat can go up to +4.
   - 3 "negative points": lowering a stat at or below 0 uses one negative point and gives one point back, down to -3.
   - Lowering a stat above 0 refunds its point.
   - Confirm is refused while points remain, or when exactly two negative points have been used (`negativePoints == 1`).
   - Closing the inventory resets the allocation.
   On confirm, the values are copied into the `SetupProfile` and `SkillChoiceGUI` opens.
3. **`SkillChoiceGUI`** (54 slots) shows the 18 skills. Left click selects (glint on), right click deselects, up to 2. Closing the inventory clears the selection. On confirm (`SkillAllocGUIListener`), `SetupProfile.finishProfileSetup()` saves the stats, gives the two skills 16 XP and marks the profile as set up.

## Listeners

| Listener | Event | What it does |
|---|---|---|
| `PlayerRegisterListener` | `PlayerJoinEvent` | If the player has no stats row, creates the "Default" profile: stats, active profile, skills, setup, character, global downtime (3), character downtime (3) and a "Character" spending preference. Then reminds them to `/setup` if the active profile is not set up. |
| `AfflictionsListener` | `PlayerInventorySlotChangeEvent`, `PlayerJoinEvent` | Recounts Arcrot and Fictis from the whole inventory and saves the stats. See below. |
| `ProfileUpdateListener` | `ProfileUpdateEvent` | For each of the 18 skills, adds the LuckPerms nodes `skysongstats.skill.<id>.<level>` for the current level and every level below it, removes the ones above it, and saves the user. |
| `SkillLevelListener` | `SkillUpdateEvent` | Same as above for the one skill in the event. Since the downtime commit it loads the LuckPerms user asynchronously (`loadUser(...).thenAccept`), so it also works for offline players. |
| `PlayerShiftRightClickListener` | `PlayerInteractEntityEvent` | Shift + right click on a player shows you their character sheet (runs `ViewCharacter`) and cancels the interaction. |
| `StatPointsGUIListener` | `InventoryClickEvent`, `InventoryCloseEvent` | Point allocation screen of the setup. |
| `SkillAllocGUIListener` | `InventoryClickEvent`, `InventoryCloseEvent` | Skill choice screen of the setup. |

### Arcrot and Fictis counting (`AfflictionsListener`)

For every non-empty stack in the player's inventory:

- **Arcrot** += `arcrot` tag value × stack size, plus `charged_arcrot` tag value × stack size when the item's `charges` tag is above 0.
- **Fictis** += `fictis` tag value × stack size.

The tags are the integer persistent data entries that Skysong-Special-Items writes on items. The listener finds their keys through Skysong-Special-Items' own classes: `org.skysongdev.skysongitems.utils.Utils.getTag("arcrot")` and `Utils.chargesKey`. That class no longer exists, which is what breaks this listener today (see [Observed issues](#observed-issues), item 1).

## Custom events

- `ProfileUpdateEvent(player, uuid, profile)`: fired by `/profile switch`.
- `SkillUpdateEvent(player, uuid, skill[, profile])`: fired by `/skills addxp|setxp`. Without a profile, it uses the active one.

Both are plain synchronous Bukkit events and can be listened to by other plugins.

## PlaceholderAPI expansion

`placeholders/StatsExpansion.java`, identifier `skysongstats`. Every value is for the player's active profile.

- Stats: `%skysongstats_strength%`, `dexterity`, `constitution`, `focus`, `speed`, `potion`, `potion_max`, `arcrot`, `arcrot_max`, `fictis`, `fictis_max`, `hp`, `hp_max`, `temp_hp`, `ac`.
- Downtime: `global_downtime`, `character_downtime` (active profile).
- Skill XP: `animalhandling`, `farming`, `forestry`, `mining`, `mistgathering`, `woodprocessing`, `alchemy`, `artificing`, `cooking`, `craftsman`, `metalworking`, `witchcraft`, `economical`, `entertainment`, `martial`, `medical`, `tande`.
- Modifier totals: `strength_modifiers`, `dexterity_modifiers`, `constitution_modifiers`, `focus_modifiers`, `speed_modifiers`, `maxhp_modifiers`, `maxpotion_modifiers`, `maxarcrot_modifiers`, `maxfictis_modifiers`.
- Stat plus modifiers: `str_full`, `dex_full`, `con_full`, `foc_full`, `spd_full`, `maxhp_full`, `maxpotion_full`, `maxarcrot_full`, `maxfictis_full`, `arcrot_full`, `fictis_full`.
- Character sheet: `character_name`, `character_age`, `character_gender`, `character_pronouns`, `character_ancestry`, `character_description`.

## Downtime

Added in commit `81c6629`. Downtime comes in two pools, plus a preference for which one is spent by default:

- **Global downtime** (`PlayerDowntime`): one value per player, shared by all their profiles. New players start at 3 (`GLOBAL_DOWNTIME_RECOVERY`). It is capped at 9 (`MAX_GLOBAL_DOWNTIME`) only by `resetall`.
- **Character downtime** (`CharacterDowntime`): one value per profile. New profiles start at 3 (`MAX_CHARACTER_DOWNTIME`).
- **Spending preference** (`ProfileManager.downtimePreferences`): per player, `true` = spend from global, `false` = spend from the active character. New players start on character.

Commands (`/downtime`, alias `/dt`; `add`, `remove` and `resetall` need `skysongstats.downtime.manage` when run by a player, and always work from console):

| Subcommand | What it does |
|---|---|
| `add <amount> <player> <global\|profile>` | Adds to the player's global downtime, or to the named profile's downtime (see issue 32). The player must be online. |
| `remove <amount> <player> [global\|profile]` | Removes from global or the named profile. Without the third argument it uses the player's spending preference: global, or the active profile. No lower limit. |
| `view [player]` | Shows global downtime, the active profile's downtime and the spending preference. Another player needs `skysongstats.downtime.view.others`. |
| `preference` | Players only. Toggles the spending preference between Global and Character. |
| `resetall` | Sets every profile's downtime to 3, and adds 3 to every player's global downtime, up to 9. |

There is no automatic refill; `resetall` is meant to be run by staff (or a scheduler) at the start of each downtime period.

## Configuration

`src/main/resources/config.yml`:

```yaml
database:            # MySQL connection
  host, port, user, password, database_name
base_stats:          # statlines offered by /setup
  <id>:
    item: <Material>
    name: <MiniMessage>
    stats: { strength, dexterity, constitution, focus, speed }
```

Only the database is configurable; `base_stats` is no longer read. Stats, skills, skill levels, maximum formulas, setup point rules, downtime limits, GUI text and the affliction tags are all in code.

## Permissions

Declared in `plugin.yml` (all default to op):

- `skysongstats.heal.other`, `skysongstats.damage.other`, `skysongstats.temphp.other`
- `skysongstats.reload`
- `skysongstats.modifier.add.other`, `skysongstats.modifier.del.other`, `skysongstats.modifier.view.other`
- `skysongstats.addxp`, `skysongstats.setxp`, `skysongstats.stats.add`, `skysongstats.stats.set`
- `skysongstats.*` (all of the above)
- `skysongstats.skill.<skill>.<novice|competent|journeyman|expert>`, granted through LuckPerms by the skill listeners.

Used in code but not declared in `plugin.yml`: `skysongstats.downtime.manage`, `skysongstats.downtime.view.others`, `skysongstats.set.other`, `skysongstats.command.setxp`.

Each command also has a `permission:` entry (`skysongstats.stats`, `skysongstats.heal`, …), which Bukkit checks before running it.

## Observed issues

Found while reading the code. Items 1–28 were found before the downtime commit; 29 onward come from it. Decisions taken on 1–28 are in the refactor proposal.

### Breaking

1. **Arcrot and Fictis fail on every inventory change.** `AfflictionsListener` imports `org.skysongdev.skysongitems.utils.Utils`, which Skysong-Special-Items deleted on 2026-10-25 (commit `dba82dc`, "Move item behaviour out of Utils and delete it"). `Utils.getTag` had already been replaced by `TagRegistry.getKey` (commit `31bef61`) and `Utils.chargesKey` by `ItemKeys.CHARGES` (commit `4abec14`). The keys themselves did not change: they are still `skysong_items:arcrot`, `skysong_items:charged_arcrot`, `skysong_items:fictis` and `skysong_items:charges`, so items already in circulation are fine. Only the Java class the listener used to look the keys up is gone. The same error also fires on join, because the join handler calls the same methods.
2. **The project does not build from a clean clone.** `libs/SkysongSpecialItems.jar` is not in the repository.

### Data that is not saved or saved wrongly

3. **Starting skills are not saved.** `finishProfileSetup()` sets the two chosen skills to 16 XP through `SkillManager.updateSkillProfile`, which only updates the in-memory list. They reach the database only when some later `/skills addxp|setxp` rewrites the whole row; a restart before that loses them. No `SkillUpdateEvent` is fired either, so the LuckPerms nodes for those skills are not granted.
4. **Modifier rows drift out of sync.** `modid` is the modifier's position in the list. Deleting modifier 1 of 3 deletes row `modid=0`, but the remaining rows keep `modid` 1 and 2 while the list now calls them 0 and 1. The next delete or add then hits the wrong row or creates a duplicate `modid`.
5. **`/potionsickness` is not saved.** It changes the value in memory but never calls `updateStats`, so it is lost on restart (unless something else saves the stats first).
6. **Deleting a profile leaves its modifiers in the database.** `DeleteProfile` does not call `deleteModifiers`; a new profile with the same name would load them back.

### Commands that report success but do nothing

7. **`/stats set` ignores short names.** `isAStat` accepts `str`, `dex`, `con`, `foc`, `spd` and `maxhp`, but the `SetStat` switch only handles the full names, so `/stats set str 3` replies "has been set" and changes nothing.
8. **`/modifier add arcrot-max|fictis-max|potion-max` is always refused.** The tab completion offers them, but `isAStat` does not accept them, so they fail with "is not a stat". `/modifier add potion` passes the check but has no case, so it replies "has been modified" and adds nothing.
9. **`/skills setxp medical` also sets T&E.** The `medical` case in `SkillManager.setSkillXP` has no `break` and falls through.
10. **Skill aliases disagree.** `getSkill` accepts `forest` and `tailor`, `isASkill` accepts `for` and not `tailor`. `/skills addxp tailor` is rejected; `/skills addxp for 5` passes the check but adds nothing.

### Crashes on bad input

11. Missing arguments: `/stats set|add`, `/modifier add|del` and `/ac` print the usage message but do not return, so they then throw `ArrayIndexOutOfBoundsException`.
12. Console: `/stats`, `/profile`, `/modifier`, `/schar`, `/setup`, `/ac`, `/potionsickness` and `/skills view` cast the sender to `Player` and throw when run from console or command blocks.
13. Confirming the skill screen with fewer than two skills throws `IndexOutOfBoundsException` in `finishProfileSetup()`. By then the stats have already been saved, but the profile is not marked as set up.
14. LuckPerms is required by two listeners but not declared as a dependency. If it loads after SkysongStats, or is absent, the listeners hold `null` and throw on every skill or profile change.

### Behaviour that may or may not be intended

15. `dealDamage` (`/damage`) can take HP below 0; `/reducehp` stops at 0.
16. Maximums (HP, Arcrot, Fictis, Potion) use the base Constitution or Focus only. A Constitution modifier does not raise max HP, and `%skysongstats_hp_max%` excludes Max HP modifiers while `/stats` includes them.
17. `/modifier add arcrot` and `fictis` add a modifier on top of the inventory count, which is now automatic.
18. There is no `%skysongstats_tailoring%` placeholder.
19. `/skills setxp` checks `skysongstats.command.setxp`, but `plugin.yml` declares `skysongstats.setxp`.
20. `plugin.yml` has `/addhp` with permission `skysongstats.damagehp`, no `forestry.expert` permission, and four `scholarly` permissions for a skill that does not exist.
21. In the skill GUI, selected skills keep their glint after the screen is closed and reopened, even though the selection was cleared.

### Technical debt

22. All MySQL calls run on the server thread, including a full stats `UPDATE` on every inventory slot change.
23. One JDBC connection for the whole server life, kept alive by a 5-minute ping and never reopened if it drops.
24. Several queries build SQL by concatenating the UUID and profile name (profile names are typed by players).
25. Global mutable state: the managers store their data in public static lists, and every class reaches them through `SkysongStats.getPlugin().getUtils()`. This makes the code impossible to unit test without a running server.
26. Large amounts of duplication: the four HP commands are copies of each other; `PlayerStats` has one getter, setter, adder and modifier-sum per stat; `SkillManager` repeats the same 18-way switch three times; the placeholder expansion has one case per value.
27. Unused code: `Database.findStats`, `updateModifierData`, `dumpModifiersDatabase`, `StatsManager.deleteModifiers`, `ProfileManager.setSetupProfile`, `SkysongStats.getSkySongItems`, `Modifier.parent`.
28. Build hygiene: no tests. (The `.gitignore` and the 1.3 version were fixed since; the Paper API is now 1.21.5, still older than the 1.21.11 server.)

### Found in the downtime commit

29. **`master` does not compile.** `StatlineGUIListener.java` still imports the deleted `StatSetupGUI` and calls `setStrength(int[])`. Fixed by PR #3.
30. **Every modifier is loaded twice on startup.** `dumpStatsDatabase` already attaches each profile's modifiers, and `dumpModifiersDatabase`, now re-enabled, adds every row again. After each restart, modifiers count double in `/stats`, placeholders and max HP, and deleting one by position hits the wrong row.
31. **Downtime is missing for everyone who existed before it.** Rows are only created on a player's first join and on `/profile create`. Older players and profiles have none, so `/downtime view|add|remove`, `%skysongstats_global_downtime%` and `%skysongstats_character_downtime%` throw `NullPointerException` for them.
32. **`/downtime add <n> <player> <profile>` uses the wrong pool.** It sets the profile's downtime to the player's *global* downtime plus `n`.
33. **`resetall` is not saved.** It changes the cached values only; a restart restores the old ones.
34. **`/downtime preference` is not saved,** and players with no preference row default to Global in that command but crash `/downtime view` (unboxing `null`).
35. **`/downtime view <player>` always fails.** It reads `args[1]` when only `args[0]` exists (`ArrayIndexOutOfBoundsException`); without the permission it then prints "Player null not found".
36. `findActiveCharacterDowntime` compares a profile name with a `ProfileIndex` object, so it never matches (it is not called yet).
37. Deleting a profile leaves its character downtime row behind. `remove` has no lower bound and `add` has no upper bound, and `remove <profile>` sends no confirmation.
38. `plugin.yml` lists `Luckperms`; the plugin's name is `LuckPerms`.
39. `SkillLevelListener` now imports `org.skysongdev.skysongitems.SkysongItems` without using it, another compile-time tie to Skysong-Special-Items.
