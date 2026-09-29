package main.services.combat;

import java.util.List;
import main.domain.combat.DialogueTemplate.StatState;

/*
interface for selecting dialogue templates based on the given StatState,
and forming the resulting sentences with the player's and enemy's names.
*/
public interface DialogueService {
    List<String> buildBattleLines(StatState statState, String playerName, String enemyName);
}
