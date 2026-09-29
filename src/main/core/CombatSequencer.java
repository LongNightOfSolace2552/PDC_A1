package main.core;

import java.util.List;
import main.cui.CombatRenderer;

/* 
Runs a resolved battle sequence on a background thread, feeding each 
BattleAction's dialogue to the CombatRenderer one at a time with a short delay 
between lines, so dialogue displays progressively rather than all at once.
*/
public class CombatSequencer {
    private static final long LINE_DELAY_MILIS = 1000;
    private final CombatRenderer combatRenderer;
    
    public CombatSequencer(CombatRenderer combatRenderer) {
        this.combatRenderer = combatRenderer;
    }
    
    public void play(List<String> dialogueLines) {
        Thread playbackThread = new Thread(() -> {
            for(String line : dialogueLines) {
                combatRenderer.printLine(line);
                try {
                    Thread.sleep(LINE_DELAY_MILIS);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        playbackThread.start();
        try {
            playbackThread.join();
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
