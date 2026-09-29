package services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.CombatResult;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.services.combat.CombatService;
import main.services.combat.CombatServiceImpl;
import main.services.combat.DialogueService;
import main.util.Randomizer;

/* demonstrates CombatService.resolveFight() producing a deterministic outcome when the random roll is fixed. */
public class CombatServiceTest {

    public static void main(String[] args) {
        testAlwaysWins();
        testAlwaysLoses();
    }

    private static void testAlwaysWins() {
        CombatService combatService = buildCombatService(0.0);
        Player player = new Player("Hero", new Stats(10, 10, 10), "village");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(1, 1, 1), new EnemyLoot(0.0, 0, 0, 0.0, null));

        CombatResult result = combatService.resolveFight(player, enemy);

        check("low roll wins the fight", result.isWon());
        check("dialogue lines are produced", !result.getDialogueLines().isEmpty());
    }

    private static void testAlwaysLoses() {
        CombatService combatService = buildCombatService(0.999);
        Player player = new Player("Hero", new Stats(1, 1, 1), "village");
        Enemy enemy = new Enemy("test_enemy", "Test Enemy", new Stats(10, 10, 10), new EnemyLoot(0.0, 0, 0, 0.0, null));

        CombatResult result = combatService.resolveFight(player, enemy);

        check("high roll loses the fight", !result.isWon());
    }

    private static CombatService buildCombatService(double fixedRoll) {
        Map<String, Item> emptyCatalog = new HashMap<>();
        DialogueService noOpDialogueService = (statState, playerName, enemyName) -> List.of(playerName + " vs " + enemyName);
        return new CombatServiceImpl(new FixedRandomizer(fixedRoll), emptyCatalog, noOpDialogueService);
    }

    private static void check(String description, boolean passed) {
        System.out.println((passed ? "PASS: " : "FAIL: ") + description);
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
}
