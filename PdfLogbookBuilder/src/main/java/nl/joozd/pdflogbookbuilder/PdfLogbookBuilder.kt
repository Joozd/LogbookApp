package nl.joozd.pdflogbookbuilder

import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import nl.joozd.joozdlogcommon.BasicFlight
import java.io.OutputStream

class PdfLogbookBuilder(private val flights: List<BasicFlight>) {
    private val document = Document(PageSize.A4.rotate())

    fun buildToOutputStream(outputStream: OutputStream){
        try {
            PdfWriter.getInstance(document, outputStream)
            document.open()

            var previousTimes = TotalsPrevious.ZERO

            flights.chunked(Values.ENTRIES_PER_PAGE).forEach{
                LeftPage(it, previousTimes).addToDocument(document)
                RightPage(it, previousTimes).addToDocument(document)
                previousTimes += it // adds times of these flights to [previousTimes] and replaces [previousTimes]
            }
        }
        catch (e: Throwable){
            throw(e)
        }
        finally {
            document.close()
        }

    }
}