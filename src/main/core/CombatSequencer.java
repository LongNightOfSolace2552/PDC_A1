package main.core;

import java.util.List;
import java.util.function.Consumer;
import javax.swing.Timer;

/* 
Plays a fight's dialogue lines on the fight screen one at a time, with a
short delay between lines, so the fight reads progressively instead of
all at once. It uses a Swing Timer, which fires on the Swing thread and
does not freeze the window the way a sleeping thread would.
*/
public class CombatSequencer {
    /*millisecond, constant; always 1000 ms which is equals 1 second*/
    private static final int DEFAULT_LINE_DELAY_MILLIS = 1000;

    private final int lineDelayMillis;
    private Timer timer;
    private List<String> lines = List.of();
    private Consumer<String> onLine;
    private Runnable onFinished;
    private int nextIndex;
    private boolean finished = true;

    public CombatSequencer() {
        this(DEFAULT_LINE_DELAY_MILLIS);
    }

    public CombatSequencer(int lineDelayMillis) {
        this.lineDelayMillis = lineDelayMillis;
    }

    /*
    shows the first line straight away, then one more every delay.
    onFinished runs once, right after the last line.
    */
    public void play(List<String> dialogueLines, Consumer<String> onLine, Runnable onFinished) {
        stop();
        this.lines = dialogueLines;
        this.onLine = onLine;
        this.onFinished = onFinished;
        this.nextIndex = 0;
        this.finished = false;

        showNextLine();
        if (!finished) {
            timer = new Timer(lineDelayMillis, event -> showNextLine());
            timer.start();
        }
    }

    /*shows every remaining line at once, then finishes.*/
    public void skip() {
        while (!finished && nextIndex < lines.size()) {
            onLine.accept(lines.get(nextIndex));
            nextIndex++;
        }
        finish();
    }

    /*stops the playback without finishing it, 
    if player decides to leave the screen early.*/
    public void stop() {
        finished = true;
        if (timer != null) {
            timer.stop();
        }
    }

    private void showNextLine() {
        if (finished) {
            return;
        }
        if (nextIndex < lines.size()) {
            onLine.accept(lines.get(nextIndex));
            nextIndex++;
        }
        if (nextIndex >= lines.size()) {
            finish();
        }
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (timer != null) {
            timer.stop();
        }
        onFinished.run();
    }
}
