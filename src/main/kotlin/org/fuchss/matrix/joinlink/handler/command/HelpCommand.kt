package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.markdown

internal class HelpCommand(private val config: Config, private val commandGetter: () -> List<Command>) : Command() {
    override val name: String = "help"
    override val help: String = "shows this help message"

    /**
     * Show the help message.
     * @param[matrixBot] The bot to show the help message.
     * @param[roomId] The room to show the help message in.
     * @param[parameters] The parameters of the command.
     */
    override suspend fun execute(matrixBot: MatrixBot, roomId: RoomId, parameters: String) {
        var helpMessage = "This is the JoinLink Bot. You can use the following commands:\n"

        for (command in commandGetter()) {
            helpMessage += "\n* `!${config.prefix} ${command.name} - ${command.help}`"
        }

        matrixBot.room().sendMessage(roomId) { markdown(helpMessage) }
    }
}
