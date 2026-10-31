FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/material-studio-backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /uploads

ENV APP_UPLOAD_DIR=/uploads
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD curl --fail --silent http://localhost:8080/templates > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
