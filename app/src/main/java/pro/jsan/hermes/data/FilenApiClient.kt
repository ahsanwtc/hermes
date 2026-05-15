package pro.jsan.hermes.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val INGEST_HOSTS = listOf(
    "ingest.filen.io", "ingest.filen.net",
    "ingest.filen-1.net", "ingest.filen-2.net",
    "ingest.filen-3.net", "ingest.filen-4.net",
    "ingest.filen-5.net", "ingest.filen-6.net"
)
private const val GATEWAY = "https://gateway.filen.io"
private const val CHUNK_SIZE = 1_048_576 // 1 MiB

@Singleton
class FilenApiClient @Inject constructor(
    private val client: HttpClient,
    private val settings: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun ingestHost() = INGEST_HOSTS.random()

    private fun authHeader() = "Bearer ${settings.apiKey}"

    /**
     * Logs in with email + raw password.
     * Derives master key and login password via PBKDF2, fetches API key.
     * Stores apiKey, masterKey, and email in SettingsRepository.
     */
    suspend fun login(email: String, rawPassword: String) {
        // 1. Fetch salt
        val authInfo = client.post("$GATEWAY/v3/auth/info") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("email", email) }.toString())
        }.body<JsonObject>()
        val salt = authInfo["data"]!!.jsonObject["salt"]!!.jsonPrimitive.content

        // 2. Derive 512-bit key via PBKDF2-SHA512
        val spec = javax.crypto.spec.PBEKeySpec(rawPassword.toCharArray(), salt.toByteArray(), 200_000, 512)
        val derived = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            .generateSecret(spec).encoded
        val derivedHex = derived.joinToString("") { "%02x".format(it) }

        val masterKey = derivedHex.substring(0, derivedHex.length / 2)
        var loginPassword = derivedHex.substring(derivedHex.length / 2)
        loginPassword = java.security.MessageDigest.getInstance("SHA-512")
            .digest(loginPassword.toByteArray()).joinToString("") { "%02x".format(it) }

        // 3. Login
        val loginRes = client.post("$GATEWAY/v3/login") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("email", email)
                put("password", loginPassword)
                put("twoFactorCode", "XXXXXX")
                put("authVersion", 2)
            }.toString())
        }.body<JsonObject>()
        val apiKey = loginRes["data"]!!.jsonObject["apiKey"]!!.jsonPrimitive.content

        settings.apiKey = apiKey
        settings.masterKey = masterKey
        settings.email = email
    }

    /** Returns the UUID of the user's root folder */
    suspend fun getBaseFolder(): String {
        val res = client.get("$GATEWAY/v3/user/baseFolder") {
            header("Authorization", authHeader())
        }.body<JsonObject>()
        return res["data"]!!.jsonObject["uuid"]!!.jsonPrimitive.content
    }

    /** Lists a directory, returns map of name→uuid for subdirectories */
    suspend fun listDir(uuid: String): Map<String, String> {
        val res = client.post("$GATEWAY/v3/dir/content") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("uuid", uuid) }.toString())
        }.body<JsonObject>()
        val folders = res["data"]!!.jsonObject["folders"]!!.jsonArray
        // Note: folder names are encrypted; we match by uuid when traversing paths
        return folders.associate {
            val obj = it.jsonObject
            obj["uuid"]!!.jsonPrimitive.content to obj["uuid"]!!.jsonPrimitive.content
        }
    }

    /** Creates a directory under parentUuid, returns new uuid */
    suspend fun mkdir(name: String, parentUuid: String, masterKey: String): String {
        val uuid = UUID.randomUUID().toString()
        val encName = FilenCrypto.encryptMetadata(buildJsonObject { put("name", name) }.toString(), masterKey)
        client.post("$GATEWAY/v3/dir/create") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("uuid", uuid)
                put("name", encName)
                put("nameHashed", name.lowercase().hashCode().toString())
                put("parent", parentUuid)
            }.toString())
        }
        return uuid
    }

    /**
     * Resolves or creates the full cloud path (e.g. "/Phone/Camera"),
     * returning the UUID of the leaf directory.
     */
    suspend fun resolveCloudPath(path: String): String {
        val masterKey = settings.masterKey
        var currentUuid = getBaseFolder()
        val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
        for (segment in segments) {
            val children = listDirByName(currentUuid, masterKey)
            currentUuid = children[segment] ?: mkdir(segment, currentUuid, masterKey)
        }
        return currentUuid
    }

    /** Lists directory and decrypts names, returns map of decryptedName→uuid */
    private suspend fun listDirByName(uuid: String, masterKey: String): Map<String, String> {
        val res = client.post("$GATEWAY/v3/dir/content") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("uuid", uuid) }.toString())
        }.body<JsonObject>()
        val folders = res["data"]!!.jsonObject["folders"]!!.jsonArray
        return folders.associate {
            val obj = it.jsonObject
            val encName = obj["name"]!!.jsonPrimitive.content
            val decName = runCatching { decryptMetadataName(encName, masterKey) }.getOrDefault(encName)
            decName to obj["uuid"]!!.jsonPrimitive.content
        }
    }

    private fun decryptMetadataName(encrypted: String, masterKey: String): String {
        // Parse "002" + 12-char iv + base64(ciphertext+tag)
        if (!encrypted.startsWith("002")) return encrypted
        val iv = encrypted.substring(3, 15).toByteArray()
        val data = android.util.Base64.decode(encrypted.substring(15), android.util.Base64.NO_WRAP)
        val key = run {
            val spec = javax.crypto.spec.PBEKeySpec(masterKey.toCharArray(), masterKey.toByteArray(), 1, 256)
            javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
        }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(data)
        val obj = Json.parseToJsonElement(String(plain)).jsonObject
        return obj["name"]!!.jsonPrimitive.content
    }

    /**
     * Uploads a file to the given parent directory UUID.
     * Returns the cloud file UUID.
     */
    suspend fun uploadFile(
        name: String,
        parentUuid: String,
        bytes: ByteArray,
        mimeType: String,
        lastModified: Long
    ): String {
        val masterKey = settings.masterKey
        val fileUuid = UUID.randomUUID().toString()
        val fileKey = FilenCrypto.generateFileKey()
        val uploadKey = (1..32).map { ('a'..'z').random() }.joinToString("")

        val chunks = bytes.toList().chunked(CHUNK_SIZE).map { it.toByteArray() }
        var bucket = ""
        var region = ""

        chunks.forEachIndexed { index, chunk ->
            val encrypted = FilenCrypto.encryptChunk(chunk, fileKey)
            val hash = FilenCrypto.sha512Hex(encrypted)
            val host = ingestHost()
            val res = client.post("https://$host/v3/upload") {
                header("Authorization", authHeader())
                parameter("uuid", fileUuid)
                parameter("index", index)
                parameter("parent", parentUuid)
                parameter("uploadKey", uploadKey)
                parameter("hash", hash)
                setBody(encrypted)
            }.body<JsonObject>()
            if (index == 0) {
                bucket = res["data"]!!.jsonObject["bucket"]!!.jsonPrimitive.content
                region = res["data"]!!.jsonObject["region"]!!.jsonPrimitive.content
            }
        }

        val encMetadata = FilenCrypto.encryptFileMetadata(name, mimeType, bytes.size.toLong(), lastModified, fileKey, masterKey)
        val encName = FilenCrypto.encryptMetadata(buildJsonObject { put("name", name) }.toString(), masterKey)

        client.post("$GATEWAY/v3/upload/done") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("uuid", fileUuid)
                put("name", encName)
                put("nameHashed", name.lowercase().hashCode().toString())
                put("size", FilenCrypto.encryptMetadata(bytes.size.toString(), masterKey))
                put("chunks", chunks.size)
                put("mime", FilenCrypto.encryptMetadata(mimeType, masterKey))
                put("rm", (1..32).map { ('a'..'z').random() }.joinToString(""))
                put("metadata", encMetadata)
                put("version", 2)
                put("uploadKey", uploadKey)
            }.toString())
        }

        return fileUuid
    }
}
