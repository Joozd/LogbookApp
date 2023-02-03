package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.exceptions.CorruptedDataException
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LogtenProExtractor: CompleteLogbookExtractor {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        val flightsMapList = buildFlightsMapList(lines)
        return flightsMapList?.mapNotNull { flightFromMap(it) }
    }

    private fun buildFlightsMapList(lines: List<String>): List<Map<String, String>>?{
        if (lines.isEmpty()) return null
        val flightsMapsList = mutableListOf<Map<String, String>>()
        with(lines.iterator()){
            val headers = getItems(next())
            while(hasNext()){
                val flightMap = getItems(next()).mapIndexed { index, s ->
                    headers[index] to s
                }.toMap()
                if (flightMap.size != headers.size) throw(CorruptedDataException("Invalid amount of items in line ${flightsMapsList.size + 2} - can be caused by a line break in a text field."))
                flightsMapsList.add(flightMap)
            }
        }
        return flightsMapsList
    }

    private fun getItems(line: String) = line.split("\t").map { it.trim() }


    private fun flightFromMap(map: Map<String, String>): BasicFlight? = with(map){
        val date = getDate(DATE_KEY) ?: return null
        val orig = get(ORIG_KEY) ?: ""
        val dest = get(DEST_KEY) ?: ""
        val timeOut = makeTime(DEPARTURE_TIME, date)
        val timeIn = makeTime(ARRIVAL_TIME, date)
        val correctedTotalTime = getMinutes(TOTAL_TIME_KEY) ?: 0
        //mp time is auto calculated
        val nightTime = getMinutes(NIGHT_TIME_KEY) ?: 0
        val ifrTime = getMinutes(IFR_TIME_KEY) ?: 0
        val simTime = getMinutes(SIM_TIME_KEY) ?: 0
        val aircraft = get(AIRCRAFT_TYPE_KEY) ?: ""
        val registration = get(AIRCRAFT_REG_KEY) ?: ""
        val namePic = get(NAME_PIC) ?: ""
        val otherNames = makeOtherNamesString(map)
        val takeOffDay: Int = getIntOrZero(DAY_TAKEOFFS_KEY)
        val takeOffNight: Int = getIntOrZero(NIGHT_TAKEOFFS_KEY)
        val landingDay: Int = getIntOrZero(DAY_LANDINGS_KEY)
        val landingNight: Int = getIntOrZero(NIGHT_LANDINGS_KEY)
        val autoLand: Int = getIntOrZero(AUTOLANDS_KEY)
        val flightNumber = get(FLIGHT_NUMBER_KEY) ?: ""
        val remarks = get(REMARKS_KEY) ?: ""
        val isPic = getBoolean(PIC_KEY)
        val isPicus = getBoolean(PICUS_KEY)
        val isCopilot = getBoolean(COPILOT_KEY)
        val isDual = (getMinutes(DUAL_TIME_KEY)?: 0) > 0
        val isInstructor = getBoolean(INSTRUCTOR_KEY)
        val isSim = simTime != 0
        val isPF = getBoolean(PF_KEY)

        BasicFlight.PROTOTYPE.copy(
            orig = orig,
            dest = dest,
            timeOut = timeOut,
            timeIn = timeIn,
            correctedTotalTime = correctedTotalTime,
            nightTime = nightTime,
            ifrTime = ifrTime,
            simTime = simTime,
            aircraft = aircraft,
            registration = registration,
            name = namePic,
            name2 = otherNames,
            takeOffDay = takeOffDay,
            takeOffNight = takeOffNight,
            landingDay = landingDay,
            landingNight = landingNight,
            autoLand = autoLand,
            flightNumber = flightNumber,
            remarks = remarks,
            isPIC = isPic,
            isPICUS = isPicus,
            isCoPilot = isCopilot,
            isDual = isDual,
            isInstructor = isInstructor,
            isSim = isSim,
            isPF = isPF,
            isPlanned = false
        )
    }

    private fun Map<String, String>.getDate(key: String): LocalDate? =
        parseDate(this[key]?.takeIf { it.isNotBlank()})

    private fun Map<String, String>.getMinutes(key: String): Int? =
        getMinutes(this[key]?.takeIf { it.isNotBlank()})

    private fun Map<String, String>.getBoolean(key: String): Boolean =
        this[key]?.let{
            it == "1"
        } ?: false

    private fun Map<String, String>.makeTime(key: String, date: LocalDate): Long =
        makeTime(this[key]?.takeIf { it.isNotBlank()}, date)


    private fun Map<String, String>.getIntOrZero(key: String): Int =
        this[key]?.takeIf { it.isNotBlank() }?.toInt() ?: 0

    private fun parseDate(date: String?): LocalDate? = date?.let {
        LocalDate.parse(it, dateFormatter)
    }


    private fun makeTime(timeString: String?, date: LocalDate): Long = timeString?.let {
        val lt = LocalTime.parse(timeString, timeFormatter)
        return lt.atDate(date).atOffset(ZoneOffset.UTC).toEpochSecond()
    } ?: date.atStartOfDay(ZoneOffset.UTC).toEpochSecond()

    private fun makeOtherNamesString(flightMap: Map<String, String>) =
        flightMap.getMultiple(otherCrewKeys).joinToString(";") { it.replace(';', '|') }// names are saved with '|' instead of ';' because ';' is used as separator. If anybody uses '|' in their names they gave tough luck.

    /*
     * Supports hh:mm and hours with decimals, can use any non-digit character as decimal sign except ':'.
     * No decimal sign assumes whole hours.
     */
    private fun getMinutes(s: String?): Int? = s?.let{
        if (':' in it) return minutesFromHoursAndMinutesString(it)
        if (it.isEmpty()) return 0
        val notANumberRegex = "[^0-9]".toRegex()
        val standardizedTimeString = it.replace(notANumberRegex, ".")
        val hours = standardizedTimeString.toDouble()
        val minutes = hours * 60
        minutes.toInt()
    }



    private fun minutesFromHoursAndMinutesString(s: String): Int{
        require (":" in s) { "only use minutesFromHoursAndMinutesString for hh:mm"}
        val hmList = s.split(":").map { it.toInt() }
        if (hmList.size != 2) return 0 // only supports Hours:Minutes
        return hmList[1] + hmList[0]*60
    }


    private fun <K, V> Map<K, V>.getMultiple(keys: List<K>): List<V> =
        keys.map { get(it) }.filterNotNull()

    companion object{
        private const val DATE_KEY = "flight_flightDate"
        private const val DEPARTURE_TIME = "flight_actualDepartureTime"
        private const val ARRIVAL_TIME = "flight_actualArrivalTime"
        private const val ORIG_KEY = "flight_from"
        private const val DEST_KEY = "flight_to"
        private const val NAME_PIC = "flight_selectedCrewPIC"
        private const val TOTAL_TIME_KEY = "flight_totalTime"
        private const val NIGHT_TIME_KEY = "flight_night"
        private const val IFR_TIME_KEY = "flight_actualInstrument"
        private const val SIM_TIME_KEY = "flight_simulator"
        private const val AIRCRAFT_TYPE_KEY = "aircraftType_type"
        private const val AIRCRAFT_REG_KEY = "aircraft_aircraftID"
        private const val DAY_LANDINGS_KEY = "flight_dayLandings"
        private const val NIGHT_LANDINGS_KEY = "flight_nightLandings"
        private const val DAY_TAKEOFFS_KEY = "flight_dayTakeoffs"
        private const val NIGHT_TAKEOFFS_KEY = "flight_nightTakeoffs"
        private const val AUTOLANDS_KEY = "flight_autolands"
        private const val FLIGHT_NUMBER_KEY = "flight_flightNumber"
        private const val REMARKS_KEY = "flight_remarks"

        private const val PIC_KEY = "flight_picCapacity"
        private const val PICUS_KEY = "flight_underSupervisionCapacity"
        private const val COPILOT_KEY = "flight_sicCapacity"
        private const val DUAL_TIME_KEY = "flight_dualReceived"
        private const val INSTRUCTOR_KEY = "flight_selectedCrewInstructor"
        private const val PF_KEY = "flight_pilotFlyingCapacity"

        //used to check if a file is a LogtenPro File
        val USED_KEYS = listOf(DATE_KEY, DEPARTURE_TIME, ARRIVAL_TIME, ORIG_KEY, DEST_KEY, NAME_PIC, TOTAL_TIME_KEY, NIGHT_TIME_KEY, IFR_TIME_KEY, SIM_TIME_KEY, AIRCRAFT_TYPE_KEY, AIRCRAFT_REG_KEY, DAY_LANDINGS_KEY, NIGHT_LANDINGS_KEY, DAY_TAKEOFFS_KEY, NIGHT_TAKEOFFS_KEY, AUTOLANDS_KEY, FLIGHT_NUMBER_KEY, REMARKS_KEY, PIC_KEY, PICUS_KEY, COPILOT_KEY, DUAL_TIME_KEY, INSTRUCTOR_KEY, PF_KEY)

        private val otherCrewKeys = "flight_selectedCrewSIC\t flight_selectedCrewRelief\t flight_selectedCrewRelief2\t flight_selectedCrewRelief3\t flight_selectedCrewRelief4\t flight_selectedCrewFlightEngineer\t flight_selectedCrewInstructor\t flight_selectedCrewStudent\t flight_selectedCrewObserver\t flight_selectedCrewObserver2\t flight_selectedCrewPurser\t flight_selectedCrewFlightAttendant\t flight_selectedCrewFlightAttendant2\t flight_selectedCrewFlightAttendant3\t flight_selectedCrewFlightAttendant4\t flight_selectedCrewCommander\t flight_selectedCrewCustom1\t flight_selectedCrewCustom2\t flight_selectedCrewCustom3\t flight_selectedCrewCustom4\t flight_selectedCrewCustom5".split("\t").map { it.trim() }
    }
}