package persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.items.CombatStyle;
import main.domain.items.Item;
import main.domain.items.Weapon;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.DataLoadException;
import main.exceptions.SaveDataException;
import main.persistence.ItemDatabaseRepository;
import main.persistence.PlayerDatabaseRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 database tests for PlayerDatabaseRepository, the DAO that saves
and loads a player in the PLAYER, PLAYER_INVENTORY and
PLAYER_DEFEATED_BOSS tables of an in-memory Derby database that holds
the real game data (see TestDatabase).
*/
public class PlayerDatabaseRepositoryTest {

    private TestDatabase database;
    private Map<String, Item> catalog;
    private PlayerDatabaseRepository repository;

    @Before
    public void setUp() throws SQLException, DataLoadException {
        database = TestDatabase.create();
        catalog = new ItemDatabaseRepository(database.connection()).loadItemCatalog();
        repository = new PlayerDatabaseRepository(database.connection(), catalog);
    }

    @After
    public void tearDown() {
        database.drop();
    }

    private int countRows(String sql) throws SQLException {
        try (Statement statement = database.connection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    public void saveExists_isFalseUntilThePlayerIsSaved() throws SaveDataException {
        assertFalse(repository.saveExists("Hero"));

        repository.save(new Player("Hero", new Stats(1, 1, 1), "village"), "Hero");

        assertTrue(repository.saveExists("Hero"));
        assertFalse("another name has no save", repository.saveExists("Someone Else"));
    }

    @Test
    public void saveThenLoad_roundTripsStatsNodeAndStatPoints() throws SaveDataException {
        Player original = new Player("Hero", new Stats(5, 3, 2), "forest");
        original.addUnallocatedStatPoints(4);

        repository.save(original, "Hero");
        Player loaded = repository.load("Hero");

        assertEquals("Hero", loaded.getName());
        assertEquals(5, loaded.getStats().getStrength());
        assertEquals(3, loaded.getStats().getMagic());
        assertEquals(2, loaded.getStats().getAgility());
        assertEquals("forest", loaded.getCurrentNodeId());
        assertEquals(4, loaded.getUnallocatedStatPoints());
    }

    @Test
    public void saveThenLoad_roundTripsEquipmentInventoryAndDefeatedBosses() throws SaveDataException {
        Player original = new Player("Hero", new Stats(0, 0, 0), "village");
        original.addItem(catalog.get("wooden_dagger"));
        original.addItem(catalog.get("silver_ring"));
        original.addItem(catalog.get("iron_sword"));
        original.addItem(catalog.get("bow"));
        original.equip(catalog.get("wooden_dagger"));
        original.equip(catalog.get("silver_ring"));
        original.markBossDefeated("village");

        repository.save(original, "Hero");
        Player loaded = repository.load("Hero");

        assertEquals("wooden_dagger", loaded.getEquippedWeapon().getId());
        assertEquals("silver_ring", loaded.getEquippedAccessory().getId());
        assertEquals("unequipped items come back in the order they were carried", 2, loaded.getInventory().size());
        assertEquals("iron_sword", loaded.getInventory().get(0).getId());
        assertEquals("bow", loaded.getInventory().get(1).getId());
        assertTrue(loaded.hasDefeatedBoss("village"));
        assertFalse(loaded.hasDefeatedBoss("forest"));
    }

    @Test
    public void saveThenLoad_aPlayerWithNothingEquipped_staysEmptyHanded() throws SaveDataException {
        Player original = new Player("Hero", new Stats(1, 1, 1), "village");
        original.addItem(catalog.get("bow"));

        repository.save(original, "Hero");
        Player loaded = repository.load("Hero");

        assertFalse(loaded.hasEquippedWeapon());
        assertFalse(loaded.hasEquippedAccessory());
        assertEquals(1, loaded.getInventory().size());
    }

    @Test
    public void savingTwice_updatesTheSameSave_withoutDuplicatingAnything() throws Exception {
        Player player = new Player("Hero", new Stats(1, 1, 1), "village");
        player.addItem(catalog.get("iron_sword"));
        player.addItem(catalog.get("bow"));
        player.markBossDefeated("village");
        repository.save(player, "Hero");

        player.getStats().setStrength(9);
        player.setCurrentNodeId("forest");
        player.equip(catalog.get("iron_sword"));
        repository.save(player, "Hero");
        Player loaded = repository.load("Hero");

        assertEquals(9, loaded.getStats().getStrength());
        assertEquals("forest", loaded.getCurrentNodeId());
        assertEquals("iron_sword", loaded.getEquippedWeapon().getId());
        assertEquals("only the bow is left unequipped", 1, loaded.getInventory().size());
        assertEquals(1, countRows("SELECT COUNT(*) FROM PLAYER"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM PLAYER_INVENTORY"));
        assertEquals(1, countRows("SELECT COUNT(*) FROM PLAYER_DEFEATED_BOSS"));
    }

    @Test
    public void twoPlayers_keepSeparateSaves() throws SaveDataException {
        Player first = new Player("Alice", new Stats(1, 0, 0), "village");
        first.addItem(catalog.get("bow"));
        Player second = new Player("Bob", new Stats(2, 0, 0), "cave");

        repository.save(first, "Alice");
        repository.save(second, "Bob");

        assertEquals(1, repository.load("Alice").getStats().getStrength());
        assertEquals(1, repository.load("Alice").getInventory().size());
        assertEquals("cave", repository.load("Bob").getCurrentNodeId());
        assertTrue(repository.load("Bob").getInventory().isEmpty());
    }

    @Test(expected = SaveDataException.class)
    public void load_ofAPlayerThatWasNeverSaved_throws() throws SaveDataException {
        repository.load("Nobody");
    }

    /*WIP*/
}
