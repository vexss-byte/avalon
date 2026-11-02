#!/bin/sh
# Minimal bootstrap: use the real wrapper jar if present, otherwise ask the user to run `gradle wrapper`.
APP_HOME=$(cd "$(dirname "$0")" && pwd)
if [ ! -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "gradle-wrapper.jar missing. Run: gradle wrapper --gradle-version 8.11.1" >&2
  exit 1
fi
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
