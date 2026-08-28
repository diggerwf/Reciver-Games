# Snake Game - Hardware/Interface Specification (IPTV Receiver)

## Output (Display) — **mpv DRM/KMS Pipeline**
- **Target:** HDMI out (TV) via **mpv** `--vo=gpu --gpu-context=drm --drm-connector=HDMI-A-1 --drm-mode=12`
- **Interface:** **LWJGL 3 / GLFW** → DRM/KMS (`/dev/dri/card0`) Atomic Modesetting
- **Resolution:** 1920×1080@50Hz (logical 960×540 via OSD scale 0.55)
- **Format:** RGB565 / XRGB8888 via DRM Planes
- **Refresh:** 50 Hz (vsynced to mpv)
- **Graphics:** 2D rects (snake, obstacles), circles (apple), text (score, level, menus)
- **Colors:** 16+ colors
- **Window:** Fullscreen, undecorated, floating, on HDMI-A-1 monitor

## Input (5 Keys) — **via InputBridge → GLFW**
| Game Action | GLFW Key | InputBridge Action | IR (NEC) | CEC Opcode |
|-------------|----------|-------------------|----------|------------|
| LEFT  | `GLFW_KEY_LEFT`  | `nav:left`  | `0x03` (`KEY_LEFT`)  | `0x03` |
| RIGHT | `GLFW_KEY_RIGHT` | `nav:right` | `0x04` (`KEY_RIGHT`) | `0x04` |
| UP    | `GLFW_KEY_UP`    | `nav:up`    | `0x01` (`KEY_UP`)    | `0x01` |
| DOWN  | `GLFW_KEY_DOWN`  | `nav:down`  | `0x02` (`KEY_DOWN`)  | `0x02` |
| OK/ENTER | `GLFW_KEY_ENTER` | `nav:ok`   | `0xD0/0x2B/0x00` (`KEY_OK`) | `0x00/0x2B/0xD0` |

- **Transport:** IR/CEC → `ir_daemon.py` (UDP 9527) → `receiver.py` (InputBridge) → mpv `script-message` → **GLFW key events**
- **No direct evdev** needed in Java — GLFW receives standard key codes
- **Debounce:** Handled by InputBridge (150ms blue_mode cache)
- **Blocking:** Non-blocking poll in game loop (~60 FPS)

## Software Stack (Pi OS Lite)
- **Java:** OpenJDK 21+ (installed)
- **Graphics:** **LWJGL 3** (GLFW + OpenGL ES 2/3 via DRM/GBM)
  - Natives: `lwjgl-glfw`, `lwjgl-opengles`, `lwjgl-egl`, `lwjgl-glm`
  - `-Dorg.lwjgl.glfw.libname=glfw` (system GLFW with DRM backend)
- **Input:** GLFW key callbacks (`glfwSetKeyCallback`)
- **Dependencies:** `libglfw3`, `libgbm1`, `libdrm2`, `libegl1`, `libgles2` (system packages)
- **User groups:** `video`, `render` (for `/dev/dri/card0` access)
- **No:** X11, Wayland, Desktop, Swing, AWT, JavaFX

## Game Logic (port from Snake.java)
- Level system (every 5 apples = level up, speed increases, obstacles from level 3)
- Score, level, speed display
- Start screen, Game Over screen
- Collision: walls, self, obstacles
- **60 FPS game loop** (fixed timestep, interpolated render)

## Integration with Receiver
- **Launch:** `java -cp snake.jar:lwjgl3-natives.jar -Dorg.lwjgl.glfw.libname=glfw Snake`
- **Systemd:** User service, `Environment=DISPLAY=`, `SupplementaryGroups=video render`
- **mpv coexistence:** Game takes over DRM plane; mpv pauses or uses separate plane
- **Exit:** `nav:exit` (CEC `0x0D` / IR `KEY_EXIT`) → return to receiver UI

## Deliverable
- **Fat JAR:** `snake.jar` (includes LWJGL3 natives for linux-arm64)
- **Or:** GraalVM native-image `snake` (fast startup, no JVM warmup)
- **Start script:** `snake.sh` (sets LD_LIBRARY_PATH, java options)
- **Config:** Optional `snake.conf` (resolution, input mapping, colors)

## Files in Repository
```
/home/diggerwf/snake/
├── Snake.java          # Game logic (port from Swing version)
├── SnakeMain.java      # LWJGL3/GLFW entry point, DRM init, game loop
├── Renderer.java       # OpenGL ES 2 renderer (rects, circles, text)
├── InputHandler.java   # GLFW key callback → game actions
├── build.gradle        # Gradle build (fatJar, natives)
├── snake.sh            # Launch script
├── snake.service       # systemd user service
└── SPECS.md            # This file
```