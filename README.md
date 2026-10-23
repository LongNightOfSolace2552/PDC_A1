# RPGGame

An RPG game written in Java, built by two collaborators. Part 1 was a console game; Part 2 (this version) replaces the console interface with a Swing GUI on top of the same game logic.

## Concept

On starting the game, a name-entry screen asks for a player name. If a save already exists under that name it loads (with a "welcome back" message); otherwise a brand-new character is created. The main menu shows the player's name, current node location, and dungeon difficulty, with five buttons:

- **Travel** - move to another node
- **Node** - read about the current node and its boss
- **Dungeon** - fight enemies and bosses at the current node
- **Stats / Inventory** - stats, equipment and stat points
- **Save & Quit**

Both players and enemies (including bosses) share three core stats: Strength, Magic, and Agility. There are currently three nodes - `village`, `forest`, `cave` - each with its own enemy pool, its own boss, and increasing difficulty. Travel to a later node is locked until the current node's boss has been defeated.

## Combat

Combat is resolved by `CombatService`, and every fight - regular enemy or boss - goes through the exact same engine, since `Boss extends Enemy`.

**Win chance is additive across Strength and Magic**, not tied to a single stat: your Strength is compared against the enemy's Strength, your Magic against the enemy's Magic, and the two resulting chances are added together. This means a player who has invested entirely in one stat can still win against an enemy who threatens mainly on the other - just at a reduced chance compared to actually matching the enemy's relevant stat. An enemy stat of `0` means it's immune on that axis (a big number there doesn't help, but doesn't hurt either). On top of that, Agility contributes a ratio-based dodge modifier for both sides, weighed against the opponent's combined offensive stats.

Which weapon you have equipped (`CombatStyle`: `MELEE`, `RANGED`, or `MAGE`) doesn't change which stats count toward winning - both Strength and Magic always matter - but it does change how the fight is *narrated*, and whether a weapon's `ItemAbility` (a chance to activate for a temporary Strength boost) can apply. Fighting unarmed automatically frames the narration around whichever of Strength/Magic you've actually invested more into.

Narration itself comes from `DialogueService`: after a fight resolves, its outcome is classified into a `StatState` (`WAY_AHEAD`, `AHEAD`, `AGILITY_CLUTCH` for an underdog win, or `BEHIND` for a loss), and templated lines for that state (including several dodge/"complex maneuver" flavored ones) are picked from the `DIALOGUE_TEMPLATE` table of the Derby database and revealed one line at a time on the combat screen, each line driving a clash animation on the battle stage.

**Winning** may drop a catalog item and/or grant stat points, both driven by the enemy's own `EnemyLoot` (a chance to trigger, then - for stat points - a random amount within a min/max range). **Losing** doesn't always cost anything: there's a chance gate (30% at low Power Level, 70% once a player's effective Power Level passes a threshold) before a single random stat is reduced by 1 (or 2, at high Power Level) - and a stat already at 0 is left alone.

## Dungeon

**Dungeon** opens a screen at your current node with up to four buttons - Explore, Challenge the boss, Fight until the end, and Leave the dungeon. A button only appears when it can be used (see "Which buttons appear when" below).

- **Explore** shows the enemy that appears and lets you Fight or Retreat; a fight returns you to the dungeon screen afterwards.
- **Challenge the boss** fights the node's real boss (same combat engine as regular enemies) - defeating it unlocks travel to the next node.
- **Fight until the end** auto-chains fights with no per-fight prompt, stopping only on a loss.
- Enemies are grouped into **floors**: each floor requires defeating a random 2-5 enemies before advancing, with a freshly-rolled quota for the next floor. Losing a fight, or leaving the dungeon, both reset floor progress back to 1 - nothing about dungeon depth is saved between visits.

## Items & Equipment

Items come from a shared abstract `Item` base (`getItemType()` is overridden polymorphically), with two concrete types: `Weapon` (carries a `CombatStyle` and optionally an `ItemAbility`) and `Accessory` (rings, amulets - stat bonuses only, may also carry an `ItemAbility`). A player has **two separate equip slots** - one Weapon, one Accessory - both usable at once; `Player.equip()` routes an item to the correct slot automatically based on its actual type.

The full item catalog lives in the `ITEM` table of the Derby database. New characters start with a Wooden Dagger and Silver Ring already equipped.

## Player Progression

Base stats start at 0/0/0 - all early growth comes from equipment and from **stat points**, which are earned (not guaranteed) from defeating enemies and spent one at a time with the + Strength / + Magic / + Agility buttons on the Stats / Inventory screen. "Power Level" (the sum of a player's or enemy's effective Strength + Magic + Agility) determines when the harsher loss-consequence chance/amount kicks in.

## Saves

Each player profile is a row in the embedded Apache Derby database (`PLAYER`, with the carried items in `PLAYER_INVENTORY` and the beaten bosses in `PLAYER_DEFEATED_BOSS`), keyed by the character name typed on the first screen (up to 50 characters). Typing an existing profile's name loads it; typing a new name creates a fresh character. A save captures stats, inventory, both equipped items, current node, boss-defeat progress, and unallocated stat points, and is written as one transaction - either all three tables are updated or none are.

## Project Structure

- `RPGDB/` - The embedded Derby database, created on the first run in the project root (git-ignored).
- `lib/` - `derby.jar` (Apache Derby 10.14.2.0, embedded) used by the game, and JUnit 4.13.2 and Hamcrest 1.3 used by the test source root.
- `nbproject/` - NetBeans project metadata.
- `src/main/` - Application source, under the `main` package (and subpackages matching folder structure):
  - `App` - the entry point: builds the game with `AppConfig` and calls `Game.start()`, which opens the window (same as Part 1, where `start()` ran the console)
  - `config/` - `AppConfig` (loads the data files and wires everything in one place; `buildGame()` returns the `Game` the GUI is given)
  - `core/` - `Game` (`start()` opens the window; `initializePlayer` turns a typed name into a character), `GameController` (the game's action functions - each `handleX` is a PASS/FAIL check), `CombatSequencer` (plays a fight's dialogue lines one at a time), `SessionStart` (what starting a session produced), `SaveManager`
  - `gui/` - Swing screens, in the place Part 1's `cui/` was: `GameFrame` (the window), one JPanel per screen, `CombatPanel` + `BattleDisplayPanel` (the fight), `TransitionPanel` (slides between screens) - see "Part 2" below
  - `domain/player/` - `Player`, `Stats`, `StatType`
  - `domain/world/` - `Node`, `NodeLocation`
  - `domain/items/` - `Item` (abstract), `Weapon`, `Accessory`, `ItemAbility`, `CombatStyle`
  - `domain/combat/` - `Enemy`, `Boss`, `EnemyLoot`, `Dungeon`, `CombatResult`, `FightOutcome`, `DialogueTemplate`, `BattleAction`
  - `services/` - `ActionResult`, the PASS/FAIL result every checked game action returns
  - `services/travel/` - `TravelService`/`TravelServiceImpl` (node lookup, node locking)
  - `services/combat/` - `CombatService`/`CombatServiceImpl`, `DialogueService`/`DialogueServiceImpl`
  - `persistence/` - the data access layer. Derby: `DatabaseManager` (the Singleton that owns the connection), `DatabaseCreateTables` (schema), `DatabaseInsertData` (the game data, inserted once) and one DAO per kind of data (`ItemDatabaseRepository`, `NodeDatabaseRepository`, `EnemyDatabaseRepository`, `DialogueDatabaseRepository`, `PlayerDatabaseRepository`). The Part 1 text-file versions (`FileReaderUtil`/`FileWriterUtil` and the `*FileRepository` classes) are still here but the game no longer uses them
  - `exceptions/` - `GameException` (base) and three specific subclasses for data/save/lookup failures
  - `util/` - `Randomizer`, `TextFormatter`
- `test/` - JUnit 4 tests, its own source root (so `test/core/GameControllerTravelTest.java` is package `core`, and `test/services/CombatServiceTest.java` is package `services`, not `main.services`). See "Testing" below.

## Object-Oriented Design

- **Abstraction**: `Item` is abstract; every service is defined as an interface (`CombatService`, `TravelService`, `DialogueService`) separate from its implementation.
- **Encapsulation**: domain state (`Player`, `Stats`, `Enemy`) is only reachable through its own methods - inventory, equipped items, and stat mutation all go through controlled methods, never direct field access.
- **Inheritance**: `Weapon extends Item`, `Accessory extends Item`, `Boss extends Enemy`.
- **Polymorphism**: `Item.getItemType()` is overridden differently by `Weapon`/`Accessory`; `CombatService.resolveFight()` accepts any `Enemy`, so a `Boss` slots in and is fought with the identical engine, with zero special-casing needed.
- **Exceptions**: `GameException` is the base checked exception, with `DataLoadException`, `SaveDataException`, and `ItemNotFoundException` covering data/save/lookup failures specifically. Expected game outcomes (a locked node, an empty slot, no stat points left) are not exceptions - they come back as a FAIL `ActionResult`.

## Design Patterns & SOLID

Patterns actually present in the codebase, not retrofitted for the sake
of a checklist:

- **MVC**: the domain classes and services (`Player`, `Stats`, `Enemy`, `TravelService`, `CombatService`, ...) are the model; they know nothing about Swing. The `gui/` JPanels are the view. `GameController` is the controller, exactly as in Part 1: it holds the game's actions and rules as PASS/FAIL functions. `GameFrame` just navigates between screens, and each panel's listeners turn a click into one `GameController` call and show what comes back. The GUI never decides anything itself (whether a button exists at all is read from `GameController` too).
- **DAO**: every `*DatabaseRepository` class in `persistence/` is a Data Access Object - the only code that knows about SQL, tables and `ResultSet`s. Everything else talks to it in terms of domain objects (`Map<String, Item>`, `Player`, `List<Node>`, ...). That boundary is why moving from text files to Derby changed `persistence/`, `AppConfig` and `SaveManager` and nothing in the services or the GUI. All SQL uses `PreparedStatement`s, and `PlayerDatabaseRepository.save` writes the player and everything they own in one transaction.
- **Singleton**: `DatabaseManager` hands out one shared Derby connection per run (`getManager()` and a private constructor), so the DAOs never open competing connections to the embedded database.
- **Factory / composition root**: `AppConfig` is the single place the whole dependency graph gets built (`buildGame()`, called by `App`). No other class ever calls `new SomeServiceImpl(...)` itself - everything is handed its collaborators from here, which is also what Dependency Inversion (below) relies on.
- **Observer**: the GUI is built directly on Swing's listener model - every `button.addActionListener(...)` registers an observer that's notified when that button (the subject) fires. This is why GUI code never polls state; screens react to events instead.
- **Result object**: every checked game action returns an immutable `ActionResult` (PASS or FAIL, plus a message) instead of printing or throwing. The GUI shows it; a test just asserts `isPass()` / `isFail()`.

SOLID, with a concrete example of each rather than just naming them:

- **S**ingle Responsibility: `GameController` only decides whether a player action is allowed (PASS/FAIL) and applies it - it hands movement and locking to `TravelService`, fight resolution to `CombatService`, and saving to `SaveManager`; `Game` only starts a session; each `*DatabaseRepository` only loads/saves one kind of data. Each GUI screen only displays state and forwards clicks. The post-fight rules exist once (`applyDungeonFight` / `applyBossFight`), not copied into each screen.
- **O**pen/Closed: `CombatService` is an interface; `CombatServiceImpl` can be replaced or a second implementation added without changing anything that calls `resolveFight()`. `Item` is abstract and closed for modification, but open for extension via `Weapon`/`Accessory`.
- **L**iskov Substitution: `Boss extends Enemy`, and `CombatService.resolveFight(Player, Enemy)` fights a `Boss` through the identical engine with zero special-casing - substitutability isn't just claimed, `DungeonPanel` relies on it by handing a `Boss` to the same combat screen as any other `Enemy`.
- **I**nterface Segregation: the services are small, focused interfaces (`TravelService`, `CombatService`, `DialogueService`) rather than one do-everything `GameService`.
- **D**ependency Inversion: `GameController` depends on the service *interfaces* (`TravelService`, `CombatService`) and on `SaveManager` - never on an `*Impl`, a repository or a data file - and `GameFrame` and every panel only ever receive the `Game` (and through it the `GameController`). Only `AppConfig` knows the concrete types, exactly as the Factory point above describes.

## Part 2: GUI (Swing)

The console interface from Part 1 has been replaced by a Swing GUI. The
project layout is the same as Part 1; only the `cui/` folder became
`gui/`, and everything in it is now a plain Swing screen (default look,
no custom colours or borders) with the same functionality - things you
used to type are now buttons. `GameController`, `Game` and
`CombatSequencer` stay in `core/`, and each function keeps its name and
its rules; the checked ones return a PASS or FAIL `ActionResult`
instead of printing, so the GUI and the unit tests call the very same
functions. The domain model and the file persistence are unchanged.

Where each Part 1 `cui/` class went:

| Part 1 (`cui/`) | Part 2 (`gui/`) |
|---|---|
| `MenuRenderer` | `MenuPanel` - the main menu with its five buttons |
| `ConsoleRenderer` | `TravelPanel`, `NodePanel`, `DungeonPanel`, `InventoryPanel`, plus `JOptionPane` dialogs for messages |
| `CommandParser` | not needed - buttons and the name field replace typed commands (`NameEntryPanel` for the name) |
| `CombatRenderer` | `CombatPanel` (log + Skip/Continue) with `BattleDisplayPanel` (the square and circle) |
| `CombatSequencer` | stays in `core/` and still plays the fight's lines one at a time, now on a Swing `Timer` |
| the loop in `Game.start()` | `Game.start()` opens `GameFrame`; `GameFrame` moves between the panels |

Where each Part 1 function went:

| Part 1 | Part 2 |
|---|---|
| `GameController.handleTravel` | `handleTravel(player, nodeId)` - FAILs for an unknown node, a locked node, or the node the player is already at |
| `GameController.handleDungeon` | `handleDungeon(node)` - FAILs when there is nothing to fight; `createDungeon(node)` makes the fresh Dungeon for each visit |
| `handleExplore`, `handleFightUntilEnd` | the screen picks the enemy and plays the fight; `applyDungeonFight(...)` applies the drop, floor advance or reset |
| `handleBossChallenge` | `handleBossChallenge(player, node)` - FAILs with no boss, an already-beaten boss, or missing boss data; `applyBossFight(...)` applies the result |
| `handleEquip`, `handleUnequipWeapon`, `handleUnequipAccessory`, `handleAllocateStatPoints` | same names; Equip takes the clicked item, and allocating spends one point per click |
| `renderHeader`, `handleNode` | display only: `findCurrentNode(player)` returns the node for the main-menu header and the Node screen |
| `saveAndQuit` | `saveProgress(player)` - the window closes itself after a PASS, so a failed save never loses progress |
| `Game.initializePlayer`, `createNewPlayer` | same names; `initializePlayer(name)` returns a `SessionStart` (PASS with a Player, or FAIL for a blank name) |

Persistence moved from the Part 1 text files to an embedded Apache Derby
database (10.14.2.0, through JDBC). On the first run `AppConfig` creates the
tables and inserts the same items, nodes, enemies, bosses and dialogue the
text files held; from then on it only reads them. Nothing has to be set up
by hand.

### Running it

```
ant run       # launches the GUI (main.App -> Game.start() -> main.gui.GameFrame)
ant run-gui   # same thing (alias)
```

In NetBeans just press Run - `main.App` is the project's main class.

### Which buttons appear when

A button only exists when its action is possible, so there is nothing to click that would just fail:

- **Travel** screen: a Travel button only on a node that is unlocked and is not the current one (locked nodes and "you are here" show a label instead).
- **Stats / Inventory**: Equip on each item actually in the inventory; Unequip Weapon / Unequip Accessory only while that slot is filled; the + Strength / + Magic / + Agility buttons only while there are banked stat points.
- **Dungeon**: Explore and Fight until the end only where the node has enemies; Challenge the boss only while the node's boss is still undefeated. A short line explains a missing button (e.g. "You have already defeated this node's boss.").
- A node with nothing at all to fight keeps you on the main menu with a message.

### Structure (`src/main/gui/`)

- `GameFrame` - the one `JFrame` everything runs in. It holds the screens and the shared `Player`, and has a method per destination (`showTravel()`, `showDungeon()`, `showCombat(...)`, ...). With `GameController` as the controller and the domain/services as the model, the panels are the view - a straightforward MVC split.
- `NameEntryPanel`, `MenuPanel`, `TravelPanel`, `NodePanel`, `DungeonPanel`, `InventoryPanel` - one JPanel per screen. Each forwards a click to `Game` / `GameController` and shows the PASS/FAIL result that comes back.
- `CombatPanel` / `BattleDisplayPanel` - the fight. `CombatService.resolveFight()` resolves the whole exchange in one call, so `CombatPanel` hands the resulting dialogue lines to `CombatSequencer`, which reveals them one at a time on a `javax.swing.Timer` (a sleeping thread would freeze the window). Each line is added to the log and moves the placeholder sprites on `BattleDisplayPanel`: the player is a blue square on the left and the enemy an orange circle on the right (a bigger red one for a boss). A line about a dodge slides the dodger sideways, away from the other, and back; any other line makes the two step toward each other and back; when the fight ends the loser turns grey. **Skip** shows the rest of the log at once and **Continue** returns to the dungeon menu.
- `TransitionPanel` - the window's content pane; it slides the next screen in from the right over about 200 ms (a `Timer`, no library) instead of switching instantly. It is the only animation besides the fight, so delete it (and use `setContentPane` directly) if page transitions aren't wanted.

The last-resort error handler (any unexpected exception is shown once in a dialog instead of vanishing on the Swing thread) lives in `App`.

## Build

This project builds with Ant (`build.xml`), targeting JDK 25. `App.java` and its subpackages live under `src/main/` on disk, which maps to `package main;` and `package main.<subfolder>;` in code - so the Ant source root is `src` (not `src/main`), letting the `main` folder itself act as the top-level package. Tests are their own separate source root at `test/`, so `test/services/...` is package `services`, without a `main.` prefix.

Open the folder directly in NetBeans 28 as a Java project with an existing Ant script (File > New Project > Java with Existing Ant Script, pointing at `build.xml`) - no manual setup needed beyond that. From the command line:

```
ant compile      # compile to build/classes
ant jar          # package into dist/RPG_Game_GUI.jar
ant run          # run the game (launches the Swing GUI)
ant run-gui      # alias for run
ant test         # run the JUnit 4 suite (jars are in lib/) - see "Testing"
ant clean        # remove build/ and dist/
```

`manifest.mf` sets the runnable jar's entry point (`Main-Class: main.App`) and is used by the `jar` target - the jar launches the GUI. Run it from the project root so the game creates and finds its `RPGDB/` database folder there.

## Testing

JUnit 4.13.2 (+ Hamcrest 1.3) ship in `lib/` and are already wired into `nbproject/project.properties`, so `ant test` / NetBeans' **Test Project** work with no manual setup. **Run the game and the tests from the project root** - the Derby database folder `RPGDB/` is created in the working directory (the tests use throw-away in-memory databases and never touch it).

The console interface is gone, but its checks live on: every action the console menus used to make (travel, equip, allocate points, boss challenge, save...) is a `GameController` function that returns **PASS or FAIL**, and each test asserts one of those outcomes.

| Test class | Layer | What a pass/fail means |
|---|---|---|
| `core.GameControllerTravelTest` | business logic | `handleTravel` PASSes only for an unlocked node that is not the current one, and FAILs (without moving the player) for an unknown, locked or current node; node lookups |
| `core.GameControllerCharacterTest` | business logic | `handleEquip` / `handleUnequipWeapon` / `handleUnequipAccessory` / `handleAllocateStatPoints`: PASS changes the player, FAIL (not in inventory, empty slot, no points) leaves it untouched |
| `core.GameControllerDungeonTest` | business logic | `handleDungeon` (can a node be entered), `handleBossChallenge` (no boss / already beaten / missing data FAIL), new dungeon at floor 1 |
| `core.GameControllerFightTest` | business logic | `applyDungeonFight` / `applyBossFight`: drops, floor progress only once the quota is met, reset on a loss, boss defeat only on a win; `resolveFight` hands the fight to `CombatService` |
| `core.GameSessionTest` | business logic + database | `Game.initializePlayer`: blank or over-long name FAILs; new character gets the starter items; returning player is welcomed with progress intact; unreadable save starts a new game with a notice. `saveProgress` PASSes into Derby and FAILs when the database cannot be written |
| `core.CombatSequencerTest` | business logic | the first line shows at once, the rest in order, the finish callback runs exactly once (after the last line or a skip, never after a stop) |
| `services.TravelServiceTest` | business logic | node-locking rules, travel and lookup in `TravelServiceImpl` |
| `services.CombatServiceTest` | business logic | win/loss resolution, drops, stat loss, with a fixed `Randomizer` so results are deterministic |
| `services.DialogueServiceTest` | business logic | narration lines; regression test for a missing `StatState` |
| `persistence.PlayerDatabaseRepositoryTest` | database | a player (stats, equipment, inventory order, bosses, points) survives a save/load round trip in Derby; saving twice updates instead of duplicating; two players stay separate; a save that fails half way is rolled back and leaves the old save intact; unknown players, unknown items and a closed connection are reported as `SaveDataException` |
| `persistence.DatabaseSetupTest` | database | tables can be created again on an existing database; the game data is inserted once and comes back as the right domain objects (nodes in unlock order, items with abilities, bosses, dialogue); the tables refuse rows that break their rules; `DatabaseManager` is a Singleton |
| `persistence.FileRepositoryTest` | data access (Part 1 text files) | a player survives a save/load round trip through the old `PlayerFileRepository` |

The tests (98 in all) were also checked by *mutation*: 23 deliberate breakages of `GameController` and `Game` (a disabled lock check, a boss that can be re-fought, drops that never arrive, saves that are never loaded, a failed save reported as PASS, ...) and 12 of `PlayerDatabaseRepository`, `DatabaseCreateTables` and `DatabaseInsertData` (no rollback after a failed save, inventory rows not cleared before re-saving, an existing player never updated, auto-commit left off, seed data inserted twice, ...) were each confirmed to make at least one test fail.

The Swing screens themselves have no automated tests; their button rules and the fight screen (Skip / Continue, the sprite movement) were checked by driving the real screens under a virtual display during development, including a run that quits with Save & Quit and a second run that checks the player is back.

## Status

Implemented: travel and node locking, full dungeon crawl with floors, real combat (regular enemies and bosses) with dialogue narration, dual equipment slots with weapon abilities, stat point allocation, and multi-profile save/load in an embedded Apache Derby database - all playable through the Swing GUI (see "Part 2" above).

Not yet implemented: enemy pools beyond the three current nodes (adding one means adding rows in `DatabaseInsertData`, or in the `ENEMY` table of an existing database), and any UI for renaming or deleting an existing save profile.

Known limits: only one copy of the game can run at a time, because an embedded Derby database can be opened by one program only (a second copy shows "Failed to load game data" at start-up). The game data is inserted when the database is first created, so changing it in `DatabaseInsertData` only takes effect after deleting `RPGDB/`.
