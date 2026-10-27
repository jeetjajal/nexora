# ==============================================================================
# NEXORA MULTI-STAGE DOCKERFILE
# ==============================================================================
# Security & Best Practice Rules:
# 1. Multi-Stage Build: Compiles source in build container; keeps final runtime image small (<250MB).
# 2. Non-Root Execution: Runs under dedicated 'nexora' user (UID 10001) to prevent container breakouts.
# 3. Layer Caching: Dependencies (pom.xml) are resolved before copying source code.
# 4. Zero Secrets In Image: All database, payment & AI credentials are provided via runtime environment variables.
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /workspace

# Cache maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Secure JRE Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="Nexora Engineering Team <dev@nexora.com>"
LABEL description="Nexora Intelligent Local-Commerce Backend Container"
LABEL version="1.0.0"

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata

# Set timezone
ENV TZ=Asia/Kolkata

# Create non-root user and group
RUN addgroup -g 10001 -S nexoragroup && \
    adduser -u 10001 -S nexorauser -G nexoragroup

WORKDIR /app

# Copy artifact from builder stage
COPY --from=builder --chown=nexorauser:nexoragroup /workspace/target/nexora-*.jar app.jar

# Switch to non-privileged user
USER nexorauser:nexoragroup

# Expose Spring Boot web port
EXPOSE 8080

# Production container JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
