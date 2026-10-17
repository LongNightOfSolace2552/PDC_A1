package main.services;

import java.util.Objects;

public class ActionResult {
    private final boolean pass;
    private final String message;
    
    private ActionResult(boolean pass, String message) {
        this.pass = pass;
        this.message = Objects.requireNonNull(message, "message");
    }
    
    public static ActionResult pass(String message) {
        
    }
}
