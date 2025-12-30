package nadinee.passgenerator.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object Storage {
    private const val FILE_PASSES = "passes.json"

    private val json = Json { prettyPrint = true }

    fun savePasses(context: Context, passes: List<Pass>) {
        val file = File(context.filesDir, FILE_PASSES)
        file.writeText(json.encodeToString(passes))
    }

    fun loadPasses(context: Context): List<Pass> {
        val file = File(context.filesDir, FILE_PASSES)
        return if (file.exists()) {
            json.decodeFromString(file.readText())
        } else emptyList()
    }
}