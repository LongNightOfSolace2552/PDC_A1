# RPGGame

A console-based (CUI) RPG game written in Java, built by two collaborators (see `@author` tags throughout the source).

## Concept

On starting the game, you're asked for a player name. If a save already exists under that name, it loads; otherwise a brand-new character is created. The CUI displays the player's name, current node location, and dungeon difficulty, with a main menu:

```
[1]: Travel
[2]: Node
[3]: Dungeon
[4]: Stats/Inventory
[5]: Save & Quit
```

Both players and enemies (including bosses) share three core stats: Strength, Magic, and Agility. There are currently three nodes - `village`, `forest`, `cave` - each with its own enemy pool, its own boss, and increasing difficulty. Travel to a later node is locked until the current node's boss has been defeated.

## Combat

Combat is resolved by `CombatService`, and every fight - regular enemy or boss - goes through the exact same engine, since `Boss extends Enemy`.

**Win chance is additive across Strength and Magic**, not tied to a single stat: your Strength is compared against the enemy's Strength, your Magic against the enemy's Magic, and the two resulting chances are added together. This means a player who has invested entirely in one stat can still win against an enemy who threatens mainly on the other - just at a reduced chance compared to actually matching the enemy's relevant stat. An enemy stat of `0` means it's immune on that axis (a big number there doesn't help, but doesn't hurt either). On top of that, Agility contributes a ratio-based dodge modifier for both sides, weighed against the opponent's combined offensive stats.

Which weapon you have equipped (`CombatStyle`: `MELEE`, `RANGED`, or `MAGE`) doesn't change which stats count toward winning - both Strength and Magic always matter - but it does change how the fight is *narrated*, and whether a weapon's `ItemAbility` (a chance to activate for a temporary Strength boost) can apply. Fighting unarmed automatically frames the narration around whichever of Strength/Magic you've actually invested more into.

Narration itself comes from `DialogueService`: after a fight resolves, its outcome is classified into a `StatState` (`WAY_AHEAD`, `AHEAD`, `AGILITY_CLUTCH` for an underdog win, or `BEHIND` for a loss), and templated lines for that state (including several dodge/"complex maneuver" flavored ones) are picked from `data/dialogue_templates.txt` and played back one line at a time via `CombatSequencer`, on a background thread with a short delay between lines.

**Winning** may drop a catalog item and/or grant stat points, both driven by the enemy's own `EnemyLoot` (a chance to trigger, then - for stat points - a random amount within a min/max range). **Losing** doesn't always cost anything: there's a chance gate (30% at low Power Level, 70% once a player's effective Power Level passes a threshold) before a single random stat is reduced by 1 (or 2, at high Power Level) - and a stat already at 0 is left alone.

## Dungeon

`[3] Dungeon` opens a submenu at your current node:

```
[1]: Explore
[2]: Challenge the boss
[3]: Fight until the end
[4]: Leave the dungeon
```

- **Explore** fights one random enemy from the node's pool, then returns to this menu.
- **Challenge the boss** fights the node's real boss (same combat engine as regular enemies) - defeating it unlocks travel to the next node.
- **Fight until the end** auto-chains fights with no per-fight prompt, stopping only on a loss.
- Enemies are grouped into **floors**: each floor requires defeating a random 2-5 enemies before advancing, with a freshly-rolled quota for the next floor. Losing a fight, or leaving the dungeon, both reset floor progress back to 1 - nothing about dungeon depth is saved between visits.

## Items & Equipment

Items come from a shared abstract `Item` base (`getItemType()` is overridden polymorphically), with two concrete types: `Weapon` (carries a `CombatStyle` and optionally an `ItemAbility`) and `Accessory` (rings, amulets - stat bonuses only, may also carry an `ItemAbility`). A player has **two separate equip slots** - one Weapon, one Accessory - both usable at once; `Player.equip()` routes an item to the correct slot automatically based on its actual type.

The full item catalog lives in `data/items.txt`. New characters start with a Wooden Dagger and Silver Ring already equipped.

## Player Progression

Base stats start at 0/0/0 - all early growth comes from equipment and from **stat points**, which are earned (not guaranteed) from defeating enemies and spent one at a time via the Stats/Inventory menu's allocation option. "Power Level" (the sum of a player's or enemy's effective Strength + Magic + Agility) determines when the harsher loss-consequence chance/amount kicks in.

## Saves

Each player profile is its own save file, named after the character name typed at startup (sanitized to a safe filename), stored under `data/player_saves/`. Typing an existing profile's name loads it; typing a new name creates a fresh character. A save captures stats, inventory, both equipped items, current node, boss-defeat progress, and unallocated stat points.

## Project Structure

- `data/` - Text files for items, enemies (per-node pools and bosses), nodes, node locations, dialogue templates, and one save file per player profile (under `data/player_saves/`).
- `images/` - ASCII art assets for the console UI.
- `lib/` - External JARs, if needed (e.g. JUnit, for the `test` target in `build.xml`).
- `nbproject/` - NetBeans project metadata.
- `src/main/` - Application source, under the `main` package (and subpackages matching folder structure):
  - `config/` - `AppConfig`, all dependency wiring in one place
  - `core/` - `Game` (main loop), `GameController` (menu dispatch), `SaveManager`, `CombatSequencer`
  - `cui/` - `ConsoleRenderer`, `MenuRenderer`, `CommandParser`, `CombatRenderer`
  - `domain/player/` - `Player`, `Stats`, `StatType`
  - `domain/world/` - `Node`, `NodeLocation`
  - `domain/items/` - `Item` (abstract), `Weapon`, `Accessory`, `ItemAbility`, `CombatStyle`
  - `domain/combat/` - `Enemy`, `Boss`, `EnemyLoot`, `Dungeon`, `CombatResult`, `DialogueTemplate`, `BattleAction`
  - `services/travel/` - `TravelService`/`TravelServiceImpl`
  - `services/combat/` - `CombatService`/`CombatServiceImpl`, `DialogueService`/`DialogueServiceImpl`
  - `persistence/` - `FileReaderUtil`/`FileWriterUtil`, and one repository per data file (`ItemFileRepository`, `EnemyFileRepository`, `NodeFileRepository`, `PlayerFileRepository`, `DialogueFileRepository`)
  - `exceptions/` - `GameException` (base) and four specific subclasses for data/save/input/lookup failures
  - `util/` - `Randomizer`, `TextFormatter`
- `test/` - Unit tests, its own source root (so `test/services/CombatServiceTest.java` is package `services`, not `main.services`).

## Object-Oriented Design

- **Abstraction**: `Item` is abstract; every service is defined as an interface (`CombatService`, `TravelService`, `DialogueService`) separate from its implementation.
- **Encapsulation**: domain state (`Player`, `Stats`, `Enemy`) is only reachable through its own methods - inventory, equipped items, and stat mutation all go through controlled methods, never direct field access.
- **Inheritance**: `Weapon extends Item`, `Accessory extends Item`, `Boss extends Enemy`.
- **Polymorphism**: `Item.getItemType()` is overridden differently by `Weapon`/`Accessory`; `CombatService.resolveFight()` accepts any `Enemy`, so a `Boss` slots in and is fought with the identical engine, with zero special-casing needed.
- **Exceptions**: `GameException` is the base checked exception, with `DataLoadException`, `SaveDataException`, `ItemNotFoundException`, and `InvalidCommandException` covering data/save/input/lookup failures specifically.

## Build

This project builds with Ant (`build.xml`), targeting JDK 25. `App.java` and its subpackages live under `src/main/` on disk, which maps to `package main;` and `package main.<subfolder>;` in code - so the Ant source root is `src` (not `src/main`), letting the `main` folder itself act as the top-level package. Tests are their own separate source root at `test/`, so `test/services/...` is package `services`, without a `main.` prefix.

Open the folder directly in NetBeans 28 as a Java project with an existing Ant script (File > New Project > Java with Existing Ant Script, pointing at `build.xml`) - no manual setup needed beyond that. From the command line:

```
ant compile      # compile to build/classes
ant jar          # package into dist/RPGGame.jar
ant run          # run the application
ant test         # run tests (requires a JUnit jar under lib/)
ant clean        # remove build/ and dist/
```

`manifest.mf` sets the runnable jar's entry point (`Main-Class: main.App`) and is used by the `jar` target.

## Status

Implemented: travel and node locking, full dungeon crawl with floors, real combat (regular enemies and bosses) with dialogue narration, dual equipment slots with weapon abilities, stat point allocation, and multi-profile save/load.

Not yet implemented: enemy pools beyond the three current nodes (adding one is just adding rows to `data/enemies.txt`), and any UI for renaming or deleting an existing save profile.
