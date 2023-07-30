package org.fuchss.matrix.joinlink.commands

import org.fuchss.matrix.joinlink.MatrixBot

/**
 * Change the username of the bot.
 * @param[matrixBot] The bot to change the username of.
 * @param[message] The message that triggered the command.
 */
internal suspend fun changeUsername(matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.rename(newNameInRoom)
    }
}
