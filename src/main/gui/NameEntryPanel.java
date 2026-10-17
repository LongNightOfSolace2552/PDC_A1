package main.gui;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import main.core.SessionStart;

/*
the first screen (the GUI's name prompt): asks for a player name. Game
decides what the name means - an existing save loads, otherwise a new
character is created - and this screen only shows the outcome.
*/
public class NameEntryPanel extends JPanel {
    private final GameFrame frame;
    private final JTextField nameField = new JTextField(18);
    
    public NameEntryPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());

        JButton beginButton = new JButton("Begin Adventure");
        nameField.addActionListener(event -> handleSubmit());
        beginButton.addActionListener(event -> handleSubmit());

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.add(new JLabel("RPG Game", JLabel.CENTER));
        form.add(new JLabel("Enter your character's name to begin.", JLabel.CENTER));
        form.add(nameField);
        form.add(beginButton);
        add(form);
    }

    private void handleSubmit() {
        SessionStart start = frame.getGame().initializePlayer(nameField.getText());
        if (start.isFail()) {
            frame.showError(start.getMessage());
            return;
        }

        if (start.hasMessage()) {
            if (start.isReturning()) {
                frame.showMessage(start.getMessage());
            } else {
                frame.showError(start.getMessage());
            }
        }
        frame.startGame(start.getPlayer());
    }
}
