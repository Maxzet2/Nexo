FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ ./gradle/
COPY client/build.gradle.kts ./client/build.gradle.kts
COPY server/ ./server/
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew :server:buildFatJar --no-daemon

FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/server/build/libs/app.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/healthy || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
