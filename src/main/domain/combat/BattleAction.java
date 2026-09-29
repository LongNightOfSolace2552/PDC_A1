package main.domain.combat;

/*
one resolved step of a battle sequence: the final formed sentence to
display. paired with DialogueTemplate/DialogueService, which build these
lines from a StatState and the player/enemy names.
*/
public class BattleAction {
    private final String line;

    public BattleAction(String line) {
        this.line = line;
    }

    public String getLine() {
        return line;
    }
}
