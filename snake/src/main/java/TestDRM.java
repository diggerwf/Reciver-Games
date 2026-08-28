import org.lwjgl.glfw.*;
import org.lwjgl.opengles.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Minimal DRM/KMS test for GLFW initialization.
 * 
 * Compile: javac -cp snake.jar TestDRM.java
 * Run:     java -cp snake.jar:. TestDRM
 * 
 * Expected: "GLFW init OK" -> "Window OK: WxH" -> RED screen 2s -> GREEN screen 2s -> Exit 0
 */
public class TestDRM {
    public static void main(String[] args) {
        System.err.println("=== TestDRM: Starting ===");
        
        // ERROR CALLBACK FIRST!
        GLFWErrorCallback.createPrint(System.err).set();
        System.err.println("Error callback set");
        
        if (!glfwInit()) {
            System.err.println("GLFW init FAILED");
            System.exit(1);
        }
        System.err.println("GLFW init OK");
        
        // OpenGL ES 2.0 for DRM
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        
        // DRM Platform Hint (GLFW 3.4+)
        try {
            glfwWindowHint(0x00020001, 0x00020001); // GLFW_PLATFORM, GLFW_PLATFORM_DRM
        } catch (Exception ignored) {}
        
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        glfwWindowHint(GLFW_AUTO_ICONIFY, GLFW_FALSE);
        glfwWindowHint(GLFW_FOCUSED, GLFW_TRUE);
        
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid == null) {
            System.err.println("No primary monitor!");
            System.exit(1);
        }
        
        long win = glfwCreateWindow(vid.width(), vid.height(), "TestDRM", glfwGetPrimaryMonitor(), NULL);
        if (win == NULL) {
            System.err.println("Window create FAILED");
            glfwTerminate();
            System.exit(1);
        }
        System.err.println("Window OK: " + vid.width() + "x" + vid.height());
        
        glfwMakeContextCurrent(win);
        GLES.createCapabilities();
        
        // Red frame
        glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        glfwSwapBuffers(win);
        System.err.println("Red frame shown");
        sleep(2000);
        
        // Green frame
        glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        glfwSwapBuffers(win);
        System.err.println("Green frame shown");
        sleep(2000);
        
        glfwDestroyWindow(win);
        glfwTerminate();
        System.err.println("TestDRM: SUCCESS");
    }
    
    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}