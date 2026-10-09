package com.graphcode.application.services

import com.graphcode.domain.models.config.GraphCodeConfig
import com.graphcode.ports.inbound.MigrationResult
import com.graphcode.ports.inbound.MigrationValidationUseCase
import com.graphcode.ports.outbound.ArchitectureValidationQueryPort
import org.springframework.stereotype.Service
import io.github.oshai.kotlinlogging.KotlinLogging

@Service
class MigrationValidationService(
    private val architectureValidationQueryPort: ArchitectureValidationQueryPort
) : MigrationValidationUseCase {

    private val log = KotlinLogging.logger {}

    override suspend fun assessMigrationEffort(config: GraphCodeConfig?): MigrationResult {
        val (violations, effortScore) = architectureValidationQueryPort.getViolations(config)

        return MigrationResult(
            totalViolations = violations.size,
            effortScore = effortScore,
            violations = violations,
            recommendation = if (violations.isEmpty()) "A arquitetura esta limpa e consistente!" else "Recomenda-se refatorar as dependencias invertidas para usar Interfaces (Ports)."
        )
    }
}
