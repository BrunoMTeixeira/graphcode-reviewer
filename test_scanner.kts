import java.io.File
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

val supportedExtensions = setOf("kt", "kts", "java", "py", "js", "jsx", "ts", "c", "cpp", "hpp", "h", "cc", "cxx")
val ignoredDirectories = setOf(".git", ".idea", ".gradle", "build", "node_modules", "target", "out")

val rootDir = File("C:\\Users\\btmic\\OneDrive\\Desktop\\poc-code-review\\poc-code-review\\src")
println("Exists: ${rootDir.exists()}")
println("IsDir: ${rootDir.isDirectory}")

Files.walk(rootDir.toPath()).use { stream ->
    stream
        .filter { path ->
            val isDirIgnored = path.none { ignoredDirectories.contains(it.name) }
            val isSupportedFile = path.isRegularFile() && supportedExtensions.contains(path.extension.lowercase())
            isDirIgnored && isSupportedFile
        }
        .forEach { println(it) }
}
