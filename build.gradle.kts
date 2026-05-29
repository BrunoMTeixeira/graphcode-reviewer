plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
}

kotlin {
    jvmToolchain(21)
}

group = "com.graphcode"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.springframework.data:spring-data-neo4j:7.2.2")
    implementation("io.github.tree-sitter:jtreesitter:0.24.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    // Neo4j & Testcontainers para testes de integração
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:neo4j:1.19.7")
    testImplementation("org.neo4j.driver:neo4j-java-driver:5.18.0")
    
    // Spring Boot Test Framework (necessário para @DataNeo4jTest funcionar out-of-the-box)
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j:3.2.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.4")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
