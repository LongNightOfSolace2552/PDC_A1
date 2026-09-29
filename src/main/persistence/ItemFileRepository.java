package main.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.items.Accessory;
import main.domain.items.CombatStyle;
import main.domain.items.Item;
import main.domain.items.ItemAbility;
import main.domain.items.Weapon;
import main.exceptions.DataLoadException;

/* loads items from text files. */
public class ItemFileRepository {
    private static final String ITEMS_PATH = "data/items.txt";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String NO_ABILITY_MARKER = "-";

    private final FileReaderUtil fileReaderUtil;

    public ItemFileRepository(FileReaderUtil fileReaderUtil) {
        this.fileReaderUtil = fileReaderUtil;
    }

    /*
    items.txt format:
    id|TYPE|name|strengthBonus|magicBonus|agilityBonus|abilityName|abilityChance|abilityBonusStrength
    TYPE is WEAPON or ACCESSORY. abilityName "-" means the item has no ability.
    */
    public Map<String, Item> loadItemCatalog() throws DataLoadException {
        Map<String, Item> catalog = new HashMap<>();
        for (String line : readDataLines()) {
            String[] fields = line.split(FIELD_SEPARATOR, 10);
            if (fields.length != 10) {
                throw new DataLoadException("Malformed line in " + ITEMS_PATH + ": " + line);
            }

            String id = fields[0].trim();
            String type = fields[1].trim().toUpperCase();
            String name = fields[2].trim();
            int strengthBonus = parseInt(fields[3], id);
            int magicBonus = parseInt(fields[4], id);
            int agilityBonus = parseInt(fields[5], id);
            String combatStyleField = fields[6].trim();
            ItemAbility ability = parseAbility(fields[7], fields[8], fields[9], id);

            Item item;
            switch (type) {
                case "WEAPON":
                    CombatStyle combatStyle = parseCombatStyle(combatStyleField, id);
                    item = new Weapon(id, name, strengthBonus, magicBonus, agilityBonus, ability, combatStyle);
                    break;
                case "ACCESSORY":
                    item = new Accessory(id, name, strengthBonus, magicBonus, agilityBonus, ability);
                    break;
                default:
                    throw new DataLoadException("Unknown item type '" + type + "' for item " + id);
            }

            catalog.put(id, item);
        }
        return catalog;
    }
    
    private CombatStyle parseCombatStyle(String value, String itemId) throws DataLoadException {
        try {
            return CombatStyle.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("Weapon " + itemId + " has an invalid combat style: " + value);
        }
    }
    
    private ItemAbility parseAbility(String nameField, String chanceField, String bonusField, String itemId)
            throws DataLoadException {
        String abilityName = nameField.trim();
        if (abilityName.equals(NO_ABILITY_MARKER)) {
            return null;
        }
        double chance;
        int bonusStrength;
        try {
            chance = Double.parseDouble(chanceField.trim());
            bonusStrength = Integer.parseInt(bonusField.trim());
        } catch (NumberFormatException e) {
            throw new DataLoadException("Item " + itemId + " has an invalid ability chance/bonus", e);
        }
        try {
            return new ItemAbility(abilityName, chance, bonusStrength);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("Item " + itemId + " has an invalid ability: " + e.getMessage(), e);
        }
    }

    private int parseInt(String field, String itemId) throws DataLoadException {
        try {
            return Integer.parseInt(field.trim());
        } catch (NumberFormatException e) {
            throw new DataLoadException("Item " + itemId + " has a non-numeric stat bonus: " + field, e);
        }
    }

    private List<String> readDataLines() throws DataLoadException {
        List<String> result = new ArrayList<>();
        for (String line : fileReaderUtil.readLines(ITEMS_PATH)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }
}
