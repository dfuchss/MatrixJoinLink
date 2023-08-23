package org.fuchss.matrix.joinlink.handler.command

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.fuchss.matrix.joinlink.ADMIN_POWER_LEVEL
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.decrypt
import org.fuchss.matrix.joinlink.encrypt
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.events.RoomToJoinEventContent
import org.fuchss.matrix.joinlink.getStateEvent
import org.fuchss.matrix.joinlink.matrixTo
import org.fuchss.matrix.joinlink.toInternalRoomIdOrNull

internal class LinkCommand(private val config: Config) : Command() {
    override val name: String = "link"
    override val help: String = "[Optional Internal Link to Room] {Readable Name of Link} - create a join link for the room"

    /**
     * Handle a link request. If the message originates from a user that is authorized, the bot tries to create a JoinLinkRoom (or uses an existing one).
     * If you provided an internal link to a room, the bot will use this room instead of the room the message was sent in.
     * @param[matrixBot] The bot to handle the link request.
     * @param[sender] The sender of the link request.
     * @param[roomId] The roomId of the link request.
     * @param[parameters] The parameters of the link request.
     */
    override suspend fun execute(matrixBot: MatrixBot, sender: UserId, roomId: RoomId, parameters: String) {
        val providedRoomId = parameters.split(" ").first().toInternalRoomIdOrNull()
        val targetRoom = providedRoomId ?: roomId

        logger.info("Requested Link for $targetRoom")

        val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(targetRoom).getOrNull()?.joinlinkRoom.decrypt(config)

        if (currentJoinLink != null) {
            matrixBot.room().sendMessage(roomId) { text("Link to share the Room (`${targetRoom.matrixTo()}`): ${currentJoinLink.matrixTo()}") }
            return
        }

        val nameOfLink = if (providedRoomId != null) parameters.substringAfter(" ").trim() else parameters.trim()

        if (nameOfLink.isBlank()) {
            matrixBot.room().sendMessage(roomId) { text("Please provide a name for the link") }
            return
        }

        val levels = matrixBot.getStateEvent<PowerLevelsEventContent>(targetRoom).getOrThrow()

        if (!matrixBot.canInvite(targetRoom, sender)) {
            matrixBot.room().sendMessage(roomId) { text("You are not allowed to invite users to this room (`${targetRoom.matrixTo()}`)") }
            return
        }

        if (!matrixBot.canInvite(targetRoom)) {
            matrixBot.room().sendMessage(roomId) { text("I am not allowed to invite users to this room (`${targetRoom.matrixTo()}`)") }
            return
        }

        if (!matrixBot.canSendStateEvents(targetRoom)) {
            matrixBot.room().sendMessage(roomId) { text("I am not allowed to send state events to this room (`${targetRoom.matrixTo()}`)") }
            return
        }

        // Create Join Room
        val joinLink = matrixBot.rooms().createRoom(
            visibility = DirectoryVisibility.PRIVATE,
            name = "Matrix Join Link '$nameOfLink'",
            topic = "This is the Matrix Join Link Room called '$nameOfLink'. You can leave the room :)",
            preset = CreateRoom.Request.Preset.PUBLIC,
            powerLevelContentOverride = PowerLevelsEventContent(
                users = mapOf(matrixBot.self() to ADMIN_POWER_LEVEL),
                events = mapOf(
                    RoomToJoinEventContent.ID to ADMIN_POWER_LEVEL,
                    JoinLinkEventContent.ID to ADMIN_POWER_LEVEL
                ),
                eventsDefault = ADMIN_POWER_LEVEL,
                stateDefault = ADMIN_POWER_LEVEL
            )
        ).getOrThrow()

        // Set History Visibility to Joined to prevent others from seeing too much history
        matrixBot.rooms().sendStateEvent(joinLink, HistoryVisibilityEventContent(HistoryVisibilityEventContent.HistoryVisibility.JOINED)).getOrThrow()

        logger.info("Create Room $joinLink for $targetRoom")
        val joinLinkEvent = JoinLinkEventContent(joinLink.encrypt(config))
        matrixBot.sendStateEvent(targetRoom, joinLinkEvent)

        val roomsToJoinEvent = RoomToJoinEventContent(targetRoom.encrypt(config))
        matrixBot.sendStateEvent(joinLink, roomsToJoinEvent)

        matrixBot.room().sendMessage(roomId) { text("Link to share the Room: ${joinLink.matrixTo()}") }
    }
}
