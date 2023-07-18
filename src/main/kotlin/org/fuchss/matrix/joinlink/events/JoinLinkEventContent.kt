package org.fuchss.matrix.joinlink.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.EventType
import net.folivo.trixnity.core.model.events.StateEventContent
import org.fuchss.matrix.joinlink.Config
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class JoinLinkEventContent(
    @SerialName("joinlink_room") val joinlinkRoom: String? = null
) : StateEventContent {
    companion object {
        val ID = EventType(JoinLinkEventContent::class, "org.fuchss.matrix.joinlink")

        private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"

        @OptIn(ExperimentalEncodingApi::class)
        fun create(config: Config, roomId: RoomId?): JoinLinkEventContent {
            if (roomId == null) {
                return JoinLinkEventContent(null)
            }

            val secretKey = SecretKeySpec(config.encryptionKey.toByteArray(), KEY_ALGORITHM)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encrypted = cipher.doFinal(roomId.full.toByteArray())
            val encodedEncryptedRoom = Base64.encode(encrypted)
            return JoinLinkEventContent(encodedEncryptedRoom)
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun decrypt(config: Config, content: JoinLinkEventContent): RoomId? {
            if (content.joinlinkRoom == null) {
                return null
            }

            val secretKey = SecretKeySpec(config.encryptionKey.toByteArray(), KEY_ALGORITHM)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decodedEncryptedRoom = Base64.decode(content.joinlinkRoom)
            val decrypted = cipher.doFinal(decodedEncryptedRoom)
            return RoomId(String(decrypted))
        }
    }
}
