package main.services.travel;

import java.util.List;
import main.domain.player.Player;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;

/*
interface for moving the player between nodes.
(if it is unlocked after defeating the boss)
*/
public interface TravelService {
    List<Node> listNodes();

    Node findNode(String nodeId) throws ItemNotFoundException;

    Node travelTo(Player player, String nodeId) throws ItemNotFoundException;

    boolean isLocked(Node node, Player player);
}
