package com.family.bang.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CreatedGame(val gameCode: String, val hostToken: String)
data class JoinedPlayer(val playerName: String, val playerToken: String)

class ApiException(message: String) : Exception(message)

class GameApi(private val baseUrl: String) {
    suspend fun gameCodes(): List<String> = request("/codes") { json ->
        json.getJSONArray("gameCodes").let { array -> List(array.length()) { array.getString(it) } }
    }

    suspend fun createGame(): CreatedGame = request("", "POST") { json ->
        CreatedGame(json.getString("gameCode"), json.getString("hostToken"))
    }

    suspend fun joinGame(code: String, name: String): JoinedPlayer =
        request("/${code.uppercase()}/players", "POST", body = JSONObject().put("playerName", name).toString()) { json ->
            JoinedPlayer(json.getString("playerName"), json.getString("playerToken"))
        }

    suspend fun lobby(code: String, token: String): List<String> =
        request("/${code.uppercase()}/lobby", token = token) { json ->
            json.getJSONArray("players").let { array -> List(array.length()) { array.getString(it) } }
        }

    suspend fun deal(code: String, hostToken: String) {
        request<Unit>("/${code.uppercase()}/deal", "POST", hostToken) { }
    }

    suspend fun role(code: String, playerToken: String): String =
        request("/${code.uppercase()}/role", token = playerToken) { it.getString("role") }

    private suspend fun <T> request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: String? = null,
        transform: (JSONObject) -> T,
    ): T = withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            body?.let {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { writer -> writer.write(it) }
            }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
                throw ApiException(detail?.takeIf(String::isNotBlank) ?: "The server returned HTTP $status")
            }
            transform(if (text.isBlank()) JSONObject() else JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }
}
