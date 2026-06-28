#!/bin/sh

# Gradle wrapper script

GRADLE_WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
    echo "ERROR: $GRADLE_WRAPPER_JAR not found."
    echo "Please run: gradle wrapper"
    exit 1
fi

exec java -Xmx64m -Xms64m \
    -Dorg.gradle.appname=gradlew \
    -classpath "$(pwd)/$GRADLE_WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain "$@"
