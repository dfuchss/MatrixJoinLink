package org.fuchss.matrix.joinlink.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.StateEventContent

@Serializable
data class JoinLinkEventContent(
    @SerialName("joinlink_room") val joinlinkRoom: RoomId? = null
) : StateEventContent {
    companion object {
        const val ID = "org.fuchss.matrix.joinlink"
    }
}
