# Multi-stage build: Build stage
FROM maven:3.9-eclipse-temurin-11 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first (for better layer caching)
COPY pom.xml .

# Download dependencies with parallel downloads and cache mount for Maven repository
# Using cache mount to persist Maven repository between builds for faster subsequent builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 4 dependency:go-offline -Dmaven.artifact.threads=10

# Copy source code
COPY src ./src

# Build the application JAR
# Using cache mount to reuse Maven repository from dependency download step
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 4 clean package -DskipTests -Dmaven.artifact.threads=10

# Runtime stage: Use a Java 11 JRE base image
FROM eclipse-temurin:11-jre

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/orchestrator-*-all.jar app.jar

# Expose the port your app listens on
EXPOSE 8080

# Optional: Set some safe JVM defaults for containers

# Start the application
ENTRYPOINT ["java", "-jar", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.sun.management.jmxremote=true", "-Dcom.sun.management.jmxremote.port=1099", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-Dvertx.disableDnsResolver=true", "-DIGNITE_JETTY_PORT=9000", "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED", "--add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED", "--add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED", "--add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED", "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED", "--illegal-access=permit", "/app/app.jar"]
