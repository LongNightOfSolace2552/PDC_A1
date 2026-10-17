package main.core;

import main.domain.player.Player;

/*
the outcome of starting a play session from a typed name and will PASS with a
Player (brand new or loaded from a save) plus an optional message to
show or FAIL with the reason (e.g. a blank name) and no Player.
(immutable)
*/
public class SessionStart {
    private final Player player;
    private final boolean returning;
    private final String message;

    private SessionStart(Player player, boolean returning, String message) {
        this.player = player;
        this.returning = returning;
        this.message = message;
    }

    public static SessionStart failed(String reason) {
        return new SessionStart(null, false, reason);
    }

    public static SessionStart newGame(Player player, String notice) {
        return new SessionStart(player, false, notice);
    }

    public static SessionStart returning(Player player) {
        return new SessionStart(player, true, "Welcome back, " + player.getName() + "!");
    }

    public boolean isPass() {
        return player != null;
    }

    public boolean isFail() {
        return player == null;
    }

    /*the Player to play as; null when isFail().*/
    public Player getPlayer() {
        return player;
    }

    /*true if an existing save was loaded rather 
    than a new character created.*/
    public boolean isReturning() {
        return returning;
    }

    /*a welcome-back line, a load/starter-item notice, 
    or the failure reason; empty when there is nothing to say.*/
    public String getMessage() {
        return message;
    }

    public boolean hasMessage() {
        return !message.isEmpty();
    }
}
