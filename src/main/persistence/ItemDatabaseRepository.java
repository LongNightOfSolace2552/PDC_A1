package main.persistence;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import main.domain.items.Accessory;
import main.domain.items.CombatStyle;
import main.domain.items.Item;
import main.domain.items.ItemAbility;
import main.domain.items.Weapon;
import main.exceptions.DataLoadException;

public class ItemDatabaseRepository {
    private final Connection connection;
    
    public ItemDatabaseRepository(Connection connection){
        this.connection=connection;
    }
    
    private static final String SELECT_ALL_ITEMS="SELECT ID, ITEM_TYPE, NAME, STRENGTH_BONUS, MAGIC_BONUS, AGILITY_BONUS, COMBAT_STYLE, ABILITY_NAME, ABILITY_CHANCE, ABILITY_BONUS_STRENGTH FROM ITEM";
    
    // Returns null if abilityName is null, otherwise, return an ItemAbility object with ability name, ability chance and ability bonus strength.
    private ItemAbility buildAbility(ResultSet rs) throws SQLException {
        String abilityName = rs.getString("ABILITY_NAME");
        if (abilityName == null) {
            return null;
        }
        return new ItemAbility(abilityName,rs.getDouble("ABILITY_CHANCE"),rs.getInt("ABILITY_BONUS_STRENGTH"));
    }
    
    // Returns Item object (Weapon or Accessory) using row of ResultSet rs.
    private Item buildItem(ResultSet rs) throws SQLException,DataLoadException{
        String id = rs.getString("ID");
        String type = rs.getString("ITEM_TYPE");
        String name = rs.getString("NAME");
        int strengthBonus = rs.getInt("STRENGTH_BONUS");
        int magicBonus = rs.getInt("MAGIC_BONUS");
        int agilityBonus = rs.getInt("AGILITY_BONUS");
        ItemAbility ability = buildAbility(rs);
        
        switch (type) {
            case "WEAPON":
                CombatStyle combatStyle = CombatStyle.valueOf(rs.getString("COMBAT_STYLE"));
                return new Weapon(id, name, strengthBonus, magicBonus, agilityBonus, ability, combatStyle);
            case "ACCESSORY":
                return new Accessory(id, name, strengthBonus, magicBonus, agilityBonus, ability);
            default:
                throw new DataLoadException("Unknown item type '" + type + "' for item " + id);
        }
    }
    
    public Map<String,Item> loadItemCatalog() throws DataLoadException{
        Map<String,Item> catalog=new HashMap<>();
        try(Statement statement=connection.createStatement();
            ResultSet rs=statement.executeQuery(SELECT_ALL_ITEMS)){
            while(rs.next()){
                Item item=buildItem(rs);
                catalog.put(item.getId(), item);
            }
        } catch(SQLException e){
            throw new DataLoadException("Loading items from database failed.",e);
        }
        return catalog;
    }
}
