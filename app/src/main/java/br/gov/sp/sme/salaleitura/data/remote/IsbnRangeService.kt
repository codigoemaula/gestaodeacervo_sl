package br.gov.sp.sme.salaleitura.data.remote

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class IsbnRangeDownload(val serial: String, val messageDate: String, val xml: String)

class IsbnRangeService {
    suspend fun download(): IsbnRangeDownload? {
        val xml = httpGet("https://www.isbn-international.org/export_rangemessage.xml", timeoutMs = 10000) ?: return null
        var serial = ""
        var messageDate = ""
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(StringReader(xml)) }
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && (serial.isBlank() || messageDate.isBlank())) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "MessageSerialNumber" -> serial = parser.nextText()
                    "MessageDate" -> messageDate = parser.nextText()
                }
            }
            event = parser.next()
        }
        if (serial.isBlank()) return null
        return IsbnRangeDownload(serial, messageDate, xml)
    }
}
