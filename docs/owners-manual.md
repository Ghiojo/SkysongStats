# SkysongStats owner's manual

This manual explains how every part of SkysongStats works on `master` after the stats, skills, messages and proficiencies refactors. It is written for the server owner: what each file and setting does, what each command and permission does, and how to make the usual changes yourself.

When this manual and the code disagree, the code wins. The last section, [Where things live in the code](#where-things-live-in-the-code), tells you which file to open for each part.

## Contents

1. [What the plugin does](#what-the-plugin-does)
2. [Requirements and installing](#requirements-and-installing)
3. [The plugin folder and reloading](#the-plugin-folder-and-reloading)
4. [config.yml](#configyml)
5. [stats.yml](#statsyml)
6. [Formulas](#formulas)
7. [skills.yml](#skillsyml)
8. [proficiencies.yml](#proficienciesyml)
9. [messages.yml](#messagesyml)
10. [Profiles and characters](#profiles-and-characters)
11. [The /setup flow](#the-setup-flow)
12. [Commands](#commands)
13. [Permissions and LuckPerms nodes](#permissions-and-luckperms-nodes)
14. [Placeholders](#placeholders)
15. [Events for other plugins](#events-for-other-plugins)
16. [The database](#the-database)
17. [Skysong Special Items compatibility](#skysong-special-items-compatibility)
18. [How to make common changes](#how-to-make-common-changes)
19. [Things to know (quirks and leftovers)](#things-to-know-quirks-and-leftovers)
20. [Where things live in the code](#where-things-live-in-the-code)

---

## What the plugin does

SkysongStats keeps a roleplay character sheet for every player. A player can have several **profiles** (characters), and each profile has its own:

- **Stats**, from `stats.yml`: plain values such as Strength, and **bars** such as HP that have a current value and a max.
- **Modifiers** on stats or on bar maximums, added with `/modifier`.
- **Skills**, from `skills.yml`: XP per skill, which gives a level and LuckPerms nodes.
- **Proficiencies**, from `proficiencies.yml`: each proficiency belongs to a skill and has a tier (Untrained, Proficient, Adept, Master), trained with points.
- **Character sheet** text: name, age, gender, pronouns, ancestry, description.
- **Character downtime**, plus one **global downtime** per player.

New profiles go through `/setup` once: point-buy for stats, then picking skills, then picking proficiencies.

Everything the plugin shows to players comes from `messages.yml`, so the whole plugin can be reworded or translated without touching code.

---

## Requirements and installing

| Needs | Why |
|---|---|
| Paper 1.21 | The plugin is built against the Paper API (`api-version: '1.20'`, compiled on 1.21.5). |
| **MySQL** | Required. Every character is stored there. If the database can't be reached at startup, the plugin logs one error saying what to check and **disables itself**. There is no in-memory fallback. |
| **LuckPerms** | Hard dependency (`depend:` in `plugin.yml`). Skill level and proficiency tier nodes are written into LuckPerms. |
| PlaceholderAPI | Optional. When it is present, the `%skysongstats_...%` placeholders are registered. |
| Skysong_Items | Optional. When its items are in a player's inventory, their item tags (Arcrot, Fictis) add to stats. See [compatibility](#skysong-special-items-compatibility). |

Paper downloads HikariCP (the database connection pool) by itself on the first start, through `libraries:` in `plugin.yml`.

**Building:** run `./gradlew build` in the repository. The jar is `build/libs/SkysongStats-<version>.jar` (the version is in `build.gradle`). `./gradlew build test` also runs the unit tests.

**First start:** the plugin writes its default `config.yml`, `stats.yml`, `skills.yml`, `proficiencies.yml` and `messages.yml` into `plugins/SkysongStats/`. Fill in the `database:` section of `config.yml` and restart.

---

## The plugin folder and reloading

| File | What it holds | `/ssreload` |
|---|---|---|
| `config.yml` | Database connection, downtime limits | Downtime: yes. `database:`: **no, restart only** |
| `stats.yml` | Every stat, its limits, its commands, its /setup button | Yes |
| `skills.yml` | Skill levels, skills, the /skills book, /setup skill picks | Yes |
| `proficiencies.yml` | Tiers, costs, proficiencies, /setup proficiency picks | Yes |
| `messages.yml` | Every player-facing text | Yes |

### What `/ssreload` does

`/ssreload` re-reads every file above in this order: `config.yml` (downtime limits only), `messages.yml`, `stats.yml`, `skills.yml`, `proficiencies.yml`.

- **A file with mistakes is not applied.** The plugin keeps using the version it already had, tells whoever ran the command `<file> has mistakes and was not reloaded`, and prints the full list of mistakes to the console. Fix them and run `/ssreload` again.
- **At startup, a file with mistakes disables the plugin**, with the list of mistakes in the console. Fix it and restart.
- After a successful reload:
  - New stats, skills and proficiencies are saved for every character at their starting value.
  - Bars that are now above their max are brought down to it (unless they have `can-exceed-max: true`).
  - Stat command names (`/heal`, `/damage`...) and the generated permissions are swapped for the new file's.
  - Everyone online gets their skill level and proficiency tier nodes recalculated for the new files.
- Things removed from a file are **not deleted from the database**: their saved values stay, unused. If you add them back, the old values come back too.
- `proficiencies.yml` is checked against the skills now loaded. If `skills.yml` failed to reload, proficiencies are checked against the old skills.

### Missing messages are added for you

When an update adds new messages, they are written into your `messages.yml` with their default text and comments on the next start or `/ssreload`. Texts you changed are never touched. (The other files are not merged like this: a file you already have is read as it is.)

---

## config.yml

```yaml
database:
  host: "localhost"
  port: "3306"
  user: "root"
  password: ""
  database_name: "skysong_titles"

downtime:
  character-max: 3
  global-start: 3
  global-recovery: 3
  global-max: 9
```

### `database:`

The MySQL connection. Read **only when the server starts**; `/ssreload` ignores it. A wrong value means the plugin disables itself at startup with an error naming the host, port, database and user it tried (the password is never logged).

### `downtime:`

| Key | Meaning |
|---|---|
| `character-max` | New profiles start with this much character downtime. `/downtime resetall` sets every profile to it. `/downtime add ... <profile>` stops at it. |
| `global-start` | New players start with this much global downtime. |
| `global-recovery` | `/downtime resetall` adds this to every player's global downtime. |
| `global-max` | `/downtime add ... global` and `resetall` never take global downtime above this. A player already above it keeps their value. |

A missing or negative value falls back to the default shown above. Removing downtime always stops at 0. See [downtime commands](#downtime-downtime-alias-dt).

### `base_stats:` (unused)

The default `config.yml` still contains a `base_stats:` section. **Nothing reads it any more.** Starting stat values come from `baseline` in `stats.yml`. You can delete the section.

---

## stats.yml

`stats.yml` defines every stat. Nothing about stats is hard-coded: the default file reproduces the old Skysong stats (Str, Dex, Con, Foc, Spd, HP, Temp HP, AC, Potion Sickness, Arcrot, Fictis), and you can change, remove or add any of them.

```yaml
setup:
  points: 7
  negative-points: 3

stats:
  str:
    name: "Strength"
    ...
```

### `setup:` (the /setup point-buy)

| Key | Meaning |
|---|---|
| `points` | Points a new character spends on the /setup stat screen. |
| `negative-points` | How many points a player may take *below* a stat's baseline in total. |

Rules of the point screen (point for point):

- Raising a stat by 1 costs 1 point.
- Lowering a stat below its baseline gives 1 point back and uses 1 negative point. Raising it back towards the baseline returns that negative point.
- A stat moves between `baseline + setup.min` and `baseline + setup.max`.
- The player can only confirm when **every point is spent**, and not with exactly one negative point left (a rule kept from the old screen).

### Stat fields

Each key under `stats:` is a stat. The key is its **id**: lowercase letters, digits, `_` and `-`. The id is what formulas, placeholders, messages and permissions use. Ids that start with `max_` are reserved (bar `hp` gives `max_hp`).

| Field | For | Meaning |
|---|---|---|
| `name` | all | Display name. |
| `color` | all | MiniMessage colour, e.g. `"<red>"` or `"<#ff8800>"`. Used on the /setup button. |
| `aliases` | all | Other names commands accept: `/stats set strength 2` works for `str`. Lowercase, no `max_` prefix, unique. |
| `baseline` | all | The value a new character starts at (default 0). For value stats, /setup moves around it. |
| `type` | all | `value` (default) or `bar`. |
| `min` | all | Lowest total the stat can have. Bars default to 0; value stats have no floor unless you set one. |
| `max` | bars | A number or a [formula](#formulas), e.g. `"60 + {con} * 10"`. Required for bars. |
| `start` | bars | `max` (starts full) or a number (the current value of a new character). |
| `shield` | bars | Another stat that absorbs `damage` first. HP's shield is `temphp`. |
| `can-exceed-max` | bars | `true` lets the current value go above the max. Default `false`: changes stop at the max. |
| `item-tags` | all | Skysong item tags that add to the current value. See [compatibility](#skysong-special-items-compatibility). |
| `commands` | all | Extra command names for this stat's actions (see below). |
| `permission-default` | all | Who can use this stat's actions without being given the node: `op` (default), `true` (everyone), `false` (nobody), `not op`. |
| `setup` | value stats | Puts the stat on the /setup point screen (see below). Bars can't be on it. |

### Value stats and bars

- A **value** stat has one number: its **base** (saved), plus modifiers, plus item counts. That sum is its **total**, which is what players, formulas and placeholders see.
- A **bar** has a **current value** (base + modifiers on the bar + item counts) and a **max** (its formula, plus modifiers on `max_<id>`).

### Actions: set, add, reduce, damage

Every stat has these actions, used through `/stats <action> <stat> <amount> [player]` or a command name from `commands:`:

| Action | What it does |
|---|---|
| `set` | Sets the stat's base to the amount. The only action that accepts a negative amount (`/stats set str -2`). |
| `add` | Base + amount. |
| `reduce` | Base − amount. |
| `damage` | Only for bars with a `shield`. The shield loses as much as it has (down to 0), the rest comes off the bar. With 5 Temp HP, 8 damage leaves Temp HP at 0 and takes 3 HP. |

After any action the total is kept within `min` and, for bars without `can-exceed-max`, within the max. When a stat a max depends on goes down (for example Con), bars above their new max are brought down to it.

### `commands:` (command aliases per stat)

```yaml
  hp:
    commands:
      add: [heal, addhp]
      reduce: [reducehp, rhp]
      damage: [damage, dhp]
```

Each name becomes a real command: `/heal <amount> [player]` runs `/stats add hp <amount> [player]`. These names are registered when the plugin starts and swapped on `/ssreload`, so you never need to edit `plugin.yml` for them.

- A name can't be one of the plugin's own commands (`stats`, `sstats`, `statsreload`, `ssreload`, `skills`, `setup`, `modifier`, `profile`, `char`, `schar`, `downtime`, `dt`, `proficiency`) or be used by two stats.
- If another plugin already owns the name, the console warns you, and the command only works as `/skysongstats:<name>`.
- `damage` names are only allowed on bars with a `shield`.

### `setup:` (a stat on the /setup point screen)

```yaml
    setup:
      icon: IRON_SWORD        # a Material, or model:<namespace:id> for an item model
      slot: 9                 # 0-26, the screen is 3 rows
      min: -3                 # how far below the baseline it can go (0 or less)
      max: 4                  # how far above the baseline it can go (0 or more)
      lore:
        - "<gray>Determines your character's Physical Strength"
```

Slots 4 (the header) and 22 (the confirm button) are taken. Two stats can't share a slot. Empty slots on the top and bottom rows get a border.

### What the default file sets up

| Stat | Type | Notes |
|---|---|---|
| `str`, `dex`, `con`, `foc` | value | Baseline 0, −3 to +4 in /setup. |
| `spd` | value | Baseline 6, −3 to +4 in /setup. |
| `hp` | bar | Max `60 + {con} * 10`, starts full, shield `temphp`. `/heal`, `/addhp`, `/reducehp`, `/rhp`, `/damage`, `/dhp`. |
| `temphp` | value | Min 0. `/temphp`. |
| `ac` | value | Min 0. `/ac`. |
| `potion` | bar | Max `9 + {con}`, starts at 0, can exceed max. `/potionsickness`. Everyone may use it (`permission-default: true`). |
| `arcrot` | bar | Max `35 + {foc} * 2`, starts at 0, can exceed max. Counts `arcrot` items and `charged_arcrot` items that have charges. |
| `fictis` | bar | Max `10 + {foc}`, starts at 0, can exceed max. Counts `fictis` items. |

### Modifiers

`/modifier add <stat|max_<bar>> <amount> [player]` adds a modifier. A modifier on a stat adds to its total; a modifier on `max_hp` adds to HP's max. Modifiers are saved per profile and listed with `/modifier view`. Removing one (`/modifier del <number>`) uses the number `view` shows.

---

## Formulas

Bar maximums are formulas. They can use:

- numbers (`60`, `2.5`)
- stats in braces: `{con}` is Con's **total** (base + modifiers + items)
- another bar's max: `{max_hp}`
- `+ - * /`, a leading minus, and parentheses, with normal precedence

The result is **rounded down** at the end only: `{foc} / 2 * 3` with Foc 3 is 4.5, so 4. Stat names in braces ignore case (`{Con}` = `{con}`). There is no scripting: a formula can only do arithmetic on stats.

When loading, the plugin rejects a formula that names a stat that doesn't exist, uses `{max_x}` for a stat that isn't a bar, or goes in a circle (`max_a` reads `max_b` which reads `max_a`).

Examples:

```yaml
max: 100                         # a fixed max
max: "60 + {con} * 10"           # 60, plus 10 per point of Con
max: "({con} + {str}) * 5"
max: "{max_hp} / 2"              # half of HP's max
```

---

## skills.yml

```yaml
levels:
  unskilled:  {name: "Unskilled",  color: "<dark_gray>", xp: 0}
  novice:     {name: "Novice",     color: "<#6cda33>",   xp: 16}
  competent:  {name: "Competent",  color: "<blue>",      xp: 40}
  journeyman: {name: "Journeyman", color: "<#cc6ce9>",   xp: 80}
  expert:     {name: "Expert",     color: "<gold>",      xp: 128}

setup:
  picks: 2
  picked-level: novice

book:
  gathering:
    title: "<green>Gathering Skills:</green>"
    skills: [animalhandling, farming, forestry, mining, mistgathering, woodprocessing]
  ...

skills:
  animalhandling:
    name: "Animal Handling"
    color: "<#45c2ea>"
    aliases: [ah]
    setup:
      icon: BONE
      slot: 10
      lore: [...]
```

### `levels:`

Shared by every skill, lowest first. A skill is at the highest level whose `xp` it has reached. The first level must start at 0, and each level needs more XP than the one before.

Every level **except the first** gives a LuckPerms node `skysongstats.skill.<skill>.<level>`, and a character has the nodes of its level and every level below. A Competent Mining character has `skysongstats.skill.mining.novice` and `skysongstats.skill.mining.competent`.

### `setup:`

| Key | Meaning |
|---|---|
| `picks` | How many skills a player picks on the /setup skill screen. Must be no more than the number of skills that have a `setup:` section. |
| `picked-level` | A level id. Each picked skill gets that level's XP. |

### `book:`

The pages of the `/skills` book, in order. Each page has a `title` and the skill ids on it. A skill on no page is shown on an extra last page (title `skills.view.other-title` in messages.yml). A skill can't be on two pages.

### Skill fields

The key is the skill's **id** (lowercase letters, digits, `_`, `-`), used in LuckPerms nodes, placeholders and `proficiencies.yml`.

| Field | Meaning |
|---|---|
| `name`, `color` | Display name and MiniMessage colour. |
| `short-name` | The name in the /skills book (default: `name`). T&E uses this. |
| `aliases` | Other names `/skills addxp` and `setxp` and the proficiency training command accept. No spaces; `t&e` is fine. |
| `baseline` | XP a new character starts with (default 0). |
| `setup` | Puts the skill on the /setup skill screen: `icon`, `slot` (9-44), `lore`, and an optional `color` just for the screen. |

---

## proficiencies.yml

Proficiencies are trained abilities (Light Armor, Bows, Medicine...). Each belongs to a skill. Every character has every proficiency, at a tier.

```yaml
tiers:
  untrained:  {name: "Untrained",  color: "<dark_gray>"}
  proficient: {name: "Proficient", color: "<green>"}
  adept:      {name: "Adept",      color: "<blue>",  extra: 2}
  master:     {name: "Master",     color: "<gold>",  extra: 2}

cost: 4

setup:
  trained: []
  picks: {}
  skill-picks:
    martial: 1
    economical: 1
    medical: 1
    entertainment: 1

proficiencies:
  light_armor: {name: "Light Armor", color: "<#e18048>", skill: martial}
  ...
```

### `tiers:` and `cost:`

Lowest first; at least two. Every proficiency starts at the first tier, which is free.

- The second tier costs `cost` points (4).
- Every later tier costs the tier before it plus its `extra`: Adept 4 + 2 = 6, Master 6 + 2 = 8.
- Only the third tier onwards may have `extra`.

Every tier **except the first** gives a LuckPerms node `skysongstats.proficiency.<proficiency>.<tier>`, and a character has the nodes of its tier and every tier below. An Adept in Bows has `skysongstats.proficiency.bows.proficient` and `skysongstats.proficiency.bows.adept`.

### How training works

1. A player picks **one** proficiency to train: their **focus** (`/proficiency focus bows`). Any proficiency that isn't at the top tier can be the focus.
2. A training business runs the training command, which gives points **for one skill and up to a maximum tier**:
   `/proficiency points add <player> <amount> <skill> <max-tier>`
3. The points go to the focus **only if** the focus belongs to that skill **and** is below the maximum tier. Otherwise nothing is added, and both the sender and the player are told why (no focus, wrong skill, or already at the trainer's max).
4. When the points reach the next tier's cost, the focus goes up **one** tier and its points go back to 0. Leftover points are lost.
5. When the focus reaches the top tier (Master), it is cleared and the player is told to pick a new one.
6. Switching focus while the current focus has points asks first. The player clicks **[Yes]** (or runs `/proficiency focus confirm`) within a minute, and the old points are lost. The question is also forgotten when the player logs out, and a confirm from another profile than the one that asked does nothing.

So a trainer who is Adept themselves would run, for example, `/proficiency points add Steve 2 martial adept`: Steve's Martial focus gains 2 points if it is Untrained or Proficient.

### `setup:` (the /setup proficiency screen)

| Key | Meaning |
|---|---|
| `trained` | Proficiency ids every new character starts Proficient in. They show on the screen as "Already trained" and can't be picked. |
| `picks` | Free picks per skill id for every new character, e.g. `{martial: 1}`. |
| `skill-picks` | Extra picks per skill id when the player picked that skill on the /setup skill screen. Default: 1 for Martial, Economical, Medical and Entertainment. |

A pick raises one proficiency to the second tier (Proficient), **never higher**, and only spends picks of that proficiency's own skill. The screen shows the trained proficiencies plus every proficiency of each skill the player has picks for, at most 36. If nothing is trained and the player has no picks, the screen is skipped.

### Proficiency fields

The key is the proficiency's **id** (lowercase letters, digits, `_`, `-`). It can't be one of `/proficiency`'s words: `view`, `focus`, `points`, `tier`, `confirm`.

| Field | Meaning |
|---|---|
| `name`, `color` | Display name and MiniMessage colour. |
| `skill` | **Required.** The id of the `skills.yml` skill it belongs to. Training adds points by skill. |
| `aliases` | Other names the commands accept. No spaces, not a `/proficiency` word, unique. |
| `cost` | The second tier's cost for this proficiency only. Later tiers add the extras on top. |
| `icon` | How it shows on the /setup screen: a Material or `model:<namespace:id>`. A book when left out. |
| `lore` | Lines under its name on the /setup screen. |

### The default list

| Skill | Proficiencies |
|---|---|
| Martial | Unarmored, Light Armor, Medium Armor, Heavy Armor, Arcane Arms, Axes, Bows, Crossbows, Firearms, Picks, Polearms, Spears, Whips, Projectile, Blunt, Long Blade, Short Blade, Exotic |
| Economical (Logistics & Economics) | Perception, Surface Vehicle, Air Vehicle, Surface Mount, Air Mount, Survival |
| Medical | Medicine |
| Entertainment | Sleight of Hand, Stealth, Performance |

---

## messages.yml

Every message players and staff see. Edit the text and run `/ssreload`.

### Format

- Texts use **MiniMessage**: `<red>`, `<#ff8800>`, `<bold>`, `<gradient:...>`, `<click:...>`, `<hover:...>` and the rest. Reference: https://docs.advntr.dev/minimessage/format.html
- `{prefix}` is replaced by the `prefix:` at the top of the file.
- Other `{names}` are filled in by the plugin. The comment above each message lists the ones it gets.
- A message can be **one line** or a **list of lines** (each list entry is sent as its own line).
- Values the plugin fills in are escaped, so a player typing `<red>` into a profile name can't add formatting.
- **Character sheet fields** (name, age, gender, pronouns, ancestry, description) are the exception: players may use every MiniMessage format that works in chat, including click and hover.
- If your file is missing a key, the default text is used, it is added to your file, and the console warns once.

### Sections

| Section | Used by |
|---|---|
| `general` | Shared messages: no permission, reloaded, player not found, not a number... |
| `downtime` | `/downtime` |
| `profile` | `/profile`, and the "set up your profile" reminder on join |
| `character` | `/char`, `/schar` and the character sheet layout |
| `skills` | `/skills`, the skills book, the /setup skill screen (`skills.choice`) |
| `proficiency` | `/proficiency`, its sheet, the /setup proficiency screen (`proficiency.choice`) |
| `setup` | `/setup` and its stat point screen |
| `stats` | Stat actions, `/stats view` sheet |
| `modifier` | `/modifier` |

### Stat action messages

For an action on a stat, the plugin looks for `stats.<stat id>.<action>` first and uses `stats.default.<action>` when it isn't there. `.player` goes to the player whose stat changed; `.sender` goes to whoever ran the command when that is someone else. They all get `{amount} {value} {old} {player} {sender} {name} {stat}`.

So to give `/heal` its own text you edit `stats.hp.add.player`; a new stat `mana` uses the default texts until you add `stats.mana.add`, etc.

### Sheet layouts

| Sheet | Key | Filled in |
|---|---|---|
| Stat sheet (`/stats`) | `stats.sheet` | `{player}` and every stat placeholder: `{str}`, `{str_base}`, `{str_modifiers}`, `{hp}`, `{max_hp}`, `{max_hp_modifiers}`... (the same names as the PlaceholderAPI ones). |
| | `stats.sheet-auto` | A stat that `stats.sheet` never mentions (one you just added) gets an automatic line, `value` or `bar`, under the sheet, so new stats always show. Mention the stat in `stats.sheet` to place it yourself. |
| | `stats.sheet-footer` | Last lines. |
| Character sheet (`/char`) | `character.view.header`, `description-line`, `footer` | `{player} {name} {age} {gender} {pronouns} {ancestry}`; the description is wrapped into lines of about 60 characters, one `description-line` each. |
| Proficiency sheet (`/proficiency view`) | `proficiency.sheet` | `{player}`, `{focus}` (a `sheet-focus` or `sheet-no-focus` line), `{proficiencies}` (one `sheet-skill` line per skill, each a list of `sheet-entry` parts joined by `sheet-separator`). |
| Skills book (`/skills`) | `skills.view.page`, `skill-line` | One page per `book:` page of skills.yml. |

### Names that are *not* in messages.yml

Stat, skill, level, proficiency and tier names and colours live in their own files (`stats.yml`, `skills.yml`, `proficiencies.yml`), and so do the lore lines of the /setup buttons. Console log lines stay in English.

---

## Profiles and characters

- A player's first join creates the profile **Default**, with every stat, skill, proficiency, the character sheet and character downtime at their starting values, and global downtime at `global-start`. Everything is saved to the database right away.
- `/profile create <name>` makes another profile the same way (name up to 20 characters, unique per player).
- `/profile switch <name>` changes the active profile. Stats, skills, proficiencies, the character sheet, item counts and LuckPerms nodes all follow the active profile. Other plugins get a `ProfileUpdateEvent`.
- `/profile delete <name>` deletes a profile that isn't the active one, with all its data.
- `/profile view` shows the active profile.
- On join, if the active profile hasn't finished /setup, the player gets `profile.setup-reminder`.

Every change (a stat, a modifier, XP, a tier, a focus, a field) is saved as it happens. Stats, skills and proficiencies are saved in the background, in order, so commands never wait for MySQL.

---

## The /setup flow

`/setup` is **per profile**: each new profile can run it once, and a profile that finished it can never run it again (`setup.already-set-up`). There is no permission check inside the command beyond the command's own `skysongstats.setup`.

1. **Stat screen** (`stats.yml` → `setup:`). Left click raises a stat, right click lowers it. Point for point, as described in [stats.yml](#setup-the-setup-point-buy). Confirm when every point is spent.
2. **Skill screen** (`skills.yml` → `setup:`). Left click picks a skill, right click un-picks it; picked skills glow. Confirm when every pick is used.
3. **Proficiency screen** (`proficiencies.yml` → `setup:`). Same clicks. The header lists the picks left per skill. Confirm when every pick is used, except picks of a skill that has nothing left to pick. Skipped when there is nothing to show.
4. **Finish:** the chosen stats are set, bars that start full (HP) are filled to their **new** max, picked skills get the `picked-level` XP, trained and picked proficiencies become Proficient (LuckPerms nodes and events follow), and the profile is marked as set up.

Nothing is applied until the last screen is confirmed. **Closing any screen** cancels: the player starts over from the stat screen with the next `/setup`.

---

## Commands

`[player]` means an optional online player name; leaving it out means yourself. The console must always name a player.

### Stats

| Command | Permission | What it does |
|---|---|---|
| `/stats` or `/stats view [player]` (also `/sstats`) | `skysongstats.stats`; others: `skysongstats.stats.view.other` (everyone by default) | Shows the stat sheet. |
| `/stats set\|add\|reduce\|damage <stat> <amount> [player]` | `skysongstats.stat.<stat>.<action>`, or `....other` on someone else | Runs a stat action. `<stat>` is the id or an alias. |
| `/<name> <amount> [player]` | Same as the action it runs | Every name in a stat's `commands:` (`/heal`, `/damage`, `/temphp`, `/ac`, `/potionsickness`...). |
| `/modifier add <stat\|max_<bar>> <amount> [player]` | `skysongstats.modifier`; others: `skysongstats.modifier.add.other` | Adds a modifier. |
| `/modifier del <number> [player]` | `skysongstats.modifier`; others: `skysongstats.modifier.del.other` | Removes the modifier with that number in `view`. |
| `/modifier view [page] [player]` | `skysongstats.modifier`; others: `skysongstats.modifier.view.other` | Lists modifiers, 10 per page. |

Stat and modifier commands only work on **online** players.

### Skills

| Command | Permission | What it does |
|---|---|---|
| `/skills [view] [player]` | `skysongstats.skills` | Opens the skills book (players only). |
| `/skills addxp <skill> <xp> [player]` | `skysongstats.addxp` | Adds XP. The player is told they gained it. |
| `/skills setxp <skill> <xp> [player]` | `skysongstats.setxp` | Sets XP. |

XP is never below 0. Every XP change is logged in the console with who did it.

### Proficiencies (`/proficiency`, alias `/prof`)

| Command | Permission | What it does |
|---|---|---|
| `/proficiency [view] [player]` | `skysongstats.proficiencies.view` (everyone); others: `....view.other` (ops) | Shows the proficiency sheet. |
| `/proficiency focus <proficiency>` | `skysongstats.proficiencies.focus` (everyone) | Picks the proficiency to train; asks first if that loses points. |
| `/proficiency focus confirm` | same | The [Yes] of that question. |
| `/proficiency points add <player> <amount> <skill> <max-tier>` | `skysongstats.proficiencies.manage` (ops) | **The training command for businesses.** See [how training works](#how-training-works). |
| `/proficiency points remove <player> <amount>` | `...manage` | Removes points from the focus, stopping at 0. Never lowers a tier. |
| `/proficiency points set <player> <amount>` | `...manage` | Sets the focus's points; must stay below the next tier's cost. |
| `/proficiency tier set <player> <proficiency> <tier>` | `...manage` | Sets a tier directly, up or down. The focus keeps its points, unless it reached the top tier (then it is cleared). |

Staff changes are logged in the console.

### Downtime (`/downtime`, alias `/dt`)

| Command | Permission | What it does |
|---|---|---|
| `/downtime view [player]` | none for yourself; others: `skysongstats.downtime.view.others` | Shows global downtime, the active profile's downtime, and the spending preference. |
| `/downtime preference` | none (players only) | Toggles whether you spend global or character downtime. New players spend character downtime. |
| `/downtime add <amount> <player> <global\|profile>` | `skysongstats.downtime.manage` (console always) | Adds to global downtime (stops at `global-max`) or to the named profile (stops at `character-max`). |
| `/downtime remove <amount> <player> [global\|profile]` | `skysongstats.downtime.manage` | Removes, stopping at 0. Without the last argument it removes from the pool the player's preference points to (global, or their active profile). |
| `/downtime resetall` | `skysongstats.downtime.manage` | New downtime period: every profile goes to `character-max`, every player gains `global-recovery` up to `global-max`. |

Downtime commands work on **offline** players too, as long as they have joined before. The amount must be a whole number of 1 or more.

### Profiles and character

| Command | Permission | What it does |
|---|---|---|
| `/profile create\|switch\|delete\|view [name]` | `skysongstats.profile` | See [profiles](#profiles-and-characters). Players only. |
| `/char [view] [player]` (also `/schar`) | `skysongstats.char` / `skysongstats.schar` | Shows an online player's character sheet. **Shift + right click** on a player shows theirs too. |
| `/char set <field> <value>` | same | Sets your own `name` (100 chars), `age` (20), `gender` (20), `pronouns` (20), `ancestry` (50) or `description` (stored up to 500). |

### Setup and reload

| Command | Permission | What it does |
|---|---|---|
| `/setup` | `skysongstats.setup` | See [the /setup flow](#the-setup-flow). |
| `/ssreload` | `skysongstats.ssreload` **and** `skysongstats.reload` | See [reloading](#what-ssreload-does). |

---

## Permissions and LuckPerms nodes

### Fixed nodes (from plugin.yml)

| Node | Default | Meaning |
|---|---|---|
| `skysongstats.stats`, `.skills`, `.setup`, `.modifier`, `.ssreload`, `.schar`, `.char`, `.profile` | op (not declared, so Bukkit treats them as op-only) | Needed to run each command at all. **Give the player-facing ones to your default group.** |
| `skysongstats.stats.view.other` | everyone | See someone else's stat sheet. |
| `skysongstats.reload` | op | Needed by `/ssreload` on top of `skysongstats.ssreload`. |
| `skysongstats.modifier.add.other`, `.del.other`, `.view.other` | op | Modifiers on other players. |
| `skysongstats.addxp`, `skysongstats.setxp` | op | `/skills addxp`, `setxp`. |
| `skysongstats.downtime.manage` | op | `/downtime add`, `remove`, `resetall`. |
| `skysongstats.downtime.view.others` | op | `/downtime view <player>`. |
| `skysongstats.proficiencies.view` | everyone | Your proficiency sheet. |
| `skysongstats.proficiencies.view.other` | op | Others' proficiency sheets. |
| `skysongstats.proficiencies.focus` | everyone | `/proficiency focus`. |
| `skysongstats.proficiencies.manage` | op | Training and staff proficiency commands. |
| `skysongstats.*` | op | Everything above (and the generated nodes below). |

### Generated nodes (from the config files)

These are created when the plugin starts and replaced on `/ssreload`, so they show in the LuckPerms editor. You never write them in `plugin.yml`.

| Node | From | Meaning |
|---|---|---|
| `skysongstats.stat.<stat>.<action>` | `stats.yml` | Use `set`/`add`/`reduce`/`damage` on your own stat. `damage` only for bars with a shield. Default: the stat's `permission-default` (op). |
| `skysongstats.stat.<stat>.<action>.other` | `stats.yml` | The same on someone else. |
| `skysongstats.skill.<skill>.<level>` | `skills.yml` | **Given by the plugin** in LuckPerms to the player, for their active profile's level and every level below (not the first level). |
| `skysongstats.proficiency.<proficiency>.<tier>` | `proficiencies.yml` | **Given by the plugin** in LuckPerms, for their active profile's tier and every tier below (not the first tier). |

Skill and proficiency nodes are set on join, on profile switch, on every XP or tier change of the active profile, and for everyone online after `/ssreload`. Changes to a profile the player isn't playing apply when they switch to it. Use these nodes in other plugins to unlock recipes, items or areas (for example, "requires `skysongstats.proficiency.heavy_armor.proficient`").

> Because these nodes are declared with default **op**, an op passes every skill and proficiency check. Test level-locked content on a non-op account.

---

## Placeholders

With PlaceholderAPI installed: `%skysongstats_<name>%`, always for the player's **active profile**. Nothing is returned for offline players.

| Placeholder | Value |
|---|---|
| `<stat>` | Total (base + modifiers + items); a bar's current value. E.g. `%skysongstats_str%`, `%skysongstats_hp%`. |
| `<stat>_base` | Saved base value. |
| `<stat>_modifiers` | Sum of the modifiers on the stat. |
| `max_<bar>` | A bar's max. |
| `max_<bar>_modifiers` | Sum of the modifiers on the bar's max. |
| `<skill>` | XP in that skill, e.g. `%skysongstats_mining%`. |
| `proficiency_<id>` | Tier name in its colour ("Adept"). |
| `proficiency_<id>_tier` | Tier id ("adept"), for conditions in other plugins. |
| `proficiency_focus` | Focused proficiency's name, empty without one. |
| `proficiency_focus_points` | Focus points, 0 without a focus. |
| `proficiency_focus_cost` | Points the focus needs for its next tier, 0 without a focus. |
| `character_name`, `_age`, `_gender`, `_pronouns`, `_ancestry`, `_description` | Character sheet fields. Placeholders inside them are filled in too. |
| `global_downtime`, `character_downtime` | Downtime. |

All of these follow the config files: add a stat, skill or proficiency and its placeholders exist after `/ssreload`.

---

## Events for other plugins

Other plugins can listen to these Bukkit events (package `org.skysongdev.skysongstats.events`). All fire **after** the change.

| Event | Fired when | `getChange()` / getters |
|---|---|---|
| `StatChangeEvent` | A stat's total changed: a command, a modifier, /setup, a reload that brought a bar down. | `StatChange(character, stat, oldValue, newValue)`. Item recounts don't fire it. |
| `SkillChangeEvent` | Skill XP changed: addxp, setxp, /setup. | `SkillChange(character, skill, before, after)` |
| `ProficiencyTierChangeEvent` | A tier changed: training tier-up, /setup, `tier set` (up **or down**). | `ProficiencyTierChange(character, proficiency, before, after, tierUp)` |
| `ProficiencyFocusChangeEvent` | The focus changed: new focus, points added/removed/set, cleared at the top tier. | `ProficiencyFocusChange(character, before, after)`; null = no focus. |
| `DowntimeChangeEvent` | Downtime changed: add, remove, resetall. | `getPlayerUUID()`, `getPool()` (GLOBAL/CHARACTER), `getProfile()`, `getOldValue()`, `getNewValue()`, `getCause()` (ADD/REMOVE/RESET) |
| `ProfileUpdateEvent` | A player switched profile. | `getPlayer()`, `getPlayerUUID()`, `getProfile()` |

`character` is a `ProfileKey(player UUID, profile name)`.

---

## The database

The database is **essential**. At startup the plugin connects to MySQL with the `database:` settings. If it can't, it logs:

> Unable to use the database DatabaseSettings[user@host:port/database]. SkysongStats needs it, so it is disabled. Check that MySQL is running and the database: section of config.yml, then restart the server.

and disables itself. Fix the settings or MySQL and **restart** (`/ssreload` does not reconnect).

### Tables

The plugin creates its tables itself.

| Table | Holds |
|---|---|
| `skysong_active_stats` | Each player's active profile. |
| `skysong_setup` | One row per profile: whether /setup is done. |
| `skysong_characters` | Character sheet fields per profile. |
| `skysong_downtime` | Global downtime per player. |
| `skysong_character_downtime` | Character downtime per profile. |
| `skysong_downtime_preferences` | Spending preference per player. |
| `ss_stat_values` | Base value per profile and stat id. |
| `ss_modifiers` | Modifiers per profile (target is a stat id or `max_<bar>`). |
| `ss_skill_xp` | XP per profile and skill id. |
| `ss_proficiency_tiers` | Tier per profile and proficiency id. |
| `ss_proficiency_focus` | The focus and its points, per profile. |

Stats, skills and proficiencies go through a small connection pool (HikariCP, 4 connections) that replaces connections MySQL drops, and are written by one background writer in order. A save that fails is logged as `Could not save a change: ...`. The `skysong_*` tables still use the older single connection, which the plugin pings every 5 minutes (you'll see `[Skysong Stats] Database pinged!` in the console).

The old tables from before the refactor, `skysong_stats`, `skysong_modifiers` and `skysong_skills`, are no longer created or read. Since Skysong is restarting, you can drop them if your database still has them.

---

## Skysong Special Items compatibility

SkysongStats reads item tags that Skysong_Items writes on items. It does **not** use Skysong_Items' code, so a refactor of Skysong_Items can't break it; it only depends on the tag names.

- A stat's `item-tags:` lists tags by name. The plugin reads the tag `skysong_items:<tag>` (an integer) on each item in the player's inventory, multiplies it by the stack size, and adds the sum to the stat's current value.
- `requires-charges: true` only counts items whose `skysong_items:charges` tag is above 0.
- Counts are recalculated whenever the inventory changes, on join, and on profile switch. They are never saved; they add on top of the saved base (so `/stats add arcrot 2` still stacks with Arcrot items).
- The tags must exist in Skysong_Items' `config.yml` under `tags:` with `type: "int"`. The defaults there are `arcrot`, `charged_arcrot` and `fictis`, which is what the default `stats.yml` uses.

To make a new item tag count towards a stat: add the tag in Skysong_Items' config (type int), put it on items there, then add `- tag: <name>` under that stat's `item-tags:` in `stats.yml` and run `/ssreload`.

---

## How to make common changes

After each change run `/ssreload` and read the reply: if a file "has mistakes and was not reloaded", the console says exactly which line and why.

### Add a stat

Add an entry under `stats:` in `stats.yml`:

```yaml
  wis:
    name: "Wisdom"
    aliases: [wisdom]
    color: "<aqua>"
    baseline: 0
    setup:                # leave out to keep it off the /setup screen
      icon: BOOK
      slot: 13            # pick a free slot; move other stats if needed
      min: -3
      max: 4
      lore:
        - "<gray>Determines your character's insight"
```

Every existing character gets the baseline. It works at once in `/stats set wis 2`, `%skysongstats_wis%`, `{wis}` in formulas, and it appears at the bottom of `/stats` (via `stats.sheet-auto`) until you place it in `stats.sheet` of messages.yml with `{wis}` / `{wis_modifiers}`. Its permission nodes are `skysongstats.stat.wis.<action>`.

### Add a bar (for example Mana)

```yaml
  mana:
    name: "Mana"
    type: bar
    max: "20 + {foc} * 5"
    start: max
    commands:
      add: [restoremana]
      reduce: [spendmana]
```

`/restoremana 5 Steve` and `/spendmana 3` now exist. To give them their own texts, add `stats.mana.add` and `stats.mana.reduce` in messages.yml (copy `stats.hp.add` as a model). For a shield, add `shield: <stat>` and a `damage:` command.

### Change a formula

Edit the bar's `max:`. For example, to make HP 50 + 12 per Con: `max: "50 + {con} * 12"`. After `/ssreload`, everyone's max changes; anyone now above the new max is brought down to it.

### Change /setup point-buy

`stats.yml` → `setup.points` and `negative-points`, and each stat's `setup.min`/`max`. Only profiles that haven't finished /setup are affected.

### Add a skill

Add it under `skills:` in `skills.yml`, add its id to a `book:` page, and (optionally) a `setup:` with a free slot 9-44. Existing characters get its `baseline` XP. LuckPerms nodes `skysongstats.skill.<id>.<level>` exist at once.

### Change skill levels

Edit `levels:`. XP stays the same; levels are recalculated from it, and LuckPerms nodes are updated for everyone online.

### Add a proficiency

```yaml
  shields: {name: "Shields", color: "<#e18048>", skill: martial, icon: SHIELD}
```

Every character starts it at the first tier. It appears on `/proficiency view` under its skill, on the /setup screen when the player has picks for that skill, and as `skysongstats.proficiency.shields.<tier>`.

### Change training costs

`proficiencies.yml` → `cost:` (second tier) and each tier's `extra:`. One proficiency can have its own `cost:`.

### Change starting proficiencies

`proficiencies.yml` → `setup.trained` (everyone starts Proficient in these), `setup.picks` (picks everyone gets) and `setup.skill-picks` (picks for choosing a skill at /setup).

### Change a message

Find the text in `messages.yml`, edit it, keep the `{placeholders}` listed in the comment above it, run `/ssreload`. To reset one message to its default, delete its line: it is written back with the default text.

### Translate the plugin

Translate every value in `messages.yml`, plus `name`, `lore` and `title` fields in `stats.yml`, `skills.yml` and `proficiencies.yml`. Ids stay as they are.

### Rename or remove something

- **Renaming a name** (`name:`) is always safe.
- **Changing an id** is like removing the old thing and adding a new one: the new id starts at its baseline, and the old id's saved data stays unused in the database. LuckPerms nodes and placeholders change with it.
- **Removing** a stat, skill or proficiency keeps its saved data in the database but stops using it. Check that nothing else names it: formulas, `shield:`, a skill's `book:` page, a proficiency's `skill:`. The reload tells you if something does.

---

## Things to know (quirks and leftovers)

- **`/statsreload`** is still listed in `plugin.yml` but does nothing (no code handles it). Use `/ssreload`.
- **`base_stats:`** in `config.yml` is unused (see [config.yml](#base_stats-unused)).
- **`/ssreload` checks two nodes**: the command's `skysongstats.ssreload` and `skysongstats.reload` inside it.
- **Command permissions** such as `skysongstats.stats` and `skysongstats.profile` are not declared in `plugin.yml`, so non-ops need them granted (see [permissions](#fixed-nodes-from-pluginyml)).
- **`/modifier` on yourself** only needs `skysongstats.modifier`; only acting on others has extra nodes.
- **Ops have every skill level and proficiency tier node** by default.
- **Stat, skill, modifier and proficiency commands** only target online players; downtime commands also work on offline ones.
- **The database ping** prints a line to the console every 5 minutes.
- **`docs/current-architecture.md`** describes the plugin *before* the refactors (its downtime section, data model, commands and permissions are out of date). Keep it as history; this manual is the current reference.

---

## Where things live in the code

All code is under `src/main/java/org/skysongdev/skysongstats/`; default files are under `src/main/resources/`.

| Part | Package / file |
|---|---|
| Startup, reload, wiring | `SkysongStats.java` (`onEnable`, `reloadSettings`) |
| Reading and checking stats.yml, skills.yml, proficiencies.yml | `definition/` (`*ConfigLoader`, `*Validator`) |
| Formulas | `formula/Formula.java` |
| Stat rules (actions, limits, shields, modifiers) | `character/StatService.java`, `character/StatCalculator.java` |
| Skill XP rules | `character/SkillService.java` |
| Proficiency rules (focus, training, tiers) | `character/ProficiencyService.java` |
| Downtime rules | `service/downtime/DowntimeService.java` |
| Stat commands, /modifier, generated stat commands and permissions | `commands/stat/` |
| /skills, /proficiency, /downtime, /profile, /char, /setup, /ssreload | `commands/<name>/` |
| /setup screens | `setup/` |
| LuckPerms skill nodes / proficiency nodes | `skill/`, `proficiency/` |
| Messages | `messages/Messages.java`, `messages/MessagesFile.java` |
| Placeholders | `placeholders/` |
| Events | `events/` |
| Database: stat, skill, proficiency tables | `storage/` |
| Database: profiles, setup, characters | `database/Database.java` |
| Skysong item tags | `items/`, `Utils/SkysongItemsKeys.java` |
| Join handling, shift right click | `listeners/` |
