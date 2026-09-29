package main.domain.combat;

/* import Stats from player since enemies uses the same Stats */
import main.domain.player.Stats;

/*
base enemy model with its own Strength/Magic/Agility thresholds and stat
immunities. defines the behavior shared by regular enemies and bosses;
subclasses may override parts of it (e.g. Boss). drop/reward info is
composed via an EnemyLoot object rather than embedded directly, so the
same loot data shape can vary per node without changing this class.
*/
public class Enemy {

    private final String id;
    private final String name;
    private final Stats stats;
    private final EnemyLoot loot;

    public Enemy(String id, String name, Stats stats, EnemyLoot loot) {
        this.id = id;
        this.name = name;
        this.stats = stats;
        this.loot = loot;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Stats getStats() {
        return stats;
    }

    public EnemyLoot getLoot() {
        return loot;
    }

    /*
    delegates to Stats' shared calculation, so an enemy's Power Level is
    computed the same way as a player's.
    */
    public int getPowerLevel() {
        return stats.getPowerLevel();
    }
}
