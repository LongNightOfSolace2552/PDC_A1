package main.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.Boss;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.player.Stats;
import main.exceptions.DataLoadException;

/*
loads each node's regular enemy pool and its boss from data/enemies.txt,
keyed by node id. a node with no MONSTER rows simply has nothing to fight
yet, adding a node's pool is just adding lines to this file.
*/
public class EnemyFileRepository {

    private static final String ENEMIES_PATH = "data/enemies.txt";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String NO_VALUE_MARKER = "-";
    private static final String MONSTER_TYPE = "MONSTER";
    private static final String BOSS_TYPE = "BOSS";

    private final FileReaderUtil fileReaderUtil;

    public EnemyFileRepository(FileReaderUtil fileReaderUtil) {
        this.fileReaderUtil = fileReaderUtil;
    }

    /*
    enemies.txt format:
    nodeId|id|type|name|strength|magic|agility|statDropChance|statDropMin|statDropMax|itemDropChance|itemDropId
    type is MONSTER or BOSS. "-" means no value (0 for numeric fields, null for itemDropId).
    */
    public Map<String, List<Enemy>> loadEnemyPoolsByNode() throws DataLoadException {
        Map<String, List<Enemy>> pools = new HashMap<>();

        for (String[] fields : readDataRows()) {
            if (!isType(fields, MONSTER_TYPE)) {
                continue;
            }
            String nodeId = fields[0].trim();
            pools.computeIfAbsent(nodeId, key -> new ArrayList<>()).add(buildEnemy(fields));
        }

        return pools;
    }

    /*
    one boss per node, looked up by node id when the player challenges
    that node's boss.
    */
    public Map<String, Boss> loadBosses() throws DataLoadException {
        Map<String, Boss> bosses = new HashMap<>();

        for (String[] fields : readDataRows()) {
            if (!isType(fields, BOSS_TYPE)) {
                continue;
            }
            String nodeId = fields[0].trim();
            String id = fields[1].trim();
            String name = fields[3].trim();
            Stats stats = buildStats(fields, id);
            EnemyLoot loot = buildLoot(fields, id);
            bosses.put(nodeId, new Boss(id, name, stats, loot));
        }

        return bosses;
    }

    private boolean isType(String[] fields, String type) {
        return fields[2].trim().equalsIgnoreCase(type);
    }

    private Enemy buildEnemy(String[] fields) throws DataLoadException {
        String id = fields[1].trim();
        String name = fields[3].trim();
        Stats stats = buildStats(fields, id);
        EnemyLoot loot = buildLoot(fields, id);
        return new Enemy(id, name, stats, loot);
    }

    private Stats buildStats(String[] fields, String enemyId) throws DataLoadException {
        int strength = parseInt(fields[4], enemyId);
        int magic = parseInt(fields[5], enemyId);
        int agility = parseInt(fields[6], enemyId);
        return new Stats(strength, magic, agility);
    }

    private EnemyLoot buildLoot(String[] fields, String enemyId) throws DataLoadException {
        double statDropChance = parseChance(fields[7], enemyId);
        int statDropMin = parseIntOrMarker(fields[8], enemyId);
        int statDropMax = parseIntOrMarker(fields[9], enemyId);
        double itemDropChance = parseChance(fields[10], enemyId);
        String itemDropField = fields[11].trim();
        String itemDropId = itemDropField.equals(NO_VALUE_MARKER) ? null : itemDropField;
        return new EnemyLoot(statDropChance, statDropMin, statDropMax, itemDropChance, itemDropId);
    }

    private double parseChance(String field, String enemyId) throws DataLoadException {
        String trimmed = field.trim();
        if (trimmed.equals(NO_VALUE_MARKER)) {
            return 0.0;
        }
        double value;
        try {
            value = Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            throw new DataLoadException("Enemy " + enemyId + " has a non-numeric chance: " + field, e);
        }
        if (value < 0.0 || value > 1.0) {
            throw new DataLoadException("Enemy " + enemyId + " has an out-of-range chance: " + value);
        }
        return value;
    }

    private int parseIntOrMarker(String field, String enemyId) throws DataLoadException {
        String trimmed = field.trim();
        if (trimmed.equals(NO_VALUE_MARKER)) {
            return 0;
        }
        return parseInt(trimmed, enemyId);
    }

    private int parseInt(String field, String enemyId) throws DataLoadException {
        try {
            return Integer.parseInt(field.trim());
        } catch (NumberFormatException e) {
            throw new DataLoadException("Enemy " + enemyId + " has a non-numeric value: " + field, e);
        }
    }

    private List<String[]> readDataRows() throws DataLoadException {
        List<String[]> rows = new ArrayList<>();
        for (String line : fileReaderUtil.readLines(ENEMIES_PATH)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] fields = trimmed.split(FIELD_SEPARATOR, 12);
            if (fields.length != 12) {
                throw new DataLoadException("Malformed line in " + ENEMIES_PATH + ": " + trimmed);
            }
            rows.add(fields);
        }
        return rows;
    }
}
