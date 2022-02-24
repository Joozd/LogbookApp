/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.joozdlogimporter.supportedFileTypes

import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class MccPilotLogFileTest {
    @Test
    fun test(){
        val fail = MccPilotLogFile.buildIfMatches(badData)
        assert (fail !is MccPilotLogFile)

        val t1 = MccPilotLogFile.buildIfMatches(data1)
        assert (t1 is MccPilotLogFile)
        val e1 = t1!!.extractCompletedFlights()

        assertEquals(1, e1.flights?.size)
        assertEquals(AirportIdentFormat.ICAO, e1.identFormat)

        val t2 = MccPilotLogFile.buildIfMatches(data2)
        assert (t2 is MccPilotLogFile)

        val e2 = t2!!.extractCompletedFlights()

        assertEquals(3, e2.flights?.size)
        assertEquals(AirportIdentFormat.IATA, e2.identFormat)

    }

    private val badData = """1-4-2007;;;;LEY;12:00;00:00;EHHV;12:35;00:00;C172;PH-CBN;;SELF;;;;;;;;;;;;;;;35;35;0;0;0;0;0;0;0;0;35;0;35;True;1;0;1;0;0;0;0;;Uitgechecked Ben Air;;;;0;False;0;0;0;;00:00;00:00;0
19-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 1;;;;0;False;0;0;0;;00:00;00:00;0
20-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 2;;;;0;False;0;0;0;;00:00;00:00;0""".lines()


    private val data1 = """mcc_DATE;IS_PREVEXP;AC_ISSIM;FlightNumber;AF_DEP;TIME_DEP;AF_ARR;TIME_ARR;AC_MODEL;AC_REG;PILOT1_ID;PILOT1_NAME;PILOT1_PHONE;PILOT1_EMAIL;PILOT2_ID;PILOT2_NAME;PILOT2_PHONE;PILOT2_EMAIL;PILOT3_ID;PILOT3_NAME;PILOT3_PHONE;PILOT3_EMAIL;PILOT4_ID;PILOT4_NAME;PILOT4_PHONE;PILOT4_EMAIL;TIME_TOTAL;TIME_PIC;TIME_PICUS;TIME_SIC;TIME_DUAL;TIME_INSTRUCTOR;TIME_EXAMINER;TIME_NIGHT;TIME_RELIEF;TIME_IFR;TIME_ACTUAL;TIME_HOOD;TIME_XC;PF;TO_DAY;TO_NIGHT;LDG_DAY;LDG_NIGHT;AUTOLAND;HOLDING;LIFT;INSTRUCTION;REMARKS;APP_1;APP_2;APP_3;PAX;DEICE;FUEL;FUELUSED;DELAY;FLIGHTLOG;TIME_TO;TIME_LDG;TIME_AIR
2014-01-14;FALSE;FALSE;LG8255;ELLX;12:08;LFMN;13:49;DHC8-400;LX-LGE;STC;Pilot STC;;;11388;self;;maarten.reerink@klm4u.com;BOJ;Pilot BOJ;;;;;;;101;0;0;101;0;0;0;0;0;101;101;0;0;FALSE;0;0;0;0;0;0;0;;;;;;29;FALSE;0;0;0;;00:00;00:00;0""".lines()

    private val data2 = """"mcc_DATE";"Is_PREVEXP";"AC_IsSIM";"FlightNumber";"AF_DEP";"TIME_DEP";"TIME_DEPSCH";"AF_ARR";"TIME_ARR";"TIME_ARRSCH";"AC_MODEL";"AC_REG";"PILOT1_ID";"PILOT1_NAME";"PILOT1_PHONE";"PILOT1_EMAIL";"PILOT2_ID";"PILOT2_NAME";"PILOT2_PHONE";"PILOT2_EMAIL";"PILOT3_ID";"PILOT3_NAME";"PILOT3_PHONE";"PILOT3_EMAIL";"PILOT4_ID";"PILOT4_NAME";"PILOT4_PHONE";"PILOT4_EMAIL";"TIME_TOTAL";"TIME_PIC";"TIME_PICUS";"TIME_SIC";"TIME_DUAL";"TIME_INSTRUCTOR";"TIME_EXAMINER";"TIME_NIGHT";"TIME_RELIEF";"TIME_IFR";"TIME_ACTUAL";"TIME_HOOD";"TIME_XC";"PF";"TO_DAY";"TO_NIGHT";"LDG_DAY";"LDG_NIGHT";"AUTOLAND";"HOLDING";"LIFT";"INSTRUCTION";"REMARKS";"APP_1";"APP_2";"APP_3";"Pax";"DEICE";"FUEL";"FUELUSED";"DELAY";"FLIGHTLOG";"TIME_TO";"TIME_LDG";"TIME_AIR"
1-4-2007;;;;LEY;12:00;00:00;EHHV;12:35;00:00;C172;PH-CBN;;SELF;;;;;;;;;;;;;;;35;35;0;0;0;0;0;0;0;0;35;0;35;True;1;0;1;0;0;0;0;;Uitgechecked Ben Air;;;;0;False;0;0;0;;00:00;00:00;0
19-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 1;;;;0;False;0;0;0;;00:00;00:00;0
20-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 2;;;;0;False;0;0;0;;00:00;00:00;0""".lines()

}