package persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.items.Accessory;
import main.domain.items.Item;
import main.domain.items.Weapon;
import main.domain.items.CombatStyle;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.SaveDataException;
import main.persistence.FileReaderUtil;
import main.persistence.FileWriterUtil;
import main.persistence.PlayerFileRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/* JUnit4 tests for PlayerFileRepository: the data-access layer's save/load round trip. */
public class FileRepositoryTest {

    private static final String TEST_PATH = "data/test_player_repository.txt";

    private Map<String, Item> catalog;
    private PlayerFileRepository repository;

    @Before
    public void setUp() {
        catalog = new HashMap<>();
        repository = new PlayerFileRepository(new FileReaderUtil(), new FileWriterUtil(), catalog);
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(TEST_PATH));
    }

    @Test
    public void saveThenLoad_roundTripsBasicStatsAndNode() throws SaveDataException {
        Player original = new Player("Tester", new Stats(5, 3, 2), "village");

        repository.save(original, TEST_PATH);
        Player loaded = repository.load(TEST_PATH);

        assertEquals("Tester", loaded.getName());
        assertEquals(5, loaded.getStats().getStrength());
        assertEquals(3, loaded.getStats().getMagic());
        assertEquals(2, loaded.getStats().getAgility());
        assertEquals("village", loaded.getCurrentNodeId());
    }

    /*
    a more "meaningful" case than a bare stats round trip: equipped
    items, unequipped inventory, defeated bosses, and unallocated stat
    points all need to survive the trip through the catalog lookup
    PlayerFileRepository does on load.
    */
    @Test
    public void saveThenLoad_roundTripsEquipmentInventoryAndProgress() throws SaveDataException {
        Weapon sword = new Weapon("iron_sword", "Iron Sword", 3, 0, 0, null, CombatStyle.MELEE);
        Item spareRing = new Accessory("spare_ring", "Spare Ring", 1, 0, 0, null);
        catalog.put(sword.getId(), sword);
        catalog.put(spareRing.getId(), spareRing);

        Player original = new Player("Tester", new Stats(1, 1, 1), "forest");
        original.addItem(sword);
        original.equip(sword);
        original.addItem(spareRing);
        original.markBossDefeated("village");
        original.addUnallocatedStatPoints(4);

        repository.save(original, TEST_PATH);
        Player loaded = repository.load(TEST_PATH);

        assertTrue("equipped weapon should round-trip", loaded.hasEquippedWeapon());
        assertEquals("Iron Sword", loaded.getEquippedWeapon().getName());
        assertEquals("unequipped items should stay in inventory, not vanish",
                List.of(spareRing), loaded.getInventory());
        assertTrue("defeated-boss progress should round-trip", loaded.hasDefeatedBoss("village"));
        assertEquals(4, loaded.getUnallocatedStatPoints());
    }

    @Test
    public void hasSave_isFalseUntilASaveFileExists() throws SaveDataException {
        assertFalse(repository.saveExists(TEST_PATH));

        repository.save(new Player("Tester", new Stats(0, 0, 0), "village"), TEST_PATH);

        assertTrue(repository.saveExists(TEST_PATH));
    }
}
