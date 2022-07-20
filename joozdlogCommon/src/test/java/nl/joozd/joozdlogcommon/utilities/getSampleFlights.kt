package nl.joozd.joozdlogcommon.utilities

import nl.joozd.joozdlogcommon.BasicFlight
import java.io.File
import java.time.Instant

internal fun getSampleFlights(f: String): List<BasicFlight> = File(f).readLines().drop(1).map{ line ->
    line.split(";").let{ f ->
        BasicFlight(
            f[0].toInt(),
            f[1],
            f[2],
            Instant.parse(f[3]).epochSecond,
            Instant.parse(f[4]).epochSecond,
            f[5].toInt(),
            f[6].toInt(),
            f[7].toInt(),
            f[8].toInt(),
            f[9].toInt(),
            f[10],
            f[11],
            f[12],
            f[13],
            f[14].toInt(),
            f[15].toInt(),
            f[16].toInt(),
            f[17].toInt(),
            f[18].toInt(),
            f[19],
            f[20],
            f[21] == "true",
            f[22] == "true",
            f[23] == "true",
            f[24] == "true",
            f[25] == "true",
            f[26] == "true",
            f[27] == "true",
            f[28] == "true",
            false,
            f[29] == "true",
            f[30].toInt(),
            false,
            0L,
            f[31]
        )
    }
}