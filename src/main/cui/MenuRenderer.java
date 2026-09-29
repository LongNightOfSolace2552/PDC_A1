package main.cui;

/* renders the main menu: [1] Travel, [2] Node, [3] Dungeon, [4] Stats/Inventory. */
public class MenuRenderer {
    public void printMainMenu() {
        System.out.println();
        System.out.println("[1]: Travel");
        System.out.println("[2]: Node");
        System.out.println("[3]: Dungeon");
        System.out.println("[4]: Stats/Inventory");
        System.out.println("[5]: Save & Quit");
        System.out.print("Choose an option: ");
    }
}
