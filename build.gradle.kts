plugins {
    // Alvo estrito em Kotlin 2.1 para a JVM
    kotlin("jvm") version "2.1.0"
}

group = "com.graphcode"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Biblioteca padrão de Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // --- DEPENDÊNCIAS DE TESTE (Sprint 1) ---
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")

    // Nota: Mais tarde, no Sprint 1, vamos injetar aqui as dependências do
    // Tree-sitter e do Spring Data Neo4j dentro deste ficheiro unificado.
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21" // Compilação estrita para Java 21 (JVM 21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}