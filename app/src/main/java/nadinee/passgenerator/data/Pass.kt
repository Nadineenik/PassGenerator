package nadinee.passgenerator.data

import kotlinx.serialization.Serializable  // ← Добавь

@Serializable  // ← Добавь
data class Pass(
    val id: Long = 0,
    val name: String
)