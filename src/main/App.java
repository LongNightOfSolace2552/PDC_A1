package main;

/*
included atomic boolean, this is a specialized boolean value that allows
it to read and be updated atomically (does it singly and indivisbly step without the use of synchronization for
multi-threading
*/
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import main.config.AppConfig;
import main.core.Game;
import main.exceptions.DataLoadException;


/* main entry point for the RPG game. */
public class App {
    private static final AtomicBoolean errorDialogOpen = new AtomicBoolean(false);

    public static void main(String[] args) {
        installErrorHandler();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            /*keep Swing's default look and feel if the system one 
            isn't available. 
            */
        }

        try {
            Game game = new AppConfig().buildGame();
            game.start();
        } catch (DataLoadException e) {
            JOptionPane.showMessageDialog(null, "Failed to load game data: " + e.getMessage(), "Startup failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /*
    last line of defence: an exception no screen handled itself is shown
    in a dialog instead of silently vanishing on the Swing thread. only
    one dialog is open at a time, so a fault that repeats (e.g. while
    painting) cannot bury the player in pop-ups.
    */
    private static void installErrorHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            if (!errorDialogOpen.compareAndSet(false, true)) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    JOptionPane.showMessageDialog(null, "Something went wrong: " + error,
                            "Unexpected error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    errorDialogOpen.set(false);
                }
            });
        });
    }
}
