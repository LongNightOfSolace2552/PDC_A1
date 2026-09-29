package main;

import main.config.AppConfig;
import main.core.Game;
import main.exceptions.DataLoadException;

/* main entry point for the RPG game. */
public class App {
    public static void main(String[] args) {
        AppConfig appConfig = new AppConfig();
        
        try {
            Game game = appConfig.buildGame();
            game.start();
        } catch (DataLoadException e) {
            System.err.println("Failed to load game data: " + e.getMessage());
            System.exit(1);
        }
    }
}
