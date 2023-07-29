package org.fuchss.matrix.joinlink.commands

import net.folivo.trixnity.core.model.RoomId
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.markdown

/**
 * Show the help message.
 * @param[roomId] The room to show the help message in.
 * @param[matrixBot] The bot to show the help message.
 * @param[config] The config to use.
 */
internal suspend fun help(roomId: RoomId, matrixBot: MatrixBot, config: Config) {
    val helpMessage = """
        This is the MatrixJoinLink Bot. You can use the following commands:
        
        * `!${config.prefix} help - shows this help message`
        * `!${config.prefix} quit - quits the bot`
        * `!${config.prefix} name [NEW_NAME] - sets the display name of the bot to NEW_NAME (only for the room)`
        * `!${config.prefix} link [Readable Name of Link] - create a join link for the room`
        * `!${config.prefix} unlink - remove all join links for the room`
    """.trimIndent()

    matrixBot.room().sendMessage(roomId) { markdown(helpMessage) }
}
