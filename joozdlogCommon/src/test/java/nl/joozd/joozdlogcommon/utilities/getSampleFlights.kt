package nl.joozd.joozdlogcommon.utilities

import nl.joozd.joozdlogcommon.BasicFlight
import java.io.File
import java.time.Instant

internal fun getSampleFlights(f: String): List<BasicFlight> = File(f).readLines().drop(1).map{ line ->
    BasicFlight.ofCsv(f)
}