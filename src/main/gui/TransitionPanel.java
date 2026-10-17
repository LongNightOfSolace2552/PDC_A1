package main.gui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/*
holds the screen being shown and swaps it for the next one with a short
slide from the right, so moving between screens is smooth instead of an
instant jump. a Swing Timer drives it; no animation library.
*/
public class TransitionPanel extends JPanel {
    /*sets the frames of transitions*/
    private static final int FRAMES = 10;
    private static final int FRAME_MILLIS = 20;

    private JComponent current;
    private JComponent incoming;
    private int frame;
    private Timer timer;

    public TransitionPanel() {
        setLayout(null);
    }

    /* shows the first screen with no slide. */
    public void showFirst(JComponent screen) {
        current = screen;
        add(screen);
        placeScreens();
    }

    /* slides the next screen in; asking for the screen already shown just redraws it. */
    public void slideTo(JComponent next) {
        finishSlide();
        if (next == current) {
            placeScreens();
            return;
        }
        incoming = next;
        add(next);
        frame = 0;
        placeScreens();
        timer = new Timer(FRAME_MILLIS, event -> nextFrame());
        timer.start();
    }

    private void nextFrame() {
        frame++;
        if (frame >= FRAMES) {
            finishSlide();
        } else {
            placeScreens();
        }
    }

    /* ends a slide in progress straight away, leaving the incoming screen as the current one. */
    private void finishSlide() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (incoming != null) {
            remove(current);
            current = incoming;
            incoming = null;
        }
        placeScreens();
    }

    private void placeScreens() {
        int width = getWidth();
        int height = getHeight();
        if (incoming == null) {
            current.setBounds(0, 0, width, height);
        } else {
            int shift = width * frame / FRAMES;
            current.setBounds(-shift, 0, width, height);
            incoming.setBounds(width - shift, 0, width, height);
        }
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        if (current != null) {
            placeScreens();
        }
    }
}
