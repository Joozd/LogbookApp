package nl.joozd.joozdlogimporter

import nl.joozd.joozdlogimporter.SupportedMimeTypes.MIME_TYPE_CSV
import nl.joozd.joozdlogimporter.SupportedMimeTypes.MIME_TYPE_PDF
import nl.joozd.joozdlogimporter.SupportedMimeTypes.MIME_TYPE_TEXT
import nl.joozd.joozdlogimporter.interfaces.FileImporter
import java.io.InputStream

object JoozdlogImporter {
    fun ofInputStream(inputStream: InputStream, mimeType: String): FileImporter? =
        when(mimeType){
            MIME_TYPE_CSV -> CsvImporter.ofInputStream(inputStream)
            MIME_TYPE_PDF -> PdfImporter.ofInputStream(inputStream)
            MIME_TYPE_TEXT -> TxtImporter.ofInputStream(inputStream)
            else -> null
        }
}