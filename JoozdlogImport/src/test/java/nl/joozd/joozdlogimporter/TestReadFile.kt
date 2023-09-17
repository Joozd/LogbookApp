package nl.joozd.joozdlogimporter

import org.junit.Test
import java.io.File

class TestReadFile {
    @Test
    fun testReadHuftuhsMonthly(){
        val lines = PdfImporter.ofInputStream(File("c:\\temp\\huftuh.pdf").inputStream()).lines
        println("LINES FROM HUFTUH:")
        println("LINES FROM HUFTUH:")
        println("LINES FROM HUFTUH:")
        println("******************")
        println(lines.joinToString("\n"))
        assert(lines.isNotEmpty())
    }
}