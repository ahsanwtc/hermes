package pro.jsan.hermes.data

import io.ktor.client.HttpClient
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
    private fun authHeader() = "Bearer ${settings.apiKey}"
    private fun ingestHost() = INGEST_HOSTS.random()

    private fun nameHash(name: String): String {
        // Filen authVersion 2: SHA1(SHA512(name.toLowerCase()))
        val sha512hex = java.security.MessageDigest.getInstance("SHA-512")
            .digest(name.lowercase().toByteArray())
            .joinToString("") { String.format("%02x", it.toInt() and 0xFF) }
        return java.security.MessageDigest.getInstance("SHA-1")
            .digest(sha512hex.toByteArray())
            .joinToString("") { String.format("%02x", it.toInt() and 0xFF) }
    }

    private suspend fun getJson(url: String): JsonObject =
        Json.parseToJsonElement(client.get(url) {
            header("Authorization", authHeader())
        }.bodyAsText()).jsonObject

    private suspend fun postJson(url: String, body: JsonObject, auth: Boolean = true): JsonObject =
        Json.parseToJsonElement(client.post(url) {
            if (auth) header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.bodyAsText()).jsonObject

    suspend fun login(email: String, rawPassword: String, twoFactorCode: String = "XXXXXX") {
        val authInfo = postJson("$GATEWAY/v3/auth/info", buildJsonObject { put("email", email) }, auth = false)
        check(authInfo["status"]?.jsonPrimitive?.booleanOrNull == true) {
            authInfo["message"]?.jsonPrimitive?.contentOrNull ?: "auth/info failed"
        }
        val salt = authInfo["data"]!!.jsonObject["salt"]!!.jsonPrimitive.content

        val spec = javax.crypto.spec.PBEKeySpec(rawPassword.toCharArray(), salt.toByteArray(), 200_000, 512)
        val derived = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            .generateSecret(spec).encoded
        val derivedHex = derived.joinToString("") { "%02x".format(it) }
        val masterKey = derivedHex.substring(0, derivedHex.length / 2)
        var loginPassword = derivedHex.substring(derivedHex.length / 2)
        loginPassword = java.security.MessageDigest.getInstance("SHA-512")
            .digest(loginPassword.toByteArray()).joinToString("") { "%02x".format(it) }

        val loginRes = postJson("$GATEWAY/v3/login", buildJsonObject {
            put("email", email)
            put("password", loginPassword)
            put("twoFactorCode", twoFactorCode)
            put("authVersion", 2)
        }, auth = false)
        check(loginRes["status"]?.jsonPrimitive?.booleanOrNull == true) {
            loginRes["message"]?.jsonPrimitive?.contentOrNull ?: "Login failed"
        }
        settings.apiKey = loginRes["data"]!!.jsonObject["apiKey"]!!.jsonPrimitive.content
        settings.masterKey = masterKey
        settings.email = email
    }

    private suspend fun getBaseFolder(): String {
        val res = getJson("$GATEWAY/v3/user/baseFolder")
        return res["data"]!!.jsonObject["uuid"]!!.jsonPrimitive.content
    }

    private suspend fun mkdir(name: String, parentUuid: String, masterKey: String): String {
        val uuid = UUID.randomUUID().toString()
        val encName = FilenCrypto.encryptMetadata(buildJsonObject { put("name", name) }.toString(), masterKey)
        postJson("$GATEWAY/v3/dir/create", buildJsonObject {
            put("uuid", uuid)
            put("name", encName)
            put("nameHashed", nameHash(name))
            put("parent", parentUuid)
        })
        return uuid
    }

    private suspend fun listDirByName(uuid: String, masterKey: String): Map<String, String> {
        val res = postJson("$GATEWAY/v3/dir/content", buildJsonObject { put("uuid", uuid) })
        val folders = res["data"]!!.jsonObject["folders"]!!.jsonArray
        return folders.associate {
            val obj = it.jsonObject
            val encName = obj["name"]!!.jsonPrimitive.content
            val decName = runCatching { decryptMetadataName(encName, masterKey) }.getOrDefault(encName)
            decName to obj["uuid"]!!.jsonPrimitive.content
        }
    }

    private fun decryptMetadataName(encrypted: String, masterKey: String): String {
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
        return Json.parseToJsonElement(String(plain)).jsonObject["name"]!!.jsonPrimitive.content
    }

    suspend fun resolveCloudPath(path: String): String {
        val masterKey = settings.masterKey
        var currentUuid = getBaseFolder()
        for (segment in path.trim('/').split('/').filter { it.isNotEmpty() }) {
            val children = listDirByName(currentUuid, masterKey)
            currentUuid = children[segment] ?: mkdir(segment, currentUuid, masterKey)
        }
        return currentUuid
    }

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
            val resText = client.post("https://${ingestHost()}/v3/upload") {
                header("Authorization", authHeader())
                parameter("uuid", fileUuid)
                parameter("index", index)
                parameter("parent", parentUuid)
                parameter("uploadKey", uploadKey)
                parameter("hash", hash)
                setBody(encrypted)
            }.bodyAsText()
            val res = Json.parseToJsonElement(resText).jsonObject
            if (index == 0) {
                bucket = res["data"]!!.jsonObject["bucket"]!!.jsonPrimitive.content
                region = res["data"]!!.jsonObject["region"]!!.jsonPrimitive.content
            }
        }

        val encMetadata = FilenCrypto.encryptFileMetadata(name, mimeType, bytes.size.toLong(), lastModified, fileKey, masterKey)
        val encName = FilenCrypto.encryptMetadata(buildJsonObject { put("name", name) }.toString(), masterKey)

        val doneBody = buildJsonObject {
            put("uuid", fileUuid)
            put("name", encName)
            put("nameHashed", nameHash(name))
            put("size", FilenCrypto.encryptMetadata(bytes.size.toString(), masterKey))
            put("chunks", chunks.size)
            put("mime", mimeType)
            put("rm", (1..32).map { ('a'..'z').random() }.joinToString(""))
            put("metadata", encMetadata)
            put("version", 2)
            put("uploadKey", uploadKey)
        }
        val doneRes = postJson("$GATEWAY/v3/upload/done", doneBody)

        return fileUuid
    }
}
