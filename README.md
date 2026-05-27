# GraphCode-Reviewer

## Descrição
Sistema de Auditoria de Software Semântica e Multiagente Baseado em Grafos de Propriedade de Código (CPG) e Model Context Protocol (MCP) com Kotlin.

## Arquitetura
Este projeto utiliza a arquitetura **Ports & Adapters (Hexagonal)** para garantir o isolamento absoluto das regras de negócio (Core) face às frameworks e tecnologias de infraestrutura.

## Requisitos de Ambiente
- Java / JDK 21 (LTS)
- Gradle 8.x+

## Como Compilar e Executar
```bash
./gradlew build