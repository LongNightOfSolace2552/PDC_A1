package main.domain.combat;

/*
a single dialogue template: which StatState it applies to (how the fight
is going for the player relative to the enemy), and a sentence pattern
with <player>/<enemy> placeholders.
*/
public class DialogueTemplate {

    public enum StatState {
        AHEAD, BEHIND, WAY_AHEAD, AGILITY_CLUTCH
    }

    private final StatState statState;
    private final String template;

    public DialogueTemplate(StatState statState, String template) {
        this.statState = statState;
        this.template = template;
    }

    public StatState getStatState() {
        return statState;
    }

    public String getTemplate() {
        return template;
    }

    public String format(String playerName, String enemyName) {
        return template.replace("<player>", playerName).replace("<enemy>", enemyName);
    }
}
