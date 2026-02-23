# ============================================================
# Tamabee API-HR — Multi-stage Dockerfile
# ============================================================
# Stage 1: Build với Maven
# Stage 2: Runtime tối ưu với Eclipse Temurin JRE 21
# ============================================================

# ---- STAGE 1: BUILD ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml trước để cache dependencies (Docker layer caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Download dependencies (layer này sẽ được cache nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src
COPY config ./config

# Build JAR (skip tests vì test sẽ chạy ở CI/CD)
RUN ./mvnw clean package -DskipTests -Dcheckstyle.skip=true -B \
    && mv target/*.jar target/app.jar

# ---- STAGE 2: RUNTIME ----
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="Tamabee <tamabee.info@gmail.com>"
LABEL description="Tamabee API-HR Service"

# Tạo user non-root (bảo mật)
RUN addgroup -S tamabee && adduser -S tamabee -G tamabee

WORKDIR /app

# Copy JAR từ build stage
COPY --from=builder /build/target/app.jar app.jar

# Tạo thư mục uploads
RUN mkdir -p /app/uploads && chown -R tamabee:tamabee /app

# Chuyển sang user non-root
USER tamabee

# Expose ports
EXPOSE 8081
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1

# JVM tuning cho VPS 8GB RAM (allocate ~2.5GB cho API)
# -XX:+UseG1GC: Garbage collector tốt nhất cho server
# -XX:MaxRAMPercentage: Giới hạn RAM dùng trong Docker container
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
