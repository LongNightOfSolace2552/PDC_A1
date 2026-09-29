package main.domain.items;

import main.util.Randomizer;

/*
a weapon's special ability: a chance to activate during a battle sequence,
granting a temporary stat boost (e.g. +8 Strength) for that battle. When
triggered, produces its own dialogue line announcing the activation.
*/
public class ItemAbility {
    private final String name;
    private final double activationChance; /* 0.0 - 1.0 */
    private final int bonusStrength;

    public ItemAbility(String name, double activationChance, int bonusStrength) {
        if (activationChance < 0.0 || activationChance > 1.0) {
            throw new IllegalArgumentException("activationChance must be between 0.0 and 1.0");
        }
        this.name = name;
        this.activationChance = activationChance;
        this.bonusStrength = bonusStrength;
    }

    public String getName() {
        return name;
    }

    public double getActivationChance() {
        return activationChance;
    }

    public int getBonusStrength() {
        return bonusStrength;
    }

    /*
    rolls whether this ability triggers this attempt. Combat will call
    this once that system exists; not wired up anywhere yet.
    */
    public boolean rollActivation(Randomizer randomizer) {
        return randomizer.nextDouble() < activationChance;
    }

    @Override
    public String toString() {
        int percent = (int) Math.round(activationChance * 100);
        return name + " (" + percent + "% chance, +" + bonusStrength + " Strength)";
    }
}
