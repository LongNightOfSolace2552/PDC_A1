package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.core.GameController;
import main.domain.combat.Boss;
import main.domain.combat.CombatResult;
import main.domain.combat.Dungeon;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.combat.FightOutcome;
import main.domain.items.Accessory;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.services.combat.CombatService;
import main.util.Randomizer;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 PASS/FAIL tests for what GameController applies after a finished
fight (applyDungeonFight, applyBossFight) - the item drop, floor
progress on a win, the reset on a loss, and boss defeat - plus
resolveFight handing the fight to the combat service. these rules live
in one place, so a screen never needs a copy of its own.
*/
public class GameControllerFightTest {

    private GameController controller;
    private Player player;
    private Dungeon dungeon;
    private Item charm;

    @Before
    public void setUp() {
        /* applying a fight's outcome only changes the player and dungeon; save and travel are left out. */
        controller = new GameController(null, null, null, new HashMap<>(), new HashMap<>(), new ZeroRandomizer());
        player = new Player("Hero", new Stats(0, 0, 0), "village");
        charm = new Accessory("charm", "Lucky Charm", 0, 0, 1, null);

        Enemy slime = new Enemy("slime", "Slime", new Stats(1, 1, 1), new EnemyLoot(0.0, 0, 0, 0.0, null));
        Map<String, List<Enemy>> pools = new HashMap<>();
        pools.put("village", List.of(slime));
        /* a zero randomizer makes every floor need exactly 2 wins. */
        dungeon = new Dungeon(new ZeroRandomizer(), pools, "village");
    }

    private CombatResult win(Item drop) {
        return new CombatResult(true, List.of("..."), drop);
    }

    private CombatResult loss() {
        return new CombatResult(false, List.of("..."), null);
    }

    @Test
    public void applyDungeonFight_win_reportsAWin_andAddsTheDropToTheInventory() {
        FightOutcome outcome = controller.applyDungeonFight(player, dungeon, win(charm));

        assertTrue(outcome.isWon());
        assertFalse(outcome.isProgressReset());
        assertTrue(player.getInventory().contains(charm));
    }

    @Test
    public void applyDungeonFight_win_withoutADrop_leavesTheInventoryAlone() {
        controller.applyDungeonFight(player, dungeon, win(null));

        assertTrue(player.getInventory().isEmpty());
    }

    @Test
    public void applyDungeonFight_win_advancesTheFloorOnlyOnceTheQuotaIsMet() {
        FightOutcome first = controller.applyDungeonFight(player, dungeon, win(null));
        assertFalse("one win is not enough to clear a floor of two", first.isFloorAdvanced());
        assertEquals(1, first.getFloor());

        FightOutcome second = controller.applyDungeonFight(player, dungeon, win(null));
        assertTrue("the second win clears the floor", second.isFloorAdvanced());
        assertEquals(2, second.getFloor());
        assertEquals(2, dungeon.getFloor());
    }

    @Test
    public void applyDungeonFight_loss_reportsALossAndResetsTheDungeonToFloorOne() {
        controller.applyDungeonFight(player, dungeon, win(null));
        controller.applyDungeonFight(player, dungeon, win(null));
        assertEquals("setup: the player reached floor 2", 2, dungeon.getFloor());

        FightOutcome outcome = controller.applyDungeonFight(player, dungeon, loss());

        assertFalse(outcome.isWon());
        assertTrue(outcome.isProgressReset());
        assertFalse(outcome.isFloorAdvanced());
        assertEquals(1, outcome.getFloor());
        assertEquals(1, dungeon.getFloor());
    }

    @Test
    public void applyDungeonFight_loss_withoutADrop_leavesTheInventoryAlone() {
        controller.applyDungeonFight(player, dungeon, loss());

        assertTrue(player.getInventory().isEmpty());
    }

    @Test
    public void applyBossFight_win_marksTheBossDefeatedAndAddsTheDrop() {
        FightOutcome outcome = controller.applyBossFight(player, "village", win(charm));

        assertTrue(outcome.isWon());
        assertTrue(outcome.isBossDefeated());
        assertTrue(player.hasDefeatedBoss("village"));
        assertTrue(player.getInventory().contains(charm));
    }

    @Test
    public void applyBossFight_loss_doesNotMarkTheBossDefeated() {
        FightOutcome outcome = controller.applyBossFight(player, "village", loss());

        assertFalse(outcome.isWon());
        assertFalse(outcome.isBossDefeated());
        assertFalse(player.hasDefeatedBoss("village"));
    }

    @Test(expected = NullPointerException.class)
    public void applyDungeonFight_withoutAResult_isRejected() {
        controller.applyDungeonFight(player, dungeon, null);
    }

    @Test(expected = NullPointerException.class)
    public void applyBossFight_withoutAPlayer_isRejected() {
        controller.applyBossFight(null, "village", win(null));
    }

    @Test
    public void resolveFight_handsTheFightToTheCombatService() {
        Enemy slime = new Enemy("slime", "Slime", new Stats(1, 1, 1), new EnemyLoot(0.0, 0, 0, 0.0, null));
        CombatResult expected = win(charm);
        Enemy[] fought = new Enemy[1];
        CombatService combat = (fightingPlayer, enemy) -> {
            fought[0] = enemy;
            return expected;
        };
        GameController withCombat = new GameController(null, null, combat, new HashMap<>(), new HashMap<>(),
                new ZeroRandomizer());

        CombatResult result = withCombat.resolveFight(player, slime);

        assertSame("the combat service's own result comes straight back", expected, result);
        assertSame("and it was asked to fight the enemy given", slime, fought[0]);
    }

    @Test
    public void resolveFight_worksForABossToo() {
        Boss chieftain = new Boss("chieftain", "Chieftain", new Stats(5, 5, 5), new EnemyLoot(0.0, 0, 0, 0.0, null));
        CombatService combat = (fightingPlayer, enemy) -> loss();
        GameController withCombat = new GameController(null, null, combat, new HashMap<>(), new HashMap<>(),
                new ZeroRandomizer());

        assertFalse(withCombat.resolveFight(player, chieftain).isWon());
    }

    /* always rolls 0, so every floor needs the minimum (2) number of enemy defeats. */
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
