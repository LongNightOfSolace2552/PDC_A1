package main.exceptions;

/*
thrown when the player enters a command the CUI does not recognize or
that is not valid in the current context (e.g. an out-of-range menu
choice).
*/
public class InvalidCommandException extends GameException {
    public InvalidCommandException(String message) {
        super(message);
    }
}
