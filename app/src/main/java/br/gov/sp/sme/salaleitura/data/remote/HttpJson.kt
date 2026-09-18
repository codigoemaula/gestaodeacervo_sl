package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal suspend fun httpGet(url: String, timeoutMs: Int = 6000): String? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json, application/xml, text/xml")
            setRequestProperty("User-Agent", "SalaLeitura-SME-SP/1.0")
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
