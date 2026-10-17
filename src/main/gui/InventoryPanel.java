package main.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import main.domain.items.Item;
import main.domain.player.Player;
import main.domain.player.StatType;
import main.domain.player.Stats;
import main.services.ActionResult;

/*
the Stats / Inventory screen: stat totals (base + equipment bonuses),
equipped weapon and accessory, stat point allocation and the inventory.
a button only appears when its action is possible: Equip for each item
in the inventory, Unequip for a filled slot, the allocate buttons while
there are points to spend. the rules themselves are GameController's.
*/
public class InventoryPanel extends JPanel {
    private static final int EQUIPPED_TEXT_WIDTH = 320;
    private static final int ITEM_ROW_TEXT_WIDTH = 210;
    
    private final GameFrame frame;
    
    private final JPanel statsSection = new JPanel();
    private final JPanel equippedSection = new JPanel();
    private final JPanel allocateSection = new JPanel();
    private final JPanel inventorySection = new JPanel();
    
    /*display inventory in GUI form.*/
    public InventoryPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JLabel("Stats / Inventory"), BorderLayout.NORTH);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        for (JPanel section : new JPanel[]{statsSection, equippedSection, allocateSection}) {
            section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
            section.setAlignmentX(LEFT_ALIGNMENT);
            left.add(section);
            left.add(Box.createVerticalStrut(12));
        }

        JPanel right = new JPanel(new BorderLayout());
        right.add(heading("Inventory"), BorderLayout.NORTH);
        inventorySection.setLayout(new BoxLayout(inventorySection, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(inventorySection);
        /* rows are sized to fit the column, so sideways scrolling would only ever hide the Equip button. */
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        right.add(scrollPane, BorderLayout.CENTER);

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.add(left);
        columns.add(right);
        add(columns, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> frame.showMainMenu());
        JPanel bottom = new JPanel();
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);
    }

    private JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private void refreshStats() {
        statsSection.removeAll();
        statsSection.add(heading("Stats"));

        Player player = frame.getPlayer();
        Stats base = player.getStats();
        Item weapon = player.getEquippedWeapon();
        Item accessory = player.getEquippedAccessory();

        statsSection.add(new JLabel(statLine("Strength", base.getStrength(), weapon, accessory, Item::getStrengthBonus)));
        statsSection.add(new JLabel(statLine("Magic", base.getMagic(), weapon, accessory, Item::getMagicBonus)));
        statsSection.add(new JLabel(statLine("Agility", base.getAgility(), weapon, accessory, Item::getAgilityBonus)));
        statsSection.add(new JLabel("Power Level: " + player.getEffectiveStats().getPowerLevel()));
        statsSection.add(new JLabel("Unallocated points: " + player.getUnallocatedStatPoints()));
    }

    private String statLine(String label, int baseValue, Item weapon, Item accessory, ToIntFunction<Item> bonusOf) {
        int weaponBonus = weapon != null ? bonusOf.applyAsInt(weapon) : 0;
        int accessoryBonus = accessory != null ? bonusOf.applyAsInt(accessory) : 0;

        List<String> contributions = new ArrayList<>();
        if (weaponBonus > 0) {
            contributions.add("+"+ weaponBonus + " from " + weapon.getName());
        }
        if (accessoryBonus > 0) {
            contributions.add("+" + accessoryBonus + " from "+ accessory.getName());
        }

        String text = label + ": " + (baseValue + weaponBonus + accessoryBonus);
        if (!contributions.isEmpty()) {
            text += " (" + String.join(", ", contributions) + ")";
        }
        return text;
    }

    private void refreshEquipped() {
        equippedSection.removeAll();
        equippedSection.add(heading("Equipped"));
        Player player = frame.getPlayer();

        /* an Unequip button exists only while there is something in that slot. */
        equippedSection.add(new JLabel("Weapon:"));
        if (player.hasEquippedWeapon()){
            equippedSection.add(wrapped(player.getEquippedWeapon().toString(), EQUIPPED_TEXT_WIDTH));
            equippedSection.add(unequipButton("Unequip Weapon", p -> frame.getController().handleUnequipWeapon(p)));
        } else {
            equippedSection.add(new JLabel("(none)"));
        }

        equippedSection.add(new JLabel("Accessory:"));
        if(player.hasEquippedAccessory()) {
            equippedSection.add(wrapped(player.getEquippedAccessory().toString(), EQUIPPED_TEXT_WIDTH));
            equippedSection.add(unequipButton("Unequip Accessory", p -> frame.getController().handleUnequipAccessory(p)));
        } else {
            equippedSection.add(new JLabel("(none)"));
        }
    }

    private JButton unequipButton(String text, Function<Player, ActionResult> action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> report(action.apply(frame.getPlayer())));
        return button;
    }

    private void refreshAllocate() {
        allocateSection.removeAll();
        allocateSection.add(heading("Allocate Stat Points"));

        /*the allocate buttons only appear while there are banked points 
        to spend.*/
        if(frame.getPlayer().getUnallocatedStatPoints() <= 0) {
            allocateSection.add(new JLabel("No stat points to allocate."));
            
            return;
        }

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(allocateButton("+ Strength", StatType.STRENGTH));
        row.add(allocateButton("+ Magic", StatType.MAGIC));
        row.add(allocateButton("+ Agility", StatType.AGILITY));
        allocateSection.add(row);
    }

    private JButton allocateButton(String text, StatType type) {
        JButton button = new JButton(text);
        button.addActionListener(event -> {
            ActionResult result = frame.getController().handleAllocateStatPoints(frame.getPlayer(), type);
            if (result.isFail()) {
                frame.showError(result.getMessage());
            }
            refresh();
        });
        return button;
    }

    private void refreshInventory() {
        inventorySection.removeAll();
        List<Item> inventory = frame.getPlayer().getInventory();

        if (inventory.isEmpty()) {
            inventorySection.add(new JLabel("(empty)"));
            return;
        }
        for (Item item : inventory) {
            inventorySection.add(buildItemRow(item));
            inventorySection.add(Box.createVerticalStrut(8));
        }
    }

    private JPanel buildItemRow(Item item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row.add(wrapped(item.toString(), ITEM_ROW_TEXT_WIDTH), BorderLayout.CENTER);

        JButton equipButton = new JButton("Equip");
        equipButton.addActionListener(event -> report(frame.getController().handleEquip(frame.getPlayer(), item)));
        row.add(equipButton, BorderLayout.EAST);
        return row;
    }

    /*
    a plain JLabel cuts long text off with "..." instead of wrapping it,
    which hides item details - an HTML label wraps. an inventory row also
    holds the Equip button, so its text gets the narrower width; otherwise
    the row outgrows its column and pushes Equip out of view.
    */
    private JLabel wrapped(String text, int widthPixels) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return new JLabel("<html><div style='width:" + widthPixels + "px;'>" + safe + "</div></html>");
    }

    /* shows a verdict (information on PASS, error on FAIL), then redraws so the buttons match the new state. */
    private void report(ActionResult result) {
        if (result.isPass()) {
            frame.showMessage(result.getMessage());
        } else {
            frame.showError(result.getMessage());
        }
        refresh();
    }
}
