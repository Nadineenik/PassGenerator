package nadinee.passgenerator.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

object Repository {

    private var passId = 0L
    val passes = mutableStateListOf<Pass>()
    fun init(context: Context) {
        passes.clear()
        val loaded = Storage.loadPasses(context)
        passes.addAll(loaded.sortedByDescending { it.createdAt })
        passId = passes.maxOfOrNull { it.id } ?: 0L
    }
    fun addPassAndReturn(pass: Pass, context: Context): Pass {
        val newPass = pass.copy(id = ++passId)
        passes.add(0, newPass) // новые сверху
        Storage.savePasses(context, passes)
        return newPass
    }

    fun updatePass(updatedPass: Pass, context: Context) {
        val index = passes.indexOfFirst { it.id == updatedPass.id }
        if (index != -1) {
            passes[index] = updatedPass
            Storage.savePasses(context, passes)
        }
    }

    fun deletePass(id: Long, context: Context) {
        passes.removeAll { it.id == id }
        Storage.savePasses(context, passes)
    }

    fun setAsCurrent(id: Long, context: Context) {
        passes.replaceAll {
            it.copy(isCurrent = it.id == id)
        }
        Storage.savePasses(context, passes)
    }
}
