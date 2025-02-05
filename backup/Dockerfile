# Dockerfile
# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy as deps

WORKDIR /build

# Copy Maven wrapper files
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./
COPY pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -DskipTests

# Build stage
FROM deps as package

WORKDIR /build

# Copy the entire src directory
COPY src/ src/

# Build application inside Docker
RUN ./mvnw clean package -DskipTests && \
    echo "Build completed, checking target directory:" && \
    ls -la target/

# Find and rename the built JAR
RUN JAR_NAME=$(ls target/*.jar | grep -v 'original-' | head -n 1) && \
    echo "Renaming $JAR_NAME to target/app.jar" && \
    mv "$JAR_NAME" target/app.jar

# Final stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the jar from builder stage
COPY --from=package /build/target/app.jar ./app.jar

# Create non-root user
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

USER appuser

EXPOSE 5050

# Add debugging to see if the jar exists and its contents
RUN ls -la /app && \
    java -jar app.jar --version || true

# Run application with debug output
ENV JAVA_OPTS="-Dlogging.level.root=DEBUG"
ENTRYPOINT java $JAVA_OPTS -jar app.jar
