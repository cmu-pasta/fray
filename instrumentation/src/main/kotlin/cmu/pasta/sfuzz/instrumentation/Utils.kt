package cmu.pasta.sfuzz.instrumentation

import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

object Utils {
    @OptIn(ExperimentalPathApi::class)
    fun prepareDebugFolder(folder: String) {
        val path = Paths.get("/tmp/out/$folder")
        path.deleteRecursively()
        path.createDirectories()
    }
}