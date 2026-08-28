import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Snake extends JFrame {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int UNIT_SIZE = 25;
    private static final int GAME_UNITS = (WIDTH * HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
    private static final int BASE_DELAY = 120;
    private static final int APPLES_PER_LEVEL = 5;
    
    private final int[] x = new int[GAME_UNITS];
    private final int[] y = new int[GAME_UNITS];
    private int bodyParts = 6;
    private int applesEaten = 0;
    private int appleX;
    private int appleY;
    private char direction = 'R';
    private boolean running = false;
    private boolean gameStarted = false;
    private Timer timer;
    private Random random;
    private GamePanel gamePanel;
    private int currentLevel = 1;
    private int currentDelay;
    private List<Point> obstacles = new ArrayList<>();

    public Snake() {
        random = new Random();
        gamePanel = new GamePanel();
        this.setTitle("Snake - Level 1");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.add(gamePanel);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    private void handleKeyPress(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (!gameStarted && key == KeyEvent.VK_ENTER) {
            startGame();
            return;
        }
        
        if (!running) return;
        
        switch (key) {
            case KeyEvent.VK_LEFT:
                if (direction != 'R') direction = 'L';
                break;
            case KeyEvent.VK_RIGHT:
                if (direction != 'L') direction = 'R';
                break;
            case KeyEvent.VK_UP:
                if (direction != 'D') direction = 'U';
                break;
            case KeyEvent.VK_DOWN:
                if (direction != 'U') direction = 'D';
                break;
        }
    }

    public void startGame() {
        gameStarted = true;
        running = true;
        bodyParts = 6;
        applesEaten = 0;
        currentLevel = 1;
        currentDelay = BASE_DELAY;
        direction = 'R';
        obstacles.clear();
        
        for (int i = 0; i < bodyParts; i++) {
            x[i] = 100 - i * UNIT_SIZE;
            y[i] = 100;
        }
        
        newApple();
        timer = new Timer(currentDelay, e -> {
            if (running) {
                move();
                checkApple();
                checkCollisions();
            }
            gamePanel.repaint();
        });
        timer.start();
    }

    public void newApple() {
        boolean validPosition;
        do {
            validPosition = true;
            appleX = random.nextInt((int)(WIDTH / UNIT_SIZE)) * UNIT_SIZE;
            appleY = random.nextInt((int)(HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
            
            // Check not on snake
            for (int i = 0; i < bodyParts; i++) {
                if (x[i] == appleX && y[i] == appleY) {
                    validPosition = false;
                    break;
                }
            }
            // Check not on obstacles
            for (Point obs : obstacles) {
                if (obs.x == appleX && obs.y == appleY) {
                    validPosition = false;
                    break;
                }
            }
        } while (!validPosition);
    }

    public void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
        
        switch (direction) {
            case 'U': y[0] -= UNIT_SIZE; break;
            case 'D': y[0] += UNIT_SIZE; break;
            case 'L': x[0] -= UNIT_SIZE; break;
            case 'R': x[0] += UNIT_SIZE; break;
        }
    }

    public void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            bodyParts++;
            applesEaten++;
            newApple();
            checkLevelUp();
        }
    }

    public void checkLevelUp() {
        int newLevel = (applesEaten / APPLES_PER_LEVEL) + 1;
        if (newLevel > currentLevel) {
            currentLevel = newLevel;
            currentDelay = Math.max(40, BASE_DELAY - (currentLevel - 1) * 10);
            timer.setDelay(currentDelay);
            this.setTitle("Snake - Level " + currentLevel);
            generateObstacles();
        }
    }

    public void generateObstacles() {
        obstacles.clear();
        if (currentLevel >= 3) {
            int obstacleCount = (currentLevel - 2) * 3;
            for (int i = 0; i < obstacleCount; i++) {
                boolean validPosition;
                int obsX, obsY;
                do {
                    validPosition = true;
                    obsX = random.nextInt((int)(WIDTH / UNIT_SIZE)) * UNIT_SIZE;
                    obsY = random.nextInt((int)(HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
                    
                    // Not on snake
                    for (int j = 0; j < bodyParts; j++) {
                        if (x[j] == obsX && y[j] == obsY) {
                            validPosition = false;
                            break;
                        }
                    }
                    // Not on apple
                    if (obsX == appleX && obsY == appleY) validPosition = false;
                    // Not on other obstacles
                    for (Point obs : obstacles) {
                        if (obs.x == obsX && obs.y == obsY) {
                            validPosition = false;
                            break;
                        }
                    }
                    // Not too close to start
                    if (obsX < 150 && obsY < 150) validPosition = false;
                } while (!validPosition);
                obstacles.add(new Point(obsX, obsY));
            }
        }
    }

    public void checkCollisions() {
        // Check body collision
        for (int i = bodyParts; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) {
                running = false;
                break;
            }
        }
        
        // Check wall collision
        if (x[0] < 0 || x[0] >= WIDTH || y[0] < 0 || y[0] >= HEIGHT) {
            running = false;
        }
        
        // Check obstacle collision
        for (Point obs : obstacles) {
            if (x[0] == obs.x && y[0] == obs.y) {
                running = false;
                break;
            }
        }
        
        if (!running) {
            timer.stop();
        }
    }

    class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            draw(g);
        }

        public void draw(Graphics g) {
            if (gameStarted) {
                if (running) {
                    // Draw obstacles
                    g.setColor(new Color(100, 100, 100));
                    for (Point obs : obstacles) {
                        g.fillRect(obs.x, obs.y, UNIT_SIZE, UNIT_SIZE);
                        g.setColor(new Color(80, 80, 80));
                        g.drawRect(obs.x, obs.y, UNIT_SIZE, UNIT_SIZE);
                        g.setColor(new Color(100, 100, 100));
                    }
                    
                    // Draw apple
                    g.setColor(Color.RED);
                    g.fillOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);
                    g.setColor(new Color(200, 50, 50));
                    g.drawOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);
                    
                    // Draw snake
                    for (int i = 0; i < bodyParts; i++) {
                        if (i == 0) {
                            g.setColor(Color.GREEN);
                        } else {
                            float ratio = (float)i / bodyParts;
                            g.setColor(new Color(45, (int)(180 + 50 * ratio), 45));
                        }
                        g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                        g.setColor(new Color(30, 140, 30));
                        g.drawRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                    }
                    
                    // Draw score & level
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.drawString("Score: " + applesEaten, 10, 25);
                    g.drawString("Level: " + currentLevel, 10, 50);
                    g.setFont(new Font("Arial", Font.PLAIN, 14));
                    g.drawString("Speed: " + (1000 / currentDelay) + "x", 10, 70);
                } else {
                    // Game Over
                    gameOver(g);
                }
            } else {
                // Start screen
                startScreen(g);
            }
        }

        public void startScreen(Graphics g) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String title = "SNAKE";
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString(title, (WIDTH - metrics.stringWidth(title)) / 2, HEIGHT / 3);
            
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String[] instructions = {
                "Steuerung:",
                "Pfeiltasten (← ↑ → ↓) = Bewegen",
                "Enter (OK) = Spiel starten / Neues Spiel",
                "",
                "Level-System:",
                "Alle 5 Äpfel = neues Level",
                "Level 1-2: Nur Wände",
                "Level 3+: Hindernisse (grau) erscheinen",
                "Geschwindigkeit steigt pro Level!",
                "",
                "Drücke ENTER zum Starten..."
            };
            
            int yPos = HEIGHT / 2;
            for (String line : instructions) {
                metrics = getFontMetrics(g.getFont());
                g.drawString(line, (WIDTH - metrics.stringWidth(line)) / 2, yPos);
                yPos += 28;
            }
        }

        public void gameOver(Graphics g) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            String gameOver = "GAME OVER";
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString(gameOver, (WIDTH - metrics.stringWidth(gameOver)) / 2, HEIGHT / 3);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 25));
            String score = "Score: " + applesEaten;
            metrics = getFontMetrics(g.getFont());
            g.drawString(score, (WIDTH - metrics.stringWidth(score)) / 2, HEIGHT / 2 - 20);
            
            String level = "Level erreicht: " + currentLevel;
            metrics = getFontMetrics(g.getFont());
            g.drawString(level, (WIDTH - metrics.stringWidth(level)) / 2, HEIGHT / 2 + 20);
            
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String restart = "Drücke ENTER für neues Spiel";
            metrics = getFontMetrics(g.getFont());
            g.drawString(restart, (WIDTH - metrics.stringWidth(restart)) / 2, HEIGHT / 2 + 80);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Snake());
    }
}