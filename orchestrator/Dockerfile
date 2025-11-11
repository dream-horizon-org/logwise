# Use a Java 17 JRE base image (class file version 61)
FROM eclipse-temurin:11-jre

# Set working directory
WORKDIR /app

# Copy the shaded fat JAR explicitly (contains Main-Class manifest)
COPY target/log-central-orchestrator-*-all.jar app.jar

# Expose the port your app listens on
EXPOSE 8080

# Optional: Set some safe JVM defaults for containers

# Start the application
ENTRYPOINT ["java", "-jar", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false", "-Dvertx.disableDnsResolver=true -DIGNITE_JETTY_PORT=9000 --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --illegal-access=permit",  "/app/app.jar"]
