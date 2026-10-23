package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import main.core.GameController;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.domain.world.Node;
import main.domain.world.NodeLocation;
import main.exceptions.ItemNotFoundException;
import main.services.ActionResult;
import main.services.travel.TravelServiceImpl;
import main.util.Randomizer;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 PASS/FAIL tests for GameController.handleTravel - the checks made
before the player moves (unknown node, still locked, already there) -
and for the node lookups the Travel, Node and main-menu screens use.
*/
public class GameControllerTravelTest {

    private Node village;
    private Node forest;
    private Node cave;
    private GameController controller;
    private Player player;

    @Before
    public void setUp() {
        village = new Node("village", new NodeLocation("village", "Starting Village", "..."), 1, true);
        forest = new Node("forest", new NodeLocation("forest", "Whispering Forest", "..."), 3, true);
        cave = new Node("cave", new NodeLocation("cave", "Shadow Cave", "..."), 5, false);

        /* travelling only needs the node list; save, combat, enemies and bosses are left out. */
        controller = new GameController(null, new TravelServiceImpl(List.of(village, forest, cave)), null,
                new HashMap<>(), new HashMap<>(), new Randomizer());
        player = new Player("Hero", new Stats(0, 0, 0), "village");
    }

    @Test
    public void handleTravel_toAnUnlockedNode_passesAndMovesThePlayer() {
        player.markBossDefeated("village");

        ActionResult result = controller.handleTravel(player, "forest");

        assertTrue(result.isPass());
        assertEquals("You travel to Whispering Forest.", result.getMessage());
        assertEquals("forest", player.getCurrentNodeId());
    }

    @Test
    public void handleTravel_toALockedNode_failsAndDoesNotMoveThePlayer() {
        ActionResult result = controller.handleTravel(player, "forest");

        assertTrue(result.isFail());
        assertEquals("Whispering Forest is locked. Defeat the previous node's boss first.", result.getMessage());
        assertEquals("village", player.getCurrentNodeId());
    }

    @Test
    public void handleTravel_toTheNodeThePlayerIsAlreadyAt_fails() {
        ActionResult result = controller.handleTravel(player, "village");

        assertTrue(result.isFail());
        assertEquals("You are already at Starting Village.", result.getMessage());
    }

    @Test
    public void handleTravel_toAnUnknownNode_failsAndDoesNotMoveThePlayer() {
        ActionResult result = controller.handleTravel(player, "does_not_exist");

        assertTrue(result.isFail());
        assertTrue(result.getMessage().contains("does_not_exist"));
        assertEquals("village", player.getCurrentNodeId());
    }

    @Test
    public void handleTravel_unlocksOneNodeAtATime() {
        player.markBossDefeated("village");
        controller.handleTravel(player, "forest");

        ActionResult toCave = controller.handleTravel(player, "cave");

        assertTrue("the cave stays locked until the forest boss is beaten", toCave.isFail());
        assertEquals("forest", player.getCurrentNodeId());
    }

    @Test(expected = NullPointerException.class)
    public void handleTravel_withoutAPlayer_isRejected() {
        controller.handleTravel(null, "forest");
    }

    @Test
    public void findCurrentNode_returnsTheNodeThePlayerIsAt() throws ItemNotFoundException {
        assertSame(village, controller.findCurrentNode(player));

        player.setCurrentNodeId("forest");

        assertSame(forest, controller.findCurrentNode(player));
    }

    @Test(expected = ItemNotFoundException.class)
    public void findCurrentNode_throwsWhenTheSavedNodeNoLongerExists() throws ItemNotFoundException {
        player.setCurrentNodeId("removed_from_the_data");

        controller.findCurrentNode(player);
    }

    @Test
    public void listNodes_returnsEveryNodeInOrder() {
        assertEquals(List.of(village, forest, cave), controller.listNodes());
    }

    @Test
    public void isLocked_followsThePreviousNodesBoss() {
        assertFalse("the first node is never locked", controller.isLocked(village, player));
        assertTrue(controller.isLocked(forest, player));

        player.markBossDefeated("village");

        assertFalse(controller.isLocked(forest, player));
    }
}
