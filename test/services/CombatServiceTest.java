package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.CombatResult;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.items.Accessory;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.services.combat.CombatService;
import main.services.combat.CombatServiceImpl;
import main.services.combat.DialogueService;
import main.util.Randomizer;
import org.junit.Test;

/*
JUnit4 tests for CombatServiceImpl.resolveFight(): win/loss resolution
with a fixed random roll, guaranteed item drops, and guaranteed stat
loss on a defeat - the core business logic of the game.
*/
public class CombatServiceTest {

    @Test
    public void resolveFight_lowRollAgainstWeakEnemyWins() {
        CombatService combatService = buildCombatService(0.0, new HashMap<>());
        Player player = new Player("Hero", new Stats(10, 10, 10), "village");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(1, 1, 1), new EnemyLoot(0.0, 0, 0, 0.0, null));

        CombatResult result = combatService.resolveFight(player, enemy);

        assertTrue("a low roll against a weak enemy should win", result.isWon());
        assertFalse("dialogue lines should be produced", result.getDialogueLines().isEmpty());
    }

    @Test
    public void resolveFight_highRollAgainstStrongEnemyLoses() {
        CombatService combatService = buildCombatService(0.999, new HashMap<>());
        Player player = new Player("Hero", new Stats(1, 1, 1), "village");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(10, 10, 10), new EnemyLoot(0.0, 0, 0, 0.0, null));

        CombatResult result = combatService.resolveFight(player, enemy);

        assertFalse("a high roll against a strong enemy should lose", result.isWon());
    }

    @Test
    public void resolveFight_guaranteedDropReturnsTheCatalogItem() {
        Map<String, Item> catalog = new HashMap<>();
        Item reward = new Accessory("lucky_charm", "Lucky Charm", 0, 0, 1, null);
        catalog.put(reward.getId(), reward);

        CombatService combatService = buildCombatService(0.0, catalog);
        Player player = new Player("Hero", new Stats(10, 10, 10), "village");
        EnemyLoot guaranteedDrop = new EnemyLoot(0.0, 0, 0, 1.0, "lucky_charm");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(1, 1, 1), guaranteedDrop);

        CombatResult result = combatService.resolveFight(player, enemy);

        assertTrue(result.isWon());
        assertTrue("a 100% item drop chance should drop an item", result.hasDroppedItem());
        assertEquals(reward, result.getDroppedItem());
    }

    @Test
    public void resolveFight_lossThatPassesTheStatLossGateReducesAStat() {
        /*
        a single fixed roll can't force both "lose the fight" (needs a
        HIGH roll against winChance) and "the post-loss stat-loss gate
        passes" (needs a LOW roll against getPendingLossChance()) at
        once, since resolveFight() draws both from the same source in
        sequence - so this uses a stub that returns a high roll first
        (loses the fight) then a low roll after (passes the gate).
        */
        CombatService combatService = buildCombatService(new SequencedRandomizer(0.999, 0.0), new HashMap<>());
        Player player = new Player("Hero", new Stats(5, 0, 0), "village");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(10, 10, 10), new EnemyLoot(0.0, 0, 0, 0.0, null));

        CombatResult result = combatService.resolveFight(player, enemy);

        assertFalse(result.isWon());
        assertEquals("the only positive stat (Strength) should be the one reduced",
                4, player.getStats().getStrength());
    }

    private CombatService buildCombatService(double fixedRoll, Map<String, Item> catalog) {
        return buildCombatService(new FixedRandomizer(fixedRoll), catalog);
    }

    private CombatService buildCombatService(Randomizer randomizer, Map<String, Item> catalog) {
        DialogueService noOpDialogueService = (statState, playerName, enemyName) -> List.of(playerName + " vs " + enemyName);
        return new CombatServiceImpl(randomizer, catalog, noOpDialogueService);
    }

    /* stands in for Randomizer so a fight's roll is fixed instead of truly random. */
    private static class FixedRandomizer extends Randomizer {
        private final double fixedRoll;

        FixedRandomizer(double fixedRoll) {
            this.fixedRoll = fixedRoll;
        }

        @Override
        public double nextDouble() {
            return fixedRoll;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    /* returns each given roll in order, then keeps returning the last one; nextInt always picks index 0 (Strength). */
    private static class SequencedRandomizer extends Randomizer {
        private final double[] rolls;
        private int index = 0;

        SequencedRandomizer(double... rolls) {
            this.rolls = rolls;
        }

        @Override
        public double nextDouble() {
            double roll = rolls[Math.min(index, rolls.length - 1)];
            index++;
            return roll;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
