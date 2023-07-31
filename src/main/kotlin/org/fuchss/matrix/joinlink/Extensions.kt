package org.fuchss.matrix.joinlink

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room.message.MessageBuilder
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.EventSubscriber
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.serialization.events.fromClass
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal const val ADMIN_POWER_LEVEL = 100

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
 * Subscribe to a certain class of event. Note that you can only subscribe for events that are sent by an admin by default.
 * @param[subscriber] the function to invoke for the events
 * @param[listenNonUsers] whether you want to subscribe for events from non users
 * @see MatrixBot.subscribe
 */
inline fun <reified T : EventContent> MatrixBot.subscribe(listenNonUsers: Boolean = false, noinline subscriber: EventSubscriber<T>) {
    subscribe(T::class, subscriber, listenNonUsers)
}

/**
 * Get a state event from a room
 * @param[C] the type of the event [StateEventContent]
 * @param[roomId] the room to get the event from
 * @return the event
 */
suspend inline fun <reified C : StateEventContent> MatrixBot.getStateEvent(
    roomId: RoomId
): Result<C> {
    val type = contentMappings().state.fromClass(C::class).type
    @Suppress("UNCHECKED_CAST")
    return getStateEvent(type, roomId) as Result<C>
}

/**
 * Create a matrix.to link from a RoomId
 * @return the matrix.to link
 */
fun RoomId.matrixTo(): String = "https://matrix.to/#/${this.full}?via=${this.domain}"
