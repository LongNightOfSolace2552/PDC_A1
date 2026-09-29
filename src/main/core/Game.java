package main.core;

import java.util.Map;
import main.cui.CommandParser;
import main.cui.ConsoleRenderer;
import main.cui.MenuRenderer;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.SaveDataException;

/*
Main game loop; presents the top-level menu 
(Travel, Node, Dungeon, Stats/Inventory) and routes player input.
*/
public class Game {
    private static final int DEFAULT_STAT_VALUE = 0;
    private static final String DEFAULT_STARTING_NODE_ID = "village";
    /*
    a couple of starter items so equip/unequip and save/load of an
    owned+equipped item are testable right away.
    */
    private static final String[] STARTER_ITEM_IDS = {"wooden_dagger", "silver_ring"};

    private final ConsoleRenderer consoleRenderer;
    private final MenuRenderer menuRenderer;
    private final CommandParser commandParser;
    private final GameController gameController;
    private final SaveManager saveManager;
    private final Map<String, Item> itemCatalog;

    public Game(ConsoleRenderer consoleRenderer, MenuRenderer menuRenderer,
                CommandParser commandParser, GameController gameController,
                SaveManager saveManager, Map<String, Item> itemCatalog) {
        this.consoleRenderer = consoleRenderer;
        this.menuRenderer = menuRenderer;
        this.commandParser = commandParser;
        this.gameController = gameController;
        this.saveManager = saveManager;
        this.itemCatalog = itemCatalog;
    }

    public void start() {
        Player player = initializePlayer();

        boolean running = true;
        while (running) {
            gameController.renderHeader(player);
            menuRenderer.printMainMenu();

            int choice = commandParser.readMenuChoice();
            running = !gameController.handle(choice, player);
        }
    }

    private Player initializePlayer() {
        String playerName = commandParser.readLine("Enter your player name: ");

        if (saveManager.hasSave(playerName)) {
            try {
                Player player = saveManager.loadPlayer(playerName);
                consoleRenderer.printMessage("Welcome back, " + player.getName() + "!");
                return player;
            } catch (SaveDataException e) {
                consoleRenderer.printError("Could not load your save: " + e.getMessage());
                consoleRenderer.printMessage("Starting a new game instead.");
            }
        }
        return createNewPlayer(playerName);
    }

    private Player createNewPlayer(String playerName) {
        Stats startingStats = new Stats(DEFAULT_STAT_VALUE, DEFAULT_STAT_VALUE, DEFAULT_STAT_VALUE);
        Player player = new Player(playerName, startingStats, DEFAULT_STARTING_NODE_ID);

        for (String itemId : STARTER_ITEM_IDS) {
            Item item = itemCatalog.get(itemId);
            if (item != null) {
                player.addItem(item);
                player.equip(item);
            } else {
                consoleRenderer.printError("Starter item '" + itemId + "' is missing from the item catalog.");
            }
        }

        return player;
    }
}
