package main.domain.combat;


/*
Once a fight finishes it reports back to this from the front-end, so that
the msseage can be worded without reforming the rules.

built statically.
*/
public class FightOutcome {
    private final boolean has_won;
    private final boolean floorAdvanced;
    private final boolean progressReset;
    private final boolean bossDefeated;
    private final int floor;
    
    
}
