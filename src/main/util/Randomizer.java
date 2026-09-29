package main.util;

import java.util.Random;

/* random number utilities. */
public class Randomizer {
    /*
    Creates a centralized Random access so that the game
    logic does not scatter its own instances around.
    */
    private final Random random = new Random();
    
    public double nextDouble() {
        return random.nextDouble();
    }
    
    public int nextInt(int bound) {
        return random .nextInt(bound);
    }
}
