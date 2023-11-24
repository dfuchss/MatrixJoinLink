package org.fuchss.matrix.joinlink.events

import net.folivo.trixnity.core.serialization.events.DefaultEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.EventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.createEventContentSerializerMappings
import net.folivo.trixnity.core.serialization.events.stateOf
import org.koin.dsl.module

private val joinLinkSerializationMapping =
    createEventContentSerializerMappings {
        setOf(
            stateOf<JoinLinkEventContent>(JoinLinkEventContent.ID.name),
            stateOf<RoomToJoinEventContent>(RoomToJoinEventContent.ID.name)
        )
    }

/**
 * Koin module for the joinlink events.
 */
val joinLinkModule =
    module {
        single<EventContentSerializerMappings> {
            DefaultEventContentSerializerMappings + joinLinkSerializationMapping
        }
    }
