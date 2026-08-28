#!/bin/bash
# Snake Game Launcher for Raspberry Pi OS Lite (DRM/KMS via LWJGL3)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Ensure user is in video/render groups
if ! groups | grep -qE '\b(video|render)\b'; then
    echo "WARN: User not in video/render group. DRM access may fail."
    echo "Run: sudo usermod -a -G video,render $USER"
    echo "Then log out and back in."
fi

# JVM options for Pi (ARM64)
JVM_OPTS=(
    -Dorg.lwjgl.glfw.libname=glfw
    -Dorg.lwjgl.system.allocator=system
    -Xmx256m
    -Xms64m
    -XX:+UseSerialGC
)

# Classpath: fatJar or exploded
if [[ -f snake.jar ]]; then
    CP="snake.jar"
elif [[ -f build/libs/snake-fat.jar ]]; then
    CP="build/libs/snake-fat.jar"
else
    echo "ERROR: snake.jar not found. Run './gradlew fatJar' first."
    exit 1
fi

echo "Starting Snake on DRM/KMS (HDMI-A-1)..."
echo "Resolution: 1920x1080@50Hz (logical 960x540)"
echo "Controls: Arrow keys + ENTER (via InputBridge)"
echo ""

exec java "${JVM_OPTS[@]}" -cp "$CP" SnakeMain