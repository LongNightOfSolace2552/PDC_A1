package persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.persistence.FileReaderUtil;
import main.persistence.FileWriterUtil;
import main.persistence.PlayerFileRepository;

/* demonstrates PlayerFileRepository's save/load round trip: a saved player's stats and node come back unchanged. */
public class FileRepositoryTest {

    public static void main(String[] args) throws Exception {
        String testPath = "data/test_player_repository.txt";
        Map<String, Item> emptyCatalog = new HashMap<>();
        PlayerFileRepository repository = new PlayerFileRepository(new FileReaderUtil(), new FileWriterUtil(), emptyCatalog);

        Player original = new Player("Tester", new Stats(5, 3, 2), "village");
        repository.save(original, testPath);

        Player loaded = repository.load(testPath);

        check("name matches", "Tester".equals(loaded.getName()));
        check("strength matches", loaded.getStats().getStrength() == 5);
        check("magic matches", loaded.getStats().getMagic() == 3);
        check("agility matches", loaded.getStats().getAgility() == 2);
        check("current node matches", "village".equals(loaded.getCurrentNodeId()));

        Files.deleteIfExists(Path.of(testPath));
    }

    private static void check(String description, boolean passed) {
        System.out.println((passed ? "PASS: " : "FAIL: ") + description);
    }
}
