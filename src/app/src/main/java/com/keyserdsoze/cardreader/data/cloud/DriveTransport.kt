package com.keyserdsoze.cardreader.data.cloud

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

class DriveAuthorizationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DriveTransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String?,
    val appProperties: Map<String, String>,
)

interface DriveAccessTokenProvider { suspend fun accessToken(): String }

class DriveTransport(private val tokenProvider: DriveAccessTokenProvider) {
    suspend fun list(): List<DriveFile> = withContext(Dispatchers.IO) {
        val url = "$FILES_URL?spaces=appDataFolder&q=trashed%3Dfalse&pageSize=1000&fields=files(id,name,modifiedTime,appProperties)"
        val root = JSONObject(execute("GET", url).toString(Charsets.UTF_8))
        val array = root.optJSONArray("files") ?: JSONArray()
        buildList { repeat(array.length()) { add(decodeFile(array.getJSONObject(it))) } }
    }

    suspend fun read(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        execute("GET", "$FILES_URL/${path(fileId)}?alt=media")
    }

    suspend fun create(name: String, properties: Map<String, String>, bytes: ByteArray): DriveFile = withContext(Dispatchers.IO) {
        val metadata = JSONObject()
            .put("name", name)
            .put("mimeType", JSON_MIME)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("appProperties", JSONObject(properties))
        decodeFile(upload("POST", UPLOAD_URL, metadata, bytes))
    }

    suspend fun update(fileId: String, name: String, properties: Map<String, String>, bytes: ByteArray): DriveFile = withContext(Dispatchers.IO) {
        val metadata = JSONObject()
            .put("name", name)
            .put("mimeType", JSON_MIME)
            .put("appProperties", JSONObject(properties))
        decodeFile(upload("PATCH", "$UPLOAD_URL/${path(fileId)}", metadata, bytes))
    }

    private suspend fun upload(method: String, baseUrl: String, metadata: JSONObject, bytes: ByteArray): JSONObject {
        val boundary = "cardreader_${UUID.randomUUID()}"
        val output = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: $JSON_MIME\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val url = "$baseUrl?uploadType=multipart&fields=id,name,modifiedTime,appProperties"
        return JSONObject(execute(method, url, output, "multipart/related; boundary=$boundary").toString(Charsets.UTF_8))
    }

    private suspend fun execute(method: String, url: String, body: ByteArray? = null, contentType: String? = null): ByteArray {
        var lastError: Exception? = null
        repeat(4) { attempt ->
            try {
                val token = tokenProvider.accessToken()
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", contentType)
                        setFixedLengthStreamingMode(body.size)
                    }
                }
                try {
                    body?.let { connection.outputStream.use { stream -> stream.write(it) } }
                    val status = connection.responseCode
                    val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.use { it.readBytes() } ?: ByteArray(0)
                    if (status in 200..299) return response
                    if (status == 401 || status == 403) throw DriveAuthorizationException("Google Drive authorization failed (HTTP $status)")
                    if (status !in 429..429 && status !in 500..504) throw DriveTransportException("Google Drive request failed (HTTP $status)")
                    lastError = DriveTransportException("Temporary Google Drive failure (HTTP $status)")
                } finally {
                    connection.disconnect()
                }
            } catch (error: DriveAuthorizationException) {
                throw error
            } catch (error: IOException) {
                lastError = error
            }
            if (attempt < 3) Thread.sleep((1L shl attempt) * 1_000L)
        }
        throw DriveTransportException("Google Drive request failed after retries", lastError)
    }

    private fun decodeFile(json: JSONObject): DriveFile {
        val properties = mutableMapOf<String, String>()
        json.optJSONObject("appProperties")?.let { objectProperties ->
            val keys = objectProperties.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                properties[key] = objectProperties.getString(key)
            }
        }
        return DriveFile(
            id = json.getString("id"),
            name = json.optString("name"),
            modifiedTime = json.optString("modifiedTime").takeIf(String::isNotBlank),
            appProperties = properties,
        )
    }

    private companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val JSON_MIME = "application/json"
        fun path(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    }
}
