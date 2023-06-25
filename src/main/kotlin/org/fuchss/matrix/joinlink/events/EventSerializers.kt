package org.fuchss.matrix.joinlink.events

import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.serialization.events.BaseEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.SerializerMapping
import net.folivo.trixnity.core.serialization.events.SerializerMapping.Companion.of
import org.koin.dsl.module

private val joinLinkSerializationMapping = object : BaseEventContentSerializerMappings() {
    override val state: Set<SerializerMapping<out StateEventContent>> = setOf(
        of<JoinLinkEventContent>(JoinLinkEventContent.ID),
        of<RoomsToJoinEventContent>(RoomsToJoinEventContent.ID)
    )
}

val joinLinkModule = module {
    single<EventContentSerializerMappings> {
        DefaultEventContentSerializerMappings + joinLinkSerializationMapping
    }
}
