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
public class NameEntryPanel {
    private final GameFrame frame;
    private final JTextField nameField = new JTextField(18);
    
    public NameEntryPanel(GameFrame frame) {
        
    }
    
    /*2 handle function here*/
}
