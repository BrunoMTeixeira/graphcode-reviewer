plugins {
    kotlin("jvm") version "2.1.0"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.graphcode"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Core do Kotlin & Coroutines para concorrência assíncrona real
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Spring Boot Starter Web para expor APIs e Webhooks
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Data Neo4j para persistência relacional do CPG
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")

    // Bibliotecas de serialização segura de JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // JUnit 5 para os teus testes unitários locais
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Testcontainers para Neo4j em Docker nos testes de integração
    testImplementation("org.testcontainers:neo4j:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
}

kotlin {
    jvmToolchain(21) // Garante a compilação estrita sob a JVM 21
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}