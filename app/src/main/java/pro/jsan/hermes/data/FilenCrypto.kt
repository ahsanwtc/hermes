package pro.jsan.hermes.data

import android.security.keystore.KeyProperties
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object FilenCrypto {

    private val rng = SecureRandom()

    private fun randomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    /** PBKDF2-SHA512, 1 iteration, 256-bit — used to transform master key before metadata encryption */
    private fun pbkdf2(password: String, salt: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 1, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
    }

    /**
     * Encrypts metadata string (e.g. JSON) with the master key.
     * Output format: "002" + 12-char nonce + base64(ciphertext + authTag)
     */
    fun encryptMetadata(metadata: String, masterKey: String): String {
        val iv = randomString(12).toByteArray()
        val key = pbkdf2(masterKey, masterKey)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(metadata.toByteArray())
        return "002${String(iv)}${Base64.encodeToString(encrypted, Base64.NO_WRAP)}"
    }

    /**
     * Generates a random 32-char file encryption key.
     * Encrypts raw bytes in 1MiB chunks.
     * Each chunk: 12-byte nonce + ciphertext + 16-byte authTag
     */
    fun generateFileKey(): String = randomString(32)

    fun encryptChunk(data: ByteArray, fileKey: String): ByteArray {
        val iv = randomString(12).toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey.toByteArray(), KeyProperties.KEY_ALGORITHM_AES), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun sha512Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-512").digest(data)
            .joinToString("") { String.format("%02x", it.toInt() and 0xFF) }

    /** Builds the file metadata JSON and encrypts it */
    fun encryptFileMetadata(
        name: String,
        mimeType: String,
        size: Long,
        lastModified: Long,
        fileKey: String,
        masterKey: String
    ): String {
        val json = buildJsonObject {
            put("name", name)
            put("size", size)
            put("mime", mimeType)
            put("key", fileKey)
            put("lastModified", lastModified / 1000) // seconds
        }.toString()
        return encryptMetadata(json, masterKey)
    }
}
