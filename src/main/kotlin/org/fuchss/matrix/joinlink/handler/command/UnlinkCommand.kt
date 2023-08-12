package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.decrypt
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.getStateEvent

internal class UnlinkCommand(private val config: Config) : Command() {
    override val name: String = "unlink"
    override val help: String = "remove all join links for the room"

    /**
     * Unlink a Matrix Join Link Room.
     * @param[matrixBot] The bot to handle the unlink request.
     * @param[roomId] The roomId of the unlink request.
     * @param[parameters] The parameters of the command.
     */
    override suspend fun execute(matrixBot: MatrixBot, roomId: RoomId, parameters: String) {
        logger.info("Requested Unlink for $roomId")

        val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomId).getOrNull()?.joinlinkRoom.decrypt(config)

        if (currentJoinLink == null) {
            matrixBot.room().sendMessage(roomId) { text("No Matrix Join Links available .. nothing to do ..") }
            return
        }

        val usersOfRoom = matrixBot.rooms().getMembers(currentJoinLink).getOrThrow()
        for (user in usersOfRoom) {
            if (user.sender.full == matrixBot.self().full || user.content.membership != Membership.JOIN) {
                continue
            }
            matrixBot.rooms().banUser(currentJoinLink, user.sender, reason = "Matrix Join Link invalidated").getOrThrow()
        }

        matrixBot.sendStateEvent(roomId, JoinLinkEventContent())
        matrixBot.rooms().leaveRoom(currentJoinLink, reason = "Matrix Join Link invalidated")

        matrixBot.room().sendMessage(roomId) { text("Unlinked the Room") }
    }
}
