package main.gui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import main.domain.player.Player;
import main.domain.world.Node;
import main.exceptions.ItemNotFoundException;

/*
the Node screen: the current node's description, 
difficulty and boss status.
*/
public class NodePanel extends JPanel {

    private final GameFrame frame;
    private final JTextArea infoArea = new JTextArea();

    public NodePanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JLabel("Current Node"), BorderLayout.NORTH);
        
        /*setting up the info text area*/
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        add(infoArea, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> frame.showMainMenu());
        JPanel bottom = new JPanel();
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);
    }

    public void refresh() {
        Player player = frame.getPlayer();
        try {
            Node node = frame.getController().findCurrentNode(player);
            StringBuilder text = new StringBuilder();
            text.append(node.getName()).append("\n\n");
            text.append(node.getLocation().getDescription()).append("\n\n");
            text.append("Difficulty: ").append(node.getDifficulty()).append('\n');
            if (node.hasBoss()) {
                text.append(player.hasDefeatedBoss(node.getId())
                        ? "Boss status: Defeated"
                        : "Boss status: Undefeated - blocks the path onward");
            }
            infoArea.setText(text.toString());
        } catch (ItemNotFoundException e) {
            infoArea.setText(e.getMessage());
        }
    }
}
