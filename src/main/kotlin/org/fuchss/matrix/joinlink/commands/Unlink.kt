package org.fuchss.matrix.joinlink.commands

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.decrypt
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.getStateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

internal suspend fun unlink(roomId: RoomId, config: Config, matrixBot: MatrixBot) {
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
