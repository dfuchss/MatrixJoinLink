package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.MatrixBot

internal class QuitCommand : Command() {
    override val name: String = "quit"
    override val help: String = "quits the bot"

    /**
     * Quit the bot.
     * @param[matrixBot] The bot to quit.
     * @param[roomId] The room to execute the command in.
     * @param[parameters] The parameters of the command.
     */
    override suspend fun execute(matrixBot: MatrixBot, roomId: RoomId, parameters: String) {
        matrixBot.quit()
    }
}
