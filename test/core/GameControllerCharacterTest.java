package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import main.core.GameController;
import main.domain.items.Accessory;
import main.domain.items.CombatStyle;
import main.domain.items.Weapon;
import main.domain.player.Player;
import main.domain.player.StatType;
import main.domain.player.Stats;
import main.services.ActionResult;
import main.util.Randomizer;
import org.junit.Before;
import org.junit.Test;

/*
JUnit4 PASS/FAIL tests for GameController's equip, unequip and
stat-point functions (handleEquip, handleUnequipWeapon,
handleUnequipAccessory, handleAllocateStatPoints). every action either
PASSes (and changes the player) or FAILs (and leaves the player
untouched) - the checks behind the Equip / Unequip / allocate buttons,
with no screen involved.
*/
public class GameControllerCharacterTest {

    private GameController controller;
    private Player player;
    private Weapon dagger;
    private Weapon sword;
    private Accessory ring;

    @Before
    public void setUp() {
        /* these rules never touch the save folder, travel or combat, so those collaborators are left out. */
        controller = new GameController(null, null, null, new HashMap<>(), new HashMap<>(), new Randomizer());
        player = new Player("Hero", new Stats(0, 0, 0), "village");
        dagger = new Weapon("dagger", "Dagger", 1, 0, 0, null, CombatStyle.MELEE);
        sword = new Weapon("sword", "Sword", 3, 0, 0, null, CombatStyle.MELEE);
        ring = new Accessory("ring", "Ring", 0, 0, 1, null);
    }

    @Test
    public void handleEquip_anItemInTheInventory_passesAndEquipsIt() {
        player.addItem(dagger);

        ActionResult result = controller.handleEquip(player, dagger);

        assertTrue(result.isPass());
        assertEquals("Equipped Dagger.", result.getMessage());
        assertSame(dagger, player.getEquippedWeapon());
        assertTrue("the equipped item leaves the inventory list", player.getInventory().isEmpty());
    }

    @Test
    public void handleEquip_anItemNotInTheInventory_fails() {
        ActionResult result = controller.handleEquip(player, dagger);

        assertTrue(result.isFail());
        assertEquals("Could not equip Dagger.", result.getMessage());
        assertFalse(player.hasEquippedWeapon());
    }

    @Test
    public void handleEquip_nothing_fails() {
        ActionResult result = controller.handleEquip(player, null);

        assertTrue(result.isFail());
        assertFalse(player.hasEquippedWeapon());
    }

    @Test
    public void handleEquip_aSecondWeapon_passesAndReturnsTheFirstToTheInventory() {
        player.addItem(dagger);
        player.addItem(sword);
        controller.handleEquip(player, dagger);

        ActionResult result = controller.handleEquip(player, sword);

        assertTrue(result.isPass());
        assertSame(sword, player.getEquippedWeapon());
        assertTrue("the replaced weapon goes back to the inventory", player.getInventory().contains(dagger));
    }

    @Test
    public void handleEquip_anAccessory_usesTheAccessorySlot() {
        player.addItem(ring);

        ActionResult result = controller.handleEquip(player, ring);

        assertTrue(result.isPass());
        assertSame(ring, player.getEquippedAccessory());
        assertFalse(player.hasEquippedWeapon());
    }

    @Test
    public void handleUnequipWeapon_withNothingEquipped_fails() {
        ActionResult result = controller.handleUnequipWeapon(player);

        assertTrue(result.isFail());
        assertEquals("You have no weapon equipped.", result.getMessage());
    }

    @Test
    public void handleUnequipWeapon_whenEquipped_passesAndReturnsItToTheInventory() {
        player.addItem(dagger);
        controller.handleEquip(player, dagger);

        ActionResult result = controller.handleUnequipWeapon(player);

        assertTrue(result.isPass());
        assertEquals("Unequipped Dagger.", result.getMessage());
        assertFalse(player.hasEquippedWeapon());
        assertTrue(player.getInventory().contains(dagger));
    }

    @Test
    public void handleUnequipWeapon_aSecondTimeFails() {
        player.addItem(dagger);
        controller.handleEquip(player, dagger);
        controller.handleUnequipWeapon(player);

        assertTrue("there is nothing left to unequip", controller.handleUnequipWeapon(player).isFail());
    }

    @Test
    public void handleUnequipAccessory_withNothingEquipped_fails() {
        ActionResult result = controller.handleUnequipAccessory(player);

        assertTrue(result.isFail());
        assertEquals("You have no accessory equipped.", result.getMessage());
    }

    @Test
    public void handleUnequipAccessory_whenEquipped_passesAndReturnsItToTheInventory() {
        player.addItem(ring);
        controller.handleEquip(player, ring);

        ActionResult result = controller.handleUnequipAccessory(player);

        assertTrue(result.isPass());
        assertEquals("Unequipped Ring.", result.getMessage());
        assertNull(player.getEquippedAccessory());
        assertTrue(player.getInventory().contains(ring));
    }

    @Test
    public void handleAllocateStatPoints_withNoPoints_failsAndChangesNothing() {
        ActionResult result = controller.handleAllocateStatPoints(player, StatType.STRENGTH);

        assertTrue(result.isFail());
        assertEquals("You have no stat points to allocate.", result.getMessage());
        assertEquals(0, player.getStats().getStrength());
    }

    @Test
    public void handleAllocateStatPoints_spendsOnePointOnTheChosenStat() {
        player.addUnallocatedStatPoints(3);

        ActionResult strength = controller.handleAllocateStatPoints(player, StatType.STRENGTH);
        ActionResult magic = controller.handleAllocateStatPoints(player, StatType.MAGIC);
        ActionResult agility = controller.handleAllocateStatPoints(player, StatType.AGILITY);

        assertTrue(strength.isPass() && magic.isPass() && agility.isPass());
        assertEquals("Allocated 1 point to STRENGTH.", strength.getMessage());
        assertEquals(1, player.getStats().getStrength());
        assertEquals(1, player.getStats().getMagic());
        assertEquals(1, player.getStats().getAgility());
        assertEquals(0, player.getUnallocatedStatPoints());
    }

    @Test
    public void handleAllocateStatPoints_failsOnceThePointsRunOut() {
        player.addUnallocatedStatPoints(1);
        controller.handleAllocateStatPoints(player, StatType.MAGIC);

        assertTrue(controller.handleAllocateStatPoints(player, StatType.MAGIC).isFail());
        assertEquals("the stat must not grow past the points spent", 1, player.getStats().getMagic());
    }

    @Test(expected = NullPointerException.class)
    public void anyAction_withoutAPlayer_isRejected() {
        controller.handleUnequipWeapon(null);
    }
}
