package org.fuchss.matrix.joinlink.commands

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import org.fuchss.matrix.joinlink.ADMIN_POWER_LEVEL
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot
import org.fuchss.matrix.joinlink.decrypt
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.events.RoomToJoinEventContent
import org.fuchss.matrix.joinlink.getStateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

private val recentHandledJoinEvents = ConcurrentHashMap<String, Long>()

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

internal suspend fun handleJoinsToMatrixJoinLinkRooms(
    eventId: EventId?,
    originTimestamp: Long?,
    roomId: RoomId?,
    userId: UserId?,
    memberEventContent: MemberEventContent,
    matrixBot: MatrixBot,
    config: Config
) {
    if (roomId == null || userId == null) {
        return
    }

    // Only interested in Joins
    if (memberEventContent.membership != Membership.JOIN) {
        return
    }

    val permissionOfBot = matrixBot.permissionLevel(roomId)
    if (permissionOfBot < ADMIN_POWER_LEVEL) {
        logger.debug("Skipping MemberEvent in {} because it's not a bot's room", roomId)
        return
    }

    // Cleanup recent list
    val now = Clock.System.now()
    recentHandledJoinEvents.toMap().filter { now - Instant.fromEpochMilliseconds(it.value) > 20.seconds }.forEach { recentHandledJoinEvents.remove(it.key) }

    handleValidJoinEvent(
        eventId?.full ?: "$roomId-$userId",
        originTimestamp ?: now.toEpochMilliseconds(),
        roomId,
        userId,
        matrixBot,
        config
    )
}

private suspend fun handleValidJoinEvent(
    eventId: String,
    originTimestamp: Long,
    roomId: RoomId,
    userId: UserId,
    matrixBot: MatrixBot,
    config: Config
) {
    val roomToJoinState = matrixBot.getStateEvent<RoomToJoinEventContent>(roomId).getOrNull() ?: return
    if (roomToJoinState.roomToJoin.isNullOrEmpty()) {
        return
    }

    if (recentHandledJoinEvents.putIfAbsent(eventId, originTimestamp) != null) {
        logger.debug("Skipping MemberEvent {} for user {} in {} because it was already handled", eventId, userId, roomId)
        return
    }

    logger.info("Inviting $userId to rooms because of JoinEvent to $roomId")

    val message = "" +
        "You've reached a MatrixJoinRoom. I'll invite you to the rooms ..\n" +
        "If this does not work, please ask the person who gave you the link that they can invite you manually.\n\n" +
        "You can leave the room now :)"

    matrixBot.room().sendMessage(roomId) { text(message) }

    val roomToJoin = roomToJoinState.roomToJoin.decrypt(config)
    if (roomToJoin == null) {
        logger.debug("Skipping MemberEvent {} for user {} in {} because the link is broken", eventId, userId, roomId)
        return
    }

    // Validate RoomsToJoin ..
    val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomToJoin).getOrNull()?.joinlinkRoom.decrypt(config)
    if (currentJoinLink != roomId) {
        logger.error("I will not invite $userId to $roomToJoin because the link to $roomId is broken. Skipping ..")
        return
    }

    try {
        matrixBot.rooms().inviteUser(roomToJoin, userId, reason = "Join via MatrixJoinLink").getOrThrow()
    } catch (e: Exception) {
        logger.error(e.message, e)
    }
}
