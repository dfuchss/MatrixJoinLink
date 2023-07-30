package org.fuchss.matrix.joinlink.commands

import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.RoomId
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

/**
 * Handle a link request. If the message originates from a user that is authorized, the bot tries to create a JoinLinkRoom (or uses an existing one).
 * @param[roomId] The roomId of the link request.
 * @param[matrixBot] The bot to handle the link request.
 * @param[config] The config to use.
 * @param[message] The message of the link request.
 */
internal suspend fun link(roomId: RoomId, matrixBot: MatrixBot, config: Config, message: String) {
    logger.info("Requested Link for $roomId")

    val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomId).getOrNull()?.joinlinkRoom.decrypt(config)

    if (currentJoinLink != null) {
        matrixBot.room().sendMessage(roomId) { text("Link to share the Room: ${currentJoinLink.matrixTo()}") }
        return
    }

    val nameOfLink = message.substring("link".length).trim()

    if (nameOfLink.isBlank()) {
        matrixBot.room().sendMessage(roomId) { text("Please provide a name for the link") }
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

    logger.info("Create Room $joinLink for $roomId")
    val joinLinkEvent = JoinLinkEventContent(joinLink.encrypt(config))
    matrixBot.sendStateEvent(roomId, joinLinkEvent)

    val roomsToJoinEvent = RoomToJoinEventContent(roomId.encrypt(config))
    matrixBot.sendStateEvent(joinLink, roomsToJoinEvent)

    matrixBot.room().sendMessage(roomId) { text("Link to share the Room: ${joinLink.matrixTo()}") }
}
