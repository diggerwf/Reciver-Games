import org.lwjgl.glfw.GLFW;

public class InputHandler {
    private final Game game;
    private boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private boolean[] keysPrev = new boolean[GLFW.GLFW_KEY_LAST + 1];

    public InputHandler(Game game) {
        this.game = game;
    }

    public void keyCallback(int key, int action) {
        if (key >= 0 && key < keys.length) {
            keys[key] = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
        }
    }

    public void poll() {
        // Detect key presses (edge detection)
        if (isPressed(GLFW.GLFW_KEY_LEFT)) game.setDirection('L');
        if (isPressed(GLFW.GLFW_KEY_RIGHT)) game.setDirection('R');
        if (isPressed(GLFW.GLFW_KEY_UP)) game.setDirection('U');
        if (isPressed(GLFW.GLFW_KEY_DOWN)) game.setDirection('D');
        if (isPressed(GLFW.GLFW_KEY_ENTER)) game.action();
        if (isPressed(GLFW.GLFW_KEY_ESCAPE)) game.exit();

        // Copy current to previous
        System.arraycopy(keys, 0, keysPrev, 0, keys.length);
    }

    private boolean isPressed(int key) {
        return keys[key] && !keysPrev[key];
    }
}