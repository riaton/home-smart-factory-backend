FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY common common
COPY api api
COPY worker worker
COPY batch batch

RUN ./gradlew :api:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=builder /workspace/api/build/libs/*.jar app.jar

RUN useradd --system --no-create-home --uid 1001 appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
