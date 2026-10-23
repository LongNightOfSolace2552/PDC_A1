/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInsertData {
    
    public void insertData(Connection connection) throws SQLException{
        insertNodeLocations(connection);
        insertNodes(connection);
        insertItems(connection);
        insertEnemies(connection);
        insertDialogue(connection);
    }
    
    // Returns false if rows exist in tableName, true if no rows exists.
    private boolean rowExist(Connection connection,String tableName) throws SQLException{
        
        try(Statement statement=connection.createStatement();
            ResultSet rs=statement.executeQuery("SELECT COUNT(*) FROM "+tableName);){
            rs.next();
            return rs.getInt(1)==0;
        }
    }
    
    // Fill slots of ps and execute sql to fill in a row in NODE_LOCATION.
    // Called by insertNodeLocations.
    private void insertNodeLocationRows(PreparedStatement ps,String id,String name,String description) throws SQLException{
        ps.setString(1,id);
        ps.setString(2,name);
        ps.setString(3,description);
        ps.executeUpdate();
    }
    
    // Insert data in NODE_LOCATION table.
    private void insertNodeLocations(Connection connection) throws SQLException{
        if (rowExist(connection,"NODE_LOCATION")==false){
            return;
        }
        String sql = "INSERT INTO NODE_LOCATION (ID, NAME, DESCRIPTION) VALUES (?, ?, ?)";
        try(PreparedStatement ps=connection.prepareStatement(sql)){
            insertNodeLocationRows(ps, "village", "Starting Village","A quiet village where new adventurers begin their journey.");
            insertNodeLocationRows(ps, "forest", "Whispering Forest","A dense forest said to hide ancient ruins and restless spirits.");
            insertNodeLocationRows(ps, "cave", "Shadow Cave","A dark cave echoing with distant growls.");
        }
    }
    
    // Fill slots of ps and execute sql to fill in a row in NODE.
    // Called by insertNode.
    private void insertNodeRows(PreparedStatement ps,String id,String locationId,int difficulty,boolean hasBoss) throws SQLException{
        ps.setString(1,id);
        ps.setString(2,locationId);
        ps.setInt(3, difficulty);
        ps.setBoolean(4, hasBoss);
        ps.executeUpdate();
    }
    
    // Insert data in NODE table.
    private void insertNodes(Connection connection) throws SQLException{
        if (rowExist(connection,"NODE")==false){
            return;
        }
        String sql = "INSERT INTO NODE (ID, LOCATION_ID, DIFFICULTY, HAS_BOSS) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            insertNodeRows(ps, "village", "village", 1, true);
            insertNodeRows(ps, "forest", "forest", 3, true);
            insertNodeRows(ps, "cave", "cave", 5, true);
        }
    }
    
    // Fill slots of ps and execute sql to fill in a row in ITEM.
    // Called by insertItems.
    private void insertItemRows(PreparedStatement ps, String id, String type, String name,
            int strengthBonus, int magicBonus, int agilityBonus,
            String combatStyle, String abilityName, double abilityChance,
            int abilityBonusStrength) throws SQLException{
        ps.setString(1, id);
        ps.setString(2, type);
        ps.setString(3, name);
        ps.setInt(4, strengthBonus);
        ps.setInt(5, magicBonus);
        ps.setInt(6, agilityBonus);
        ps.setString(7, combatStyle);      
        ps.setString(8, abilityName);      
        ps.setDouble(9, abilityChance);
        ps.setInt(10, abilityBonusStrength);
        ps.executeUpdate();
    }

    // Insert data in ITEM table.
    private void insertItems(Connection connection) throws SQLException{
        if (rowExist(connection,"ITEM")==false){
            return;
        }
        String sql = "INSERT INTO ITEM (ID, ITEM_TYPE, NAME, STRENGTH_BONUS, MAGIC_BONUS, "
                + "AGILITY_BONUS, COMBAT_STYLE, ABILITY_NAME, ABILITY_CHANCE, ABILITY_BONUS_STRENGTH) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            insertItemRows(ps, "wooden_dagger", "WEAPON", "Wooden Dagger", 2, 0, 0, "MELEE", "Frenzy", 0.15, 8);
            insertItemRows(ps, "iron_sword", "WEAPON", "Iron Sword", 3, 0, 0, "MELEE", null, 0, 0);
            insertItemRows(ps, "bow", "WEAPON", "Bow", 2, 0, 0, "RANGED", null, 0, 0);
            insertItemRows(ps, "apprentice_staff", "WEAPON", "Apprentice Staff", 0, 3, 0, "MAGE", null, 0, 0);
            insertItemRows(ps, "silver_ring", "ACCESSORY", "Silver Ring", 1, 0, 0, null, null, 0, 0);
            insertItemRows(ps, "lucky_amulet", "ACCESSORY", "Lucky Amulet", 0, 0, 2, null, "Lucky Charm", 0.05, 5);
            insertItemRows(ps, "wooden_ring", "ACCESSORY", "Wooden Ring", 1, 0, 0, null, null, 0, 0);
        }
    }
    
    // Fill slots of ps and execute sql to fill in a row in ENEMY.
    // Called by insertEnemies.
    private void insertEnemyRows(PreparedStatement ps, String nodeId, String id, String type,
            String name, int strength, int magic, int agility, double statDropChance,
            int statDropMin, int statDropMax, double itemDropChance,
            String itemDropId) throws SQLException{
        ps.setString(1, nodeId);
        ps.setString(2, id);
        ps.setString(3, type);
        ps.setString(4, name);
        ps.setInt(5, strength);
        ps.setInt(6, magic);
        ps.setInt(7, agility);
        ps.setDouble(8, statDropChance);
        ps.setInt(9, statDropMin);
        ps.setInt(10, statDropMax);
        ps.setDouble(11, itemDropChance);
        ps.setString(12, itemDropId);      
        ps.executeUpdate();
    }

    // Insert data in ENEMY table.
    private void insertEnemies(Connection connection) throws SQLException{
        if (rowExist(connection,"ENEMY")==false){
            return;
        }
        String sql = "INSERT INTO ENEMY (NODE_ID, ID, ENEMY_TYPE, NAME, STRENGTH, MAGIC, AGILITY, "
                + "STAT_DROP_CHANCE, STAT_DROP_MIN, STAT_DROP_MAX, ITEM_DROP_CHANCE, ITEM_DROP_ID) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            insertEnemyRows(ps, "village", "slime", "MONSTER", "Slime", 3, 1, 1, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "village", "bandit", "MONSTER", "Bandit", 4, 2, 2, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "village", "wolf", "MONSTER", "Dire Wolf", 4, 3, 3, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "village", "village_boss", "BOSS", "Village Chieftain", 12, 8, 5, 1.0, 2, 3, 0, null);

            insertEnemyRows(ps, "forest", "slime", "MONSTER", "Slime", 6, 3, 2, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "forest", "bandit", "MONSTER", "Bandit", 8, 4, 4, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "forest", "wolf", "MONSTER", "Dire Wolf", 12, 6, 8, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "forest", "forest_boss", "BOSS", "Forest Guardian", 20, 20, 10, 1.0, 2, 3, 0, null);

            insertEnemyRows(ps, "cave", "slime", "MONSTER", "Slime", 10, 5, 3, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "cave", "bandit", "MONSTER", "Bandit", 15, 8, 8, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "cave", "wolf", "MONSTER", "Dire Wolf", 24, 12, 14, 0.6, 1, 2, 0.15, null);
            insertEnemyRows(ps, "cave", "cave_boss", "BOSS", "Cave Dweller", 50, 45, 20, 1.0, 2, 3, 0, null);
        }
    }
    
    // Fill slots of ps and execute sql to fill in a row in DIALOGUE_TEMPLATE.
    // Called by insertDialogue.
    private void insertDialogueRows(PreparedStatement ps, String statState, String templateText) throws SQLException{
        ps.setString(1, statState);
        ps.setString(2, templateText);
        ps.executeUpdate();
    }

    // Insert data in DIALOGUE_TEMPLATE table.
    private void insertDialogue(Connection connection) throws SQLException{
        if (rowExist(connection,"DIALOGUE_TEMPLATE")==false){
            return;
        }
        String sql = "INSERT INTO DIALOGUE_TEMPLATE (STAT_STATE, TEMPLATE_TEXT) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            insertDialogueRows(ps, "AHEAD", "You strike true! <enemy> could not withstand your strength.");
            insertDialogueRows(ps, "AHEAD", "<player> presses the advantage and brings <enemy> down.");

            insertDialogueRows(ps, "BEHIND", "<enemy> overwhelms you. You stagger back in defeat.");
            insertDialogueRows(ps, "BEHIND", "Your attack falls short - <enemy> proves too strong.");

            insertDialogueRows(ps, "WAY_AHEAD", "<player> crushes <enemy> effortlessly, barely breaking a sweat.");
            insertDialogueRows(ps, "WAY_AHEAD", "<enemy> never stood a chance against <player>'s might.");

            insertDialogueRows(ps, "AGILITY_CLUTCH", "Just as <enemy> gains the upper hand, <player>'s swift reflexes turn the tide!");
            insertDialogueRows(ps, "AGILITY_CLUTCH", "<player> barely dodges the killing blow and turns it into victory.");
            insertDialogueRows(ps, "AGILITY_CLUTCH", "<player> weaves through a flurry of attacks with a series of complex maneuvers, turning defense into triumph.");
            insertDialogueRows(ps, "AGILITY_CLUTCH", "A single well-timed dodge is all <player> needed to seize victory from <enemy>.");
        }
    }
}

