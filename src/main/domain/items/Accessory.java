package main.domain.items;

/*
abstract base for anything a player can hold or equip (weapons, rings,
amulets, etc.). Concrete item types provide their own stat-bonus behavior.
*/
public class Accessory extends Item {
    public Accessory(String id, String name, int strengthBonus, int magicBonus, int agilityBonus, ItemAbility ability) {
        super(id, name, strengthBonus, magicBonus, agilityBonus, ability);
    }
    
    @Override
    public String getItemType() {
        return "Accessory";
    }
}
