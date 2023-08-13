package org.fuchss.matrix.joinlink

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.createDefaultModules
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.getEventId
import net.folivo.trixnity.client.getRoomId
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.repository.exposed.createExposedRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import okio.Path.Companion.toOkioPath
import org.fuchss.matrix.joinlink.events.joinLinkModule
import org.fuchss.matrix.joinlink.handler.command.ChangeUsernameCommand
import org.fuchss.matrix.joinlink.handler.command.Command
import org.fuchss.matrix.joinlink.handler.command.HelpCommand
import org.fuchss.matrix.joinlink.handler.command.LinkCommand
import org.fuchss.matrix.joinlink.handler.command.LogoutCommand
import org.fuchss.matrix.joinlink.handler.command.QuitCommand
import org.fuchss.matrix.joinlink.handler.command.UnlinkCommand
import org.fuchss.matrix.joinlink.handler.handleJoinsToMatrixJoinLinkRooms
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)
private lateinit var commands: List<Command>

/**
 * The main function to start the bot.
 */
fun main() {
    runBlocking {
        val config = Config.load()
        commands = listOf(HelpCommand(config) { commands }, QuitCommand(), LogoutCommand(), ChangeUsernameCommand(), LinkCommand(config), UnlinkCommand(config))

        val matrixClient = getMatrixClient(config)

        val matrixBot = MatrixBot(matrixClient, config)
        matrixBot.subscribe { event -> handleTextMessage(event.getRoomId(), event.content, matrixBot, config) }
        matrixBot.subscribe { event -> handleEncryptedTextMessage(event, matrixClient, matrixBot, config) }
        matrixBot.subscribe<MemberEventContent> { event -> handleJoinsToMatrixJoinLinkRooms(event, event.content, matrixBot, config) }

        val loggedOut = matrixBot.startBlocking()
        if (loggedOut) {
            // Cleanup database
            val databaseFiles = listOf(File(config.dataDirectory + "/database.mv.db"), File(config.dataDirectory + "/database.trace.db"))
            databaseFiles.filter { it.exists() }.forEach { it.delete() }
        }
    }
}

private suspend fun getMatrixClient(config: Config): MatrixClient {
    val existingMatrixClient = MatrixClient.fromStore(createRepositoriesModule(config), createMediaStore(config)) {
        modules = createDefaultModules() + joinLinkModule
    }.getOrThrow()
    if (existingMatrixClient != null) {
        return existingMatrixClient
    }

    val matrixClient = MatrixClient.login(
        baseUrl = Url(config.baseUrl),
        identifier = IdentifierType.User(config.username),
        password = config.password,
        repositoriesModule = createRepositoriesModule(config),
        mediaStore = createMediaStore(config),
        initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}"
    ) {
        modules = createDefaultModules() + joinLinkModule
    }.getOrThrow()

    return matrixClient
}

private suspend fun createRepositoriesModule(config: Config) =
    createExposedRepositoriesModule(database = Database.connect("jdbc:h2:${config.dataDirectory}/database;DB_CLOSE_DELAY=-1"))

private fun createMediaStore(config: Config) = OkioMediaStore(File(config.dataDirectory + "/media").toOkioPath())

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

    val command = message.split(Regex(" "), 2)[0]
    val parameters = message.substring(command.length).trim()

    commands.find { it.name == command }?.execute(matrixBot, roomId, parameters)
}
