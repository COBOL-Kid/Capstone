# syntax=docker/dockerfile:1

# --- Build: fat JAR with IBM Semeru JDK 25 ---
FROM icr.io/appcafe/ibm-semeru-runtimes:open-25-jdk-ubi-minimal AS builder

USER root

RUN microdnf install -y findutils \
    && microdnf clean all

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon -q

COPY src/ src/

RUN ./gradlew bootJar -x test --no-daemon

# --- Runtime: IBM Semeru JRE 25 on UBI minimal ---
FROM icr.io/appcafe/ibm-semeru-runtimes:open-25-jre-ubi-minimal

USER root

RUN microdnf install -y shadow-utils ca-certificates \
    && microdnf clean all \
    && useradd --system --uid 65532 --home-dir /app --shell /sbin/nologin nonroot

WORKDIR /app

COPY --from=builder --chown=65532:65532 /workspace/build/libs/capstone-server.jar ./app.jar

USER 65532

# Cloud Run injects PORT at runtime; prod profile binds via server.port=${PORT:8080}.
ENV SPRING_PROFILES_ACTIVE=prod

# Informational only — the process listens on $PORT (8080 when unset).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
