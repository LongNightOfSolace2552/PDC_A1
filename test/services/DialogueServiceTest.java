package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;
import main.services.combat.DialogueService;
import main.services.combat.DialogueServiceImpl;
import org.junit.Before;
import org.junit.Test;

/* JUnit4 tests for DialogueServiceImpl: template selection and <player>/<enemy> substitution. */
public class DialogueServiceTest {

    private Map<StatState, List<DialogueTemplate>> templatesByState;

    @Before
    public void setUp() {
        templatesByState = new HashMap<>();
    }

    @Test
    public void buildBattleLines_substitutesPlayerAndEnemyNames() {
        addTemplate(StatState.AHEAD, "<player> defeats <enemy>!");
        DialogueService dialogueService = new DialogueServiceImpl(templatesByState);

        List<String> lines = dialogueService.buildBattleLines(StatState.AHEAD, "Hero", "Goblin");

        assertEquals(1, lines.size());
        assertEquals("Hero defeats Goblin!", lines.get(0));
    }

    @Test
    public void buildBattleLines_returnsOneLinePerTemplateForThatState() {
        addTemplate(StatState.WAY_AHEAD, "<player> crushes <enemy> effortlessly.");
        addTemplate(StatState.WAY_AHEAD, "<enemy> never stood a chance against <player>.");
        addTemplate(StatState.BEHIND, "<enemy> overwhelms you.");
        DialogueService dialogueService = new DialogueServiceImpl(templatesByState);

        List<String> lines = dialogueService.buildBattleLines(StatState.WAY_AHEAD, "Hero", "Goblin");

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("Hero"));
        assertTrue(lines.get(1).contains("Hero"));
    }

    /*
    regression test for the NPE DialogueServiceImpl.buildBattleLines() used to throw
    when a StatState had no templates loaded at all (e.g. an incomplete
    dialogue_templates.txt) - it must degrade to an empty list instead.
    */
    @Test
    public void buildBattleLines_missingStatStateReturnsEmptyListInsteadOfThrowing() {
        addTemplate(StatState.AHEAD, "<player> defeats <enemy>!");
        DialogueService dialogueService = new DialogueServiceImpl(templatesByState);

        List<String> lines = dialogueService.buildBattleLines(StatState.AGILITY_CLUTCH, "Hero", "Goblin");

        assertTrue(lines.isEmpty());
    }

    private void addTemplate(StatState state, String template) {
        templatesByState.computeIfAbsent(state, key -> new ArrayList<>()).add(new DialogueTemplate(state, template));
    }
}
