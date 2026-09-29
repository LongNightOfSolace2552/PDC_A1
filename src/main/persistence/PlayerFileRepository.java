package main.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.exceptions.DataLoadException;
import main.exceptions.SaveDataException;

/* loads and saves player data. */
public class PlayerFileRepository {
    /* create constants used internally by this class */
    private static final String KEY_NAME = "name";
    private static final String KEY_STRENGTH = "strength";
    private static final String KEY_MAGIC = "magic";
    private static final String KEY_AGILITY = "agility";
    private static final String KEY_NODE = "currentNodeId";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_EQUIPPED_WEAPON = "equippedWeapon";
    private static final String KEY_EQUIPPED_ACCESSORY = "equippedAccessory";
    private static final String KEY_DEFEATED_BOSSES = "defeatedBosses";
    private static final String KEY_UNALLOCATED_POINTS = "unallocatedStatPoints";
    private static final String LIST_SEPARATOR = ",";
    
    private final FileReaderUtil fileReaderUtil;
    private final FileWriterUtil fileWriterUtil;
    private final Map<String, Item> itemCatalog;
    
    public PlayerFileRepository(FileReaderUtil fileReaderUtil, FileWriterUtil fileWriterUtil, Map<String, Item> itemCatalog) {
        this.fileReaderUtil = fileReaderUtil;
        this.fileWriterUtil = fileWriterUtil;
        this.itemCatalog = itemCatalog;
    }
    
    public boolean saveExists(String path) {
        return fileReaderUtil.exists(path);
    }
    
    public Player load(String path) throws SaveDataException {
        if (!saveExists(path)) {
            throw new SaveDataException("No save file found at " + path);
        }
        
        List<String> lines;
        try {
            lines = fileReaderUtil.readLines(path);
        } catch(DataLoadException e) {
            throw new SaveDataException("Could not read save file: " + path, e);
        }
        
        Map<String, String> values = parseKeyValueLines(lines, path);
        
        Player player;
        try {
            String name = requireValue(values, KEY_NAME, path);
            int strength = Integer.parseInt(requireValue(values, KEY_STRENGTH, path));
            int magic = Integer.parseInt(requireValue(values, KEY_MAGIC, path));
            int agility = Integer.parseInt(requireValue(values, KEY_AGILITY, path));
            String currentNodeId = requireValue(values, KEY_NODE, path);

            Stats stats = new Stats(strength, magic, agility);
            player = new Player(name, stats, currentNodeId);
        } catch (NumberFormatException e) {
            throw new SaveDataException("Save file has a non-numeric stat value: " + path, e);
        } catch (IllegalArgumentException e) {
            throw new SaveDataException("Save file has an invalid stat value: " + path, e);
        }
        
        restoreInventory(player, values.getOrDefault(KEY_INVENTORY, ""), path);
        restoreEquippedItem(player, values.getOrDefault(KEY_EQUIPPED_WEAPON, ""), path);
        restoreEquippedItem(player, values.getOrDefault(KEY_EQUIPPED_ACCESSORY, ""), path);
        restoreDefeatedBosses(player, values.getOrDefault(KEY_DEFEATED_BOSSES, ""));
        restoreUnallocatedStatPoints(player, values.getOrDefault(KEY_UNALLOCATED_POINTS, ""));
        
        return player;
    }
    
    public void save(Player player, String path) throws SaveDataException {
        Stats stats = player.getStats();
        List<String> lines = new ArrayList<>();
        lines.add(KEY_NAME + "=" + player.getName());
        lines.add(KEY_STRENGTH + "=" + stats.getStrength());
        lines.add(KEY_MAGIC + "=" + stats.getMagic());
        lines.add(KEY_AGILITY + "=" + stats.getAgility());
        lines.add(KEY_NODE + "=" + player.getCurrentNodeId());
        lines.add(KEY_INVENTORY + "=" + joinItemIds(player.getInventory()));
        lines.add(KEY_EQUIPPED_WEAPON + "=" + (player.hasEquippedWeapon() ? player.getEquippedWeapon().getId() : ""));
        lines.add(KEY_EQUIPPED_ACCESSORY + "=" + (player.hasEquippedAccessory() ? player.getEquippedAccessory().getId() : ""));
        lines.add(KEY_DEFEATED_BOSSES + "=" + String.join(LIST_SEPARATOR, player.getDefeatedBossNodeIds()));
        lines.add(KEY_UNALLOCATED_POINTS + "=" + player.getUnallocatedStatPoints());

        fileWriterUtil.writeLines(path, lines);
    }
    
    /* joins string together using ',' */
    private String joinItemIds(List<Item> items) {
        List<String> ids = new ArrayList<>();
        for (Item item : items) {
            ids.add(item.getId());
        }
        return String.join(LIST_SEPARATOR, ids);
    }
    
    private void restoreInventory(Player player, String field, String path) throws SaveDataException {
        for (String itemId : splitList(field)) {
            player.addItem(resolveItem(itemId, path));
        }
    }

    private void restoreEquippedItem(Player player, String field, String path) throws SaveDataException {
        if (field.isEmpty()) {
            return;
        }
        Item item = resolveItem(field, path);
        /*equip() removes the item from the inventory and moves it into the
        equip slot, so give it to the inventory first, then equip it.*/
        player.addItem(item);
        player.equip(item);
    }

    private void restoreDefeatedBosses(Player player, String field) {
        for (String nodeId : splitList(field)) {
            player.markBossDefeated(nodeId);
        }
    }
    
    private void restoreUnallocatedStatPoints(Player player, String field) {
        if (field.isEmpty()) {
            return;
        }
        try {
            int points = Integer.parseInt(field);
            player.addUnallocatedStatPoints(points);
        } catch (NumberFormatException e) {
            /*malformed value, leave unallocated points at 0 rather than
            failing the whole load over a non-critical field.*/
        }
    }
    
    private Item resolveItem(String itemId, String path) throws SaveDataException {
        Item item = itemCatalog.get(itemId);
        if (item == null) {
            throw new SaveDataException("Save file " + path + " references an unknown item: " + itemId);
        }
        return item;
    }

    private List<String> splitList(String field) {
        List<String> result = new ArrayList<>();
        if (field == null || field.isEmpty()) {
            return result;
        }
        for (String part : field.split(LIST_SEPARATOR)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
    
    /* parseKeyValueLines method */
    private Map<String, String> parseKeyValueLines(List<String> lines, String path) throws SaveDataException {
        Map<String, String> values = new HashMap<>();
        
        for(String line : lines) {
            String trimmed = line.trim();
            
            if(trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            
            int separatorIndex = trimmed.indexOf("=");
            if (separatorIndex < 0) {
                throw new SaveDataException("Malformed line in save file " + path + ": " + line);
            }
            
            String key = trimmed.substring(0, separatorIndex).trim();
            String value = trimmed.substring(separatorIndex + 1).trim();
            values.put(key, value);
        }
        
        return values;
    }
    
    /* requireValue method */
    private String requireValue(Map<String, String> values, String key, String path) throws SaveDataException {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw new SaveDataException("Save file " + path + " is missing required field: " + key);
        }
        return value;
    }
}
