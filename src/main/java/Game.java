import java.awt.Point;
import java.util.*;

public class Game {
    private static final int UNIT_SIZE = 25;
    private static final int APPLES_PER_LEVEL = 5;
    private static final int BASE_DELAY_MS = 120;
    private static final int MIN_DELAY_MS = 40;

    private final int logicalWidth;
    private final int logicalHeight;
    private final int gridW, gridH;

    private final int[] snakeX;
    private final int[] snakeY;
    private int snakeLength;
    private char direction = 'R';
    private char nextDirection = 'R';

    private int appleX, appleY;
    private int score = 0;
    private int level = 1;
    private int delayMs = BASE_DELAY_MS;
    private double moveTimer = 0;

    private final List<Point> obstacles = new ArrayList<>();
    private final Random random = new Random();

    private State state = State.START;
    private boolean pendingAction = false;
    private boolean exitRequested = false;

    public enum State { START, RUNNING, GAME_OVER }

    public Game(int logicalWidth, int logicalHeight) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.gridW = logicalWidth / UNIT_SIZE;
        this.gridH = logicalHeight / UNIT_SIZE;
        int maxSegments = gridW * gridH;
        snakeX = new int[maxSegments];
        snakeY = new int[maxSegments];
    }

    public void setDirection(char dir) {
        if ((dir == 'L' && direction != 'R') ||
            (dir == 'R' && direction != 'L') ||
            (dir == 'U' && direction != 'D') ||
            (dir == 'D' && direction != 'U')) {
            nextDirection = dir;
        }
    }

    public void action() {
        pendingAction = true;
    }

    public void exit() {
        exitRequested = true;
    }

    public void update(double dt) {
        if (exitRequested) System.exit(0);

        switch (state) {
            case START:
                if (pendingAction) startGame();
                break;
            case RUNNING:
                moveTimer += dt * 1000;
                if (moveTimer >= delayMs) {
                    moveTimer -= delayMs;
                    step();
                }
                break;
            case GAME_OVER:
                if (pendingAction) startGame();
                break;
        }
        pendingAction = false;
        direction = nextDirection;
    }

    private void startGame() {
        state = State.RUNNING;
        snakeLength = 6;
        score = 0;
        level = 1;
        delayMs = BASE_DELAY_MS;
        direction = 'R';
        nextDirection = 'R';
        moveTimer = 0;
        obstacles.clear();

        int startX = gridW / 4;
        int startY = gridH / 2;
        for (int i = 0; i < snakeLength; i++) {
            snakeX[i] = (startX - i) * UNIT_SIZE;
            snakeY[i] = startY * UNIT_SIZE;
        }
        spawnApple();
    }

    private void step() {
        // Move body
        for (int i = snakeLength; i > 0; i--) {
            snakeX[i] = snakeX[i-1];
            snakeY[i] = snakeY[i-1];
        }

        // Move head
        switch (direction) {
            case 'L': snakeX[0] -= UNIT_SIZE; break;
            case 'R': snakeX[0] += UNIT_SIZE; break;
            case 'U': snakeY[0] -= UNIT_SIZE; break;
            case 'D': snakeY[0] += UNIT_SIZE; break;
        }

        checkApple();
        checkCollisions();
    }

    private void checkApple() {
        if (snakeX[0] == appleX && snakeY[0] == appleY) {
            snakeLength++;
            score++;
            spawnApple();
            checkLevelUp();
        }
    }

    private void spawnApple() {
        boolean valid;
        do {
            valid = true;
            appleX = random.nextInt(gridW) * UNIT_SIZE;
            appleY = random.nextInt(gridH) * UNIT_SIZE;
            for (int i = 0; i < snakeLength; i++) {
                if (snakeX[i] == appleX && snakeY[i] == appleY) { valid = false; break; }
            }
            for (Point o : obstacles) {
                if (o.x == appleX && o.y == appleY) { valid = false; break; }
            }
        } while (!valid);
    }

    private void checkLevelUp() {
        int newLevel = score / APPLES_PER_LEVEL + 1;
        if (newLevel > level) {
            level = newLevel;
            delayMs = Math.max(MIN_DELAY_MS, BASE_DELAY_MS - (level - 1) * 10);
            if (level >= 3) generateObstacles();
        }
    }

    private void generateObstacles() {
        obstacles.clear();
        int count = (level - 2) * 3;
        for (int i = 0; i < count; i++) {
            boolean valid;
            int ox, oy;
            do {
                valid = true;
                ox = random.nextInt(gridW) * UNIT_SIZE;
                oy = random.nextInt(gridH) * UNIT_SIZE;
                if (ox < 150 && oy < 150) { valid = false; continue; }
                if (ox == appleX && oy == appleY) { valid = false; continue; }
                for (int j = 0; j < snakeLength; j++) {
                    if (snakeX[j] == ox && snakeY[j] == oy) { valid = false; break; }
                }
                for (Point o : obstacles) {
                    if (o.x == ox && o.y == oy) { valid = false; break; }
                }
            } while (!valid);
            obstacles.add(new Point(ox, oy));
        }
    }

    private void checkCollisions() {
        // Wall
        if (snakeX[0] < 0 || snakeX[0] >= logicalWidth || snakeY[0] < 0 || snakeY[0] >= logicalHeight) {
            state = State.GAME_OVER; return;
        }
        // Self
        for (int i = snakeLength; i > 0; i--) {
            if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                state = State.GAME_OVER; return;
            }
        }
        // Obstacles
        for (Point o : obstacles) {
            if (snakeX[0] == o.x && snakeY[0] == o.y) {
                state = State.GAME_OVER; return;
            }
        }
    }

    public void render(Renderer r, double alpha) {
        // Background
        r.drawRect(0, 0, logicalWidth, logicalHeight, 0, 0, 0, 1);

        if (state == State.START) {
            drawStartScreen(r);
        } else if (state == State.RUNNING) {
            drawGame(r);
        } else {
            drawGame(r);
            drawGameOver(r);
        }
    }

    private void drawStartScreen(Renderer r) {
        r.drawText("SNAKE", logicalWidth/2f - 80, logicalHeight/3f, 40, 1, 1, 1, 1);
        String[] lines = {
            "Pfeiltasten: Bewegen",
            "ENTER: Starten",
            "",
            "Alle 5 Aepfel = Level Up",
            "Level 3+: Hindernisse",
            "Geschwindkeit steigt pro Level",
            "",
            "Druecke ENTER..."
        };
        float y = logicalHeight / 2f;
        for (String s : lines) {
            r.drawText(s, logicalWidth/2f - s.length()*6, y, 20, 1, 1, 1, 1);
            y += 28;
        }
    }

    private void drawGame(Renderer r) {
        // Obstacles
        for (Point o : obstacles) {
            r.drawRect(o.x, o.y, UNIT_SIZE, UNIT_SIZE, 0.4f, 0.4f, 0.4f, 1);
        }
        // Apple
        r.drawRect(appleX, appleY, UNIT_SIZE, UNIT_SIZE, 1, 0.2f, 0.2f, 1);
        // Snake
        for (int i = 0; i < snakeLength; i++) {
            float g = i == 0 ? 1f : 0.7f;
            r.drawRect(snakeX[i], snakeY[i], UNIT_SIZE, UNIT_SIZE, 0.2f, g, 0.2f, 1);
        }
        // HUD
        r.drawText("Score: " + score, 10, 20, 20, 1, 1, 1, 1);
        r.drawText("Level: " + level, 10, 45, 20, 1, 1, 1, 1);
        r.drawText("Speed: " + (1000/delayMs) + "x", 10, 70, 16, 0.8f, 0.8f, 0.8f, 1);
    }

    private void drawGameOver(Renderer r) {
        // Overlay
        r.drawRect(0, 0, logicalWidth, logicalHeight, 0, 0, 0, 0.7f);
        r.drawText("GAME OVER", logicalWidth/2f - 100, logicalHeight/3f, 50, 1, 0.2f, 0.2f, 1);
        r.drawText("Score: " + score, logicalWidth/2f - 70, logicalHeight/2f - 10, 25, 1, 1, 1, 1);
        r.drawText("Level: " + level, logicalWidth/2f - 70, logicalHeight/2f + 25, 25, 1, 1, 1, 1);
        r.drawText("ENTER = Neues Spiel", logicalWidth/2f - 110, logicalHeight/2f + 80, 20, 1, 1, 1, 1);
    }
}