package main.services.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import main.domain.combat.CombatResult;
import main.domain.combat.DialogueTemplate.StatState;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.items.CombatStyle;
import main.domain.items.Item;
import main.domain.items.Weapon;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.util.Randomizer;

/*
implementation of CombatService.

win chance is additive across both Strength and Magic, rather than
picking a single axis by weapon style: each stat is compared against the
enemy's same stat independently (its own threshold/immunity/floor/ceiling,
all at half-scale), and the two resulting chances are added together.

this means a player heavily invested in one stat can still win against an
enemy who threatens mainly on the other stat, just at a reduced chance
compared to actually matching the enemy's relevant stat, since an enemy
stat of 0 on an axis is immune there (a big number on that axis does not
help) and a real deficit on the other axis still drags the total down.
when both axes are simultaneously at parity, the two halves sum back to
the same 76% "full match" value when both axes are individually at parity.

agility plays no part in this threshold, it is purely a dodge stat,
layered on afterward exactly as before: a ratio of agility against the
opponent's total offensive stats, contributing to both the player's own
dodge and the enemy's dodge on the player.

the outcome (win or loss, and how favorable it was) is narrated through
DialogueService, which picks templated lines by StatState rather than a
fixed hardcoded line every time.
*/
public class CombatServiceImpl implements CombatService {

    private static final double PARITY_WIN_CHANCE_PER_AXIS = 0.38;
    private static final double WIN_CHANCE_FLOOR_PER_AXIS = 0.20;
    private static final double WIN_CHANCE_CEILING_PER_AXIS = 0.5;
    private static final double ADVANTAGE_STEP = 0.05;
    private static final double DISADVANTAGE_STEP = 0.05;
    private static final double IMMUNE_BASE_CHANCE_PER_AXIS = 0.30;
    private static final double DODGE_INFLUENCE_SCALE = 0.2;
    private static final double WAY_AHEAD_THRESHOLD = 0.75;
    private static final double AHEAD_THRESHOLD = 0.5;

    private final Randomizer randomizer;
    private final Map<String, Item> itemCatalog;
    private final DialogueService dialogueService;

    public CombatServiceImpl(Randomizer randomizer, Map<String, Item> itemCatalog, DialogueService dialogueService) {
        this.randomizer = randomizer;
        this.itemCatalog = itemCatalog;
        this.dialogueService = dialogueService;
    }

    @Override
    public CombatResult resolveFight(Player player, Enemy enemy) {
        List<String> lines = new ArrayList<>();

        Weapon weapon = player.getEquippedWeapon();
        CombatStyle style = (weapon != null) ? weapon.getCombatStyle() : null;

        Stats effective = player.getEffectiveStats();
        /*
        magicAxis is purely cosmetic, it only picks which flavor of
        unarmed dialogue line to show. it no longer decides which stat(s)
        count toward the win chance; both always do.
        */
        boolean magicAxis = isMagicAxis(effective, style);

        int playerStrength = effective.getStrength();
        int playerMagic = effective.getMagic();

        lines.add(openingLine(player, enemy, style, magicAxis));

        if (weapon != null && weapon.hasAbility()) {
            boolean activated = weapon.getAbility().rollActivation(randomizer);
            if (activated) {
                int bonus = weapon.getAbility().getBonusStrength();
                lines.add(player.getName() + "'s " + weapon.getAbility().getName()
                        + " flares to life! (+" + bonus + " Strength)");
                /*
                strength always contributes to the additive total now, so
                the ability's bonus always matters, regardless of weapon style.
                */
                playerStrength += bonus;
            }
        }

        Stats enemyStats = enemy.getStats();
        int enemyStrength = enemyStats.getStrength();
        int enemyMagic = enemyStats.getMagic();

        double strengthChance = computeAxisChance(playerStrength, enemyStrength);
        double magicChance = computeAxisChance(playerMagic, enemyMagic);
        double baseChance = strengthChance + magicChance;

        /*
        dodge, unchanged mechanism, now measured against each side's
        combined Strength+Magic total, to match the additive model.
        */
        int enemyAttackTotal = enemyStrength + enemyMagic;
        double playerDodgeContribution = computeDodgeRatio(effective.getAgility(), enemyAttackTotal) * DODGE_INFLUENCE_SCALE;

        int playerAttackTotal = playerStrength + playerMagic;
        double enemyDodgeContribution = computeDodgeRatio(enemyStats.getAgility(), playerAttackTotal) * DODGE_INFLUENCE_SCALE;

        if (playerDodgeContribution > 0) {
            lines.add(player.getName() + " stays light on their feet, watching for a chance to dodge.");
        }
        if (enemyDodgeContribution > 0) {
            lines.add("The " + enemy.getName() + " weaves unpredictably, hard to pin down.");
        }

        lines.add(attackLine(player, enemy, style, magicAxis));

        double winChance = clamp(baseChance + playerDodgeContribution - enemyDodgeContribution, 0.0, 1.0);
        boolean won = randomizer.nextDouble() < winChance;
        StatState outcomeState = determineOutcomeState(baseChance, won);
        lines.addAll(dialogueService.buildBattleLines(outcomeState, player.getName(), enemy.getName()));

        Item droppedItem = null;
        if (won) {
            droppedItem = rollDrop(enemy);
            if (droppedItem != null) {
                lines.add("The " + enemy.getName() + " drops " + droppedItem.getName() + "!");
            }

            int reward = rollStatReward(enemy);
            if (reward > 0) {
                player.addUnallocatedStatPoints(reward);
                lines.add(player.getName() + " gains " + reward + " stat point(s) to allocate!");
            }
        } else {
            String lostStat = player.applyRandomStatLoss(randomizer);
            if (lostStat != null) {
                lines.add(player.getName() + " loses " + lostStat + " in the struggle.");
            } else {
                lines.add(player.getName() + " narrowly avoids losing anything more.");
            }
        }

        return new CombatResult(won, lines, droppedItem);
    }

    /*
    decides which flavor of unarmed dialogue to show (Strength-flavored
    vs Magic-flavored). purely cosmetic now, no longer used to decide
    which stat(s) count toward the win chance, since both always do.
    */
    private boolean isMagicAxis(Stats effectiveStats, CombatStyle style) {
        if (style == CombatStyle.MAGE) {
            return true;
        }
        if (style == null) {
            return effectiveStats.getMagic() > effectiveStats.getStrength();
        }
        return false; /* MELEE, RANGED */
    }

    /*
    one axis's contribution to the additive win chance (called once for
    Strength, once for Magic, then summed). half-scale of what a single
    combined axis used to be, so two axes simultaneously at parity add
    back up to the same 76% a full match gives.
    */
    private double computeAxisChance(int playerStat, int enemyStat) {
        if (enemyStat == 0) {
            return IMMUNE_BASE_CHANCE_PER_AXIS;
        }
        if (playerStat >= enemyStat) {
            int excess = playerStat - enemyStat;
            return Math.min(WIN_CHANCE_CEILING_PER_AXIS, PARITY_WIN_CHANCE_PER_AXIS + excess * ADVANTAGE_STEP);
        }
        int deficit = enemyStat - playerStat;
        return Math.max(WIN_CHANCE_FLOOR_PER_AXIS, PARITY_WIN_CHANCE_PER_AXIS - deficit * DISADVANTAGE_STEP);
    }

    /*
    picks which StatState narrates this fight's outcome, based on how
    favorable the raw stat comparison was (baseChance, before dodge) and
    whether the player actually won. a loss is always BEHIND. a win with
    a low baseChance means the player was the underdog on stats and
    still pulled through, framed as an agility-driven comeback. a win
    with a high baseChance is a dominant win, and anything in between is
    a straightforward favored win.
    */
    private StatState determineOutcomeState(double baseChance, boolean won) {
        if (!won) {
            return StatState.BEHIND;
        }
        if (baseChance >= WAY_AHEAD_THRESHOLD) {
            return StatState.WAY_AHEAD;
        }
        if (baseChance >= AHEAD_THRESHOLD) {
            return StatState.AHEAD;
        }
        return StatState.AGILITY_CLUTCH;
    }

    /*
    ratio-based dodge: agility measured against the opponent's combined
    Strength+Magic total. if the opponent has no offensive stats at all,
    there is nothing to dodge in the first place, so it is treated as a
    free pass when the dodger has any agility at all, and neutral otherwise.
    */
    private double computeDodgeRatio(int agility, int opposingAttackTotal) {
        if (opposingAttackTotal <= 0) {
            return agility > 0 ? 1.0 : 0.0;
        }
        if (agility <= 0) {
            return 0.0;
        }
        return (double) agility / (agility + opposingAttackTotal);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Item rollDrop(Enemy enemy) {
        EnemyLoot loot = enemy.getLoot();
        if (!loot.dropItem()) {
            return null;
        }
        if (randomizer.nextDouble() >= loot.getItemDropChance()) {
            return null;
        }
        return itemCatalog.get(loot.getItemId());
    }

    /*
    stat point rewards are probabilistic and ranged, driven by the
    enemy's EnemyLoot rather than a flat guaranteed amount: a chance
    to drop any points at all, then a random amount within [min, max].
    */
    private int rollStatReward(Enemy enemy) {
        EnemyLoot loot = enemy.getLoot();
        if (randomizer.nextDouble() >= loot.getStatDropChance()) {
            return 0;
        }
        int min = loot.getMinStatDrop();
        int max = loot.getMaxStatDrop();
        if (max <= min) {
            return min;
        }
        return min + randomizer.nextInt(max - min + 1);
    }

    private String openingLine(Player player, Enemy enemy, CombatStyle style, boolean magicAxis) {
        if (style == CombatStyle.MELEE) {
            return "The " + enemy.getName() + " closes in - this is an up-close fight.";
        }
        if (style == CombatStyle.RANGED) {
            return "The " + enemy.getName() + " keeps its distance as "
                    + player.getName() + " draws back an arrow.";
        }
        if (style == CombatStyle.MAGE) {
            return player.getName() + " begins weaving arcane energy as the "
                    + enemy.getName() + " draws near.";
        }
        /* unarmed, framed by whichever stat the fight is actually on. */
        if (magicAxis) {
            return player.getName() + "'s bare hands crackle with latent magic as the "
                    + enemy.getName() + " approaches.";
        }
        return player.getName() + " cracks their knuckles as the " + enemy.getName() + " approaches.";
    }

    private String attackLine(Player player, Enemy enemy, CombatStyle style, boolean magicAxis) {
        if (style == CombatStyle.MELEE) {
            return player.getName() + " lunges forward and strikes the " + enemy.getName() + " up close.";
        }
        if (style == CombatStyle.RANGED) {
            return player.getName() + " looses an arrow at the " + enemy.getName() + " from afar.";
        }
        if (style == CombatStyle.MAGE) {
            return player.getName() + " hurls a bolt of raw magic at the " + enemy.getName() + ".";
        }
        /* unarmed, framed by whichever stat the fight is actually on. */
        if (magicAxis) {
            return player.getName() + " channels a burst of raw magic through bare hands into the "
                    + enemy.getName() + ".";
        }
        return player.getName() + " drives a powerful bare-handed strike into the " + enemy.getName() + ".";
    }
}
