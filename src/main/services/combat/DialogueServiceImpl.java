package main.services.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;

public class DialogueServiceImpl implements DialogueService {
    private final Map<StatState, List<DialogueTemplate>> templatesByState;

    public DialogueServiceImpl(Map<StatState, List<DialogueTemplate>> templatesByState) {
        this.templatesByState = templatesByState;
    }

    /*
    returns the full, ready-to-output dialogue lines for the given
    StatState, with the player's and enemy's names filled in.
    
    modified: change ' .get ' with ' .getOrDefault ', retrieves the key of a
    specific value map or it returns a default fallback value if the key
    does not exist.
    */
    @Override
    public List<String> buildBattleLines(StatState statState, String playerName, String enemyName) {
        List<DialogueTemplate> templates = templatesByState.getOrDefault(statState, List.of());
        List<String> lines = new ArrayList<>();
        for (DialogueTemplate template : templates) {
            lines.add(template.format(playerName, enemyName));
        }
        return lines;
    }
}
