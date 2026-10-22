/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.SaveDataException;

/* loads and saves player data using the PLAYER, PLAYER_INVENTORY and
   PLAYER_DEFEATED_BOSS tables.
   Structure drafted with AI assistance (Claude) - see AI usage log. */
public class PlayerDatabaseRepository {
    private static final String COUNT_PLAYER =
            "SELECT COUNT(*) FROM PLAYER WHERE NAME = ?";
    private static final String SELECT_PLAYER =
            "SELECT NAME, STRENGTH, MAGIC, AGILITY, CURRENT_NODE_ID, "
            + "EQUIPPED_WEAPON, EQUIPPED_ACCESSORY, UNALLOCATED_STAT_POINTS "
            + "FROM PLAYER WHERE NAME = ?";
    private static final String SELECT_INVENTORY =
            "SELECT ITEM_ID FROM PLAYER_INVENTORY WHERE PLAYER_NAME = ? ORDER BY ID";
    private static final String SELECT_DEFEATED_BOSSES =
            "SELECT NODE_ID FROM PLAYER_DEFEATED_BOSS WHERE PLAYER_NAME = ?";

    private final Connection connection;
    private final Map<String, Item> itemCatalog;

    public PlayerDatabaseRepository(Connection connection, Map<String, Item> itemCatalog) {
        this.connection = connection;
        this.itemCatalog = itemCatalog;
    }

    /* true if a PLAYER row exists with this exact name. */
    public boolean saveExists(String name) {
        try (PreparedStatement ps = connection.prepareStatement(COUNT_PLAYER)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public Player load(String name) throws SaveDataException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SaveDataException("No save found for player " + name);
                }
                Stats stats = new Stats(rs.getInt("STRENGTH"), rs.getInt("MAGIC"), rs.getInt("AGILITY"));
                Player player = new Player(rs.getString("NAME"), stats, rs.getString("CURRENT_NODE_ID"));
                player.addUnallocatedStatPoints(rs.getInt("UNALLOCATED_STAT_POINTS"));

                restoreInventory(player, name);
                restoreEquippedItem(player, rs.getString("EQUIPPED_WEAPON"));
                restoreEquippedItem(player, rs.getString("EQUIPPED_ACCESSORY"));
                restoreDefeatedBosses(player, name);
                return player;
            }
        } catch (SQLException e) {
            throw new SaveDataException("Could not load player " + name + " from the database", e);
        }
    }

    /* one PLAYER_INVENTORY row per item the player is carrying, in the order saved. */
    private void restoreInventory(Player player, String name) throws SQLException, SaveDataException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_INVENTORY)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    player.addItem(resolveItem(rs.getString("ITEM_ID")));
                }
            }
        }
    }

    /* itemId is NULL when nothing is equipped in that slot. */
    private void restoreEquippedItem(Player player, String itemId) throws SaveDataException {
        if (itemId == null) {
            return;
        }
        Item item = resolveItem(itemId);
        /* equip() removes the item from the inventory and moves it into the
           equip slot, so give it to the inventory first, then equip it. */
        player.addItem(item);
        player.equip(item);
    }

    private void restoreDefeatedBosses(Player player, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_DEFEATED_BOSSES)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    player.markBossDefeated(rs.getString("NODE_ID"));
                }
            }
        }
    }

    private Item resolveItem(String itemId) throws SaveDataException {
        Item item = itemCatalog.get(itemId);
        if (item == null) {
            throw new SaveDataException("Save references an unknown item: " + itemId);
        }
        return item;
    }
}