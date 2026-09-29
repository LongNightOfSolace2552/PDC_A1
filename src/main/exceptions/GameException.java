package main.exceptions;

/*
base checked exception for domain-level game errors. Specific failure
cases are represented by subclasses so callers can catch and handle them
distinctly where useful, or catch GameException generically otherwise.
*/
public class GameException extends Exception {
    /* parameterized constructors */
    public GameException(String message) {
        super(message);
    }
    
    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
