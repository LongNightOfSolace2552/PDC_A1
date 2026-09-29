package main.domain.items;

/* weapon item; a concrete Item that can carry a WeaponAbility. */
public class Weapon extends Item {
    private final CombatStyle combatStyle;

    public Weapon(String id, String name, int strengthBonus, int magicBonus, int agilityBonus, 
            ItemAbility ability, CombatStyle combatStyle) {
        super(id, name, strengthBonus, magicBonus, agilityBonus, ability);
        this.combatStyle = combatStyle;
    }

    public CombatStyle getCombatStyle() {
        return combatStyle;
    }

    @Override
    public String getItemType() {
        return "Weapon";
    }
}
