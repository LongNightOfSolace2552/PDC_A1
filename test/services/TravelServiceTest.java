package services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.domain.world.Node;
import main.domain.world.NodeLocation;
import main.exceptions.ItemNotFoundException;
import main.services.travel.TravelService;
import main.services.travel.TravelServiceImpl;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 tests for TravelServiceImpl: the node-locking business rule
(a node is locked until the PREVIOUS node's boss is defeated) and basic
travel/lookup behavior. (the PASS/FAIL checks made before travelling
are GameController.handleTravel's - see GameControllerTravelTest.)
*/
public class TravelServiceTest {

    private Node village;
    private Node forest;
    private Node cave;
    private TravelService travelService;

    @Before
    public void setUp() {
        village = new Node("village", new NodeLocation("village", "Starting Village", "..."), 1, true);
        forest = new Node("forest", new NodeLocation("forest", "Whispering Forest", "..."), 3, true);
        cave = new Node("cave", new NodeLocation("cave", "Shadow Cave", "..."), 5, false);
        travelService = new TravelServiceImpl(List.of(village, forest, cave));
    }

    @Test
    public void firstNode_isNeverLocked() {
        Player player = new Player("Hero", new Stats(0, 0, 0), "village");

        assertFalse(travelService.isLocked(village, player));
    }

    @Test
    public void laterNode_isLockedUntilPreviousBossDefeated() {
        Player player = new Player("Hero", new Stats(0, 0, 0), "village");

        assertTrue("forest should be locked before the village boss is defeated",
                travelService.isLocked(forest, player));

        player.markBossDefeated("village");

        assertFalse("forest should unlock once the village boss is defeated",
                travelService.isLocked(forest, player));
    }

    @Test
    public void nodeWithNoBoss_doesNotLockTheNextNode() {
        Player player = new Player("Hero", new Stats(0, 0, 0), "village");
        player.markBossDefeated("village");

        /* cave has no boss of its own, but that doesn't matter here - what's checked is whether cave itself is locked by forest. */
        assertTrue("cave should still be locked until the forest boss is defeated",
                travelService.isLocked(cave, player));

        player.markBossDefeated("forest");

        assertFalse(travelService.isLocked(cave, player));
    }

    @Test
    public void travelTo_movesThePlayerToTheDestinationNode() throws ItemNotFoundException {
        Player player = new Player("Hero", new Stats(0, 0, 0), "village");

        travelService.travelTo(player, "forest");

        assertEquals("forest", player.getCurrentNodeId());
    }

    @Test(expected = ItemNotFoundException.class)
    public void findNode_throwsForAnUnknownNodeId() throws ItemNotFoundException {
        travelService.findNode("does_not_exist");
    }
}
