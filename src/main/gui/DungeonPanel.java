package main.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import main.core.GameController;
import main.domain.combat.Boss;
import main.domain.combat.CombatResult;
import main.domain.combat.Dungeon;
import main.domain.combat.Enemy;
import main.domain.combat.FightOutcome;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;
import main.services.ActionResult;

/*
the dungeon menu: Explore (one fight, then back here), Challenge the
boss, Fight until the end (chains fights, stopping only on a loss) and
Leave. a fresh Dungeon is created every time the player enters from the
main menu - floor progress is never saved between visits.

this screen only drives the flow and shows results. whether a button
exists at all (enemies to fight, a boss still to beat) comes from
GameController's PASS/FAIL checks, and what a finished fight changes
(drops, floor progress, boss defeat) is applied by GameController too.
*/
public class DungeonPanel extends JPanel {

    private final GameFrame frame;
    private Dungeon dungeon;
    private Node currentNode;

    private final JLabel titleLabel = new JLabel("Dungeon");
    private final JLabel floorLabel = new JLabel("Floor 1");
    private final JLabel statusLabel = new JLabel(" ");

    private final JButton exploreButton = new JButton("Explore");
    private final JButton bossButton = new JButton("Challenge the boss");
    private final JButton fightUntilEndButton = new JButton("Fight until the end");
    private final JButton leaveButton = new JButton("Leave the dungeon");

    public DungeonPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(titleLabel, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridLayout(0, 1, 0, 8));
        centre.add(floorLabel);
        centre.add(statusLabel);
        add(centre, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        buttons.add(exploreButton);
        buttons.add(bossButton);
        buttons.add(fightUntilEndButton);
        buttons.add(leaveButton);
        add(buttons, BorderLayout.SOUTH);

        exploreButton.addActionListener(event -> doSingleExplore());
        bossButton.addActionListener(event -> doBossChallenge());
        fightUntilEndButton.addActionListener(event -> doChainedFight());
        leaveButton.addActionListener(event -> frame.showMainMenu());
    }

    private GameController controller() {
        return frame.getController();
    }

    /*
    called only when entering from the main menu - starts a brand-new
    Dungeon at floor 1. returns false (after telling the player why) if
    there is nothing to fight at the current node, so the caller stays on
    the menu.
    */
    public boolean enterFresh() {
        try {
            currentNode = controller().findCurrentNode(frame.getPlayer());
        } catch (ItemNotFoundException e) {
            frame.showError(e.getMessage());
            return false;
        }

        ActionResult canEnter = controller().handleDungeon(currentNode);
        if (canEnter.isFail()) {
            frame.showMessage(canEnter.getMessage());
            return false;
        }

        dungeon = controller().createDungeon(currentNode);

        /*a boss that is already beaten (or unavailable) is explained up 
        front instead of just missing.*/
        ActionResult bossCheck = controller().handleBossChallenge(frame.getPlayer(), currentNode);
        statusLabel.setText(currentNode.hasBoss() && bossCheck.isFail() ? bossCheck.getMessage() : " ");

        refreshButtonsOnly();
        return true;
    }

    /*
    redraws the floor and shows only the buttons that apply right now.
    also used when returning from a fight, leaving the status line the
    fight just set (e.g. "You defeated the boss!") on screen.
    */
    public void refreshButtonsOnly() {
        titleLabel.setText("Dungeon - " + currentNode.getName());
        floorLabel.setText("Floor " + dungeon.getFloor());

        boolean hasEnemies = controller().hasEnemies(currentNode.getId());
        exploreButton.setVisible(hasEnemies);
        fightUntilEndButton.setVisible(hasEnemies);
        bossButton.setVisible(controller().handleBossChallenge(frame.getPlayer(), currentNode).isPass());

        revalidate();
        repaint();
    }

    /* the status line after a dungeon fight, worded from what the outcome changed. */
    private String describe(FightOutcome outcome) {
        if (outcome.isProgressReset()) {
            return "Your dungeon progress resets - back to Floor 1.";
        }
        if (outcome.isFloorAdvanced()) {
            return "Floor cleared! Descending to Floor " + outcome.getFloor() + ".";
        }
        return " ";
    }

    private boolean confirmFight(String prompt) {
        int choice = JOptionPane.showOptionDialog(frame, prompt, "Encounter",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Fight", "Retreat"}, "Fight");
        return choice == 0;
    }

    private void doSingleExplore() {
        Enemy enemy = dungeon.nextEnemy();
        if (enemy == null) {
            frame.showMessage("There are no enemies to find here yet.");
            return;
        }
        if (!confirmFight("A " + enemy.getName() + " appears! (Floor " + dungeon.getFloor() + ")\nFight or retreat?")) {
            statusLabel.setText("You retreat before the fight begins.");
            return;
        }
        frame.showCombat(enemy, this::handleSingleExploreResult);
    }

    private void handleSingleExploreResult(CombatResult result) {
        FightOutcome outcome = controller().applyDungeonFight(frame.getPlayer(), dungeon, result);
        statusLabel.setText(describe(outcome));
        frame.returnToDungeonMenu();
    }

    private void doChainedFight() {
        statusLabel.setText("You press onward, fighting without pause...");
        continueChain();
    }

    private void continueChain() {
        Enemy enemy = dungeon.nextEnemy();
        if (enemy == null) {
            statusLabel.setText("There are no enemies to find here yet.");
            frame.returnToDungeonMenu();
            return;
        }
        frame.showCombat(enemy, this::handleChainedResult);
    }

    private void handleChainedResult(CombatResult result) {
        FightOutcome outcome = controller().applyDungeonFight(frame.getPlayer(), dungeon, result);
        if (outcome.isWon()) {
            statusLabel.setText(describe(outcome));
            continueChain();
        } else {
            statusLabel.setText(describe(outcome) + " You can no longer continue - returning to the dungeon menu.");
            frame.returnToDungeonMenu();
        }
    }

    private void doBossChallenge() {
        ActionResult check = controller().handleBossChallenge(frame.getPlayer(), currentNode);
        if (check.isFail()) {
            frame.showMessage(check.getMessage());
            refreshButtonsOnly();
            return;
        }

        Boss boss = controller().getBoss(currentNode.getId());
        if (!confirmFight(check.getMessage() + "\nFight or retreat?")) {
            statusLabel.setText("You retreat.");
            return;
        }
        frame.showCombat(boss, this::handleBossResult);
    }

    private void handleBossResult(CombatResult result) {
        FightOutcome outcome = controller().applyBossFight(frame.getPlayer(), currentNode.getId(), result);
        statusLabel.setText(outcome.isBossDefeated()
                ? "You defeated the boss! The path onward is now unlocked." : " ");
        frame.returnToDungeonMenu();
    }
}
