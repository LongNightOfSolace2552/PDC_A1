package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.core.GameController;
import main.domain.combat.Boss;
import main.domain.combat.Dungeon;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.domain.world.Node;
import main.domain.world.NodeLocation;
import main.services.ActionResult;
import main.util.Randomizer;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 PASS/FAIL tests for the checks GameController makes before a
fight (handleDungeon, handleBossChallenge) and its dungeon helpers
(hasEnemies, createDungeon, getBoss): does a node have anything to
fight, can its boss be challenged, does a new dungeon start at floor 1.
the GUI shows or hides its Explore and Boss buttons from exactly these
answers.
*/
public class GameControllerDungeonTest {

    private Node withEnemiesAndBoss;
    private Node withEnemiesOnly;
    private Node withBossOnly;
    private Node withNothing;
    private Node bossMissingFromData;
    private Boss chieftain;
    private Player player;
    private GameController controller;

    @Before
    public void setUp() {
        withEnemiesAndBoss = node("village", "Starting Village", true);
        withEnemiesOnly = node("meadow", "Quiet Meadow", false);
        withBossOnly = node("tower", "Lonely Tower", true);
        withNothing = node("void", "Empty Void", false);
        bossMissingFromData = node("ghost", "Ghost Town", true);

        EnemyLoot noLoot = new EnemyLoot(0.0, 0, 0, 0.0, null);
        Enemy slime = new Enemy("slime", "Slime", new Stats(1, 1, 1), noLoot);
        chieftain = new Boss("chieftain", "Village Chieftain", new Stats(5, 5, 5), noLoot);

        Map<String, List<Enemy>> pools = new HashMap<>();
        pools.put("village", List.of(slime));
        pools.put("meadow", List.of(slime));

        Map<String, Boss> bosses = new HashMap<>();
        bosses.put("village", chieftain);
        bosses.put("tower", chieftain);

        /*
        only the enemy pools, bosses and randomizer matter here; save, 
        travel and combat are left out.
        */
        controller = new GameController(null, null, null, pools, bosses, new ZeroRandomizer());
        player = new Player("Hero", new Stats(0, 0, 0), "village");
    }

    private Node node(String id, String name, boolean hasBoss) {
        return new Node(id, new NodeLocation(id, name, "..."), 1, hasBoss);
    }
    
    /*function names are named exactly what it describes to test.*/
    
    @Test
    public void hasEnemies_isTrueOnlyForNodesWithAnEnemyPool() {
        assertTrue(controller.hasEnemies("village"));
        assertTrue(controller.hasEnemies("meadow"));
        assertFalse(controller.hasEnemies("tower"));
        assertFalse("an unknown node has no enemies", controller.hasEnemies("nowhere"));
    }

    @Test
    public void handleDungeon_passesForANodeWithEnemies() {
        assertTrue(controller.handleDungeon(withEnemiesOnly).isPass());
    }

    @Test
    public void handleDungeon_passesForANodeWithOnlyABoss() {
        assertTrue(controller.handleDungeon(withBossOnly).isPass());
    }

    @Test
    public void handleDungeon_failsWhenThereIsNothingToFight() {
        ActionResult result = controller.handleDungeon(withNothing);

        assertTrue(result.isFail());
        assertEquals("There is nothing to fight here yet.", result.getMessage());
    }

    @Test
    public void createDungeon_startsAFreshDungeonAtFloorOne() {
        Dungeon dungeon = controller.createDungeon(withEnemiesAndBoss);

        assertEquals(1, dungeon.getFloor());
        assertEquals("village", dungeon.getNodeId());
        assertEquals("Slime", dungeon.nextEnemy().getName());
    }

    @Test
    public void handleBossChallenge_passesWithTheBossIntroduction() {
        ActionResult result = controller.handleBossChallenge(player, withEnemiesAndBoss);

        assertTrue(result.isPass());
        assertEquals("A Village Chieftain blocks your path.", result.getMessage());
    }
    
    @Test
    public void handleBossChallenge_failsOnceTheBossIsDefeated() {
        player.markBossDefeated("village");

        ActionResult result = controller.handleBossChallenge(player, withEnemiesAndBoss);

        assertTrue(result.isFail());
        assertEquals("You have already defeated this node's boss.", result.getMessage());
    }
    
    @Test
    public void handleBossChallenge_failsOnANodeWithoutABoss() {
        ActionResult result = controller.handleBossChallenge(player, withEnemiesOnly);

        assertTrue(result.isFail());
        assertEquals("This node has no boss.", result.getMessage());
    }

    @Test
    public void handleBossChallenge_failsWhenTheBossDataIsMissing() {
        ActionResult result = controller.handleBossChallenge(player, bossMissingFromData);

        assertTrue(result.isFail());
        assertEquals("This node's boss data could not be found.", result.getMessage());
    }

    @Test
    public void getBoss_returnsTheNodesBossOrNull() {
        assertSame(chieftain, controller.getBoss("village"));
        assertNull(controller.getBoss("meadow"));
    }

    @Test(expected = NullPointerException.class)
    public void handleDungeon_withoutANode_isRejected() {
        controller.handleDungeon(null);
    }

    /* always rolls 0: every floor needs the minimum number of enemies and the first enemy in a pool is picked. */
    private static class ZeroRandomizer extends Randomizer {
        @Override
        public double nextDouble() {
            return 0.0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
