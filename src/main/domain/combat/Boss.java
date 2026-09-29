package main.domain.combat;

import main.domain.player.Stats;

/*
a node's boss enemy. a Boss is an Enemy with stronger stats/thresholds and
boss-specific behavior (e.g. reward or dialogue overrides), so it can be
used anywhere an Enemy is expected.
*/
public class Boss extends Enemy {
    public Boss(String id, String name, Stats stats, EnemyLoot loot) {
        super(id, name, stats, loot);
    }
}
