#!/bin/bash
# Gradle wrapper downloader
GRADLE_VERSION="8.5"
WRAPPER_DIR="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
WRAPPER_JAR="$WRAPPER_DIR/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

mkdir -p "$WRAPPER_DIR"
if [[ ! -f "$WRAPPER_DIR/gradle-${GRADLE_VERSION}/bin/gradle" ]]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    curl -L -o "$WRAPPER_JAR" "$GRADLE_URL"
    unzip -q "$WRAPPER_JAR" -d "$WRAPPER_DIR"
fi

exec "$WRAPPER_DIR/gradle-${GRADLE_VERSION}/bin/gradle" "$@"
