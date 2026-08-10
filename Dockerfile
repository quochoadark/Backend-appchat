# ==========================================
# Stage 1: Build Java Spring Boot Application
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy pom.xml and download dependencies (for layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Hardened Production Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create non-root user for security (Least Privilege principle)
RUN addgroup -g 1001 -S appgroup && \
    adduser -S appuser -u 1001 -G appgroup

# Create uploads directory with correct permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy built JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Environment defaults
ENV PORT=8080
EXPOSE 8080

# Container Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/ || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
