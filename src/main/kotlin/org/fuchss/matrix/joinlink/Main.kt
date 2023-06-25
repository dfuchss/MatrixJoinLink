package org.fuchss.matrix.joinlink

import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.createDefaultModules
import net.folivo.trixnity.client.getEventId
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.getSender
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.rooms.CreateRoom
import net.folivo.trixnity.clientserverapi.model.rooms.DirectoryVisibility
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.Path.Companion.toOkioPath
import org.fuchss.matrix.joinlink.events.JoinLinkEventContent
import org.fuchss.matrix.joinlink.events.RoomsToJoinEventContent
import org.fuchss.matrix.joinlink.events.joinLinkModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

private const val ADMIN_POWER_LEVEL = 100

fun main() {
    runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val config = Config.load()

        val matrixClient = MatrixClient.login(
            baseUrl = Url(config.baseUrl),
            identifier = IdentifierType.User(config.username),
            password = config.password,
            repositoriesModule = createInMemoryRepositoriesModule(),
            mediaStore = OkioMediaStore(File("media").toOkioPath()),
            scope = scope,
            initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}"
        ) {
            modules = createDefaultModules() + joinLinkModule
        }.getOrThrow()

        val matrixBot = MatrixBot(matrixClient, config)
        matrixBot.subscribe { event -> handleTextMessage(event.getRoomId(), event.content, matrixBot, config) }
        matrixBot.subscribe { event -> handleEncryptedTextMessage(event, matrixClient, matrixBot, config) }
        matrixBot.subscribe<MemberEventContent> { event -> handleJoinsToMatrixJoinLinkRooms(event.getRoomId(), event.getSender(), event.content, matrixBot) }
        matrixBot.startBlocking()
    }
}

private suspend fun handleEncryptedTextMessage(event: Event<EncryptedEventContent>, matrixClient: MatrixClient, matrixBot: MatrixBot, config: Config) {
    val roomId = event.getRoomId() ?: return
    val eventId = event.getEventId() ?: return

    logger.debug("Waiting for decryption of {} ..", event)
    val decryptedEvent = matrixClient.room.getTimelineEvent(roomId, eventId).firstWithTimeout { it?.content != null }
    if (decryptedEvent != null) {
        logger.debug("Decryption of {} was successful", event)
    }

    if (decryptedEvent == null) {
        logger.error("Cannot decrypt event $event within the given time ..")
        return
    }

    val content = decryptedEvent.content?.getOrNull() ?: return
    if (content is RoomMessageEventContent.TextMessageEventContent) {
        handleTextMessage(roomId, content, matrixBot, config)
    }
}

private suspend fun handleTextMessage(roomId: RoomId?, content: RoomMessageEventContent.TextMessageEventContent, matrixBot: MatrixBot, config: Config) {
    if (roomId == null) {
        return
    }

    var message = content.body
    if (!message.startsWith("!${config.prefix}")) {
        return
    }

    message = message.substring("!${config.prefix}".length).trim()

    when (message.split(Regex(" "), 2)[0]) {
        "quit" -> matrixBot.quit()
        "help" -> help(roomId, matrixBot, config)
        "name" -> changeUsername(roomId, matrixBot, message)
        "link" -> link(roomId, matrixBot)
        "unlink" -> unlink(roomId, matrixBot)
    }
}

private suspend fun help(roomId: RoomId, matrixBot: MatrixBot, config: Config) {
    val helpMessage = """
        This is the MatrixJoinLink Bot. You can use the following commands:
        
        * `!${config.prefix} help - shows this help message`
        * `!${config.prefix} quit - quits the bot`
        * `!${config.prefix} name [NEW_NAME] - sets the display name of the bot to NEW_NAME (only for the room)`
        * `!${config.prefix} link - create a join link for the room`
        * `!${config.prefix} unlink - remove all join links for the room`
    """.trimIndent()

    matrixBot.room().sendMessage(roomId) { markdown(helpMessage) }
}

private suspend fun changeUsername(roomId: RoomId, matrixBot: MatrixBot, message: String) {
    val newNameInRoom = message.substring("name".length).trim()
    if (newNameInRoom.isNotBlank()) {
        matrixBot.renameInRoom(roomId, newNameInRoom)
    }
}

private suspend fun link(roomId: RoomId, matrixBot: MatrixBot) {
    logger.info("Requested Link for $roomId")

    val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomId).getOrNull()?.joinlinkRoom

    if (currentJoinLink != null) {
        matrixBot.room().sendMessage(roomId) { text("Link to share the Room: ${currentJoinLink.matrixTo()}") }
        return
    }

    // Create Join Room
    val joinLink = matrixBot.rooms().createRoom(
        visibility = DirectoryVisibility.PRIVATE,
        name = "Matrix Join Link $roomId",
        topic = "This is the Matrix Join Link Room to $roomId. You can leave the room :)",
        preset = CreateRoom.Request.Preset.PUBLIC,
        powerLevelContentOverride = PowerLevelsEventContent(
            users = mapOf(matrixBot.self() to ADMIN_POWER_LEVEL),
            events = mapOf(
                RoomsToJoinEventContent.ID to ADMIN_POWER_LEVEL,
                JoinLinkEventContent.ID to ADMIN_POWER_LEVEL
            ),
            eventsDefault = ADMIN_POWER_LEVEL,
            stateDefault = ADMIN_POWER_LEVEL
        )
    ).getOrThrow()

    logger.info("Create Room $joinLink for $roomId")
    val joinLinkEvent = JoinLinkEventContent(joinLink)
    matrixBot.sendStateEvent(roomId, joinLinkEvent)

    val roomsToJoinEvent = RoomsToJoinEventContent(listOf(roomId))
    matrixBot.sendStateEvent(joinLink, roomsToJoinEvent)

    matrixBot.room().sendMessage(roomId) { text("Link to share the Room: ${joinLink.matrixTo()}") }
}

private suspend fun unlink(roomId: RoomId, matrixBot: MatrixBot) {
    logger.info("Requested Unlink for $roomId")

    val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomId).getOrNull()?.joinlinkRoom

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
}

private suspend fun handleJoinsToMatrixJoinLinkRooms(roomId: RoomId?, userId: UserId?, memberEventContent: MemberEventContent, matrixBot: MatrixBot) {
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

    val roomsToJoin = matrixBot.getStateEvent<RoomsToJoinEventContent>(roomId).getOrNull() ?: return
    if (roomsToJoin.roomsToJoin.isNullOrEmpty()) {
        return
    }

    logger.info("Inviting $userId to rooms because of JoinEvent to $roomId")

    val message = "" +
        "You've reached a MatrixJoinRoom. I'll invite you to the rooms ..\n" +
        "If this does not work, please ask the person who gave you the link that they can invite you manually.\n\n" +
        "You can leave the room now :)"

    matrixBot.room().sendMessage(roomId) { text(message) }

    for (roomToJoin in roomsToJoin.roomsToJoin) {
        // Validate RoomsToJoin ..
        val currentJoinLink = matrixBot.getStateEvent<JoinLinkEventContent>(roomToJoin).getOrNull()?.joinlinkRoom
        if (currentJoinLink != roomId) {
            logger.error("I will not invite $userId to $roomToJoin because the link to $roomId is broken. Skipping ..")
            continue
        }

        try {
            matrixBot.rooms().inviteUser(roomToJoin, userId, reason = "Join via MatrixJoinLink").getOrThrow()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }
}
