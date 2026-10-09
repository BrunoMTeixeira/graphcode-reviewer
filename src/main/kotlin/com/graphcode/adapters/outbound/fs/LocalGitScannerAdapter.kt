package com.graphcode.adapters.outbound.fs

import com.graphcode.ports.outbound.DirectoryScannerPort
import com.graphcode.domain.models.ScannedFile
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

@Component
class LocalGitScannerAdapter : DirectoryScannerPort {

    // Extensões suportadas pelo nosso Extrator TreeSitter
    private val supportedExtensions = setOf(
        "kt", "kts", "java", "py", "js", "jsx", "ts", "tsx", "c", "cpp", "hpp", "h", "cc", "cxx",
        "cs", "go", "rb", "php", "rs", "scala", "swift", "sh", "bash", "html", "htm", "css",
        "json", "yml", "yaml", "toml", "hs", "jl", "ml", "mli", "nim", "erl", "hrl", "lua",
        "pl", "pm", "vue", "svelte", "md", "tex", "rkt", "scm", "ss", "pas", "agda", "pgn"
    )
    
    // Pastas a ignorar para não poluir o parser
    private val ignoredDirectories = setOf(".git", ".idea", ".gradle", "build", "node_modules", "target", "out")

    override fun scanDirectory(directoryPath: String): List<ScannedFile> {
        val rootDir = File(directoryPath)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return emptyList()
        }

        val scannedFiles = mutableListOf<ScannedFile>()

        Files.walk(rootDir.toPath()).use { stream ->
            stream
                .filter { path ->
                    val isDirIgnored = path.none { ignoredDirectories.contains(it.name) }
                    val isSupportedFile = path.isRegularFile() && supportedExtensions.contains(path.extension.lowercase())
                    isDirIgnored && isSupportedFile
                }
                .forEach { path ->
                    try {
                        val content = path.readText(Charsets.UTF_8)
                        // Guardar caminhos relativos ao invés de absolutos para padronização
                        val relativePath = rootDir.toPath().relativize(path).toString().replace("\\", "/")
                        scannedFiles.add(ScannedFile(relativePath, content))
                    } catch (e: Exception) {
                        // Ignorar falhas de leitura de ficheiros individuais
                    }
                }
        }

        return scannedFiles
    }
}
