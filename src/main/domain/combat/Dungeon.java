package main.domain.combat;

import java.util.List;
import java.util.Map;
import main.util.Randomizer;

/*
the single infinite dungeon; draws its active enemy pool from the
player's current node until the player exits.

tracks floor progression: each floor requires a randomized number of
enemy defeats (2 to 5) before advancing to the next floor, at which
point a new random quota is rolled. losing a fight or leaving the
dungeon both reset progress back to floor 1 - nothing here is
persisted across saves.
*/
public class Dungeon {

    private static final int MIN_ENEMIES_PER_FLOOR = 2;
    private static final int MAX_ENEMIES_PER_FLOOR = 5;

    private final Randomizer random;
    private final Map<String, List<Enemy>> enemyPool;
    private final String nodeId;

    private int floor = 1;
    private int enemiesDefeatedThisFloor = 0;
    private int enemiesRequiredThisFloor;

    public Dungeon(Randomizer random, Map<String, List<Enemy>> enemyPool, String nodeId) {
        this.random = random;
        this.enemyPool = enemyPool;
        this.nodeId = nodeId;
        this.enemiesRequiredThisFloor = rollFloorQuota();
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getFloor() {
        return floor;
    }

    /*
    picks a random enemy from the current node's pool. returns null if
    the node has no enemies at all, rather than crashing on an empty or
    missing list.
    */
    public Enemy nextEnemy() {
        List<Enemy> enemies = enemyPool.get(nodeId);
        if (enemies == null || enemies.isEmpty()) {
            return null;
        }
        int index = random.nextInt(enemies.size());
        return enemies.get(index);
    }

    /*
    call after a won fight. advances the floor (and rolls a new random
    quota for the next one) once enough enemies have been defeated on
    the current floor.
    */
    public void recordVictory() {
        enemiesDefeatedThisFloor++;
        if (enemiesDefeatedThisFloor >= enemiesRequiredThisFloor) {
            floor++;
            enemiesDefeatedThisFloor = 0;
            enemiesRequiredThisFloor = rollFloorQuota();
        }
    }

    /*
    call after a lost fight. drops the player straight back to floor 1
    with a fresh random quota, same as leaving and re-entering.
    */
    public void resetProgress() {
        floor = 1;
        enemiesDefeatedThisFloor = 0;
        enemiesRequiredThisFloor = rollFloorQuota();
    }

    private int rollFloorQuota() {
        return MIN_ENEMIES_PER_FLOOR + random.nextInt(MAX_ENEMIES_PER_FLOOR - MIN_ENEMIES_PER_FLOOR + 1);
    }
}
