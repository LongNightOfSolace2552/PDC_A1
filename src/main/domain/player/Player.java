package main.domain.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import main.domain.items.Item;
import main.domain.items.Weapon;
import main.util.Randomizer;

/* player model, including core stats (Strength, Magic, Agility), equipped items, and current node location. */
public class Player {
    
    private static final int STAT_LOSS_AMOUNT = 1;
    private static final int HIGH_POWER_STAT_LOSS_AMOUNT = 2;
    private static final int HIGH_POWER_LEVEL_THRESHOLD = 10;
    private static final double LOW_POWER_LOSS_CHANCE = 0.3;
    private static final double HIGH_POWER_LOSS_CHANCE = 0.7;
    
    private String name;
    private final Stats stats;
    private String currentNodeId;
    private final List<Item> inventory = new ArrayList<>();
    private Weapon equippedWeapon;
    private Item equippedAccessory;
    private final Set<String> defeatedBossNodeIds = new HashSet<>();
    private int unallocatedStatPoints;
    
    /* constructor */
    public Player(String name, Stats stats, String currentNodeId) {
        this.name = name;
        this.stats = stats;
        this.currentNodeId = currentNodeId;
    }
    
    /* get method */
    public String getName() {
        return name;
    }
    
    public Stats getStats() {
        return stats;
    }
    
    public String getCurrentNodeId() {
        return currentNodeId;
    }
    
    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }
    
    public Item getEquippedAccessory() {
        return equippedAccessory;
    }
    
    /* set method */
    public void setName(String name) {
        this.name = name;
    }
    
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
    
    /*
    returns a copy of players inventory, so players can't bypass
    addItem and removeItem to mutate the player's inventory directly
    */
    public List<Item> getInventory() {
        return new ArrayList<>(inventory);
    }
    
    public void addItem(Item item) {
        inventory.add(item);
    }
    
    public boolean removeItem(Item item) {
        return inventory.remove(item);
    }
    
    public boolean hasEquippedWeapon() {
        return equippedWeapon != null;
    }
    
    public boolean hasEquippedAccessory() {
        return equippedAccessory != null;
    }
    
    /*
    equips an item from the inventory. Any previously equipped item is
    returned to the inventory first, so only one item is ever equipped
    at a time regardless of type (Weapon or Accessory).
    */
    public boolean equip(Item item) {
        if (!inventory.remove(item)) {
            return false;
        }
        if (item instanceof Weapon) {
            if (equippedWeapon != null) {
                inventory.add(equippedWeapon);
            }
            equippedWeapon = (Weapon) item;
        } else {
            if (equippedAccessory != null) {
                inventory.add(equippedAccessory);
            }
            equippedAccessory = item;
        }
        return true;
    }
    
    /* returns the equipped item to the inventory and clears the slot. */
    public void unequipWeapon() {
        if (equippedWeapon == null) {
            return;
        }
        inventory.add(equippedWeapon);
        equippedWeapon = null;
    }
    
    public void unequipAccessory() {
        if (equippedAccessory == null) {
            return;
        }
        inventory.add(equippedAccessory);
        equippedAccessory = null;
    }

    /*
    base stats plus the equipped item's bonuses, if any. Returns a new
    Stats instance - never mutates the player's base stats.
    */
    public Stats getEffectiveStats() {
        int strength = stats.getStrength();
        int magic = stats.getMagic();
        int agility = stats.getAgility();
        
        if (equippedWeapon != null) {
            strength += equippedWeapon.getStrengthBonus();
            magic += equippedWeapon.getMagicBonus();
            agility += equippedWeapon.getAgilityBonus();
        }
        if (equippedAccessory != null) {
            strength += equippedAccessory.getStrengthBonus();
            magic += equippedAccessory.getMagicBonus();
            agility += equippedAccessory.getAgilityBonus();
        }
        return new Stats(strength, magic, agility);
    }

    public void markBossDefeated(String nodeId) {
        defeatedBossNodeIds.add(nodeId);
    }

    public boolean hasDefeatedBoss(String nodeId) {
        return defeatedBossNodeIds.contains(nodeId);
    }

    /* returns a copy so callers can't mutate defeat progress directly. */
    public Set<String> getDefeatedBossNodeIds() {
        return new HashSet<>(defeatedBossNodeIds);
    }
    
    public int getUnallocatedStatPoints() {
        return unallocatedStatPoints;
    }
    
    public void addUnallocatedStatPoints(int amount) {
        unallocatedStatPoints += amount;
    }
    
    /*this part of the code is for when the player spends their points and
    adds to one of the stats.*/
    public boolean allocateStatPoint(StatType type) {
        if (unallocatedStatPoints <= 0) {
            return false;
        }
        
        switch(type) {
            case STRENGTH:
                stats.setStrength(stats.getStrength() + 1);
                break;
            case MAGIC:
                stats.setMagic(stats.getMagic() + 1);
                break;
            case AGILITY:
                stats.setAgility(stats.getAgility() + 1);
                break;
            default:
                return false;
        }
        
        unallocatedStatPoints--;
        return true;
    }
    
    /*compare conditions, high power level player has more to lose, so the
    loss consequence for losing a fight scales up once effective power level 
    passes this threshold.*/
    public boolean isHighPowerLevel() {
        return getEffectiveStats().getPowerLevel() > HIGH_POWER_LEVEL_THRESHOLD;
    }
    
    /*
    the amount the player would lose when a fight is lost. Used when narrating
    the battle first and then apply the changes.
    */
    public int getPendingStatLossAmount() {
        return isHighPowerLevel() ? HIGH_POWER_STAT_LOSS_AMOUNT : STAT_LOSS_AMOUNT;
    }
    
    /*
    the chance that losing a fight actually costs a stat point at all.
    low power level players rarely lose anything - the consequence only
    becomes a real risk once effective power level passes the high-power
    threshold, matching how the loss amount itself also scales up there.
    */
    public double getPendingLossChance() {
        return isHighPowerLevel() ? HIGH_POWER_LOSS_CHANCE : LOW_POWER_LOSS_CHANCE;
    }
    
    /*
    losing a fight does not always cost a stat point - first rolls
    against getPendingLossChance() (low at low power level, higher once
    the high-power threshold is passed). if that roll fails, nothing is
    lost at all. otherwise, a random stat is chosen, and more is lost
    per hit once the player surpasses the same power level threshold.
    chosen stat cannot be 0, if all stats are 0 it just skips.
    */
    public String applyRandomStatLoss(Randomizer randomizer) {
        if (randomizer.nextDouble() >= getPendingLossChance()) {
            return null;
        }

        int lossAmount = getPendingStatLossAmount();
        int pick = randomizer.nextInt(3);
        
        switch(pick) {
            case 0:
                if(stats.getStrength() > 0) {
                    int actualLoss = Math.min(lossAmount, stats.getStrength());
                    stats.setStrength(stats.getStrength() - actualLoss);
                    return actualLoss + " Strength";
                }
                return null;
            case 1:
                if (stats.getMagic() > 0) {
                    int actualLoss = Math.min(lossAmount, stats.getMagic());
                    stats.setMagic(stats.getMagic() - actualLoss);
                    return actualLoss + " Magic";
                }
                return null;
            default:
                if (stats.getAgility() > 0) {
                    int actualLoss = Math.min(lossAmount, stats.getAgility());
                    stats.setAgility(stats.getAgility() - actualLoss);
                    return actualLoss + " Agility";
                }
                return null;
        }
    }
}
