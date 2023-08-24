package org.fuchss.matrix.joinlink.helper

import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import org.fuchss.matrix.joinlink.Config
import org.fuchss.matrix.joinlink.MatrixBot

internal const val ADMIN_POWER_LEVEL = 100
internal const val MOD_POWER_LEVEL = 50

/**
 * Get the current permission level of the bot in a room
 * @param[roomId] the id to the room
 * @param[userId] the id of the user to get the permission level of (if null, the bot's own id is used)
 * @return the permission level of the bot
 */
suspend fun MatrixBot.powerLevel(roomId: RoomId, userId: UserId? = null): Int {
    val levels = getStateEvent<PowerLevelsEventContent>(roomId).getOrThrow()
    return levels.users[userId ?: this.self()] ?: levels.usersDefault
}

suspend fun MatrixBot.canInvite(roomId: RoomId, userId: UserId? = null): Boolean {
    val levels = getStateEvent<PowerLevelsEventContent>(roomId).getOrThrow()
    val levelToInvite = levels.invite
    val userLevel = levels.users[userId ?: this.self()] ?: levels.usersDefault
    return userLevel >= levelToInvite
}

suspend fun MatrixBot.canSendStateEvents(roomId: RoomId, userId: UserId? = null, stateEventType: EventType? = null): Boolean {
    val levels = getStateEvent<PowerLevelsEventContent>(roomId).getOrThrow()
    val levelToSendState = levels.events[stateEventType] ?: levels.stateDefault
    val userLevel = levels.users[userId ?: this.self()] ?: levels.usersDefault
    return userLevel >= levelToSendState
}

suspend fun MatrixBot.canSendMessages(roomId: RoomId, userId: UserId? = null, eventType: EventType? = null): Boolean {
    val levels = getStateEvent<PowerLevelsEventContent>(roomId).getOrThrow()
    val levelToSendMessages = levels.events[eventType] ?: levels.eventsDefault
    val userLevel = levels.users[userId ?: this.self()] ?: levels.usersDefault
    return userLevel >= levelToSendMessages
}

suspend fun UserId.isAdminInRoom(matrixBot: MatrixBot, roomId: RoomId): Boolean = matrixBot.powerLevel(roomId, this) >= ADMIN_POWER_LEVEL
suspend fun UserId.isModerator(matrixBot: MatrixBot, roomId: RoomId): Boolean = matrixBot.powerLevel(roomId, this) >= MOD_POWER_LEVEL
fun UserId.isBotAdmin(config: Config): Boolean = config.admins.contains(this.full)
