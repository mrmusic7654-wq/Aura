#!/bin/sh

# Simple Gradle wrapper
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Default JVM options
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Find java
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

# Execute gradle
exec $JAVA $DEFAULT_JVM_OPTS -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
