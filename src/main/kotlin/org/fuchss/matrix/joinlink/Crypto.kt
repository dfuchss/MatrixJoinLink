package org.fuchss.matrix.joinlink

import net.folivo.trixnity.core.model.RoomId
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val ENCRYPTION_ALGORITHM = "AES"
private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val CIPHER_NAME = "AES/GCM/NoPadding"
private val logger = LoggerFactory.getLogger("org.fuchss.matrix.joinlink.Crypto")

fun RoomId.encrypt(config: Config): String = encryptRoomId(config, this)

private val random = SecureRandom()

@OptIn(ExperimentalEncodingApi::class)
private fun encryptRoomId(config: Config, roomId: RoomId): String {
    val salt = ByteArray(16)
    random.nextBytes(salt)

    val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
    val pbeKeySpec: KeySpec = PBEKeySpec(config.encryptionKey.toCharArray(), salt, 1000, 256)
    val secretKey = factory.generateSecret(pbeKeySpec)
    val secret = SecretKeySpec(secretKey.encoded, ENCRYPTION_ALGORITHM)

    val cipher = Cipher.getInstance(CIPHER_NAME)
    val iv = ByteArray(cipher.blockSize)
    random.nextBytes(iv)

    val gcmParameterSpec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secret, gcmParameterSpec)

    val encrypted = cipher.doFinal(roomId.full.toByteArray())
    return "${Base64.encode(salt)}|${Base64.encode(iv)}|${Base64.encode(encrypted)}"
}

fun String?.decrypt(config: Config): RoomId? = decryptRoomId(config, this)

@OptIn(ExperimentalEncodingApi::class)
private fun decryptRoomId(config: Config, content: String?): RoomId? {
    if (content == null) return null

    return try {
        val (salt, iv, encryptedContent) = content.split("|").map { Base64.decode(it) }

        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val pbeKeySpec: KeySpec = PBEKeySpec(config.encryptionKey.toCharArray(), salt, 1000, 256)
        val secretKey = factory.generateSecret(pbeKeySpec)
        val secret = SecretKeySpec(secretKey.encoded, ENCRYPTION_ALGORITHM)

        val cipher = Cipher.getInstance(CIPHER_NAME)
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv))

        val decrypted = cipher.doFinal(encryptedContent)
        RoomId(String(decrypted))
    } catch (e: Exception) {
        logger.error(e.message, e)
        null
    }
}
