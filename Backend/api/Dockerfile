# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Resolve dependencies first: this layer is cached (locally and in GitHub Actions)
# until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests \
    && cp target/api-*.jar /build/app.jar

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /build/app.jar app.jar
USER app

# Tuned for small (512MB) containers; override via JAVA_TOOL_OPTIONS if needed
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70 -Xss512k"
EXPOSE 8005

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
