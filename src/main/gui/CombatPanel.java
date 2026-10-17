package main.gui;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import main.core.CombatSequencer;
import main.core.GameController;
import main.domain.combat.Boss;
import main.domain.combat.CombatResult;
import main.domain.combat.Enemy;
import main.domain.player.Player;

/*
the fight screen (the GUI's CombatRenderer): the fight is resolved
straight away, then CombatSequencer hands its dialogue lines over one at
a time. each line is added to the log and moves the sprites on the
BattleStagePanel. Skip shows the rest at once; Continue hands the result
back to the caller.
*/
public class CombatPanel extends JPanel {
    private final BattleStagePanel stage = new BattleStagePanel();
    private final JTextArea logArea = new JTextArea(8, 40);
    private final JButton actionButton = new JButton("Skip");
    private final CombatSequencer sequencer = new CombatSequencer();
    private final CombatResult result;
    private final String playerName;
    private final String enemyName;
    private final Consumer<CombatResult> onFinished;
    private boolean finished;

    public CombatPanel(GameController gameController, Player player, Enemy enemy, Consumer<CombatResult> onFinished) {
        this.onFinished = onFinished;
        this.playerName = player.getName();
        this.enemyName = enemy.getName();
        this.result = gameController.resolveFight(player, enemy);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        boolean isBoss = enemy instanceof Boss;
        add(new JLabel(isBoss ? "Boss encounter: " + enemyName : "A " + enemyName + " appears!"), BorderLayout.NORTH);

        add(stage, BorderLayout.CENTER);
        stage.setup(playerName, enemyName, isBoss);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        actionButton.addActionListener(event -> handleActionButton());

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(new JScrollPane(logArea), BorderLayout.CENTER);
        bottom.add(actionButton, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        sequencer.play(result.getDialogueLines(), this::showLine, this::showResult);
    }

    /*adds the line to the log and moves the sprites to match what it says.*/
    private void showLine(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());

        String lower = line.toLowerCase();
        if (lower.contains("dodge") || lower.contains("weaves") || lower.contains("reflexes")) {
            /* the enemy dodges only when the line is about the enemy alone. */
            stage.dodge(!(line.contains(enemyName) && !line.contains(playerName)));
        } else if (!isOutcomeNote(lower)) {
            stage.clash();
        }
    }

    /*drops, stat points and stat loss are notes after the fight, 
    so nothing moves for them.*/
    private boolean isOutcomeNote(String lowerLine) {
        return lowerLine.contains(" drops ") || lowerLine.contains("stat point")
                || lowerLine.contains(" loses ") || lowerLine.contains("narrowly");
    }

    private void showResult() {
        finished = true;
        stage.showResult(result.isWon());
        logArea.append("\n" + (result.isWon() ? "You won the battle!" : "You were defeated...") + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        actionButton.setText("Continue");
    }

    private void handleActionButton() {
        if (!finished) {
            sequencer.skip();
        } else {
            dispose();
            onFinished.accept(result);
        }
    }

    /*
    stops the playback and the stage timer - call before leaving the screen, 
    even if Continue was never clicked. 
    */
    public void dispose() {
        sequencer.stop();
        stage.dispose();
    }
}
