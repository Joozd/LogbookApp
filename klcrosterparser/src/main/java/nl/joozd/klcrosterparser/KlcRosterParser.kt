package nl.joozd.klcrosterparser

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import java.io.InputStream
import java.util.*

/**
 * Does all kinds of useful things with a KLC roster, like split it into days, or whetever else
 * I think about.
 * @param inputStream: Inputstream holding a PDF file containing a KLC roster.
 */

class KlcRosterParser(inputStream: InputStream) {
    companion object{
        private const val endOfHeaderMarker = "date H duty R dep arr AC info date H duty R dep arr AC info date H duty R dep arr AC info"
        private const val dateRangeStartMarker = "Period: "
        private const val dateRangeEndMarker = " contract:"
        private const val endOfRosterMarker = "Flight time"
        private const val legendMarker = "Absence/Ground Activity Legend"
        private const val hotelsMarker = "Hotels"
        private const val hotelsEndMarker = "Recurrent Training / Checks"

        private const val CLICK = "CLICK"
        private const val weekDay = "Mon|Tue|Wed|Thu|Fri|Sat|Sun"
        private const val carrier = "DH/[A-Z]{2}|WA|KL"
        private const val simString = "TSTR1|TSTR1H|TSTR2|TSTR2H|TSFCL|TSLOE|TSLOEH|TSOD|TSODH|TSACTI|TSACT"
        private const val dayOffString = "LPFLC|LVEC|LVES|ALC|LFD|LXD"
        private const val standbyString = "RESH|RESK"
        private const val extraString = "TCRM|TCRMI|TCUG"
        private const val otherDutyString = "MMCS|TCBT|TGC|TFIE|TBEXI|OE"
        private const val singleLineActivityString = standbyString + "|Pick"
        private const val defaultCheckoutTimeInSeconds = 30*60L


        val dayRegEx = "($weekDay)\\d\\d".toRegex()
        // eg. Wed04

        val pickUpRegEx = "Pick Up \\d{4}".toRegex()
        // eg. Pick Up 0345

        val standbyRegEx = "($standbyString)\\s[A-Z]{3}\\s\\d{4}\\s\\d{4}".toRegex()
        // eg. RESK AMS 0900 2100 (...)

        val checkInRegex = "C/I\\s[A-Z]{3}\\s\\d{4}".toRegex()
        // eg. C/I TRN 0435

        val checkOutRegex = "C/O\\s\\d{4}\\s[A-Z]{3}\\s\\[FDP\\s\\d{2}:\\d{2}]".toRegex()
        // eg. C/O 1040 TRN [FDP 04:50]

        val flightRegex = "($carrier)\\s\\d+\\sR?\\s?[A-Z]{3}\\s\\d{4}\\s\\d{4}\\s[A-Z]{3}".toRegex()
        //eg. KL 1582 BLQ 0400 0600 AMS E90 89113 RI -> KL 1582 BLQ 0400 0600 AMS / rest of line is extraMessage for that event

        val simRegex = "$simString".toRegex()
        // eg. TSACT

        val otherRegex = "($otherDutyString)\\s[A-Z]{3}\\s\\d{4}\\s\\d{4}".toRegex()

        val timeOffRegex = dayOffString.toRegex()

    }

    private val legend = mutableMapOf<String, String>()
    private val hotels = mutableMapOf<String, String>()
    var pageWithHotelsTEMP = ""

    private val reader = PdfReader(inputStream)
    @Suppress("MemberVisibilityCanBePrivate")
    val header: String
    init{
        val firstPage = PdfTextExtractor.getTextFromPage(reader, 1, SimpleTextExtractionStrategy())
        header =
            if (endOfHeaderMarker in firstPage && dateRangeStartMarker in firstPage && dateRangeEndMarker in firstPage)
                firstPage.slice(0 until firstPage.indexOf(endOfHeaderMarker))
            else ""
    }
    val seemsValid = header.isNotEmpty()

    val text: String by lazy{
        var completeString = ""
        repeat(reader.numberOfPages){pageNumber ->
            val pageText = PdfTextExtractor.getTextFromPage(reader, pageNumber+1, SimpleTextExtractionStrategy()).drop(header.length).trim()
            completeString += pageText

            //fill legend map
            if (legendMarker in pageText){
                val legendText = pageText.drop(pageText.indexOf(legendMarker)).split("\n").drop(2) // drop marker line and next one which says "code description"
                legendText.forEach { l ->
                    val words = l.split (" ")
                    legend[words[0]] = words.drop(1).joinToString(" ")
                }
            }

            //fill hotels map
            if (hotelsMarker in pageText){
                pageWithHotelsTEMP = pageText
                val hotelsText = if (hotelsEndMarker in pageText)
                    pageText.slice(pageText.indexOf(hotelsMarker) until pageText.indexOf(hotelsEndMarker)).split("\n").drop(1)
                else
                    pageText.drop(pageText.indexOf(hotelsMarker)).split("\n").drop(1)
                hotelsText.forEach { l ->
                    val words = l.split (" ")
                    hotels[words[0]] = words.drop(1).joinToString(" ")
                }
            }

        }
        //next line will cause exception if roster doesnt end with _endOfRosterMarker_
        completeString.slice(0 until completeString.indexOf(endOfRosterMarker))
    }
    val startOfRoster: Instant?
    val endOfRoster: Instant?
    init{
        if (!seemsValid){
            startOfRoster = null
            endOfRoster = null
        } else {
            val dateRangeString = header.slice(header.indexOf(dateRangeStartMarker)+ dateRangeStartMarker.length until header.indexOf(dateRangeEndMarker))
            val startEnd = dateRangeString.split(" - ")
            startOfRoster = LocalDateTime.of (LocalDate.parse(startEnd[0], DateTimeFormatter.ofPattern("ddMMMyy", Locale.US)), LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
            endOfRoster = LocalDateTime.of (LocalDate.parse(startEnd[1], DateTimeFormatter.ofPattern("ddMMMyy", Locale.US)), LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
        }
    }


    val days: List<RosterDay> by lazy{
        val workingList = mutableListOf<RosterDay>()
        var workingText = text
        var foundDay = dayRegEx.find(workingText)
        val startDate = LocalDateTime.ofInstant(startOfRoster, ZoneOffset.UTC).toLocalDate()
        fillADay@while (foundDay != null){
            val todaysEvents = mutableListOf<KlcRosterEvent>()
            var todaysExtraInfo = ""


            val dayString: String = foundDay.value
            val dayNumber = dayString.filter{it.isDigit()}.toInt()
            // remove the day marker:
            workingText = workingText.drop(workingText.indexOf(dayString)+dayString.length).trim()

            // get the start of the next day, if any; also sets condition for @fillADay loop
            foundDay = dayRegEx.find(workingText)

            // if there is another day, current text is everything until that, take it from the workingText:
            //if there isn't take everything
            val todaysText =
                if (foundDay != null)
                    workingText.slice(0 until workingText.indexOf(foundDay.value))
                else workingText
            // todaysExtraInfo = todaysText

            workingText.drop(todaysText.length)

            // activeDate is the day this cycle of @fillADay is filling
            val activeDate = if (dayNumber >= startDate.dayOfMonth)
                    startDate.withDayOfMonth(dayNumber)
                else {
                startDate.withDayOfMonth(dayNumber).plusMonths(1)
            }

            // get events on activeDate:
            /**
             * Structure of a day block:
             * Day (already stripped here)
             * first line is either Pick-up or todays activity: C/I for a flight day or other for other:
             * - T??? for training (eg TSACT or TSTR2), L???? for leave (eg. LVEC, LPFLC of LXD), RES? for standby:
             * Thu30 Pick Up 0450
             * Mon05 LVEC R AMS 0330 0329
             * Sun09 RESK AMS 0900 1900 [FDP 00:00]
             * Wed22 TSACT
             * Tue13 C/I AMS 0400
             *
             * Depending on activity, the rest will be according to some rules:
             * Pick Up: Make taxi event, next line is first line
             * RES?: Standby. Single line. Next line (if any) is first line.
             *
             * C/I: Flight day. Check in from given time until start of next event
             *      Events will continue untill a C/O event is found. This runs from end of previous until given time.
             *      After C/O, there can be a "H\\d" event, this is a nightstop with destination
             *      After C/O, there can be a CLICK event with times.
             *      Any other text after C/O is a note, this gets added to C/I and C/O events as extraMessage
             * T???: Training duty.
             *      Can be single line, it will have times in that case.
             *      Can be a single word, then next line will be something like "B3 AMS 0615 0945 EMJ [FDP 00:00]"
             *      Any lines after that are notes, this gets added to named event (eg. TSACT) as extraMessage
             *      Sim dutie times (the TSLOE kind) are to be taken from header, as that includes briefing times.
             * X: Day Over. No times; adds a day to previous days hotel TODO: will require rebuilding the previous day)
             *      Next lines will be ignored
             * L???? / ALC: Leave / vacation. Only one event, starts at 0000 and ends 24 hrs later.
             * Other gets added as extraMessage to last Event found, if any, or discarded
             */

            var lines = todaysText.split("\n").map{it.trim()}
            fillEvents@while (lines.isNotEmpty()){
                val line = lines[0]
                lines = lines.drop(1)

                when{
                    pickUpRegEx.find(line) != null -> {
                        val time = line.filter{it.isDigit()}.toInt()
                        val nextEventTime = lines[0].filter{it.isDigit()}.toInt() // will cause exception if pickup going nowhere
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(time/100, time%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(nextEventTime/100, nextEventTime%100)).atZone(ZoneOffset.UTC).toInstant()
                        val description = line.filter{!it.isDigit()}.trim()
                        todaysEvents.add(KlcRosterEvent(Activities.TAXI, startTime, endTime, description))


                        // edit previous hotel endtime to end now

                        //keep going back untill a date with a hotel in it is found
                        var yesterday: RosterDay? = RosterDay(activeDate, emptyList(), "")
                        innerWhile@while (yesterday!!.events.none { it.type == Activities.HOTEL }) {
                            yesterday = workingList.firstOrNull { it.date == yesterday?.date?.minusDays(1) }
                            if (yesterday == null) break@innerWhile
                        }
                        yesterday?.let {
                            val yesterdaysUpdatedEvents = it.events.map {
                                if (it.type == Activities.HOTEL) it.copy(end = startTime)
                                else it.copy()
                            }
                            workingList.remove(yesterday)
                            workingList.add(
                                RosterDay(
                                    yesterday.date,
                                    yesterdaysUpdatedEvents,
                                    yesterday.extraInfo
                                )
                            )
                        }
                    }

                    standbyRegEx.find(line) != null -> {
                        val words = standbyRegEx.find(line)!!.value.split(" ") // strip any FDP info that may or may not be there
                        // val description = words.slice(0..1).joinToString(" ") // makes nice RESK AMS again
                        val startTimeInt = words[2].toInt()
                        val endTimeInt = words[3].toInt()
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(startTimeInt/100, startTimeInt%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(endTimeInt/100, endTimeInt%100)).atZone(ZoneOffset.UTC).toInstant()
                        val description = if (legend[words[0]] != null) "${words[0]} (${legend[words[0]]}) ${words[1]}" else line
                        todaysEvents.add(KlcRosterEvent(Activities.STANDBY, startTime, endTime, description))
                    }

                    checkInRegex.find(line) != null -> {
                        val words = checkInRegex.find(line)!!.value.split(" ")
                        val time = line.filter{it.isDigit()}.toInt()
                        val nexttime = getNextActivityStartTimeInt(lines)
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(time/100, time%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(nexttime/100, nexttime%100)).atZone(ZoneOffset.UTC).toInstant()
                        val description = "${words[0]} ${words[1]}"
                        todaysEvents.add(KlcRosterEvent(Activities.CHECKIN, startTime, endTime, description))
                    }

                    flightRegex.find(line) != null -> {
                        val words = flightRegex.find(line)!!.value.split(" ").filter{it != "R"}
                        val numbers = flightRegex.find(line)!!.value.filter{it in "0123456789 "}.trim().replace("\\s+".toRegex(), " ").split(" ").map{it.toInt()}
                        val time = numbers[1]
                        val nexttime = numbers[2]
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(time/100, time%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(nexttime/100, nexttime%100)).atZone(ZoneOffset.UTC).toInstant()

                        val orig = words[2]
                        val dest = words[5]
                        val description = "${words[0]}${words[1]} $orig -> $dest" // KL1234 AMS -> BLQ
                        val extraInfo = line.drop(flightRegex.find(line)!!.value.length) // anything after DEST is extra info, usually just a/c type

                        todaysEvents.add(KlcRosterEvent(Activities.FLIGHT, startTime, endTime, description, extraInfo))
                    }

                    checkOutRegex.find(line) != null -> {
                        val numbers = checkOutRegex.find(line)!!.value.filter{it in "0123456789 "}.trim().replace("\\s+".toRegex(), " ").split(" ")
                        val time = numbers[0].toInt()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(time/100, time%100)).atZone(ZoneOffset.UTC).toInstant()
                        val startTime = todaysEvents.maxBy { it.end }?.end ?: endTime.minusSeconds(defaultCheckoutTimeInSeconds)
                        val extraMessage = line.slice(line.indexOf("[")..line.indexOf("]"))

                        todaysEvents.add(KlcRosterEvent(Activities.CHECKOUT, startTime, endTime, line))

                        while (lines.isNotEmpty()){ // everything after checkOut is either Hotel, CLICK or extra info
                            @Suppress("NAME_SHADOWING") val line = lines[0]
                            lines = lines.drop(1)
                            val words = line.trim().replace("\\s+".toRegex(), " ").split(" ").filter{it.isNotEmpty()}
                            if (words.isNotEmpty()) {
                                when {
                                    words[0][0] == 'H' && words[0][1].isDigit() && words[0].length == 2 -> { // line is Hotel
                                        val hotelEndTime = LocalDateTime.of(
                                            activeDate.plusDays(1),
                                            LocalTime.MIDNIGHT
                                        ).atZone(ZoneOffset.UTC)
                                            .toInstant() // hotel ends at midnight. Fix that on following pickup!
                                        val hotelDescription = hotels[words[0]] ?: words[0]
                                        todaysEvents.add(
                                            KlcRosterEvent(
                                                Activities.HOTEL,
                                                endTime, hotelEndTime, hotelDescription
                                            )
                                        )
                                    }
                                    words[0] == CLICK -> {
                                        @Suppress("NAME_SHADOWING") val words = line.split(" ")
                                        @Suppress("NAME_SHADOWING") val numbers =
                                            line.filter { it in "0123456789 " }.trim()
                                                .replace("\\s+".toRegex(), " ").split(" ")
                                                .map { it.toInt() }
                                        @Suppress("NAME_SHADOWING") val startTime =
                                            LocalDateTime.of(
                                                activeDate,
                                                LocalTime.of(numbers[0] / 100, numbers[0] % 100)
                                            ).atZone(ZoneOffset.UTC).toInstant()
                                        @Suppress("NAME_SHADOWING") val endTime = LocalDateTime.of(
                                            activeDate,
                                            LocalTime.of(numbers[1] / 100, numbers[1] % 100)
                                        ).atZone(ZoneOffset.UTC).toInstant()
                                        todaysEvents.add(
                                            KlcRosterEvent(
                                                Activities.CLICK,
                                                startTime,
                                                endTime,
                                                line
                                            )
                                        )
                                    }
                                    else -> { // line is extra info
                                        if (todaysExtraInfo.isNotEmpty()) todaysExtraInfo += "\n"
                                        todaysExtraInfo += line
                                    }
                                }
                            }
                        }
                    }

                    simRegex.find(line) != null -> {
                        // get start and end times from header (go to dayString, then chop into words, 3rd and 4th word are startt and end time)
                        val relevantTimes = header.drop(header.indexOf(dayString)).split("\n").take(4).drop(2).map{it.toInt()}
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(relevantTimes[0]/100, relevantTimes[0]%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(relevantTimes[1]/100, relevantTimes[1]%100)).atZone(ZoneOffset.UTC).toInstant()
                        val description = if (legend[line] != null) "$line (${legend[line]})" else line
                        todaysEvents.add(KlcRosterEvent(Activities.SIM, startTime, endTime, description))

                        while (lines.isNotEmpty()) { // everything after sim is either actual sim or extra info
                            @Suppress("NAME_SHADOWING") val line = lines[0]
                            lines = lines.drop(1)
                            val words = line.trim().replace("\\s+".toRegex(), " ").split(" ").filter{it.isNotEmpty()}
                            if (words.isEmpty()) continue

                                if (words[0][0].isUpperCase() && words[0][1].isDigit() && words[0].length == 2) { // line is actual sim
                                    @Suppress("NAME_SHADOWING") val relevantTimes =
                                        line.filter { it in "0123456789 " }.trim()
                                            .replace("\\s+".toRegex(), " ").split(" ")
                                            .map { it.toInt() }.drop(1)
                                    @Suppress("NAME_SHADOWING") val startTime = LocalDateTime.of(
                                        activeDate,
                                        LocalTime.of(relevantTimes[0] / 100, relevantTimes[0] % 100)
                                    ).atZone(ZoneOffset.UTC).toInstant()
                                    @Suppress("NAME_SHADOWING") val endTime = LocalDateTime.of(
                                        activeDate,
                                        LocalTime.of(relevantTimes[1] / 100, relevantTimes[1] % 100)
                                    ).atZone(ZoneOffset.UTC).toInstant()
                                    todaysEvents.add(
                                        KlcRosterEvent(
                                            Activities.ACTUALSIM,
                                            startTime,
                                            endTime,
                                            words[0]
                                        )
                                    )
                                } else {


                                    if (todaysExtraInfo.isNotEmpty()) todaysExtraInfo += "\n"
                                    todaysExtraInfo += line
                                }


                        }
                    }

                    otherRegex.find(line) != null -> {
                        val words = line.split(" ")
                        val numbers = line.filter{it in "0123456789 "}.trim().replace("\\s+".toRegex(), " ").split(" ").map{it.toInt()}.drop(1)
                        val startTime = LocalDateTime.of(activeDate, LocalTime.of(numbers[0]/100, numbers[0]%100)).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate, LocalTime.of(numbers[1]/100, numbers[1]%100)).atZone(ZoneOffset.UTC).toInstant()
                        val description = if (legend[words[0]] != null) "$${words[0]} (${legend[line]})" else line
                        todaysEvents.add(KlcRosterEvent(Activities.OTHER_DUTY, startTime, endTime, description))
                    }

                    timeOffRegex.find(line) != null -> {
                        val words = line.split(" ")
                        val startTime = LocalDateTime.of(activeDate, LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
                        val endTime = LocalDateTime.of(activeDate.plusDays(1), LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
                        val description = if (legend[words[0]] != null) "${words[0]} (${legend[words[0]]})" else line
                        todaysEvents.add(KlcRosterEvent(Activities.LEAVE, startTime, endTime, description))
                    }
                    else -> { // add unknown lines to day's extra info
                        if (todaysExtraInfo.isNotEmpty()) todaysExtraInfo += "\n"
                        todaysExtraInfo += line
                    }
                }
            }
            workingList.add(RosterDay(activeDate, todaysEvents, todaysExtraInfo))
        }
        workingList.toList()
    }



    private fun getNextActivityStartTimeInt(l: List<String>): Int{
        val line = l.filter{ line -> line.filter{it.isDigit()}.isNotEmpty()}.firstOrNull()?.filter{it in "0123456789 "} // makes string of first line with numbers in it, only numbers and spaces
            ?: return 0 // gets only lines with digits in it, to lose lines like "TLC"
        val lineAsWords = line.split(" ").filter {it.isNotEmpty()}

        //return either the only numbers in it, or the second group (as first will be flightnumber)
        return if (lineAsWords.size == 1) lineAsWords[0].toInt() else lineAsWords[1].toInt()
    }

}

