FROM bellsoft/liberica-runtime-container:jdk-25-glibc AS builder

RUN apk add --no-cache libstdc++ # для токенизатора

WORKDIR /app

COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ ./gradle/
COPY client/build.gradle.kts ./client/build.gradle.kts
COPY server/ ./server/
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew :server:buildFatJar --no-daemon

FROM bellsoft/liberica-runtime-container:jre-25-slim-glibc

COPY --from=builder /usr/lib/libstdc++.so.6* /usr/lib/libgcc_s.so.1 /usr/lib/

WORKDIR /app
COPY --from=builder /app/server/build/libs/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
