package nl.joozd.pdflogbookbuilder

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


object SvgToPng {
    fun makePdfByteArray(svgString: String): ByteArray {
        val input = TranscoderInput(ByteArrayInputStream(svgString.toByteArray()))
        val output = ByteArrayOutputStream()

        val transcoderOutput = TranscoderOutput(output)

        PNGTranscoder().transcode(input, transcoderOutput)

        return output.toByteArray()
    }
}