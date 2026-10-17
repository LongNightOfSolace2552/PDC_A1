package main.services;

import java.util.Objects;

/*The result in which the game outcome respond for player action.
PASS if something did happen and FAIL if something did not 
(like a rule is refused). 

This file is meant to test the functionality of the CUI conversion to GUI.
*/
public class ActionResult {
    private final boolean pass;
    private final String message;
    
    private ActionResult(boolean pass, String message) {
        this.pass = pass;
        this.message = Objects.requireNonNull(message, "message");
    }
    
    public static ActionResult pass(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(false, message);
    }

    public boolean isPass() {
        return pass;
    }

    public boolean isFail() {
        return !pass;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return (pass ? "PASS: " : "FAIL: ") + message;
    }
}
