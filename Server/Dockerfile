# syntax=docker/dockerfile:1

FROM ghcr.io/graalvm/native-image-community:25 AS builder

ENV JAVA_HOME=/usr/lib64/graalvm/graalvm-community-java25
ENV GRAALVM_HOME=/usr/lib64/graalvm/graalvm-community-java25

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY src/ src/

ENV NATIVE_IMAGE_BUILD=true
RUN chmod +x gradlew && ./gradlew nativeCompile -x test --no-daemon

FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 65532 --home-dir /app --shell /usr/sbin/nologin nonroot

WORKDIR /app

COPY --from=builder /workspace/build/native/nativeCompile/capstone-server ./capstone-server

USER nonroot

EXPOSE 8080

ENTRYPOINT ["./capstone-server"]
