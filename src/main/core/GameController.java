package main.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import main.cui.CommandParser;
import main.cui.ConsoleRenderer;
import main.domain.combat.Boss;
import main.domain.combat.CombatResult;
import main.domain.combat.Dungeon;
import main.domain.combat.Enemy;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.StatType;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;
import main.exceptions.SaveDataException;
import main.services.combat.CombatService;
import main.services.travel.TravelService;
import main.util.Randomizer;

/* orchestrates services and CUI rendering in response to player actions. */
public class GameController {

    private final ConsoleRenderer consoleRenderer;
    private final CommandParser commandParser;
    private final SaveManager saveManager;
    private final TravelService travelService;
    private final CombatService combatService;
    private final CombatSequencer combatSequencer;
    private final Map<String, List<Enemy>> enemyPoolsByNode;
    private final Map<String, Boss> bossesByNode;
    private final Randomizer randomizer;

    public GameController(ConsoleRenderer consoleRenderer, CommandParser commandParser,
                           SaveManager saveManager, TravelService travelService,
                           CombatService combatService, CombatSequencer combatSequencer,
                           Map<String, List<Enemy>> enemyPoolsByNode, Map<String, Boss> bossesByNode,
                           Randomizer randomizer) {
        this.consoleRenderer = consoleRenderer;
        this.commandParser = commandParser;
        this.saveManager = saveManager;
        this.travelService = travelService;
        this.combatService = combatService;
        this.combatSequencer = combatSequencer;
        this.enemyPoolsByNode = enemyPoolsByNode;
        this.bossesByNode = bossesByNode;
        this.randomizer = randomizer;
    }

    /*
    renders the status header for the player's current node. Falls back
    to a plain message if the player's saved node id no longer exists
    in the node data (e.g. node data changed since the save was made).
    */
    public void renderHeader(Player player) {
        try {
            Node currentNode = travelService.findNode(player.getCurrentNodeId());
            consoleRenderer.printHeader(player, currentNode);
        } catch (ItemNotFoundException e) {
            consoleRenderer.printError("Current node '" + player.getCurrentNodeId() + "' is unknown: " + e.getMessage());
        }
    }

    /* returns true if the game loop should stop after this action. */
    public boolean handle(int choice, Player player) {
        switch (choice) {
            case 1:
                handleTravel(player);
                return false;
            case 2:
                handleNode(player);
                return false;
            case 3:
                handleDungeon(player);
                return false;
            case 4:
                handleInventory(player);
                return false;
            case 5:
                return saveAndQuit(player);
            default:
                /*
                commandParser already restricts choice to a valid range,
                so this should be unreachable.
                */
                consoleRenderer.printError("Unknown option: " + choice);
                return false;
        }
    }

    private void handleTravel(Player player) {
        List<Node> nodes = travelService.listNodes();
        if (nodes.isEmpty()) {
            consoleRenderer.printMessage("There is nowhere to travel to.");
            return;
        }

        List<Boolean> locked = new ArrayList<>();
        for (Node node : nodes) {
            locked.add(travelService.isLocked(node, player));
        }

        consoleRenderer.printNodeList(nodes, locked);
        int choice = commandParser.readValidChoice(0, nodes.size());
        if (choice == 0) {
            consoleRenderer.printMessage("Travel cancelled.");
            return;
        }

        Node destination = nodes.get(choice - 1);
        if (locked.get(choice - 1)) {
            consoleRenderer.printError(destination.getName() + " is locked. Defeat the previous node's boss first.");
            return;
        }

        if (destination.getId().equals(player.getCurrentNodeId())) {
            consoleRenderer.printMessage("You are already at " + destination.getName() + ".");
            return;
        }

        try {
            travelService.travelTo(player, destination.getId());
            consoleRenderer.printMessage("You travel to " + destination.getName() + ".");
        } catch (ItemNotFoundException e) {
            consoleRenderer.printError(e.getMessage());
        }
    }

    private void handleNode(Player player) {
        try {
            Node currentNode = travelService.findNode(player.getCurrentNodeId());
            boolean bossDefeated = player.hasDefeatedBoss(currentNode.getId());
            consoleRenderer.printNodeInfo(currentNode, bossDefeated);
        } catch (ItemNotFoundException e) {
            consoleRenderer.printError(e.getMessage());
        }
    }

    /*
    dungeon menu: explore for a random enemy from the current node's
    pool (one fight, then back to this menu), challenge the node's boss,
    fight continuously until a loss, or leave. both fight options are
    shown even when the node has no enemies, with a message explaining
    why, rather than hiding them.
    */
    private void handleDungeon(Player player) {
        Node currentNode;
        try {
            currentNode = travelService.findNode(player.getCurrentNodeId());
        } catch (ItemNotFoundException e) {
            consoleRenderer.printError(e.getMessage());
            return;
        }

        List<Enemy> pool = enemyPoolsByNode.getOrDefault(currentNode.getId(), Collections.emptyList());
        if (pool.isEmpty() && !currentNode.hasBoss()) {
            consoleRenderer.printMessage("There is nothing to fight here yet.");
            return;
        }

        /*
        a fresh Dungeon per visit - floor progress is not saved between
        dungeon sessions, so re-entering later starts back at floor 1.
        */
        Dungeon dungeon = new Dungeon(randomizer, enemyPoolsByNode, currentNode.getId());

        boolean inDungeon = true;
        while (inDungeon) {
            consoleRenderer.printMessage("");
            consoleRenderer.printMessage("You are in the dungeon at " + currentNode.getName()
                    + " - Floor " + dungeon.getFloor() + ".");
            consoleRenderer.printMessage("[1]: Explore   [2]: Challenge the boss   "
                    + "[3]: Fight until the end   [4]: Leave the dungeon");
            int choice = commandParser.readValidChoice(1, 4);

            switch (choice) {
                case 1:
                    handleExplore(player, dungeon);
                    break;
                case 2:
                    handleBossChallenge(player, currentNode);
                    break;
                case 3:
                    handleFightUntilEnd(player, dungeon);
                    break;
                case 4:
                    inDungeon = false;
                    break;
                default:
                    /* readValidChoice already restricts to 1-4. */
                    break;
            }
        }
    }

    private void handleExplore(Player player, Dungeon dungeon) {
        Enemy enemy = dungeon.nextEnemy();
        if (enemy == null) {
            consoleRenderer.printMessage("There are no enemies to find here yet.");
            return;
        }

        consoleRenderer.printMessage("A " + enemy.getName() + " appears! (Floor " + dungeon.getFloor() + ")");
        consoleRenderer.printMessage("[1]: Fight   [2]: Retreat");
        int choice = commandParser.readValidChoice(1, 2);

        if (choice == 2) {
            consoleRenderer.printMessage("You retreat before the fight begins.");
            return;
        }

        CombatResult result = combatService.resolveFight(player, enemy);
        combatSequencer.play(result.getDialogueLines());

        if (result.hasDroppedItem()) {
            player.addItem(result.getDroppedItem());
        }

        if (result.isWon()) {
            advanceFloorIfCleared(dungeon);
        } else {
            dungeon.resetProgress();
            consoleRenderer.printMessage("Your dungeon progress resets - back to Floor 1.");
        }
    }

    /*
    auto-chains fights with no per-fight prompt, one after another,
    until the player loses. each win still checks for floor advancement
    and drops, same as a single Explore fight.
    */
    private void handleFightUntilEnd(Player player, Dungeon dungeon) {
        consoleRenderer.printMessage("You press onward, fighting without pause...");

        boolean continueFighting = true;
        while (continueFighting) {
            Enemy enemy = dungeon.nextEnemy();
            if (enemy == null) {
                consoleRenderer.printMessage("There are no enemies to find here yet.");
                return;
            }

            consoleRenderer.printMessage("A " + enemy.getName() + " appears! (Floor " + dungeon.getFloor() + ")");
            CombatResult result = combatService.resolveFight(player, enemy);
            combatSequencer.play(result.getDialogueLines());

            if (result.hasDroppedItem()) {
                player.addItem(result.getDroppedItem());
            }

            if (result.isWon()) {
                advanceFloorIfCleared(dungeon);
            } else {
                dungeon.resetProgress();
                consoleRenderer.printMessage("Your dungeon progress resets - back to Floor 1.");
                consoleRenderer.printMessage("You can no longer continue - returning to the dungeon menu.");
                continueFighting = false;
            }
        }
    }

    private void advanceFloorIfCleared(Dungeon dungeon) {
        int floorBefore = dungeon.getFloor();
        dungeon.recordVictory();
        if (dungeon.getFloor() > floorBefore) {
            consoleRenderer.printMessage("You've cleared this floor! Descending to Floor " + dungeon.getFloor() + ".");
        }
    }

    private void handleBossChallenge(Player player, Node currentNode) {
        if (!currentNode.hasBoss()) {
            consoleRenderer.printMessage("This node has no boss.");
            return;
        }

        if (player.hasDefeatedBoss(currentNode.getId())) {
            consoleRenderer.printMessage("You have already defeated this node's boss.");
            return;
        }

        Boss boss = bossesByNode.get(currentNode.getId());
        if (boss == null) {
            consoleRenderer.printError("This node's boss data could not be found.");
            return;
        }

        consoleRenderer.printMessage("A " + boss.getName() + " blocks your path.");
        consoleRenderer.printMessage("[1]: Fight   [2]: Retreat");
        int choice = commandParser.readValidChoice(1, 2);

        if (choice == 2) {
            consoleRenderer.printMessage("You retreat.");
            return;
        }

        CombatResult result = combatService.resolveFight(player, boss);
        combatSequencer.play(result.getDialogueLines());

        if (result.hasDroppedItem()) {
            player.addItem(result.getDroppedItem());
        }

        if (result.isWon()) {
            player.markBossDefeated(currentNode.getId());
            consoleRenderer.printMessage("You defeated the boss! The path onward is now unlocked.");
        }
    }

    private void handleInventory(Player player) {
        boolean back = false;
        while (!back) {
            consoleRenderer.printStats(player);
            consoleRenderer.printInventoryMenu();
            int choice = commandParser.readValidChoice(1, 5);
            switch (choice) {
                case 1:
                    handleEquip(player);
                    break;
                case 2:
                    handleUnequipWeapon(player);
                    break;
                case 3:
                    handleUnequipAccessory(player);
                    break;
                case 4:
                    handleAllocateStatPoints(player);
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    /* readValidChoice already restricts to 1-5. */
                    break;
            }
        }
    }

    private void handleEquip(Player player) {
        List<Item> inventory = player.getInventory();
        if (inventory.isEmpty()) {
            consoleRenderer.printMessage("Your inventory is empty.");
            return;
        }

        consoleRenderer.printItemList(inventory);
        int choice = commandParser.readValidChoice(0, inventory.size());
        if (choice == 0) {
            return;
        }

        Item chosen = inventory.get(choice - 1);
        if (player.equip(chosen)) {
            consoleRenderer.printMessage("Equipped " + chosen.getName() + ".");
        } else {
            consoleRenderer.printError("Could not equip " + chosen.getName() + ".");
        }
    }

    private void handleUnequipWeapon(Player player) {
        if (!player.hasEquippedWeapon()) {
            consoleRenderer.printMessage("You have no weapon equipped.");
            return;
        }
        String name = player.getEquippedWeapon().getName();
        player.unequipWeapon();
        consoleRenderer.printMessage("Unequipped " + name + ".");
    }

    private void handleUnequipAccessory(Player player) {
        if (!player.hasEquippedAccessory()) {
            consoleRenderer.printMessage("You have no accessory equipped.");
            return;
        }
        String name = player.getEquippedAccessory().getName();
        player.unequipAccessory();
        consoleRenderer.printMessage("Unequipped " + name + ".");
    }

    /*
    lets the player spend banked stat points (earned from defeated
    enemies) one at a time, stopping whenever they choose or when the
    points run out.
    */
    private void handleAllocateStatPoints(Player player) {
        if (player.getUnallocatedStatPoints() <= 0) {
            consoleRenderer.printMessage("You have no stat points to allocate.");
            return;
        }

        while (player.getUnallocatedStatPoints() > 0) {
            consoleRenderer.printMessage("Unallocated points: " + player.getUnallocatedStatPoints());
            consoleRenderer.printMessage("[1]: Strength   [2]: Magic   [3]: Agility   [0]: Stop");
            int choice = commandParser.readValidChoice(0, 3);
            if (choice == 0) {
                return;
            }

            StatType type;
            switch (choice) {
                case 1:
                    type = StatType.STRENGTH;
                    break;
                case 2:
                    type = StatType.MAGIC;
                    break;
                default:
                    type = StatType.AGILITY;
                    break;
            }

            player.allocateStatPoint(type);
            consoleRenderer.printMessage("Allocated 1 point to " + type + ".");
        }
    }

    private boolean saveAndQuit(Player player) {
        try {
            saveManager.savePlayer(player, player.getName());
            consoleRenderer.printMessage("Progress saved. Goodbye, " + player.getName() + "!");
        } catch (SaveDataException e) {
            consoleRenderer.printError("Could not save your progress: " + e.getMessage());
        }
        return true;
    }
}