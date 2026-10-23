package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import main.core.Game;
import main.core.GameController;
import main.core.SaveManager;
import main.core.SessionStart;
import main.domain.items.Item;
import main.domain.player.Player;
import main.persistence.ItemDatabaseRepository;
import main.persistence.PlayerDatabaseRepository;
import main.services.ActionResult;
import main.util.Randomizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import persistence.TestDatabase;

/*
JUnit4 PASS/FAIL tests for Game.initializePlayer (turning a typed name
into a player: blank or too-long name, brand-new character, returning
player, unreadable save) and GameController.saveProgress. the saves go
into a real Derby database that lives only in memory (see TestDatabase),
so nothing on disk is touched.
*/
public class GameSessionTest {

    private static final String NAME = "JUnitSessionTester";

    private TestDatabase database;
    private Map<String, Item> catalog;
    private Game game;
    private GameController controller;

    @Before
    public void setUp() throws Exception {
        database = TestDatabase.create();
        catalog = new ItemDatabaseRepository(database.connection()).loadItemCatalog();
        game = gameWith(catalog);
        controller = game.getGameController();
    }

    @After
    public void dropDatabase() {
        database.drop();
    }

    /* a Game whose saves go to the test database, resolving saved item ids against the given catalog. */
    private Game gameWith(Map<String, Item> itemCatalog) {
        PlayerDatabaseRepository repository = new PlayerDatabaseRepository(database.connection(), itemCatalog);
        SaveManager saveManager = new SaveManager(repository);
        GameController gameController =
                new GameController(saveManager, null, null, new HashMap<>(), new HashMap<>(), new Randomizer());
        return new Game(gameController, saveManager, itemCatalog);
    }

    @Test
    public void initializePlayer_withABlankName_fails() {
        SessionStart blank = game.initializePlayer("   ");
        SessionStart missing = game.initializePlayer(null);

        assertTrue(blank.isFail());
        assertEquals("Please enter a name.", blank.getMessage());
        assertNull(blank.getPlayer());
        assertTrue(missing.isFail());
    }

    @Test
    public void initializePlayer_forANewName_createsACharacterInTheVillageWithTheStarterItemsEquipped() {
        SessionStart start = game.initializePlayer(NAME);

        assertTrue(start.isPass());
        assertFalse(start.isReturning());
        assertFalse("a clean new game has nothing to announce", start.hasMessage());

        Player player = start.getPlayer();
        assertEquals(NAME, player.getName());
        assertEquals("village", player.getCurrentNodeId());
        assertEquals("wooden_dagger", player.getEquippedWeapon().getId());
        assertEquals("silver_ring", player.getEquippedAccessory().getId());
        assertEquals(0, player.getStats().getStrength());
    }

    @Test
    public void initializePlayer_trimsTheName() {
        SessionStart start = game.initializePlayer("  " + NAME + "  ");

        assertEquals(NAME, start.getPlayer().getName());
    }

    @Test
    public void initializePlayer_withAStarterItemMissingFromTheCatalog_stillStartsButSaysSo() {
        Game emptyCatalogGame = gameWith(new HashMap<>());

        SessionStart start = emptyCatalogGame.initializePlayer(NAME);

        assertTrue(start.isPass());
        assertTrue(start.getMessage().contains("wooden_dagger"));
        assertTrue(start.getMessage().contains("silver_ring"));
        assertFalse(start.getPlayer().hasEquippedWeapon());
    }

    @Test
    public void initializePlayer_withANameTooLongForTheDatabase_failsWithoutCreatingAnyone() {
        String tooLong = "x".repeat(Game.MAX_NAME_LENGTH + 1);

        SessionStart start = game.initializePlayer(tooLong);

        assertTrue(start.isFail());
        assertEquals("Please use a name of " + Game.MAX_NAME_LENGTH + " characters or fewer.", start.getMessage());
        assertNull(start.getPlayer());
    }

    @Test
    public void initializePlayer_withTheLongestAllowedName_canBeSavedAndLoadedAgain() {
        String longest = "y".repeat(Game.MAX_NAME_LENGTH);
        Player player = game.initializePlayer(longest).getPlayer();

        assertTrue(controller.saveProgress(player).isPass());

        assertTrue(game.initializePlayer(longest).isReturning());
    }

    @Test
    public void saveProgress_passesAndWritesTheSave() {
        Player player = game.initializePlayer(NAME).getPlayer();

        ActionResult result = controller.saveProgress(player);

        assertTrue(result.isPass());
        assertEquals("Progress saved. Goodbye, " + NAME + "!", result.getMessage());
        assertTrue(game.initializePlayer(NAME).isReturning());
    }

    @Test
    public void saveProgress_failsWhenTheDatabaseCannotBeWritten() throws Exception {
        Player player = game.initializePlayer(NAME).getPlayer();
        /* a closed connection makes the write impossible. */
        database.connection().close();

        ActionResult result = controller.saveProgress(player);

        assertTrue(result.isFail());
        assertTrue(result.getMessage().startsWith("Could not save your progress:"));
    }

    @Test
    public void initializePlayer_forASavedName_welcomesTheReturningPlayerWithProgressIntact() {
        Player player = game.initializePlayer(NAME).getPlayer();
        player.setCurrentNodeId("forest");
        player.addUnallocatedStatPoints(2);
        player.markBossDefeated("village");
        player.unequipWeapon();
        controller.saveProgress(player);

        SessionStart again = game.initializePlayer(NAME);

        assertTrue(again.isPass());
        assertTrue(again.isReturning());
        assertEquals("Welcome back, " + NAME + "!", again.getMessage());
        Player loaded = again.getPlayer();
        assertEquals("forest", loaded.getCurrentNodeId());
        assertEquals(2, loaded.getUnallocatedStatPoints());
        assertTrue(loaded.hasDefeatedBoss("village"));
        assertFalse("the unequipped weapon stays unequipped", loaded.hasEquippedWeapon());
        assertEquals("...and is back in the inventory", "wooden_dagger", loaded.getInventory().get(0).getId());
    }

    @Test
    public void initializePlayer_withAnUnreadableSave_startsANewGameAndSaysWhy() {
        Player player = game.initializePlayer(NAME).getPlayer();
        controller.saveProgress(player);
        /* the same database, but a catalog that no longer knows the silver ring the save holds. */
        Map<String, Item> smallerCatalog = new HashMap<>(catalog);
        smallerCatalog.remove("silver_ring");
        Game gameWithSmallerCatalog = gameWith(smallerCatalog);

        SessionStart start = gameWithSmallerCatalog.initializePlayer(NAME);

        assertTrue("the player can still play", start.isPass());
        assertFalse(start.isReturning());
        assertTrue(start.getMessage().startsWith("Could not load your save:"));
        assertTrue(start.getMessage().contains("Starting a new game instead."));
        assertEquals("village", start.getPlayer().getCurrentNodeId());
        assertTrue("the fresh character gets the starter items", start.getPlayer().hasEquippedWeapon());
    }
}
