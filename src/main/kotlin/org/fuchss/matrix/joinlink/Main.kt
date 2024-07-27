package org.fuchss.matrix.joinlink

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.createDefaultTrixnityModules
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.command.ChangeUsernameCommand
import org.fuchss.matrix.bots.command.Command
import org.fuchss.matrix.bots.command.HelpCommand
import org.fuchss.matrix.bots.command.LogoutCommand
import org.fuchss.matrix.bots.command.QuitCommand
import org.fuchss.matrix.bots.helper.createMediaStore
import org.fuchss.matrix.bots.helper.createRepositoriesModule
import org.fuchss.matrix.bots.helper.handleCommand
import org.fuchss.matrix.bots.helper.handleEncryptedCommand
import org.fuchss.matrix.joinlink.events.joinLinkModule
import org.fuchss.matrix.joinlink.handler.command.LinkCommand
import org.fuchss.matrix.joinlink.handler.command.UnlinkCommand
import org.fuchss.matrix.joinlink.handler.handleJoinsToMatrixJoinLinkRooms
import java.io.File
import kotlin.random.Random

private lateinit var commands: List<Command>

/**
 * The main function to start the bot.
 */
fun main() {
    runBlocking {
        val config = Config.load()
        commands =
            listOf(
                HelpCommand(config, "JoinLink") {
                    commands
                },
                QuitCommand(config),
                LogoutCommand(config),
                ChangeUsernameCommand(),
                LinkCommand(config),
                UnlinkCommand(config)
            )

        val matrixClient = getMatrixClient(config)

        val matrixBot = MatrixBot(matrixClient, config)
        matrixBot.subscribeContent { event -> handleCommand(commands, event, matrixBot, config) }
        matrixBot.subscribeContent { event -> handleEncryptedCommand(commands, event, matrixBot, config) }
        matrixBot.subscribeContent<MemberEventContent> { event -> handleJoinsToMatrixJoinLinkRooms(event, event.content, matrixBot, config) }

        val loggedOut = matrixBot.startBlocking()
        if (loggedOut) {
            // Cleanup database
            val databaseFiles = listOf(File(config.dataDirectory + "/database.mv.db"), File(config.dataDirectory + "/database.trace.db"))
            databaseFiles.filter { it.exists() }.forEach { it.delete() }
        }
    }
}

private suspend fun getMatrixClient(config: Config): MatrixClient {
    val existingMatrixClient =
        MatrixClient.fromStore(createRepositoriesModule(config), createMediaStore(config)) {
            modules = createDefaultTrixnityModules() + joinLinkModule
        }.getOrThrow()
    if (existingMatrixClient != null) {
        return existingMatrixClient
    }

    val matrixClient =
        MatrixClient.login(
            baseUrl = Url(config.baseUrl),
            identifier = IdentifierType.User(config.username),
            password = config.password,
            repositoriesModule = createRepositoriesModule(config),
            mediaStore = createMediaStore(config),
            initialDeviceDisplayName = "${MatrixBot::class.java.`package`.name}-${Random.Default.nextInt()}"
        ) {
            modules = createDefaultTrixnityModules() + joinLinkModule
        }.getOrThrow()

    return matrixClient
}
