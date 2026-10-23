package core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import main.core.CombatSequencer;
import org.junit.Test;

/*
JUnit4 tests for CombatSequencer: the lines of a fight reach the screen
in order, the first straight away, and the finish callback runs exactly
once - after the last line, after a skip, and never after a stop.
*/
public class CombatSequencerTest {

    private static final int FAST_DELAY_MILLIS = 10;

    private final List<String> shown = new CopyOnWriteArrayList<>();
    private final AtomicInteger finishCount = new AtomicInteger();
    private final CountDownLatch finishedLatch = new CountDownLatch(1);

    private void play(CombatSequencer sequencer, List<String> lines) {
        sequencer.play(lines, shown::add, () -> {
            finishCount.incrementAndGet();
            finishedLatch.countDown();
        });
    }

    @Test
    public void play_showsTheFirstLineImmediately_andTheRestLater() {
        CombatSequencer sequencer = new CombatSequencer(60_000);

        play(sequencer, List.of("one", "two", "three"));

        assertEquals(List.of("one"), shown);
        assertEquals(0, finishCount.get());
        sequencer.stop();
    }

    @Test
    public void play_showsEveryLineInOrder_thenFinishesOnce() throws InterruptedException {
        CombatSequencer sequencer = new CombatSequencer(FAST_DELAY_MILLIS);

        play(sequencer, List.of("one", "two", "three"));

        assertTrue("the fight should finish by itself", finishedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("one", "two", "three"), shown);
        assertEquals(1, finishCount.get());
    }

    @Test
    public void skip_showsTheRemainingLines_andFinishesOnce() {
        CombatSequencer sequencer = new CombatSequencer(60_000);
        play(sequencer, List.of("one", "two", "three"));

        sequencer.skip();
        sequencer.skip();

        assertEquals(List.of("one", "two", "three"), shown);
        assertEquals("a second skip must not finish it again", 1, finishCount.get());
    }

    @Test
    public void stop_cancelsThePlaybackWithoutFinishing() throws InterruptedException {
        CombatSequencer sequencer = new CombatSequencer(FAST_DELAY_MILLIS);
        play(sequencer, List.of("one", "two", "three"));

        sequencer.stop();
        Thread.sleep(100);

        assertEquals("only the line shown before stop()", List.of("one"), shown);
        assertEquals(0, finishCount.get());
    }

    @Test
    public void skip_afterStop_doesNothing() {
        CombatSequencer sequencer = new CombatSequencer(60_000);
        play(sequencer, List.of("one", "two"));

        sequencer.stop();
        sequencer.skip();

        assertEquals(List.of("one"), shown);
        assertEquals(0, finishCount.get());
    }

    @Test
    public void play_withNoLines_finishesAtOnce() {
        CombatSequencer sequencer = new CombatSequencer(60_000);

        play(sequencer, List.of());

        assertTrue(shown.isEmpty());
        assertEquals(1, finishCount.get());
    }

    @Test
    public void play_withOneLine_showsItAndFinishesAtOnce() {
        CombatSequencer sequencer = new CombatSequencer(60_000);

        play(sequencer, List.of("only"));

        assertEquals(List.of("only"), shown);
        assertEquals(1, finishCount.get());
    }
}
