/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.joozdlogfiletypedetector

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import java.io.InputStream

/**
 * Detects type in csv and txt files
 */
class CsvTypeDetector(inputStream: InputStream): FileTypeDetector {
    private val lines = try{
        inputStream.reader().readLines()
    } catch (e: Exception){ null }

    override val seemsValid: Boolean
        get() = lines != null && lines.isNotEmpty()
    override val typeOfFile
        get() = getType(lines)

    override val debugData: String
        get() = lines?.firstOrNull() ?: "input is invalid or empty"


    private fun getType(lines: List<String>?): SupportedTypes.SupportedType = when (lines?.firstOrNull()){
        MCC_PILOT_LOG_CSV_IDENTIFIER -> SupportedTypes.MCC_PILOT_LOG_LOGBOOK
        JOOZDLOG_V4_IDENTIFIER -> SupportedTypes.JOOZDLOG_CSV_BACKUP
        LOGTEN_PRO_IDENTIFIER -> SupportedTypes.LOGTEN_PRO_LOGBOOK
        else -> SupportedTypes.UNSUPPORTED_CSV
    }

    companion object{
        private const val MCC_PILOT_LOG_CSV_IDENTIFIER = "\"mcc_DATE\";\"Is_PREVEXP\";\"AC_IsSIM\";\"FlightNumber\";\"AF_DEP\";\"TIME_DEP\";\"TIME_DEPSCH\";\"AF_ARR\";\"TIME_ARR\";\"TIME_ARRSCH\";\"AC_MODEL\";\"AC_REG\";\"PILOT1_ID\";\"PILOT1_NAME\";\"PILOT1_PHONE\";\"PILOT1_EMAIL\";\"PILOT2_ID\";\"PILOT2_NAME\";\"PILOT2_PHONE\";\"PILOT2_EMAIL\";\"PILOT3_ID\";\"PILOT3_NAME\";\"PILOT3_PHONE\";\"PILOT3_EMAIL\";\"PILOT4_ID\";\"PILOT4_NAME\";\"PILOT4_PHONE\";\"PILOT4_EMAIL\";\"TIME_TOTAL\";\"TIME_PIC\";\"TIME_PICUS\";\"TIME_SIC\";\"TIME_DUAL\";\"TIME_INSTRUCTOR\";\"TIME_EXAMINER\";\"TIME_NIGHT\";\"TIME_RELIEF\";\"TIME_IFR\";\"TIME_ACTUAL\";\"TIME_HOOD\";\"TIME_XC\";\"PF\";\"TO_DAY\";\"TO_NIGHT\";\"LDG_DAY\";\"LDG_NIGHT\";\"AUTOLAND\";\"HOLDING\";\"LIFT\";\"INSTRUCTION\";\"REMARKS\";\"APP_1\";\"APP_2\";\"APP_3\";\"Pax\";\"DEICE\";\"FUEL\";\"FUELUSED\";\"DELAY\";\"FLIGHTLOG\";\"TIME_TO\";\"TIME_LDG\";\"TIME_AIR\""
        private const val JOOZDLOG_V4_IDENTIFIER = "flightID;Origin;dest;timeOut;timeIn;correctedTotalTime;nightTime;ifrTime;simTime;aircraftType;registration;name;name2;takeOffDay;takeOffNight;landingDay;landingNight;autoLand;flightNumber;remarks;isPIC;isPICUS;isCoPilot;isDual;isInstructor;isSim;isPF;isPlanned;autoFill;augmentedCrew;signature"
        private const val LOGTEN_PRO_IDENTIFIER = "flight_flightDate\tflight_type\tflight_flightNumber\t flight_from\t flight_to\t flight_selectedCrewPIC\t flight_selectedCrewSIC\t flight_selectedCrewRelief\t flight_selectedCrewRelief2\t flight_selectedCrewRelief3\t flight_selectedCrewRelief4\t flight_selectedCrewFlightEngineer\t flight_selectedCrewInstructor\t flight_selectedCrewStudent\t flight_selectedCrewObserver\t flight_selectedCrewObserver2\t flight_selectedCrewPurser\t flight_selectedCrewFlightAttendant\t flight_selectedCrewFlightAttendant2\t flight_selectedCrewFlightAttendant3\t flight_selectedCrewFlightAttendant4\t flight_selectedCrewCommander\t flight_selectedCrewCustom1\t flight_selectedCrewCustom2\t flight_selectedCrewCustom3\t flight_selectedCrewCustom4\t flight_selectedCrewCustom5\t flight_scheduledDepartureTime\t flight_actualDepartureTime\t flight_scheduledArrivalTime\t flight_actualArrivalTime\t flight_taxiOutTime\t  flight_taxiInTime\t flight_totalPushTime\t flight_hobbsStart\tflight_hobbsStop\tflight_tachStart\tflight_tachStop\tflight_distance\t flight_route\t flight_totalTime\t flight_pic\t flight_sic\t flight_night\t flight_crossCountry\t flight_actualInstrument\t flight_simulatedInstrument\t flight_dualReceived\t flight_dualGiven\t flight_simulator\t flight_solo\t flight_flightEngineer\t flight_relief\t flight_nightVisionGoggle\t flight_p1us\t flight_multiPilot\t flight_ground\t flight_sfi\t flight_picNight\t flight_sicNight\t flight_dualReceivedNight\t flight_p1usNight\t flight_scheduledTotalTime\t flight_customTime1\t flight_customTime2\t flight_customTime3\t flight_customTime4\t flight_customTime5\t flight_customTime6\t flight_customTime7\t flight_customTime8\t flight_customTime9\t flight_customTime10\tflight_customTime11\t flight_customTime12\t flight_customTime13\t flight_customTime14\t flight_customTime15\t flight_customTime16\t flight_customTime17\t flight_customTime18\t flight_customTime19\t flight_customTime20\t flight_dayLandings\t flight_dayTakeoffs\t flight_nightLandings\t flight_nightTakeoffs\t flight_waterLandings\t flight_waterTakeoffs\t flight_shipboardLandings\t flight_shipboardTakeoffs\t flight_nightVisionGoggleLandings\t flight_nightVisionGoggleTakeoffs\t flight_customLanding1\t flight_customTakeoff1\t flight_customLanding2\t flight_customTakeoff2\t flight_customLanding3\t flight_customTakeoff3\t flight_customLanding4\t flight_customTakeoff4\t flight_customLanding5\t flight_customTakeoff5\t flight_customLanding6\t flight_customTakeoff6\t flight_customLanding7\t flight_customTakeoff7\t flight_customLanding8\t flight_customTakeoff8\t flight_customLanding9\t flight_customTakeoff9\t flight_customLanding10\t flight_customTakeoff10\t flight_touchAndGoes\t flight_fullStops\t flight_autolands\t flight_selectedApproach1\tflight_selectedApproach2\tflight_selectedApproach3\tflight_selectedApproach4\tflight_selectedApproach5\tflight_selectedApproach6\tflight_selectedApproach7\tflight_selectedApproach8\tflight_selectedApproach9\tflight_selectedApproach10\t flight_catII\t flight_catIII\t flight_holds\tflight_ifr\tflight_ifrCapacity\t flight_aeroTows\t flight_groundLaunches\t flight_poweredLaunches\t flight_goArounds\t flight_customOp1\t flight_customOp2\t flight_customOp3\t flight_customOp4\t flight_customOp5\t flight_customOp6\t flight_customOp7\t flight_customOp8\t flight_customOp9\t flight_customOp10\t flight_customOp11\t flight_customOp12\t flight_customOp13\t flight_customOp14\t flight_customOp15\t flight_customOp16\t flight_customOp17\t flight_customOp18\t flight_customOp19\t flight_customOp20\t flight_onDutyTime\t flight_offDutyTime\t flight_totalDutyTime\tflight_flightDutyStartTime\t flight_flightDutyEndTime\t flight_flightDutyTotal\tflight_rest\t flight_landingTime\t flight_takeoffTime\t flight_duration\t flight_pilotFlyingCapacity\t flight_picCapacity\t flight_sicCapacity\t flight_landingCapacity\t flight_underSupervisionCapacity\t flight_flightEngineerCapacity\t flight_reliefCrewCapacity\t flight_far1\t flight_faaPart61\t flight_faaPart91\t flight_faaPart121\t flight_faaPart135\tflight_customCapacity1\tflight_customCapacity2\tflight_customCapacity3\tflight_customCapacity4\tflight_customCapacity5\tflight_customCapacity6\tflight_customCapacity7\tflight_customCapacity8\tflight_customCapacity9\tflight_customCapacity10\tflight_customCapacity11\tflight_customCapacity12\tflight_customCapacity13\tflight_customCapacity14\tflight_customCapacity15\tflight_customCapacity16\tflight_customCapacity17\tflight_customCapacity18\tflight_customCapacity19\tflight_customCapacity20\t flight_remarks\t flight_leg\t flight_legCount\t flight_weather\t flight_sky\t flight_visibility\t flight_cloudbase\t flight_windDirection\t flight_windVelocity\t flight_review\t flight_instrumentProficiencyCheck\t flight_customNote1\t flight_customNote2\t flight_customNote3\t flight_customNote4\t flight_customNote5\t flight_customNote6\t flight_customNote7\t flight_customNote8\t flight_customNote9\t flight_customNote10\t flight_fuelAdded\t flight_fuelBurned\t flight_fuelMinimumForDiversion\t flight_fuelTotalAboard\t flight_fuelTotalBeforeUplift\t flight_fuelUplift\t aircraft_aircraftID\t aircraft_secondaryID\t aircraft_weight\t aircraft_selectedOperatorName\t aircraft_selectedOwnerName\t aircraft_serialNumber\t aircraft_notes\t aircraft_aerobatic\t aircraft_autoEngine\t aircraft_complex\t aircraft_efis\t aircraft_experimental\t aircraft_fuelInjection\t aircraft_highPerformance\t aircraft_military\t aircraft_pressurized\t aircraft_radialEngine\t aircraft_tailwheel\t aircraft_technicallyAdvanced\t aircraft_turboCharged\t aircraft_undercarriageAmphib\t aircraft_undercarriageFloats\t aircraft_undercarriageRetractable\t aircraft_undercarriageSkids\t aircraft_undercarriageSkis\t aircraft_warbird\t aircraft_wheelConfiguration\t aircraft_customAttribute1\t aircraft_customAttribute2\t aircraft_customAttribute3\t aircraft_customAttribute4\t aircraft_customAttribute5\t aircraft_year\t aircraft_customText1\t aircraft_customText2\t aircraft_customText3\t aircraft_customText4\t aircraftType_type\t aircraftType_make\t aircraftType_model\t aircraftType_selectedEngineType\t aircraftType_selectedCategory\t aircraftType_selectedAircraftClass\t aircraftType_notes\tflight_selectedPax1\tflight_selectedPax2\tflight_selectedPax3\tflight_selectedPax4\tflight_selectedPax5\tflight_selectedPax6\tflight_selectedPax7\tflight_selectedPax8\tflight_selectedPax9\tflight_selectedPax10\tflight_paxCount\tflight_paxCountBusiness"
    }
}