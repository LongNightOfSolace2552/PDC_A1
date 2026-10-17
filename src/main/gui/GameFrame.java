package main.gui;

import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import main.core.Game;
import main.core.GameController;
import main.domain.combat.CombatResult;
import main.domain.combat.Enemy;
import main.domain.player.Player;
import main.services.ActionResult;

/*
What used to render the main menu: [1] Travel, [2] Node, [3] Dungeon, 
[4] Stats/Inventory. This class has been converted to run one window 
in which the whole GUI runs in. Each screen is a JPanel and moving
between them is just calling the TransitionPanel which one to show next.
screens reach the Player, Game and GameController through here. MVC: the
domain and services are the model, the panels are the view, and
GameController plus the buttons' listeners are the controller - every
rule lives in GameController's PASS/FAIL functions, the panels only show
their results.

renamed from MenuRenderer to GameFrame, added extend to JFrame.
*/
public class GameFrame extends JFrame {
    private final Game game;
    private final TransitionPanel transitionPanel = new TransitionPanel();

    private Player player;

    private final NameEntryPanel nameEntryPanel;
    private MenuPanel menuPanel;
    private TravelPanel travelPanel;
    private NodePanel nodePanel;
    private DungeonPanel dungeonPanel;
    private InventoryPanel inventoryPanel;
    private CombatPanel activeCombatPanel;

    public GameFrame(Game game) {
        super("RPG Game");
        this.game = game;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(700, 520));
        setSize(800, 600);
        setLocationRelativeTo(null);
        setContentPane(transitionPanel);

        nameEntryPanel = new NameEntryPanel(this);
        transitionPanel.showFirst(nameEntryPanel);
    }

    public Game getGame() {
        return game;
    }

    public GameController getController() {
        return game.getGameController();
    }

    public Player getPlayer() {
        return player;
    }

    /* called once, by NameEntryPanel, after the typed name has become a (new or loaded) Player. */
    public void startGame(Player player) {
        this.player = player;
        menuPanel = new MenuPanel(this);
        travelPanel = new TravelPanel(this);
        nodePanel = new NodePanel(this);
        dungeonPanel = new DungeonPanel(this);
        inventoryPanel = new InventoryPanel(this);

        menuPanel.refresh();
        transitionPanel.slideTo(menuPanel);
    }

    public void showMainMenu() {
        menuPanel.refresh();
        transitionPanel.slideTo(menuPanel);
    }

    public void showTravel() {
        travelPanel.refresh();
        transitionPanel.slideTo(travelPanel);
    }

    public void showNode() {
        nodePanel.refresh();
        transitionPanel.slideTo(nodePanel);
    }

    /* entering from the menu always starts a fresh Dungeon (floor 1); a node with nothing to fight keeps the player on the menu. */
    public void showDungeon() {
        if (dungeonPanel.enterFresh()) {
            transitionPanel.slideTo(dungeonPanel);
        }
    }

    /* back to the dungeon menu after a fight - the existing Dungeon and its floor progress are kept. */
    public void returnToDungeonMenu() {
        dungeonPanel.refreshButtonsOnly();
        transitionPanel.slideTo(dungeonPanel);
    }

    public void showInventory() {
        inventoryPanel.refresh();
        transitionPanel.slideTo(inventoryPanel);
    }

    /* starts a fight; onFinished gets the CombatResult once the player clicks Continue. */
    public void showCombat(Enemy enemy, Consumer<CombatResult> onFinished) {
        if (activeCombatPanel != null) {
            activeCombatPanel.dispose();
        }
        activeCombatPanel = new CombatPanel(getController(), player, enemy, onFinished);
        transitionPanel.slideTo(activeCombatPanel);
    }

    /* saves, then quits. if the save fails the game stays open so progress is not silently lost. */
    public void saveAndExit() {
        ActionResult result = getController().saveProgress(player);
        if (result.isFail()) {
            showError(result.getMessage());
            return;
        }
        showMessage(result.getMessage());
        dispose();
        System.exit(0);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "RPG Game", JOptionPane.INFORMATION_MESSAGE);
    }
}
