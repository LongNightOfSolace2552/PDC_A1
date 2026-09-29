package main.domain.combat;

import java.util.List;
import main.domain.items.Item;

public class CombatResult {
    private final boolean won;
    private final List<String> dialogueLines;
    private final Item droppedItem;
    
    public CombatResult(boolean won, List<String> dialogueLines, Item droppedItem) {
        this.won = won;
        this.dialogueLines = dialogueLines;
        this.droppedItem = droppedItem;
    }
    
    public boolean isWon() {
        return won;
    }
    
    public List<String> getDialogueLines() {
        return dialogueLines;
    }
    
    public boolean hasDroppedItem() {
        return droppedItem != null;
    }
    
    public Item getDroppedItem() {
        return droppedItem;
    }
}
