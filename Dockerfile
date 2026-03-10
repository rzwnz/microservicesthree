FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="rzwnz"
LABEL service="file-service"

WORKDIR /app

# Create non-root user
RUN addgroup -S sthree && adduser -S sthree -G sthree

# Copy the JAR produced by Maven
COPY target/file-service-*.jar app.jar

# Set ownership
RUN chown -R sthree:sthree /app

USER sthree

# JVM options tuned for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SERVER_PORT=8087

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
