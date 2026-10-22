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
    
        private static final String UPDATE_PLAYER =
            "UPDATE PLAYER SET STRENGTH = ?, MAGIC = ?, AGILITY = ?, CURRENT_NODE_ID = ?, "
            + "EQUIPPED_WEAPON = ?, EQUIPPED_ACCESSORY = ?, UNALLOCATED_STAT_POINTS = ? WHERE NAME = ?";
    private static final String INSERT_PLAYER =
            "INSERT INTO PLAYER (STRENGTH, MAGIC, AGILITY, CURRENT_NODE_ID, "
            + "EQUIPPED_WEAPON, EQUIPPED_ACCESSORY, UNALLOCATED_STAT_POINTS, NAME) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_INVENTORY =
            "DELETE FROM PLAYER_INVENTORY WHERE PLAYER_NAME = ?";
    private static final String INSERT_INVENTORY =
            "INSERT INTO PLAYER_INVENTORY (PLAYER_NAME, ITEM_ID) VALUES (?, ?)";
    private static final String DELETE_DEFEATED_BOSSES =
            "DELETE FROM PLAYER_DEFEATED_BOSS WHERE PLAYER_NAME = ?";
    private static final String INSERT_DEFEATED_BOSS =
            "INSERT INTO PLAYER_DEFEATED_BOSS (PLAYER_NAME, NODE_ID) VALUES (?, ?)";
    
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
    
    /* writes the player and everything they own as one transaction:
       either all three tables are saved, or none are. */
    public void save(Player player, String name) throws SaveDataException {
        try {
            connection.setAutoCommit(false);
            writePlayerRow(player, name);
            replaceInventory(player, name);
            replaceDefeatedBosses(player, name);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new SaveDataException("Could not save player " + name + " to the database", e);
        } finally {
            restoreAutoCommit();
        }
    }

    /* UPDATE first; if no row matched (executeUpdate returned 0) it's a new player. */
    private void writePlayerRow(Player player, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_PLAYER)) {
            bindPlayerColumns(ps, player, name);
            if (ps.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(INSERT_PLAYER)) {
            bindPlayerColumns(ps, player, name);
            ps.executeUpdate();
        }
    }

    /* UPDATE_PLAYER and INSERT_PLAYER list their ? in the same order, so one
       helper fills both. setString(n, null) stores SQL NULL (nothing equipped). */
    private void bindPlayerColumns(PreparedStatement ps, Player player, String name) throws SQLException {
        ps.setInt(1, player.getStats().getStrength());
        ps.setInt(2, player.getStats().getMagic());
        ps.setInt(3, player.getStats().getAgility());
        ps.setString(4, player.getCurrentNodeId());
        ps.setString(5, player.hasEquippedWeapon() ? player.getEquippedWeapon().getId() : null);
        ps.setString(6, player.hasEquippedAccessory() ? player.getEquippedAccessory().getId() : null);
        ps.setInt(7, player.getUnallocatedStatPoints());
        ps.setString(8, name);
    }

    /* getInventory() holds only unequipped items; equipped ones live in the PLAYER row. */
    private void replaceInventory(Player player, String name) throws SQLException {
        deleteRowsFor(DELETE_INVENTORY, name);
        try (PreparedStatement ps = connection.prepareStatement(INSERT_INVENTORY)) {
            for (Item item : player.getInventory()) {
                ps.setString(1, name);
                ps.setString(2, item.getId());
                ps.executeUpdate();
            }
        }
    }

    private void replaceDefeatedBosses(Player player, String name) throws SQLException {
        deleteRowsFor(DELETE_DEFEATED_BOSSES, name);
        try (PreparedStatement ps = connection.prepareStatement(INSERT_DEFEATED_BOSS)) {
            for (String nodeId : player.getDefeatedBossNodeIds()) {
                ps.setString(1, name);
                ps.setString(2, nodeId);
                ps.executeUpdate();
            }
        }
    }

    private void deleteRowsFor(String sql, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            /* nothing more can be done; the original error is the one to report */
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            /* same reason as rollbackQuietly */
        }
    }
}