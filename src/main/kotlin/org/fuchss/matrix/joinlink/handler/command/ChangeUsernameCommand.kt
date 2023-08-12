package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.MatrixBot

internal class ChangeUsernameCommand : Command() {
    override val name: String = "name"
    override val help: String = "[NEW_NAME] - sets the display name of the bot to NEW_NAME"

    /**
     * Change the username of the bot.
     * @param[matrixBot] The bot to change the username of.
     * @param[roomId] The room to execute the command in.
     * @param[parameters] the new name of the bot.
     */
    override suspend fun execute(matrixBot: MatrixBot, roomId: RoomId, parameters: String) {
        if (parameters.isNotBlank()) {
            matrixBot.rename(parameters)
        }
    }
}
