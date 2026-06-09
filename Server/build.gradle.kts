import org.springframework.boot.gradle.tasks.aot.ProcessAot

plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
    id("org.owasp.dependencycheck") version "12.1.0"
    id("org.graalvm.buildtools.native") version "0.11.5"
}

group = "com.capstone"
version = "1.0-SNAPSHOT"
description = "Server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    implementation("com.mailjet:mailjet-client:6.0.1")

    runtimeOnly("org.flywaydb:flyway-mysql:11.15.0")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testRuntimeOnly("com.h2database:h2")

    add("nativeImageCompileOnly", sourceSets["main"].output)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("live")
    }
}

tasks.register<Test>("liveTest") {
    description = "Integration smoke tests against Auto.dev and Vehicle Databases APIs"
    group = "verification"
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    useJUnitPlatform {
        includeTags("live")
    }
    environment("RUN_VEHICLE_DATA_LIVE_TESTS", "true")
    System.getenv("RUN_MAILJET_LIVE_TESTS")?.let {
        environment("RUN_MAILJET_LIVE_TESTS", it)
    }
}

tasks.jar {
    enabled = false
}

val aotProfiles = (findProperty("aotProfiles") as String?) ?: "prod"

tasks.withType<ProcessAot>().configureEach {
    args("--spring.profiles.active=$aotProfiles")
    environment(
        mapOf(
            "SPRING_PROFILES_ACTIVE" to aotProfiles,
            "DB_URL" to "jdbc:mysql://localhost:3306/aot",
            "DB_USER" to "aot",
            "DB_PASS" to "aot",
            "SECRET_KEY" to "aot-jwt-secret-key-at-least-32-characters-long",
            "JWT_EXPIRATION_MINUTES" to "15",
            "JWT_REFRESH_EXPIRATION_DAYS" to "7",
            "AUTODEV_BASE_URL" to "https://autodev.placeholder.invalid",
            "AUTODEV_API_KEY" to "aot",
            "AUTODEV_API_KEY_HEADER" to "x-api-key",
            "VEHICLE_DATA_BASE_URL" to "https://vdb.placeholder.invalid",
            "VEHICLE_DATA_API_KEY" to "aot",
            "VEHICLE_DATA_API_KEY_HEADER" to "x-authkey",
            "MAILJET_ENABLED" to "true",
            "MAILJET_API_KEY_PUBLIC" to "aot",
            "MAILJET_API_KEY_PRIVATE" to "aot",
            "MAILJET_FROM_EMAIL" to "aot@example.com",
            "MAILJET_FROM_NAME" to "AOT",
            "GCP_PROJECT_ID" to "aot-project",
        ),
    )
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("capstone-server")
            buildArgs.add("--enable-http")
            buildArgs.add("--enable-https")
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(25))
                    vendor.set(JvmVendorSpec.GRAAL_VM)
                },
            )
        }
    }
}

tasks.named("nativeCompile") {
    onlyIf("Native image builds run in Docker (NATIVE_IMAGE_BUILD=true)") {
        System.getenv("NATIVE_IMAGE_BUILD") == "true"
    }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        googleJavaFormat()
    }
}
