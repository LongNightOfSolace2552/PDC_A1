package main.services.travel;

import java.util.Collections;
import java.util.List;
import main.domain.player.Player;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;

/* implementation of TravelService. */
public class TravelServiceImpl implements TravelService{
    private final List<Node> nodes;

    public TravelServiceImpl(List<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public List<Node> listNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public Node findNode(String nodeId) throws ItemNotFoundException {
        for (Node node : nodes) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
        }
        throw new ItemNotFoundException("No such node: " + nodeId);
    }

    @Override
    public Node travelTo(Player player, String nodeId) throws ItemNotFoundException {
        Node destination = findNode(nodeId);
        player.setCurrentNodeId(nodeId);
        return destination;
    }

    @Override
    public boolean isLocked(Node node, Player player) {
        int index = nodes.indexOf(node);
        if (index <= 0) {
            return false;
        }
        Node previous = nodes.get(index - 1);
        if (!previous.hasBoss()) {
            return false;
        }
        return !player.hasDefeatedBoss(previous.getId());
    }
}
