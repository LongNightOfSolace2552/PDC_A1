package main.core;

import main.domain.player.Player;
import main.exceptions.SaveDataException;
import main.persistence.PlayerDatabaseRepository;

/*
handles save/load via File I/O. one save file per player profile,
identified by the player's own character name, so the rest of the
game never deals with file paths directly - just profile names.
*/
public class SaveManager {

    /*
    a field declaration with a reference to an object of type
    PlayerFileRepository.
    context: dependency injection and single responsibility principle
    */
    private final PlayerDatabaseRepository playerDatabaseRepository;

    public SaveManager(PlayerDatabaseRepository playerDatabaseRepository) {
        this.playerDatabaseRepository = playerDatabaseRepository;
    }

    /*
    checks if the save file exists at the given path
    (not directly just checking the repository).
    */
    public boolean hasSave(String profileName) {
        return playerDatabaseRepository.saveExists(profileName);
    }

    /*
    loads player object from the save file
    (but does not parse, read or convert text to Player)
    */
    public Player loadPlayer(String profileName) throws SaveDataException {
        return playerDatabaseRepository.load(profileName);
    }

    /*
    calls the save() method
    (just calling it, it does not write, open or serialize the Player)
    */
    public void savePlayer(Player player, String profileName) throws SaveDataException {
        playerDatabaseRepository.save(player, profileName);
    }
}
