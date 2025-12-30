package nadinee.passgenerator.data

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Pass(
    val id: Long = 0,
    val password: String,
    val isCurrent: Boolean = false,
    val createdAt: LocalDateTime
)