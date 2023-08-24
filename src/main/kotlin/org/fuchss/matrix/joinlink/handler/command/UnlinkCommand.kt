package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.events.RoomToJoinEventContent
import org.fuchss.matrix.joinlink.helper.canInvite
import org.fuchss.matrix.joinlink.helper.decrypt
import org.fuchss.matrix.joinlink.matrixTo
import org.fuchss.matrix.joinlink.toInternalRoomIdOrNull

internal class UnlinkCommand(private val config: Config) : Command() {
    override val name: String = "unlink"
    override val params: String = "[Link/ID to TargetRoom]"
    override val help: String = "remove all join links for the room"

    /**
     * Unlink a Matrix Join Link Room. If you provided an internal link to a room, the bot will use this room instead of the room the message was sent in.
     * @param[matrixBot] The bot to handle the unlink request.
     * @param[sender] The sender of the command.
     * @param[roomId] The roomId of the unlink request.
     * @param[parameters] The parameters of the command.
     */
    override suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String) {
        val providedRoomId = parameters.trim().toInternalRoomIdOrNull()
        val targetRoom = providedRoomId ?: roomId

        logger.info("Requested Unlink for $targetRoom")

        if (!matrixBot.canInvite(targetRoom, sender)) {
            matrixBot.room().sendMessage(roomId) {
                text("You are not allowed to invite users to this room (${targetRoom.matrixTo()}). Therefore, you cannot remove a join link.")
            }
            return
        }

        val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(targetRoom).getOrNull()?.joinlinkRoom.decrypt(config)

        if (currentJoinLink == null) {
            matrixBot.room().sendMessage(roomId) { text("No Matrix Join Links available .. nothing to do ..") }
            return
        }

        matrixBot.sendStateEvent(targetRoom, JoinLinkEventContent())
        matrixBot.sendStateEvent(currentJoinLink, RoomToJoinEventContent())

        val usersOfRoom = matrixBot.rooms().getMembers(currentJoinLink).getOrThrow()
        for (user in usersOfRoom) {
            if (user.sender.full == matrixBot.self().full || user.content.membership != Membership.JOIN) {
                continue
            }
            matrixBot.rooms().banUser(currentJoinLink, user.sender, reason = "Matrix Join Link invalidated").getOrThrow()
        }

        matrixBot.rooms().leaveRoom(currentJoinLink, reason = "Matrix Join Link invalidated")
        matrixBot.room().sendMessage(roomId) { text("Unlinked the Room") }
    }
}
