package org.fuchss.matrix.joinlink

import net.folivo.trixnity.core.model.RoomId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CryptoTest {
    @Test
    fun testEnDeCrypt() {
        val config = Config("prefix", "test", "user", "password", listOf<String>(), "test123")
        val roomId = RoomId("!test:localhost")

        val encryptedRoomId = roomId.encrypt(config)
        val decryptedRoomId = encryptedRoomId.decrypt(config)

        assertEquals(roomId, decryptedRoomId)
    }
}
