FROM eclipse-temurin:25-jdk

WORKDIR /app

# Copy gradle files first for caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Copy source
COPY src ./src

# Build
RUN ./gradlew installDist

# Expose gRPC port
EXPOSE 50051

# Run application (path may vary based on project name, assuming 'engine')
CMD ["./build/install/engine/bin/engine"]
