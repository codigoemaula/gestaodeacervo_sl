package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class MetadataHttpException(val status: Int) : IOException("Serviço bibliográfico retornou HTTP $status")

/** The public Open Library API limits unidentified clients to one request per second. */
private val openLibraryMutex = Mutex()
private var nextOpenLibraryRequestMs = 0L

private suspend fun awaitOpenLibrarySlot() {
    openLibraryMutex.lock()
    try {
        val now = System.nanoTime() / 1_000_000L
        val waitMs = (nextOpenLibraryRequestMs - now).coerceAtLeast(0L)
        if (waitMs > 0) delay(waitMs)
        nextOpenLibraryRequestMs = System.nanoTime() / 1_000_000L + 1100L
    } finally {
        openLibraryMutex.unlock()
    }
}

/** Distinguishes a 404 (not found) from unavailable API, rate limits, and offline operation. */
internal suspend fun httpGet(url: String, timeoutMs: Int = 8000): String? = withContext(Dispatchers.IO) {
    val target = URL(url)
    if (target.host == "openlibrary.org") awaitOpenLibrarySlot()
    val connection = (target.openConnection() as HttpURLConnection).apply {
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json, application/xml, text/xml")
        setRequestProperty("User-Agent", "SalaLeituraApp/1.1 (https://github.com/codigoemaula/gestaodeacervo_sl)")
    }
    try {
        when (val status = connection.responseCode) {
            in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            404 -> null
            else -> throw MetadataHttpException(status)
        }
    } finally {
        connection.disconnect()
    }
}
