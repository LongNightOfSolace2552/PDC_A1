package main.core;

import main.domain.player.Player;
import main.exceptions.SaveDataException;
import main.persistence.PlayerFileRepository;

/*
handles save/load via File I/O. one save file per player profile,
identified by the player's own character name, so the rest of the
game never deals with file paths directly - just profile names.
*/
public class SaveManager {
    /*
    dynamic location of save file, defined as final - every time a
    player enters the game they will be prompted to enter a name. it
    acts as a search for the player's file: if not recognised, a new
    player file is created, otherwise the existing file is accessed.
    */
    private static final String SAVE_DIRECTORY = "data/player_saves/";
    private static final String SAVE_EXTENSION = ".txt";

    /*
    a field declaration with a reference to an object of type
    PlayerFileRepository.
    context: dependency injection and single responsibility principle
    */
    private final PlayerFileRepository playerFileRepository;

    public SaveManager(PlayerFileRepository playerFileRepository) {
        this.playerFileRepository = playerFileRepository;
    }

    /*
    checks if the save file exists at the given path
    (not directly just checking the repository).
    */
    public boolean hasSave(String profileName) {
        return playerFileRepository.saveExists(pathFor(profileName));
    }

    /*
    loads player object from the save file
    (but does not parse, read or convert text to Player)
    */
    public Player loadPlayer(String profileName) throws SaveDataException {
        return playerFileRepository.load(pathFor(profileName));
    }

    /*
    calls the save() method
    (just calling it, it does not write, open or serialize the Player)
    */
    public void savePlayer(Player player, String profileName) throws SaveDataException {
        playerFileRepository.save(player, pathFor(profileName));
    }

    /* the path it is going to be saved towards (the name of the player is the file) */
    private String pathFor(String profileName) {
        return SAVE_DIRECTORY + cleanUp(profileName) + SAVE_EXTENSION;
    }

    /*
    cleans up the inputted text so it is safe to use as a filename -
    for example, spaces or slashes typed in a name become underscores.
    capital letters are not affected; a-zA-Z matches both cases, so
    this only touches characters outside the allowed set.
    */
    private String cleanUp(String profileName) {
        /*
        "^" as the first character inside the brackets negates the
        set, so it matches anything that is NOT one of the characters
        stated.

        accepts: a-zA-Z means any letter, 0-9 is any digit, "_" is
        underscore and "-" is hyphen.
        */
        return profileName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
