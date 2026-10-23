package main.services;

import java.util.Objects;

/*The result in which the game outcome respond for player action.
PASS if something did happen and FAIL if something did not 
(like a rule is refused). 

This file is meant to test the functionality of the CUI conversion to GUI.
It does not go into the "test/core" because this file is still a mandatory 
as a safe guard, it is required for GameController's action returns. It safe guards
in the sense that when case is refused it returns FAIL with a message of an
exception instead of crashing the program.
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
