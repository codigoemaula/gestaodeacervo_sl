#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://services.gradle.org/distributions/gradle-9.6.0-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR ausente; baixando Gradle 9.6.0..." >&2
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Instale curl/wget ou baixe $WRAPPER_URL para $WRAPPER_JAR" >&2
    exit 1
  fi
fi
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
