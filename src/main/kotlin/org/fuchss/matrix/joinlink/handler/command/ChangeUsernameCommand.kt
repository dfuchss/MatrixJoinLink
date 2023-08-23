package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.fuchss.matrix.joinlink.MatrixBot

internal class ChangeUsernameCommand : Command() {
    override val name: String = "name"
    override val help: String = "{NEW_NAME} - sets the display name of the bot for this channel to NEW_NAME"

    /**
     * Change the username of the bot.
     * @param[matrixBot] The bot to change the username of.
     * @param[sender] The sender of the command.
     * @param[roomId] The room to execute the command in.
     * @param[parameters] the new name of the bot.
     */
    override suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String) {
        if (parameters.isNotBlank()) {
            matrixBot.renameInRoom(roomId, parameters)
        }
    }
}
