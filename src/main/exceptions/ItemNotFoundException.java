package main.exceptions;

/*
thrown when a referenced item, enemy, node, or dialogue template ID does
not exist in the loaded data.
*/
public class ItemNotFoundException extends GameException {
    public ItemNotFoundException(String message) {
        super(message);
    }
}
