package main.domain.combat;


/*
Once a fight finishes it reports back to this from the front-end, so that
the msseage can be worded without reforming the rules.

built statically. (immutable)
*/
public class FightOutcome {
    private final boolean has_won;
    private final boolean floorAdvanced;
    private final boolean progressReset;
    private final boolean bossDefeated;
    private final int floor;
    
    private FightOutcome(boolean has_won, boolean floorAdvanced, boolean progressReset,
                         boolean bossDefeated, int floor) {
        this.has_won = has_won;
        this.floorAdvanced = floorAdvanced;
        this.progressReset = progressReset;
        this.bossDefeated = bossDefeated;
        this.floor = floor;
    }

    /*in a dungeon fight, the player won and so 
    floorAdvanced is true when the floor is cleared.*/
    public static FightOutcome dungeonWin(boolean floorAdvanced, int floorNow) {
        return new FightOutcome(true, floorAdvanced, false, false, floorNow);
    }

    /*in a dungeon fight, the player lost and so 
    dungeon progress went back to floor 1.*/
    public static FightOutcome dungeonLoss(int floorNow) {
        return new FightOutcome(false, false, true, false, floorNow);
    }

    /*a boss fight, bossDefeated mirrors whether the player won.*/
    public static FightOutcome bossFight(boolean has_won) {
        return new FightOutcome(has_won, false, false, has_won, 0);
    }
    
    /*boolean checks*/
    public boolean isWon() {
        return has_won;
    }

    public boolean isFloorAdvanced() {
        return floorAdvanced;
    }

    public boolean isProgressReset() {
        return progressReset;
    }

    public boolean isBossDefeated() {
        return bossDefeated;
    }
    //---

    /* the dungeon floor after the fight; only meaningful for dungeon fights (0 for a boss fight). */
    public int getFloor() {
        return floor;
    }
}
