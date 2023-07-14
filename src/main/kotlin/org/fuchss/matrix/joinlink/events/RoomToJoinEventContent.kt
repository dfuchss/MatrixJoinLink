package org.fuchss.matrix.joinlink.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.StateEventContent

@Serializable
data class RoomToJoinEventContent(
    @SerialName("room_to_join") val roomToJoin: String? = null
) : StateEventContent {
    companion object {
        const val ID = "org.fuchss.matrix.room_to_join"
    }
}
