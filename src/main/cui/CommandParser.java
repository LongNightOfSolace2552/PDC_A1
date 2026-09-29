package main.cui;

import java.util.Scanner;
import main.exceptions.InvalidCommandException;

/* parses player input, including menu selections and the dungeon exit command ('x'). */
public class CommandParser {
    private static final int MIN_MENU_CHOICE = 1;
    private static final int MAX_MENU_CHOICE = 5;

    private final Scanner scanner = new Scanner(System.in);

    /*
    reads a valid main-menu choice. Never throws - a mistyped
    character just re-prompts for the main menu itself, instead of
    bubbling up and being mistaken for a different kind of failure.
    */
    public int readMenuChoice() {
        return readValidChoice(MIN_MENU_CHOICE, MAX_MENU_CHOICE);
    }

    /*
    reads a single integer choice, validated to be within [min, max].
    Used for the main menu and for sub-menus (node selection, item
    selection) that have their own valid ranges.
    */
    public int readChoiceInRange(int min, int max) throws InvalidCommandException {
        String input = scanner.nextLine().trim();

        int choice;
        try {
            choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("'" + input + "' is not a valid option.");
        }

        if (choice < min || choice > max) {
            throw new InvalidCommandException("Choose a number between " + min + " and " + max + ".");
        }

        return choice;
    }

    /*
    reads a choice in [min, max], retrying and re-prompting on invalid
    input instead of throwing - so a single mistyped character in any
    menu (dungeon, inventory, travel, and so on) can never unwind out
    of that menu and lose whatever progress was local to it.
    */
    public int readValidChoice(int min, int max) {
        while (true) {
            try {
                return readChoiceInRange(min, max);
            } catch (InvalidCommandException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}
