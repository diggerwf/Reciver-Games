import org.lwjgl.glfw.*;
import org.lwjgl.opengles.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class SnakeMain {
    private long window;
    private Renderer renderer;
    private InputHandler input;
    private Game game;

    private static final int TARGET_FPS = 60;
    private static final double TARGET_TIME = 1.0 / TARGET_FPS;

    public static void main(String[] args) {
        new SnakeMain().run();
    }

    private void run() {
        initGLFW();
        initGLES();
        initGame();
        loop();
        cleanup();
    }

    private void initGLFW() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        glfwWindowHint(GLFW_FLOATING, GLFW_TRUE);
        glfwWindowHint(GLFW_FOCUSED, GLFW_TRUE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_RED_BITS, 8);
        glfwWindowHint(GLFW_GREEN_BITS, 8);
        glfwWindowHint(GLFW_BLUE_BITS, 8);
        glfwWindowHint(GLFW_ALPHA_BITS, 8);
        glfwWindowHint(GLFW_DEPTH_BITS, 0);
        glfwWindowHint(GLFW_STENCIL_BITS, 0);
        glfwWindowHint(GLFW_SAMPLES, 0);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        int width = vid.width();
        int height = vid.height();

        window = glfwCreateWindow(width, height, "Snake", glfwGetPrimaryMonitor(), NULL);
        if (window == NULL) throw new IllegalStateException("Window creation failed");

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (input != null) input.keyCallback(key, action);
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }

    private void initGLES() {
        GLES.createCapabilities();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        glViewport(0, 0, w[0], h[0]);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void initGame() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        renderer = new Renderer(w[0], h[0]);
        game = new Game(renderer.getLogicalWidth(), renderer.getLogicalHeight());
        input = new InputHandler(game);
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;
            lastTime = currentTime;
            accumulator += frameTime;

            glfwPollEvents();
            input.poll();

            while (accumulator >= TARGET_TIME) {
                game.update(TARGET_TIME);
                accumulator -= TARGET_TIME;
            }

            renderer.beginFrame();
            game.render(renderer, accumulator / TARGET_TIME);
            renderer.endFrame();

            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        renderer.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}