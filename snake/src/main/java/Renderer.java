import org.lwjgl.opengles.GLES20;
import org.lwjgl.system.MemoryStack;

import java.nio.*;
import java.util.*;

import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryStack.*;

public class Renderer {
    private final int logicalWidth;
    private final int logicalHeight;

    private int shaderProgram;
    private int vboRect;
    private int vboCircle;
    private int circleVertexCount;
    private int fontTexture;
    private int vboText;

    private int aPosLoc, aColorLoc, uProjLoc;

    public Renderer(int fbWidth, int fbHeight) {
        this.logicalWidth = 960;
        this.logicalHeight = 540;

        initShaders();
        initGeometry();
        initFont();
    }

    private void initShaders() {
        String vs = "#version 100\n" +
            "attribute vec2 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "uniform mat4 uProjection;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "   gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
            "   vColor = aColor;\n" +
            "}";

        String fs = "#version 100\n" +
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "   gl_FragColor = vColor;\n" +
            "}";

        shaderProgram = createProgram(vs, fs);
        aPosLoc = glGetAttribLocation(shaderProgram, "aPos");
        aColorLoc = glGetAttribLocation(shaderProgram, "aColor");
        uProjLoc = glGetUniformLocation(shaderProgram, "uProjection");
    }

    private int createProgram(String vsSource, String fsSource) {
        int vs = compileShader(GL_VERTEX_SHADER, vsSource);
        int fs = compileShader(GL_FRAGMENT_SHADER, fsSource);
        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link failed: " + glGetProgramInfoLog(prog));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Compile failed: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private void initGeometry() {
        // Rectangle VBO (snake, obstacles, apple square)
        vboRect = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboRect);
        glBufferData(GL_ARRAY_BUFFER, 4 * 6 * 4, GL_DYNAMIC_DRAW); // 4 verts * 6 floats * 4 bytes

        // Circle VBO (apple)
        List<Float> circleVerts = new ArrayList<>();
        int segments = 16;
        circleVerts.add(0f); circleVerts.add(0f); // center
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            circleVerts.add((float)Math.cos(angle));
            circleVerts.add((float)Math.sin(angle));
        }
        circleVertexCount = segments + 2;

        try (MemoryStack stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(circleVerts.size());
            for (float f : circleVerts) buf.put(f);
            buf.flip();

            vboCircle = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboCircle);
            glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void initFont() {
        int texW = 128, texH = 128;
        ByteBuffer pixels = ByteBuffer.allocateDirect(texW * texH);
        for (int y = 0; y < texH; y++) {
            for (int x = 0; x < texW; x++) {
                pixels.put((byte) 255);
            }
        }
        pixels.flip();

        fontTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, texW, texH, 0, GL_ALPHA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        vboText = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboText);
        glBufferData(GL_ARRAY_BUFFER, 256 * 4, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void beginFrame() {
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(shaderProgram);

        float[] proj = ortho(0, logicalWidth, logicalHeight, 0, -1, 1);
        glUniformMatrix4fv(uProjLoc, false, proj);
    }

    private float[] ortho(float l, float r, float b, float t, float n, float f) {
        return new float[]{
            2f/(r-l), 0, 0, 0,
            0, 2f/(t-b), 0, 0,
            0, 0, -2f/(f-n), 0,
            -(r+l)/(r-l), -(t+b)/(t-b), -(f+n)/(f-n), 1
        };
    }

    public void endFrame() {
        glUseProgram(0);
    }

    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        glBindBuffer(GL_ARRAY_BUFFER, vboRect);
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(4 * 6);
            float[] verts = {
                x, y, r, g, b, a,
                x+w, y, r, g, b, a,
                x, y+h, r, g, b, a,
                x+w, y+h, r, g, b, a
            };
            buf.put(verts).flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        }

        glEnableVertexAttribArray(aPosLoc);
        glVertexAttribPointer(aPosLoc, 2, GL_FLOAT, false, 6 * 4, 0);
        glEnableVertexAttribArray(aColorLoc);
        glVertexAttribPointer(aColorLoc, 4, GL_FLOAT, false, 6 * 4, 2 * 4);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableVertexAttribArray(aPosLoc);
        glDisableVertexAttribArray(aColorLoc);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void drawCircle(float cx, float cy, float radius, float r, float g, float b, float a) {
        // Draw as rect for simplicity (GLES2 no VAO)
        drawRect(cx - radius, cy - radius, radius * 2, radius * 2, r, g, b, a);
    }

    public void drawText(String text, float x, float y, float size, float r, float g, float b, float a) {
        // Placeholder: draw colored rect for text
        float charW = size * 0.6f;
        drawRect(x, y, text.length() * charW, size, r, g, b, a);
    }

    public int getLogicalWidth() { return logicalWidth; }
    public int getLogicalHeight() { return logicalHeight; }

    public void dispose() {
        glDeleteBuffers(vboRect);
        glDeleteBuffers(vboCircle);
        glDeleteBuffers(vboText);
        glDeleteTextures(fontTexture);
        glDeleteProgram(shaderProgram);
    }
}