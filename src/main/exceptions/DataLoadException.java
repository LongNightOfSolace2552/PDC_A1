package main.exceptions;

/*
thrown when a game data text file (items, enemies, nodes, node locations,
dialogue templates) fails to load or contains malformed data.
*/
public class DataLoadException extends GameException {
    public DataLoadException(String message) {
        super(message);
    }
    
    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
