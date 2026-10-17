package main.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import java.util.Objects;
import main.domain.combat.FightOutcome;
import main.services.ActionResult;

/* orchestrates services and CUI rendering in response to player actions. */
public class GameController {

     private final SaveManager saveManager;
    private final TravelService travelService;
    private final CombatService combatService;
    private final Map<String, List<Enemy>> enemyPoolsByNode;
    private final Map<String, Boss> bossesByNode;
    private final Randomizer randomizer;

    public GameController(SaveManager saveManager, TravelService travelService,
                           CombatService combatService,
                           Map<String, List<Enemy>> enemyPoolsByNode, Map<String, Boss> bossesByNode,
                           Randomizer randomizer) {
        this.saveManager = saveManager;
        this.travelService = travelService;
        this.combatService = combatService;
        this.enemyPoolsByNode = enemyPoolsByNode;
        this.bossesByNode = bossesByNode;
        this.randomizer = randomizer;
    }

    /*
    the node the player is currently at. throws if the player's saved
    node id no longer exists in the node data (e.g. node data changed
    since the save was made) - the screen decides how to word that.
    (what renderHeader and handleNode used to look up before printing.)
    */
    public Node findCurrentNode(Player player) throws ItemNotFoundException {
        return travelService.findNode(player.getCurrentNodeId());
    }

    public List<Node> listNodes() {
        return travelService.listNodes();
    }

    public boolean isLocked(Node node, Player player) {
        return travelService.isLocked(node, player);
    }

    /*
    travel: FAILs (and leaves the player where they are) if the node is
    unknown, still locked, or already the player's current node;
    otherwise moves the player and PASSes.
    */
    public ActionResult handleTravel(Player player, String nodeId) {
        Objects.requireNonNull(player, "player");

        Node destination;
        try {
            destination = travelService.findNode(nodeId);
        } catch (ItemNotFoundException e) {
            return ActionResult.fail(e.getMessage());
        }

        if (travelService.isLocked(destination, player)) {
            return ActionResult.fail(destination.getName() + " is locked. Defeat the previous node's boss first.");
        }

        if (destination.getId().equals(player.getCurrentNodeId())) {
            return ActionResult.fail("You are already at " + destination.getName() + ".");
        }

        try {
            travelService.travelTo(player, destination.getId());
        } catch (ItemNotFoundException e) {
            return ActionResult.fail(e.getMessage());
        }
        return ActionResult.pass("You travel to " + destination.getName() + ".");
    }

    /* true if the node has at least one regular enemy to fight - the Explore and Fight buttons only exist then. */
    public boolean hasEnemies(String nodeId) {
        return !enemyPoolsByNode.getOrDefault(nodeId, Collections.emptyList()).isEmpty();
    }

    /* the node's boss, or null if it has none. */
    public Boss getBoss(String nodeId) {
        return bossesByNode.get(nodeId);
    }

    /*
    dungeon entry: PASS if the node has regular enemies or a boss to
    fight; FAILs with a message when there is nothing to fight here yet.
    */
    public ActionResult handleDungeon(Node currentNode) {
        Objects.requireNonNull(currentNode, "currentNode");

        if(!hasEnemies(currentNode.getId()) && !currentNode.hasBoss()) {
            return ActionResult.fail("There is nothing to fight here yet.");
        }
        return ActionResult.pass("You enter the dungeon at " + currentNode.getName() + ".");
    }

    /*a fresh Dungeon per visit - floor progress is not saved between
    dungeon sessions, so re-entering later starts back at floor 1.*/
    public Dungeon createDungeon(Node currentNode) {
        Objects.requireNonNull(currentNode,"currentNode");
        return new Dungeon(randomizer, enemyPoolsByNode, currentNode.getId());
    }

    /*resolves one fight (regular enemy or boss) with no delay; 
    the dialogue lines are replayed by the screen. */
    public CombatResult resolveFight(Player player, Enemy enemy) {
        return combatService.resolveFight(player, enemy);
    }

    /*
    what a finished dungeon fight changes (the second half of
    handleExplore / handleFightUntilEnd): adds any drop, then advances
    the floor on a win or resets dungeon progress on a loss.
    */
    public FightOutcome applyDungeonFight(Player player, Dungeon dungeon, CombatResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(dungeon, "dungeon");
        Objects.requireNonNull(result, "result");

        addDroppedItem(player, result);

        if (result.isWon()) {
            int floorBefore = dungeon.getFloor();
            dungeon.recordVictory();
            return FightOutcome.dungeonWin(dungeon.getFloor() > floorBefore, dungeon.getFloor());
        }

        dungeon.resetProgress();
        return FightOutcome.dungeonLoss(dungeon.getFloor());
    }

    /*
    boss challenge: PASS if the node's boss can be fought right now,
    with the boss's introduction as the message. FAILs when the node has
    no boss, the boss is already defeated, or its data is missing.
    */
    public ActionResult handleBossChallenge(Player player, Node currentNode) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(currentNode, "currentNode");

        if (!currentNode.hasBoss()) {
            return ActionResult.fail("This node has no boss.");
        }

        if (player.hasDefeatedBoss(currentNode.getId())) {
            return ActionResult.fail("You have already defeated this node's boss.");
        }

        Boss boss = getBoss(currentNode.getId());
        if (boss == null) {
            return ActionResult.fail("This node's boss data could not be found.");
        }

        return ActionResult.pass("A " + boss.getName() + " blocks your path.");
    }

    /* what a finished boss fight changes: adds any drop, and marks the node's boss defeated on a win. */
    public FightOutcome applyBossFight(Player player, String bossNodeId, CombatResult result) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(bossNodeId, "bossNodeId");
        Objects.requireNonNull(result, "result");

        addDroppedItem(player, result);

        if (result.isWon()) {
            player.markBossDefeated(bossNodeId);
        }
        return FightOutcome.bossFight(result.isWon());
    }

    private void addDroppedItem(Player player, CombatResult result) {
        if (result.hasDroppedItem()) {
            player.addItem(result.getDroppedItem());
        }
    }

    /* equips an item from the player's inventory; FAILs if there is no such item to equip. */
    public ActionResult handleEquip(Player player, Item chosen) {
        Objects.requireNonNull(player, "player");

        if (chosen == null) {
            return ActionResult.fail("There is no item to equip.");
        }

        if (player.equip(chosen)) {
            return ActionResult.pass("Equipped " + chosen.getName() + ".");
        }
        return ActionResult.fail("Could not equip " + chosen.getName() + ".");
    }

    public ActionResult handleUnequipWeapon(Player player) {
        Objects.requireNonNull(player, "player");

        if (!player.hasEquippedWeapon()) {
            return ActionResult.fail("You have no weapon equipped.");
        }
        String name = player.getEquippedWeapon().getName();
        player.unequipWeapon();
        return ActionResult.pass("Unequipped " + name + ".");
    }

    public ActionResult handleUnequipAccessory(Player player) {
        Objects.requireNonNull(player, "player");

        if (!player.hasEquippedAccessory()) {
            return ActionResult.fail("You have no accessory equipped.");
        }
        String name = player.getEquippedAccessory().getName();
        player.unequipAccessory();
        return ActionResult.pass("Unequipped " + name + ".");
    }

    /*
    spends ONE banked stat point (earned from defeated enemies) on the
    chosen stat; the screen calls it once per click, so the player can
    stop whenever they choose. FAILs when no points are left.
    */
    public ActionResult handleAllocateStatPoints(Player player, StatType type) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");

        if (!player.allocateStatPoint(type)) {
            return ActionResult.fail("You have no stat points to allocate.");
        }
        return ActionResult.pass("Allocated 1 point to " + type + ".");
    }

    /*
    saves the player's progress under their own name. (saveAndQuit
    before - the window now closes itself once this PASSes, so a failed
    save keeps the game open instead of losing progress.)
    */
    public ActionResult saveProgress(Player player) {
        Objects.requireNonNull(player, "player");

        try {
            saveManager.savePlayer(player, player.getName());
            return ActionResult.pass("Progress saved. Goodbye, " + player.getName() + "!");
        } catch (SaveDataException e) {
            return ActionResult.fail("Could not save your progress: " + e.getMessage());
        }
    }
}