package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.fuchss.matrix.joinlink.MatrixBot
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal abstract class Command {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    abstract val name: String
    abstract val help: String

    /**
     * Execute the command.
     * @param[matrixBot] The bot to execute the command.
     * @param[sender] The sender of the command.
     * @param[roomId] The room to execute the command in.
     * @param[parameters] The parameters of the command.
     */
    internal abstract suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String)
}
