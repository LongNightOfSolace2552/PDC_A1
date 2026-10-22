/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCreateTables {
    
    // Checks whether tableName exists or not.
    private boolean tableExist(Connection connection,String tableName) throws SQLException{
        DatabaseMetaData metaData=connection.getMetaData();
        try(ResultSet rs=metaData.getTables(null,null,tableName.toUpperCase(),null)){
            return rs.next();
        }
    }
    
    // Runs the sql that creates table only if the table does not exist.
    private void createTable(Connection connection,String tableName,String sql) throws SQLException{
        if (tableExist(connection,tableName)){
            return;
        }
        try(Statement statement=connection.createStatement()){
            statement.executeUpdate(sql);
        }
    }
    
    // Creates all tables for all data repositories
    public void createTables(Connection connection) throws SQLException {
        createTable(connection, "NODE_LOCATION",
                "CREATE TABLE NODE_LOCATION ("
                + "ID VARCHAR(30) PRIMARY KEY, "
                + "NAME VARCHAR(60) NOT NULL, "
                + "DESCRIPTION VARCHAR(255) NOT NULL)");

        createTable(connection, "NODE",
                "CREATE TABLE NODE ("
                + "ID VARCHAR(30) PRIMARY KEY, "
                + "LOCATION_ID VARCHAR(30) NOT NULL, "
                + "DIFFICULTY INT NOT NULL CHECK (DIFFICULTY > 0), "
                + "HAS_BOSS BOOLEAN NOT NULL, "
                + "FOREIGN KEY (LOCATION_ID) REFERENCES NODE_LOCATION(ID))");

        createTable(connection, "ITEM",
                "CREATE TABLE ITEM ("
                + "ID VARCHAR(40) PRIMARY KEY, "
                + "ITEM_TYPE VARCHAR(10) NOT NULL CHECK (ITEM_TYPE IN ('WEAPON', 'ACCESSORY')), "
                + "NAME VARCHAR(60) NOT NULL, "
                + "STRENGTH_BONUS INT NOT NULL, "
                + "MAGIC_BONUS INT NOT NULL, "
                + "AGILITY_BONUS INT NOT NULL, "
                + "COMBAT_STYLE VARCHAR(10) CHECK (COMBAT_STYLE IN ('MELEE', 'RANGED', 'MAGE')), "
                + "ABILITY_NAME VARCHAR(40), "
                + "ABILITY_CHANCE DOUBLE NOT NULL CHECK (ABILITY_CHANCE BETWEEN 0 AND 1), "
                + "ABILITY_BONUS_STRENGTH INT NOT NULL)");

        createTable(connection, "ENEMY",
                "CREATE TABLE ENEMY ("
                + "NODE_ID VARCHAR(30) NOT NULL, "
                + "ID VARCHAR(40) NOT NULL, "
                + "ENEMY_TYPE VARCHAR(10) NOT NULL CHECK (ENEMY_TYPE IN ('MONSTER', 'BOSS')), "
                + "NAME VARCHAR(60) NOT NULL, "
                + "STRENGTH INT NOT NULL CHECK (STRENGTH >= 0), "
                + "MAGIC INT NOT NULL CHECK (MAGIC >= 0), "
                + "AGILITY INT NOT NULL CHECK (AGILITY >= 0), "
                + "STAT_DROP_CHANCE DOUBLE NOT NULL CHECK (STAT_DROP_CHANCE BETWEEN 0 AND 1), "
                + "STAT_DROP_MIN INT NOT NULL CHECK (STAT_DROP_MIN >= 0), "
                + "STAT_DROP_MAX INT NOT NULL CHECK (STAT_DROP_MAX >= 0), "
                + "ITEM_DROP_CHANCE DOUBLE NOT NULL CHECK (ITEM_DROP_CHANCE BETWEEN 0 AND 1), "
                + "ITEM_DROP_ID VARCHAR(40), "
                + "CHECK (STAT_DROP_MIN <= STAT_DROP_MAX), "
                + "PRIMARY KEY (NODE_ID, ID), "
                + "FOREIGN KEY (NODE_ID) REFERENCES NODE(ID), "
                + "FOREIGN KEY (ITEM_DROP_ID) REFERENCES ITEM(ID))");

        createTable(connection, "DIALOGUE_TEMPLATE",
                "CREATE TABLE DIALOGUE_TEMPLATE ("
                + "ID INT NOT NULL GENERATED ALWAYS AS IDENTITY, "
                + "STAT_STATE VARCHAR(20) NOT NULL, "
                + "TEMPLATE_TEXT VARCHAR(500) NOT NULL, "
                + "PRIMARY KEY (ID))");

        createTable(connection, "PLAYER",
                "CREATE TABLE PLAYER ("
                + "NAME VARCHAR(50) PRIMARY KEY, "
                + "STRENGTH INT NOT NULL CHECK (STRENGTH >= 0), "
                + "MAGIC INT NOT NULL CHECK (MAGIC >= 0), "
                + "AGILITY INT NOT NULL CHECK (AGILITY >= 0), "
                + "CURRENT_NODE_ID VARCHAR(30) NOT NULL, "
                + "EQUIPPED_WEAPON VARCHAR(40), "
                + "EQUIPPED_ACCESSORY VARCHAR(40), "
                + "UNALLOCATED_STAT_POINTS INT NOT NULL CHECK (UNALLOCATED_STAT_POINTS >= 0), "
                + "FOREIGN KEY (CURRENT_NODE_ID) REFERENCES NODE(ID), "
                + "FOREIGN KEY (EQUIPPED_WEAPON) REFERENCES ITEM(ID), "
                + "FOREIGN KEY (EQUIPPED_ACCESSORY) REFERENCES ITEM(ID))");

        createTable(connection, "PLAYER_INVENTORY",
                "CREATE TABLE PLAYER_INVENTORY ("
                + "ID INT NOT NULL GENERATED ALWAYS AS IDENTITY, "
                + "PLAYER_NAME VARCHAR(50) NOT NULL, "
                + "ITEM_ID VARCHAR(40) NOT NULL, "
                + "PRIMARY KEY (ID), "
                + "FOREIGN KEY (PLAYER_NAME) REFERENCES PLAYER(NAME), "
                + "FOREIGN KEY (ITEM_ID) REFERENCES ITEM(ID))");

        createTable(connection, "PLAYER_DEFEATED_BOSS",
                "CREATE TABLE PLAYER_DEFEATED_BOSS ("
                + "PLAYER_NAME VARCHAR(50) NOT NULL, "
                + "NODE_ID VARCHAR(30) NOT NULL, "
                + "PRIMARY KEY (PLAYER_NAME, NODE_ID), "
                + "FOREIGN KEY (PLAYER_NAME) REFERENCES PLAYER(NAME), "
                + "FOREIGN KEY (NODE_ID) REFERENCES NODE(ID))");        
    }
}
