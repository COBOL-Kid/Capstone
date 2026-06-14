# syntax=docker/dockerfile:1

# --- Build: GraalVM native image (glibc) for Cloud Run ---
FROM ghcr.io/graalvm/native-image-community:25.0.2 AS builder

ENV JAVA_HOME=/usr/lib64/graalvm/graalvm-community-java25
ENV GRAALVM_HOME=/usr/lib64/graalvm/graalvm-community-java25
ENV NATIVE_IMAGE_BUILD=true

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon -q

COPY src/ src/

RUN ./gradlew nativeCompile -x test --no-daemon

# --- Runtime: minimal glibc image for Cloud Run ---
FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 65532 --home-dir /app --shell /usr/sbin/nologin nonroot

WORKDIR /app

COPY --from=builder /workspace/build/native/nativeCompile/capstone-server ./capstone-server

USER nonroot

# Cloud Run contract: HTTP on PORT (injected at runtime; 8080 is the default).
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["./capstone-server"]
