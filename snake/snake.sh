#!/bin/bash
# Snake Game Launcher for Raspberry Pi OS Lite (DRM/KMS via LWJGL3)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure user is in video/render groups
if ! groups | grep -qE '\b(video|render)\b'; then
    echo "WARN: User not in video/render group. DRM access may fail."
    echo "Run: sudo usermod -a -G video,render \$USER"
    echo "Then log out and back in."
fi

# Stop mpv/receiver to release DRM master
echo "Stopping receiver service (releasing DRM master)..."
systemctl --user stop reciver.service 2>/dev/null || true
sleep 2

# DRM/KMS Environment
export LIBGL_DRI3_DISABLE=1
export GBM_BACKEND=drm
export EGL_PLATFORM=drm
export GLFW_DRM_DEVICE="/dev/dri/card0"

# Library path: system libs (libglfw3, libgbm, libdrm, libEGL, libGLESv2) FIRST
# This allows -Dorg.lwjgl.glfw.libname=glfw to use system GLFW with DRM support
export LD_LIBRARY_PATH="/usr/lib/aarch64-linux-gnu:${LD_LIBRARY_PATH}"

# JVM options for Pi (ARM64)
JVM_OPTS=(
    # Use system GLFW (libglfw3 Debian package) - may have DRM support
    -Dorg.lwjgl.glfw.libname=glfw
    -Dorg.lwjgl.system.allocator=system
    # Debug logging for troubleshooting
    -Dorg.lwjgl.util.Debug=true
    -Dorg.lwjgl.util.DebugLoader=true
    -Xmx256m
    -Xms64m
    -XX:+UseSerialGC
)

# Classpath: fatJar or exploded
if [[ -f snake.jar ]]; then
    CP="snake.jar"
elif [[ -f build/libs/snake-1.0.jar ]]; then
    CP="build/libs/snake-1.0.jar"
else
    echo "ERROR: snake.jar not found. Run './gradlew fatJar' first."
    exit 1
fi

echo "Starting Snake on DRM/KMS (HDMI-A-1)..."
echo "DRM Device: /dev/dri/card0"
echo "Resolution: 1920x1080@50Hz (logical 960x540)"
echo "Controls: Arrow keys + ENTER (via InputBridge)"
echo "GLFW Debug: enabled"
echo ""

exec java "${JVM_OPTS[@]}" -cp "$CP" SnakeMain 2>&1