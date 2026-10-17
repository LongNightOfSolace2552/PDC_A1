package main.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.Timer;

/*
the fight picture: the player is a square and the enemy is a circle
(placeholder art, no image files). a clash moves both toward each other
and back, a dodge moves the dodger sideways away and back, and when the
fight ends the loser turns grey. the Swing Timer only runs while a move
is in progress.
*/
public class BattleStagePanel extends JPanel {
    private static final int MOVE_MILLIS = 350;
    private static final int MOVE_DISTANCE = 45;
    private static final int TICK_MILLIS = 20;
    private static final int PLAYER_SIZE = 60;
    private static final int ENEMY_SIZE = 80;
    private static final int BOSS_SIZE = 110;
    
    private enum Move { NONE, CLASH, PLAYER_DODGE, ENEMY_DODGE }
    
    private String playerName = "Player";
    private String enemyName = "Enemy";
    private boolean enemyIsBoss;
    
    private Move move = Move.NONE;
    private long moveStart;
    private boolean resultShown;
    private boolean won;

    private final Timer timer = new Timer(TICK_MILLIS, event -> tick());

    /*prepares the stage for a new fight.*/
    public void setup(String playerName, String enemyName, boolean enemyIsBoss) {
        this.playerName = playerName;
        this.enemyName = enemyName;
        this.enemyIsBoss = enemyIsBoss;
        this.move = Move.NONE;
        this.resultShown = false;
        repaint();
    }

    /*both sprites step toward each other and back. */
    public void clash() {
        startMove(Move.CLASH);
    }

    /*the player or the enemy slides sideways, away from the other, and back.*/
    public void dodge(boolean byPlayer) {
        startMove(byPlayer ? Move.PLAYER_DODGE : Move.ENEMY_DODGE);
    }

    /*the loser turns grey once the fight is over.*/
    public void showResult(boolean playerWon) {
        this.resultShown = true;
        this.won = playerWon;
        repaint();
    }

    /* stops the timer - call when this stage is no longer shown. */
    public void dispose() {
        timer.stop();
    }

    private void startMove(Move newMove) {
        move = newMove;
        moveStart = System.currentTimeMillis();
        timer.start();
    }

    private void tick() {
        if (System.currentTimeMillis() - moveStart >= MOVE_MILLIS) {
            move = Move.NONE;
            timer.stop();
        }
        repaint();
    }

    /*0 at rest, up to 1 in the middle of a move, back to 0 at the end. */
    private double moveProgress() {
        if (move == Move.NONE) {
            return 0.0;
        }
        double t = (System.currentTimeMillis() - moveStart) / (double) MOVE_MILLIS;
        return Math.sin(Math.min(1.0, t) * Math.PI);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int baseline = getHeight() * 2 / 3;
        int offset = (int) (moveProgress() * MOVE_DISTANCE);

        /* the player starts on the left, the enemy on the right. */
        int playerOffset = 0;
        int enemyOffset = 0;
        if (move == Move.CLASH) {
            playerOffset = offset;
            enemyOffset = -offset;
        } else if (move == Move.PLAYER_DODGE) {
            playerOffset = -offset;
        } else if (move == Move.ENEMY_DODGE) {
            enemyOffset = offset;
        }
        
        /*determines players position on the screen*/
        int playerX = width / 4 - PLAYER_SIZE / 2 + playerOffset;
        g.setColor(resultShown && !won ? Color.GRAY : Color.BLUE);
        g.fillRect(playerX, baseline - PLAYER_SIZE, PLAYER_SIZE, PLAYER_SIZE);
        drawLabel(g, playerName, width / 4, baseline + 20);
        
        /*determines enemies (including boss) position on the screen*/
        int enemySize = enemyIsBoss ? BOSS_SIZE : ENEMY_SIZE;
        int enemyX = width * 3 / 4 - enemySize / 2 + enemyOffset;
        g.setColor(resultShown && won ? Color.GRAY : (enemyIsBoss ? Color.RED : Color.ORANGE));
        g.fillOval(enemyX, baseline - enemySize, enemySize, enemySize);
        drawLabel(g, enemyIsBoss ? enemyName + " (Boss)" : enemyName, width * 3 / 4, baseline + 20);
    }

    private void drawLabel(Graphics g, String text, int centerX, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.BLACK);
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
    }
}
