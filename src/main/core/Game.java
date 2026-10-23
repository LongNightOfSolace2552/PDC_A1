package main.core;

import java.util.Map;
import javax.swing.SwingUtilities;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.SaveDataException;
import main.gui.GameFrame;

/*
Main game loop; presents the top-level menu 
(Travel, Node, Dungeon, Stats/Inventory).

start of a play session, runs "start()" opens the game window, and
initializePlayer turns the name typed there into a Player (loading an
existing save or creating a new character). the GameController it holds
runs the game from then on.
*/
public class Game {
    /* the longest name the database's PLAYER.NAME column (VARCHAR(50)) can hold. */
    public static final int MAX_NAME_LENGTH = 50;
    private static final int DEFAULT_STAT_VALUE = 0;
    private static final String DEFAULT_STARTING_NODE_ID = "village";
    /*
    a couple of starter items so equip/unequip and save/load of an
    owned+equipped item are testable right away.
    */
    private static final String[] STARTER_ITEM_IDS = {"wooden_dagger", "silver_ring"};

    private final GameController gameController;
    private final SaveManager saveManager;
    private final Map<String, Item> itemCatalog;

    public Game(GameController gameController, SaveManager saveManager, Map<String, Item> itemCatalog) {
        this.gameController = gameController;
        this.saveManager = saveManager;
        this.itemCatalog = itemCatalog;
    }

    public GameController getGameController() {
        return gameController;
    }

    /* opens the game window on the Swing thread (Part 1's start() ran the console loop here). */
    public void start() {
        SwingUtilities.invokeLater(() -> new GameFrame(this).setVisible(true));
    }

    /*
    an existing save for the name is loaded; no save, or one that cannot
    be read, starts a new character with the starter items equipped. a
    blank name, or one longer than MAX_NAME_LENGTH, FAILs. (the name
    prompt itself is now the GUI's.)
    */
    public SessionStart initializePlayer(String playerName) {
        String name = playerName == null ? "" : playerName.trim();
        if (name.isEmpty()) {
            return SessionStart.failed("Please enter a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return SessionStart.failed("Please use a name of " + MAX_NAME_LENGTH + " characters or fewer.");
        }

        String notice = "";
        if (saveManager.hasSave(name)) {
            try {
                return SessionStart.returning(saveManager.loadPlayer(name));
            } catch (SaveDataException e) {
                notice = "Could not load your save: " + e.getMessage() + "\nStarting a new game instead.";
            }
        }
        return createNewPlayer(name, notice);
    }

    private SessionStart createNewPlayer(String playerName, String notice) {
        Stats startingStats = new Stats(DEFAULT_STAT_VALUE, DEFAULT_STAT_VALUE, DEFAULT_STAT_VALUE);
        Player player = new Player(playerName, startingStats, DEFAULT_STARTING_NODE_ID);

        StringBuilder messages = new StringBuilder(notice);
        for (String itemId : STARTER_ITEM_IDS) {
            Item item = itemCatalog.get(itemId);
            if (item != null) {
                player.addItem(item);
                player.equip(item);
            } else {
                if (messages.length() > 0) {
                    messages.append('\n');
                }
                messages.append("Starter item '").append(itemId).append("' is missing from the item catalog.");
            }
        }
        
        /*now returns the message and player*/
        return SessionStart.newGame(player, messages.toString());
    }
}
