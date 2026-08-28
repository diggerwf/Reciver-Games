# Snake Game - Technical Specification for AI Implementation

## Project Overview
- **Name:** Snake (Level-based)
- **Platform:** Raspberry Pi OS Lite (no X11/Wayland)
- **Display:** HDMI via DRM/KMS (`/dev/dri/card0`) → mpv GPU context
- **Input:** 5 keys via InputBridge (UDP 9527) → mpv script-message → GLFW key events
- **Language:** Java 21 + LWJGL 3.3.6 (GLFW + OpenGL ES 2)
- **Build:** Gradle Fat-JAR (`snake.jar`, 1.8 MB, includes ARM64 natives)

---

## Display / Graphics Pipeline
| Layer | Technology |
|-------|------------|
| Hardware | Raspberry Pi 4/5 (VC6/V3D), HDMI-A-1 |
| Kernel | DRM/KMS (`/dev/dri/card0`), Atomic Modesetting |
| Userspace | mpv `--vo=gpu --gpu-context=drm --drm-connector=HDMI-A-1 --drm-mode=12` |
| Game Graphics | LWJGL 3 / GLFW (DRM backend) → OpenGL ES 2 |
| Resolution | 1920×1080@50Hz physical, 960×540 logical (OSD scale 0.55) |
| Color Format | RGB565 / XRGB8888 via DRM Planes |
| Refresh | 50 Hz (vsynced to mpv) |

**GLFW Window Hints (SnakeMain.java:38-53):**
```java
GLFW_VISIBLE = FALSE
GLFW_DECORATED = FALSE
GLFW_FLOATING = TRUE
GLFW_CLIENT_API = GLFW_OPENGL_ES_API
GLFW_CONTEXT_VERSION = 2.0
GLFW_AUTO_ICONIFY = FALSE
```
Fullscreen on primary monitor (HDMI-A-1).

---

## Input System
### Physical Sources
- **IR Remote (RG405 DT5 @ GPIO18, NEC):** `ir_daemon.py` → UDP 9527
- **CEC (HDMI-CEC):** `receiver.py` → same UDP 9527
- **InputBridge (`receiver.py`):** Maps scancodes → Actions → mpv `script-message`

### Key Mapping (5 Game Actions)
| Game Action | GLFW Key | InputBridge Action | IR NEC Scancode | CEC Opcode |
|-------------|----------|-------------------|-----------------|------------|
| LEFT  | `GLFW_KEY_LEFT` (263) | `nav:left` | `0x03` | `0x03` |
| RIGHT | `GLFW_KEY_RIGHT` (262) | `nav:right` | `0x04` | `0x04` |
| UP    | `GLFW_KEY_UP` (265) | `nav:up` | `0x01` | `0x01` |
| DOWN  | `GLFW_KEY_DOWN` (264) | `nav:down` | `0x02` | `0x02` |
| OK/ENTER | `GLFW_KEY_ENTER` (257) | `nav:ok` | `0xD0/0x2B/0x00` | `0x00/0x2B/0xD0` |

**Exit:** `GLFW_KEY_ESCAPE` (256) → `nav:exit` (CEC `0x0D` / IR `KEY_EXIT`)

### Input Flow
```
IR/CEC → ir_daemon.py (UDP 9527) → receiver.py (InputBridge)
→ mpv script-message (cec-nav, blue-enter)
→ GLFW key callback (InputHandler.java:12-18)
→ Game.setDirection() / Game.action()
```

---

## Game Logic (Game.java)
### Core Parameters
```java
UNIT_SIZE = 25px (logical)
GRID = 960/25 × 540/25 = 38 × 21 cells
BASE_DELAY_MS = 120 (≈8.3 updates/sec)
MIN_DELAY_MS = 40 (25 updates/sec)
APPLES_PER_LEVEL = 5
```

### Level System
- **Level 1-2:** Empty arena, walls only
- **Level 3+:** Obstacles spawn (gray blocks)
  - Count: `(level - 2) * 3` obstacles
  - Placement: Random, not on snake/apple/start zone (150×150 top-left)
- **Speed:** `delayMs = max(40, 120 - (level-1)*10)` per level

### States
| State | Transitions |
|-------|-------------|
| `START` | `action()` (ENTER) → `RUNNING` |
| `RUNNING` | Collision → `GAME_OVER` |
| `GAME_OVER` | `action()` (ENTER) → `START` (reset) |

### Collision Rules
1. **Wall:** Head x < 0 or ≥ 960, y < 0 or ≥ 540
2. **Self:** Head overlaps any body segment (index 1..length)
3. **Obstacle:** Head overlaps any obstacle point (Level 3+)

### Scoring
- 1 apple = 1 point, snake grows by 1 segment
- Level = `score / 5 + 1`
- HUD: Score, Level, Speed multiplier (1000/delayMs)

---

## Rendering (Renderer.java)
### OpenGL ES 2 Pipeline
- **Shader:** Simple vertex/fragment (position + color, orthographic projection)
- **Geometry:** Immediate-mode VBO updates (no VAOs for GLES2 compatibility)
- **Primitives:**
  - Snake/Obstacles/Apple: `GL_TRIANGLE_STRIP` (4 vertices, rect)
  - Text: Placeholder colored rect (no real font rendering yet)

### Draw Calls per Frame (Running)
```
1. Background (fullscreen rect)
2. Obstacles: N rects (gray)
3. Apple: 1 rect (red)
4. Snake: Length rects (green head, darker body)
5. HUD: 3 text rects (score, level, speed)
```
~50-100 draw calls max. 60 FPS fixed timestep.

---

## Build & Deployment
### Dependencies (build.gradle)
```groovy
implementation "org.lwjgl:lwjgl:3.3.6"
implementation "org.lwjgl:lwjgl-glfw:3.3.6"
implementation "org.lwjgl:lwjgl-opengles:3.3.6"
runtimeOnly "org.lwjgl:lwjgl:3.3.6:natives-linux-arm64"
runtimeOnly "org.lwjgl:lwjgl-glfw:3.3.6:natives-linux-arm64"
runtimeOnly "org.lwjgl:lwjgl-opengles:3.3.6:natives-linux-arm64"
```

### System Requirements (Pi OS Lite)
```bash
sudo apt install libglfw3 libgbm1 libdrm2 libegl1 libgles2
sudo usermod -a -G video,render $USER
```

### Launch (snake.sh)
```bash
java -Dorg.lwjgl.glfw.libname=glfw \
     -Dorg.lwjgl.system.allocator=system \
     -Xmx256m -Xms64m \
     -cp snake.jar SnakeMain
```

### systemd Service (snake.service)
```ini
User=diggerwf
SupplementaryGroups=video render
Environment=DISPLAY=
Environment=LIBGL_DRI3_DISABLE=1
```

---

## File Structure
```
/home/diggerwf/snake/
├── snake.jar              # Fat-JAR (Main-Class: SnakeMain)
├── snake.sh               # Launch script
├── snake.service          # systemd user service
├── build.gradle           # Gradle build config
├── SPECS.md               # This specification
└── src/main/java/
    ├── SnakeMain.java     # GLFW/DRM init, GLES2 caps, game loop
    ├── Renderer.java      # GLES2 renderer (rects, circles, text stub)
    ├── Game.java          # Game logic, level system, collision
    └── InputHandler.java  # GLFW key callback → Game actions
```

---

## Integration Notes for Receiver
1. **Coexistence with mpv:** Game takes DRM plane; mpv pauses or uses separate plane
2. **InputBridge:** Must forward `nav:left/right/up/down/ok` as GLFW key events
3. **Exit Handling:** `nav:exit` (ESC) should return to receiver UI
4. **Blue Mode:** Game ignores `blue-navigate` / digit keys (only menu uses them)
5. **Performance:** 60 FPS fixed step, <5ms frame time on Pi 4

---

## Known Limitations / TODOs
- [ ] Real text rendering (stb_truetype or bitmap font atlas)
- [ ] Apple as circle (currently rect)
- [ ] Sound effects (via mpv or ALSA)
- [ ] High score persistence (file in /tmp or /home)
- [ ] Pause state (currently only START/RUNNING/GAME_OVER)
- [ ] Config file (resolution, colors, key mapping)

---

## Test Checklist
- [ ] JAR runs on Pi 4/5 (ARM64)
- [ ] DRM/KMS window opens on HDMI-A-1
- [ ] IR remote arrows + OK control snake
- [ ] CEC TV remote arrows + OK control snake
- [ ] Level progression works (5 apples → speed up, obstacles at L3)
- [ ] Game Over → Enter restarts
- [ ] ESC returns to receiver UI
- [ ] No memory leaks over 30 min play
- [ ] systemd service starts on boot