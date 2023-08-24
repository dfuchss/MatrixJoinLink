package org.fuchss.matrix.joinlink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
fun RoomId.matrixTo(): String = "https://matrix.to/#/${this.full}?via=${this.domain}"

/**
 * Extract a RoomId from a string. The string can be a matrix.to link or a room id.
 * @return the RoomId or null if the string is not a valid room id
 */
fun String.toInternalRoomIdOrNull(): RoomId? {
    var cleanedInput = this.trim()
    if (cleanedInput.startsWith("https://matrix.to/#/")) {
        cleanedInput = cleanedInput.removePrefix("https://matrix.to/#/")
        cleanedInput = cleanedInput.substringBefore("?")
    }

    if (cleanedInput.matches(Regex("^![a-zA-Z0-9]+:[a-zA-Z0-9.]+\$"))) {
        return RoomId(cleanedInput)
    }
    return null
}
