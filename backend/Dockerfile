# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S waterai && adduser -S waterai -G waterai && mkdir -p /app/storage && chown -R waterai:waterai /app

COPY --from=builder /workspace/target/*.jar /app/water-ai-consultant.jar

ENV STORAGE_TYPE=local \
    STORAGE_PATH=/app/storage \
    MINIO_ENDPOINT=http://minio:9000 \
    MINIO_BUCKET=water-ai-consultant \
    LLM_PROVIDER=deepseek \
    EMBEDDING_PROVIDER=none

EXPOSE 8080

USER waterai
ENTRYPOINT ["java", "-jar", "/app/water-ai-consultant.jar"]
