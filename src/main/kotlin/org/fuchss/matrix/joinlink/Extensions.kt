package org.fuchss.matrix.joinlink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CanonicalAliasEventContent
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val MATRIX_TO_PREFIX = "https://matrix.to/#/"

/**
 * Same as [Flow.first] but with a defined timeout that leads to null if reached.
 * @param predicate a predicate to filter the results of [Flow.first]
 * @return the result of [Flow.first] or null
 */
suspend fun <T> Flow<T>.firstWithTimeout(timeout: Duration = 3000.milliseconds, predicate: suspend (T) -> Boolean): T? {
    val that = this
    return withTimeoutOrNull(timeout) { that.first { predicate(it) } }
}

/**
 * Format a markdown message and send it using a [MessageBuilder]
 * @param[markdown] the plain Markdown text
 */
fun MessageBuilder.markdown(markdown: String) {
    val document = Parser.builder().build().parse(markdown)
    val html = HtmlRenderer.builder().build().render(document)
    text(markdown, format = "org.matrix.custom.html", formattedBody = html)
}

/**
 * Create a matrix.to link from a RoomId
 * @return the matrix.to link
 */
fun RoomId.matrixTo(): String = "$MATRIX_TO_PREFIX${this.full}?via=${this.domain}"

/**
 * Indicates if a string is a valid RoomId (syntax)
 */
fun String.syntaxOfRoomId(): Boolean {
    var cleanedInput = this.trim()
    if (cleanedInput.startsWith(MATRIX_TO_PREFIX)) {
        cleanedInput = cleanedInput.removePrefix(MATRIX_TO_PREFIX)
        cleanedInput = cleanedInput.substringBefore("?")
    }
    return cleanedInput.matches(Regex("^![a-zA-Z0-9]+:[a-zA-Z0-9.]+\$")) || cleanedInput.matches(Regex("^#[a-zA-Z0-9_-]+:[a-zA-Z0-9._-]+\$"))
}

/**
 * Extract a RoomId from a string. The string can be a matrix.to link or a room id.
 * @return the RoomId or null if the string is not a valid room id
 */
suspend fun String.toInternalRoomIdOrNull(matrixBot: MatrixBot): RoomId? {
    var cleanedInput = this.trim()
    if (cleanedInput.startsWith(MATRIX_TO_PREFIX)) {
        cleanedInput = cleanedInput.removePrefix(MATRIX_TO_PREFIX)
        cleanedInput = cleanedInput.substringBefore("?")
    }

    if (cleanedInput.startsWith("#")) {
        // Alias RoomId
        return matrixBot.resolvePublicRoomIdOrNull(cleanedInput)
    }

    if (cleanedInput.matches(Regex("^![a-zA-Z0-9]+:[a-zA-Z0-9.]+\$"))) {
        return RoomId(cleanedInput)
    }
    return null
}

suspend fun MatrixBot.resolvePublicRoomIdOrNull(publicRoomAlias: String): RoomId? {
    val roomAlias = RoomAliasId(publicRoomAlias)

    val allKnownRooms = rooms().getJoinedRooms().getOrThrow()
    for (room in allKnownRooms) {
        val aliasState = getStateEvent<CanonicalAliasEventContent>(room).getOrNull() ?: continue
        if (aliasState.alias == roomAlias) {
            return room
        }
        if (roomAlias in (aliasState.aliases ?: emptySet())) {
            return room
        }
    }
    return null
}
