# ═══════════════════════════════════════════════════════════════
#  Stage 1 — Build
#  Menggunakan Gradle wrapper di dalam container untuk memastikan
#  build reproducible tanpa bergantung pada JDK lokal.
# ═══════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle wrapper dulu (agar cache hit saat dependencies tidak berubah)
COPY gradlew .
COPY gradle gradle
RUN chmod +x ./gradlew

# Copy dependency manifests sebelum source code
COPY build.gradle.kts settings.gradle.kts* ./

# Download dependencies (layer terpisah supaya di-cache)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# Copy seluruh source code & proto
COPY src src
COPY config config

# Build fat JAR, skip tests (tests dijalankan di CI)
RUN ./gradlew assemble --no-daemon -x test -x checkstyleMain -x checkstyleTest

# ═══════════════════════════════════════════════════════════════
#  Stage 2 — Runtime
#  Image lebih kecil, hanya JRE
# ═══════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine AS runtime

# Metadata
LABEL maintainer="MySawit Team"
LABEL service="manage-pengiriman"
LABEL version="0.0.1-SNAPSHOT"

# Buat non-root user untuk keamanan
RUN addgroup -S mysawit && adduser -S pengiriman -G mysawit

WORKDIR /app

# Salin JAR dari stage builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Ubah ownership ke non-root user
RUN chown pengiriman:mysawit app.jar

USER pengiriman

# HTTP REST port
EXPOSE 8084

# gRPC port
EXPOSE 9094

# JVM tuning untuk container (memory-aware)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
