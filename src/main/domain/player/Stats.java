package main.domain.player;

/* holds a set of Strength, Magic, and Agility values, shared by players and enemies. */
public class Stats {

    private int strength;
    private int magic;
    private int agility;

    public Stats(int strength, int magic, int agility) {
        this.strength = requireNonNegative(strength, "Strength");
        this.magic = requireNonNegative(magic, "Magic");
        this.agility = requireNonNegative(agility, "Agility");
    }

    public int getStrength() {
        return strength;
    }
    
    public void setStrength(int strength) {
        this.strength = requireNonNegative(strength, "Strength");
    }
    
    public int getMagic() {
        return magic;
    }
    
    public void setMagic(int magic) {
        this.magic = requireNonNegative(magic, "Magic");
    }
    
    public int getAgility() {
        return agility;
    }
    
    public void setAgility(int agility) {
        this.agility = requireNonNegative(agility, "Agility");
    }
    
    public int getPowerLevel() {
        return strength + magic + agility;
    }
    
    private static int requireNonNegative(int value, String fieldName) {
        if(value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
        }
        return value;
    }
    
    /*
    A toString method to protect from any typo, @Override annotation
    for safety.
    */
    @Override
    public String toString() {
        return "Strength: " + strength + ", Magic: " + magic + ", Agility: " + agility;
    }
}
