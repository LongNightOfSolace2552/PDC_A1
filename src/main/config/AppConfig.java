package main.config;

/*builds the whole game: opens the Derby database, loads the game data and 
wires the services together.*/
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import main.core.Game;
import main.core.GameController;
import main.core.SaveManager;
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
import main.persistence.PlayerDatabaseRepository;
import main.services.combat.CombatService;
import main.services.combat.CombatServiceImpl;
import main.services.combat.DialogueService;
import main.services.combat.DialogueServiceImpl;
import main.services.travel.TravelService;
import main.services.travel.TravelServiceImpl;
import main.util.Randomizer;

/* dependency wiring for services and repositories. */
public class AppConfig {
    // Connects to embedded Derby database, creates on first run,
    // makes sure every table exists and fill data if they are empty.
    private Connection openDatabase() throws DataLoadException {
        try {
            Connection connection = DatabaseManager.getManager().getConnection();
            new DatabaseCreateTables().createTables(connection);
            new DatabaseInsertData().insertData(connection);
            return connection;
        } catch (SQLException e) {
            throw new DataLoadException("Could not open or set up the database.", e);
        }
    }    
    
    /*
    builds every service and its dependencies in one place.
    Classes in the upper inheritance tree only depend on the
    abstractions and collaborator needed
    */
    public Game buildGame() throws DataLoadException {
        Randomizer randomizer = new Randomizer();
        Connection connection= openDatabase();

        ItemDatabaseRepository itemDatabaseRepository = new ItemDatabaseRepository(connection);
        Map<String, Item> itemCatalog = itemDatabaseRepository.loadItemCatalog();

        PlayerDatabaseRepository playerDatabaseRepository =
                new PlayerDatabaseRepository(connection,itemCatalog);
        SaveManager saveManager = new SaveManager(playerDatabaseRepository);

        NodeDatabaseRepository nodeDatabaseRepository = new NodeDatabaseRepository(connection);
        List<Node> nodes = nodeDatabaseRepository.loadNodes();
        TravelService travelService = new TravelServiceImpl(nodes);

        EnemyDatabaseRepository enemyDatabaseRepository = new EnemyDatabaseRepository(connection);
        Map<String, List<Enemy>> enemyPoolsByNode = enemyDatabaseRepository.loadEnemyPoolsByNode();
        Map<String, Boss> bossesByNode = enemyDatabaseRepository.loadBosses();

        DialogueDatabaseRepository dialogueDatabaseRepository = new DialogueDatabaseRepository(connection);
        Map<StatState, List<DialogueTemplate>> templatesByState = dialogueDatabaseRepository.loadDialogueTemplates();
        DialogueService dialogueService = new DialogueServiceImpl(templatesByState);

        CombatService combatService = new CombatServiceImpl(randomizer, itemCatalog, dialogueService);

        GameController gameController = new GameController(saveManager, travelService, combatService,
                enemyPoolsByNode, bossesByNode, randomizer);

        return new Game(gameController, saveManager, itemCatalog);
    }
}
