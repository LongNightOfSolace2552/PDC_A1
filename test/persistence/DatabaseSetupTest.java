package persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import main.domain.combat.Boss;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;
import main.domain.combat.Enemy;
import main.domain.items.Item;
import main.domain.world.Node;
import main.exceptions.DataLoadException;
import main.persistence.DatabaseCreateTables;
import main.persistence.DatabaseInsertData;
import main.persistence.DatabaseManager;
import main.persistence.DialogueDatabaseRepository;
import main.persistence.EnemyDatabaseRepository;
import main.persistence.ItemDatabaseRepository;
import main.persistence.NodeDatabaseRepository;
import org.junit.After;
import org.junit.Test;

/*
JUnit4 database tests for the Derby set-up: the tables are created once
(and safely again on the next start), the game data is inserted once,
the data comes back through the DAOs as the right domain objects, and
the tables refuse data that breaks the rules.
*/
public class DatabaseSetupTest {

    private TestDatabase database;

    @After
    public void tearDown() {
        if (database != null) {
            database.drop();
        }
    }

    @Test
    public void createTables_canRunAgainOnAnExistingDatabase() throws SQLException {
        database = TestDatabase.createEmpty();
        DatabaseCreateTables creator = new DatabaseCreateTables();

        creator.createTables(database.connection());
        creator.createTables(database.connection());

        /* the second run must not fail with "table already exists"; the tables are usable. */
        new DatabaseInsertData().insertData(database.connection());
    }

    @Test
    public void insertData_runTwice_doesNotDuplicateTheGameData() throws Exception {
        database = TestDatabase.create();

        new DatabaseInsertData().insertData(database.connection());

        assertEquals(7, new ItemDatabaseRepository(database.connection()).loadItemCatalog().size());
        assertEquals(3, new NodeDatabaseRepository(database.connection()).loadNodes().size());
        assertEquals(3, new EnemyDatabaseRepository(database.connection())
                .loadEnemyPoolsByNode().get("village").size());
    }

    @Test
    public void nodes_comeBackInTheOrderTheyUnlock() throws Exception {
        database = TestDatabase.create();

        List<Node> nodes = new NodeDatabaseRepository(database.connection()).loadNodes();

        assertEquals(List.of("village", "forest", "cave"), nodes.stream().map(Node::getId).toList());
        assertEquals("Starting Village", nodes.get(0).getName());
        assertEquals(1, nodes.get(0).getDifficulty());
        assertEquals(3, nodes.get(1).getDifficulty());
        assertEquals(5, nodes.get(2).getDifficulty());
        assertTrue(nodes.get(0).hasBoss());
    }

    @Test
    public void items_comeBackAsWeaponsAndAccessoriesWithTheirAbilities() throws Exception {
        database = TestDatabase.create();

        Map<String, Item> catalog = new ItemDatabaseRepository(database.connection()).loadItemCatalog();

        assertEquals(7, catalog.size());
        Item dagger = catalog.get("wooden_dagger");
        assertEquals("Wooden Dagger", dagger.getName());
        assertEquals(2, dagger.getStrengthBonus());
        assertNotNull("the dagger has the Frenzy ability", dagger.getAbility());
        assertEquals("Frenzy", dagger.getAbility().getName());
        assertNull("the silver ring has no ability", catalog.get("silver_ring").getAbility());
        assertEquals(2, catalog.get("lucky_amulet").getAgilityBonus());
    }

    @Test
    public void enemies_comeBackAsPoolsPerNodeAndOneBossEach() throws Exception {
        database = TestDatabase.create();
        EnemyDatabaseRepository repository = new EnemyDatabaseRepository(database.connection());

        Map<String, List<Enemy>> pools = repository.loadEnemyPoolsByNode();
        Map<String, Boss> bosses = repository.loadBosses();

        assertEquals(3, pools.size());
        for (String nodeId : List.of("village", "forest", "cave")) {
            assertEquals(nodeId + " has three regular enemies", 3, pools.get(nodeId).size());
            assertNotNull(nodeId + " has a boss", bosses.get(nodeId));
        }
        assertEquals("Village Chieftain", bosses.get("village").getName());
        assertEquals("a boss is never in the regular pool", 3, pools.get("village").size());
    }

    @Test
    public void dialogueTemplates_coverEveryStatState() throws Exception {
        database = TestDatabase.create();

        Map<StatState, List<DialogueTemplate>> templates =
                new DialogueDatabaseRepository(database.connection()).loadDialogueTemplates();

        for (StatState state : StatState.values()) {
            assertNotNull("templates for " + state, templates.get(state));
            assertTrue("at least one template for " + state, templates.get(state).size() >= 1);
        }
    }

    @Test
    public void loadingFromADatabaseWithoutTables_reportsADataLoadException() throws Exception {
        database = TestDatabase.createEmpty();

        try {
            new ItemDatabaseRepository(database.connection()).loadItemCatalog();
            fail("there is no ITEM table");
        } catch (DataLoadException expected) {
            assertTrue(expected.getMessage().contains("items"));
        }
    }

    @Test
    public void theTables_refuseARowThatBreaksTheRules() throws Exception {
        database = TestDatabase.create();

        /* a player standing on a node that does not exist (foreign key). */
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO PLAYER (NAME, STRENGTH, MAGIC, AGILITY, CURRENT_NODE_ID, UNALLOCATED_STAT_POINTS) "
                + "VALUES ('Ghost', 0, 0, 0, 'atlantis', 0)")) {
            ps.executeUpdate();
            fail("the node does not exist");
        } catch (SQLException expected) {
            assertNotNull(expected.getSQLState());
        }

        /* negative stat points (check constraint). */
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO PLAYER (NAME, STRENGTH, MAGIC, AGILITY, CURRENT_NODE_ID, UNALLOCATED_STAT_POINTS) "
                + "VALUES ('Negative', 0, 0, 0, 'village', -1)")) {
            ps.executeUpdate();
            fail("stat points cannot be negative");
        } catch (SQLException expected) {
            assertNotNull(expected.getSQLState());
        }
    }

    /* Singleton: the whole game shares one DatabaseManager (nothing is connected by asking for it). */
    @Test
    public void databaseManager_isASingleton() {
        assertSame(DatabaseManager.getManager(), DatabaseManager.getManager());
    }
}
