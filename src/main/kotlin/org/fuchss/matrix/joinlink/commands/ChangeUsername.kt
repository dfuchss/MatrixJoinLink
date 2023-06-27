package org.fuchss.matrix.joinlink.commands

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.MatrixBot

internal suspend fun changeUsername(roomId: RoomId, matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.renameInRoom(roomId, newNameInRoom)
    }
}
