# ============================================================
# AI RAG ChatBot Backend — Production Dockerfile
# Multi-stage build: Maven build → lightweight JRE runtime
# ============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache maven
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copy source and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the fat JAR from the build stage
COPY --from=builder /build/target/rag-chatbot-backend-*.jar app.jar

# Create data directory for uploads (if needed)
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

# Default port
EXPOSE 8081

# Health check against the /api/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/api/health || exit 1

# JVM tuning for containerized environments
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
