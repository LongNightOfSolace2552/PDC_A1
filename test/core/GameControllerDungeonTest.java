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
 
       /*plan on adding test case for enemy, enemy boss and randomization*/
    }
    
    /*continue here*/
}
