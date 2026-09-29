package main.services.combat;

import main.domain.combat.CombatResult;
import main.domain.combat.Enemy;
import main.domain.player.Player;

/*
interface for battle resolution: threshold-based win chance, dodge chance,
weapon ability activation rolls, and Power Level calculation.
*/
public interface CombatService {
    CombatResult resolveFight(Player player, Enemy enemy);
}
