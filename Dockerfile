# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle \
    for attempt in 1 2 3; do \
      ./gradlew resolveDockerDependencies --no-daemon && exit 0; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "Dependency resolution failed (attempt ${attempt}/3). Retrying..."; \
      sleep $((attempt * 10)); \
    done

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    for attempt in 1 2 3; do \
      ./gradlew bootJar --no-daemon -x test && exit 0; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "bootJar failed (attempt ${attempt}/3). Retrying..."; \
      sleep $((attempt * 10)); \
    done

FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
