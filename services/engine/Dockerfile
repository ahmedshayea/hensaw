FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY ../proto ./proto

RUN ./gradlew clean installDist

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/build/install/engine ./engine

EXPOSE 50051

ENV ENGINE_PORT=50051

CMD ["./engine/bin/engine"]
