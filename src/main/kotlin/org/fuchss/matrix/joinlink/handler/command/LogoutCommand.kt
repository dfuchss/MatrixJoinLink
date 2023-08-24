package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.helper.isBotAdmin

internal class LogoutCommand(private val config: Config) : Command() {
    override val name: String = "logout"
    override val help: String = "quits the bot and logs out all sessions"

    /**
     * Quit the bot and logout all sessions.
     * @param[matrixBot] The bot to quit.
     * @param[sender] The sender of the command.
     * @param[roomId] The room to execute the command in.
     * @param[parameters] The parameters of the command.
     */
    override suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String) {
        if (!sender.isBotAdmin(config)) {
            matrixBot.room().sendMessage(roomId) { text("You are not an admin.") }
            return
        }

        matrixBot.quit(logout = true)
    }
}
