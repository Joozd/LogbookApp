package nl.joozd.pdflogbookbuilder.extensions

import com.itextpdf.text.Image
import nl.joozd.joozdlogcommon.AugmentedCrew
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.pdflogbookbuilder.SvgToPng
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun BasicFlight.date(): String{
    val instant = Instant.ofEpochSecond(timeOut)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yy")
    return LocalDate.ofInstant(instant, ZoneOffset.UTC).format(formatter)
}

fun BasicFlight.timeOut(): String {
    val instant = Instant.ofEpochSecond(timeOut)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return LocalTime.ofInstant(instant, ZoneOffset.UTC).format(timeFormatter)
}

fun BasicFlight.timeIn(): String {
    val instant = Instant.ofEpochSecond(timeIn)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return LocalTime.ofInstant(instant, ZoneOffset.UTC).format(timeFormatter)
}

fun BasicFlight.totalTime() = correctedTotalTime.takeIf { it != 0 }
    ?: AugmentedCrew.fromInt(augmentedCrew).getLogTime(((timeIn - timeOut) / 60).toInt(), isPIC)

fun BasicFlight.signatureAsPng(): Image =
    Image.getInstance(SvgToPng.makePdfByteArray(signature)).apply{
        isScaleToFitHeight = true
    }



/**
 * Used to get the "hours" component of a time that is saved as an amount of minutes. So 79 will become "1"
 */
fun Int.hours() = (this / 60).toString()

/**
 * Used to get the "minutes" component of a time that is saved as an amount of minutes. So 79 will become "19"
 */
fun Int.minutes() = (this % 60).toString()

/**
 * Outputs a " " if zero
 */
fun Int.toStringIfNotZero() = if (this == 0) " " else this.toString()