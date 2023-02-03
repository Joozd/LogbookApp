package nl.joozd.joozdlogimporter

import nl.joozd.joozdlogimporter.interfaces.FileImporter
import nl.joozd.joozdlogimporter.supportedFileTypes.*
import java.io.IOException
import java.io.InputStream

class TxtImporter(private val lines: List<String>): FileImporter() {
    override fun getFile() = findTypeOfFile(lines)

    private fun findTypeOfFile(lines: List<String>): ImportedFile =
        LogTenProFile.buildIfMatches(lines)
            ?: UnsupportedTxtFile()

    companion object{
        @Throws(IOException::class)
        fun ofInputStream(inputStream: InputStream) =
            CsvImporter(inputStream.reader().readLines())
    }
}