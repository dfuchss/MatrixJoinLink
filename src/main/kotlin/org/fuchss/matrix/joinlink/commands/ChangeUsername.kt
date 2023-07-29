package org.fuchss.matrix.joinlink.commands

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.MatrixBot

/**
 * Change the username of the bot in a room.
 * @param[roomId] The room to change the username in.
 * @param[matrixBot] The bot to change the username of.
 * @param[message] The message that triggered the command.
 */
internal suspend fun changeUsername(roomId: RoomId, matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.renameInRoom(roomId, newNameInRoom)
    }
}
