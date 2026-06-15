import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("com.diffplug.spotless") version "8.6.0"
    id("org.owasp.dependencycheck") version "12.1.0"
}

group = "com.capstone"
version = "1.0-SNAPSHOT"
description = "Server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
        vendor = JvmVendorSpec.IBM
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val springBootBom = platform(SpringBootPlugin.BOM_COORDINATES)
    implementation(springBootBom)
    developmentOnly(springBootBom)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("com.mailjet:mailjet-client:6.0.1")

    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.15.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.gson)

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("com.h2database:h2")
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

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("capstone-server.jar")
}

tasks.named<BootRun>("bootRun") {
    outputs.upToDateWhen { false }
}

tasks.named<BootRun>("bootTestRun") {
    outputs.upToDateWhen { false }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        googleJavaFormat()
    }
}
