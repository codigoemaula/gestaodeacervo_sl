package br.gov.sp.sme.salaleitura.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class MetadataHttpException(val status: Int) : IOException("Serviço bibliográfico retornou HTTP $status")

/** A 404 is a missing record. Offline, timeouts, 403 and 429 must NOT be reported as missing records. */
internal suspend fun httpGet(url: String, timeoutMs: Int = 8000): String? = withContext(Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
