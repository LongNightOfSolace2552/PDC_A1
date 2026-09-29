package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;
import main.services.combat.DialogueService;
import main.services.combat.DialogueServiceImpl;

/* demonstrates DialogueService substituting player/enemy names into a template for the requested StatState. */
public class DialogueServiceTest {

    public static void main(String[] args) {
        Map<StatState, List<DialogueTemplate>> templates = new HashMap<>();
        List<DialogueTemplate> aheadTemplates = new ArrayList<>();
        aheadTemplates.add(new DialogueTemplate(StatState.AHEAD, "<player> defeats <enemy>!"));
        templates.put(StatState.AHEAD, aheadTemplates);

        DialogueService dialogueService = new DialogueServiceImpl(templates);
        List<String> lines = dialogueService.buildBattleLines(StatState.AHEAD, "Hero", "Goblin");

        check("exactly one line returned", lines.size() == 1);
        check("names substituted correctly", lines.get(0).equals("Hero defeats Goblin!"));
    }

    private static void check(String description, boolean passed) {
        System.out.println((passed ? "PASS: " : "FAIL: ") + description);
    }
}
