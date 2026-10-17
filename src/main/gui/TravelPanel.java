package main.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import main.domain.player.Player;
import main.domain.world.Node;
import main.services.ActionResult;

/*
the Travel screen: every node, marked LOCKED or "you are here", with a
Travel button only on the nodes the player can actually go to.
*/
public class TravelPanel extends JPanel {
    private final GameFrame frame;
    private final JPanel listPanel = new JPanel();

    public TravelPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JLabel("Travel"), BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(listPanel), BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.addActionListener(event -> frame.showMainMenu());
        JPanel bottom = new JPanel();
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);
    }

    public void refresh() {
        listPanel.removeAll();
        Player player = frame.getPlayer();

        for (Node node : frame.getController().listNodes()) {
            boolean locked = frame.getController().isLocked(node, player);
            boolean here = node.getId().equals(player.getCurrentNodeId());
            listPanel.add(buildRow(node, locked, here));
            listPanel.add(Box.createVerticalStrut(8));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildRow(Node node, boolean locked, boolean here) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String status = here ? "  (you are here)" : locked ? "  (LOCKED)" : "";
        row.add(new JLabel(node.getName() + " - Difficulty " + node.getDifficulty() + status), BorderLayout.CENTER);

        /*only a node the player can travel to gets a button.*/
        if (!locked && !here) {
            JButton travelButton = new JButton("Travel");
            travelButton.addActionListener(event -> handleTravel(node));
            row.add(travelButton, BorderLayout.EAST);
        }
        return row;
    }

    /*GameController makes the call (locked / already here / unknown).
    This just shows its ruling.*/
    private void handleTravel(Node destination) {
        ActionResult result = frame.getController().handleTravel(frame.getPlayer(), destination.getId());
        if (result.isFail()) {
            frame.showError(result.getMessage());
            refresh();
            return;
        }
        frame.showMessage(result.getMessage());
        frame.showMainMenu();
    }
}
