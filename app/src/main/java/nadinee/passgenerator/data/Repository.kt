package nadinee.passgenerator.data

import android.content.Context

object Repository {
    private var passId = 0L
    private val _passes = mutableListOf<Pass>()

    val passes: List<Pass> get() = _passes.sortedByDescending { it.createdAt }  // новые сверху!

    fun init(context: Context) {
        _passes.clear()
        _passes.addAll(Storage.loadPasses(context))
        passId = _passes.maxOfOrNull { it.id } ?: 0L
    }

    fun addPass(pass: Pass, context: Context) {
        val newPass = pass.copy(id = ++passId)
        _passes.add(newPass)
        Storage.savePasses(context, _passes)
    }

    fun updatePass(updatedPass: Pass, context: Context) {
        val index = _passes.indexOfFirst { it.id == updatedPass.id }
        if (index != -1) {
            _passes[index] = updatedPass
            Storage.savePasses(context, _passes)
        }
    }

    fun deletePass(id: Long, context: Context) {
        _passes.removeAll { it.id == id }
        Storage.savePasses(context, _passes)
    }

    // Удобный метод: сделать пароль текущим (сбрасываем у остальных)
    fun setAsCurrent(id: Long, context: Context) {
        _passes.forEachIndexed { index, pass ->
            _passes[index] = pass.copy(isCurrent = pass.id == id)
        }
        Storage.savePasses(context, _passes)
    }
    fun addPassAndReturn(pass: Pass, context: Context): Pass {
        val newPass = pass.copy(id = ++passId)
        _passes.add(newPass)
        Storage.savePasses(context, _passes)
        return newPass
    }
}