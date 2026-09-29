package main.cui;

import java.util.ArrayList;
import java.util.List;
import main.domain.items.Item;
import main.domain.items.Weapon;
import main.domain.player.Player;
import main.domain.player.Stats;
import main.domain.world.Node;

/*
prints core screens to the console: player name, current node location, 
dungeon difficulty, and the main menu.
*/
public class ConsoleRenderer {
    public void printHeader(Player player, Node currentNode) {
        System.out.println("=======================================");
        System.out.println("Player: " + player.getName());
        System.out.println("Current Node: " + currentNode.getName());
        System.out.println("Dungeon Difficulty: " + currentNode.getDifficulty());
        System.out.println("=======================================");
    }

    public void printStats(Player player) {
        Stats base = player.getStats();
        Item weapon = player.getEquippedWeapon();
        Item accessory = player.getEquippedAccessory();
        
        String weaponName = (weapon != null) ? weapon.getName() : null;
        String accessoryName = (accessory != null) ? accessory.getName() : null;
        
        int weaponStrength = (weapon != null) ? weapon.getStrengthBonus() : 0;
        int weaponMagic = (weapon != null) ? weapon.getMagicBonus() : 0;
        int weaponAgility = (weapon != null) ? weapon.getAgilityBonus() : 0;
        
        int accessoryStrength = (accessory != null) ? accessory.getStrengthBonus() : 0;
        int accessoryMagic = (accessory != null) ? accessory.getMagicBonus() : 0;
        int accessoryAgility = (accessory != null) ? accessory.getAgilityBonus() : 0;

        System.out.println("--- Stats ---");
        System.out.println(formatStatLine("Strength", base.getStrength(),
                weaponStrength, weaponName, accessoryStrength, accessoryName));
        System.out.println(formatStatLine("Magic", base.getMagic(),
                weaponMagic, weaponName, accessoryMagic, accessoryName));
        System.out.println(formatStatLine("Agility", base.getAgility(),
                weaponAgility, weaponName, accessoryAgility, accessoryName));
        
        Stats effective = player.getEffectiveStats();
        System.out.println("Power Level: " + effective.getPowerLevel());
        System.out.println("Unallocated stat points: " + player.getUnallocatedStatPoints());
        
        System.out.println("--- Equipped Weapon ---");
        System.out.println(player.hasEquippedWeapon() ? player.getEquippedWeapon() : "(none)");
        
        System.out.println("--- Equipped Accessory ---");
        System.out.println(player.hasEquippedAccessory() ? player.getEquippedAccessory() : "(none)");

        System.out.println("--- Inventory ---");
        List<Item> inventory = player.getInventory();
        if (inventory.isEmpty()) {
            System.out.println("(empty)");
        } else {
            for (Item item : inventory) {
                System.out.println("- " + item);
            }
        }
    }

    public void printInventoryMenu() {
        System.out.println();
        System.out.println("[1]: Equip an item");
        System.out.println("[2]: Unequip weapon");
        System.out.println("[3]: Unequip accessory");
        System.out.println("[4]: Allocate stat points");
        System.out.println("[5]: Back");
        System.out.println("-> Choose an option: ");
    }

    public void printItemList(List<Item> items) {
        System.out.println("[0]: Cancel");
        for (int i = 0; i < items.size(); i++) {
            System.out.println("[" + (i + 1) + "]: " + items.get(i));
        }
        System.out.print("Choose an item: ");
    }

    public void printNodeList(List<Node> nodes, List<Boolean> locked) {
        System.out.println("[0]: Cancel");
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String suffix = locked.get(i) ? " (LOCKED)" : "";
            System.out.println("[" + (i + 1) + "]: " + node.getName() + " (Difficulty " + node.getDifficulty() + ")" + suffix);
        }
        System.out.print("Choose a destination: ");
    }

    public void printNodeInfo(Node node, boolean bossDefeated) {
        System.out.println("--- " + node.getName() + " ---");
        System.out.println(node.getLocation().getDescription());
        System.out.println("Difficulty: " + node.getDifficulty());
        if (node.hasBoss()) {
            System.out.println(bossDefeated
                    ? "Boss status: Defeated"
                    : "Boss status: Undefeated - blocks the path onward");
        }
    }

    private String formatStatLine(String label, int baseValue, int weaponBonus, 
                                String weaponName, int accessoryBonus, 
                                String accessoryName) {
        int total = baseValue + weaponBonus + accessoryBonus;
        
        List<String> contributions = new ArrayList<>();
        
        if(weaponBonus > 0 && weaponName != null) {
            contributions.add("+" + weaponBonus + " from " + weaponName);
        } if(accessoryBonus > 0 && accessoryName != null) {
            contributions.add("+" + accessoryBonus + " from " + accessoryName);
        }
        
        String line = label + ": " + total;
        
        if (!contributions.isEmpty()) {
            line += " (" + String.join(", ", contributions) + ")";
        }
        return line;
    }
    
    public void printMessage(String message) {
        System.out.println(message);
    }
    
    public void printError(String message) {
        System.out.println("[Error] " + message);
    }
}
