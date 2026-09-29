package main.domain.world;

/* represents a node the player can travel to; owns its own enemy pool and a boss. */
public class Node {
    private final String id;
    private final NodeLocation location;
    private final int difficulty;
    private final boolean hasBoss;

    public Node(String id, NodeLocation location, int difficulty, boolean hasBoss) {
        this.id = id;
        this.location = location;
        this.difficulty = difficulty;
        this.hasBoss = hasBoss;
    }

    public String getId() {
        return id;
    }

    public NodeLocation getLocation() {
        return location;
    }

    public String getName() {
        return location.getName();
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean hasBoss() {
        return hasBoss;
    }
}
