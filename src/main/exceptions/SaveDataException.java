package main.exceptions;

/*
thrown when the player's save file is missing, unreadable, or corrupted,
or when writing save data fails.
*/
public class SaveDataException extends GameException {
    public SaveDataException(String message) {
        super(message);
    }
    
    public SaveDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
