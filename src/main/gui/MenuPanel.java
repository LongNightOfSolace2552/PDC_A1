package main.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import main.domain.player.Player;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;

/*
the main menu (the GUI's MenuRenderer): who is playing and where, plus
the five options; Travel, Node, Dungeon, Stats/Inventory, Save&Quit.
replaces the console renderer (in which the command renderer also got removed).
*/
public class MenuPanel extends JPanel {

    private final GameFrame frame;
    private final JLabel welcomeLabel = new JLabel();
    private final JLabel locationLabel = new JLabel();

    public MenuPanel(GameFrame frame){
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.add(welcomeLabel);
        header.add(locationLabel);
        add(header, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 10));
        buttons.add(menuButton("Travel", frame::showTravel));
        buttons.add(menuButton("Node", frame::showNode));
        buttons.add(menuButton("Dungeon", frame::showDungeon));
        buttons.add(menuButton("Stats / Inventory", frame::showInventory));
        buttons.add(menuButton("Save & Quit", frame::saveAndExit));

        JPanel centre = new JPanel(new GridBagLayout());
        centre.add(buttons);
        add(centre, BorderLayout.CENTER);
    }

    private JButton menuButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    public void refresh() {
        Player player = frame.getPlayer();
        welcomeLabel.setText("Welcome, " + player.getName());

        try {
            Node currentNode = frame.getController().findCurrentNode(player);
            locationLabel.setText("Current location: " + currentNode.getName()
                    + "   |   Dungeon difficulty: " + currentNode.getDifficulty());
        } catch (ItemNotFoundException e) {
            locationLabel.setText("Current node '" + player.getCurrentNodeId() + "' is unknown: " + e.getMessage());
        }
    }
}
