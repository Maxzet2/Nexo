FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

COPY gradle/libs.versions.toml gradle/libs.versions.toml

RUN chmod +x gradlew

COPY . .

RUN ./gradlew :server:buildFatJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /app/server/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]