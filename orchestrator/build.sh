#!/bin/bash
# Build script that ensures Java 11 is used
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
mvn clean install "$@"

