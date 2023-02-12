package nl.joozd.joozdlogimporter.supportedFileTypes

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class KlmIcaMonthlyFileTest {
    @Test
    fun badData() {
        val fail = KlmIcaMonthlyFile.buildIfMatches(badData)
        assert(fail !is KlmIcaMonthlyFile)
    }

    @Test
    fun monthSixFlightsEast(){
        val file = KlmIcaMonthlyFile.buildIfMatches(sixFlightsEast)
        assert(file is KlmIcaMonthlyFile)
        assertEquals(6, (file as KlmIcaMonthlyFile).extractCompletedFlights().flights?.size)
    }

    @Test
    fun flightsWithInstructionAndDepartNextDayTest(){
        val file = KlmIcaMonthlyFile.buildIfMatches(eightFlightsWithTraining)
        assert(file is KlmIcaMonthlyFile)
        val flights = (file as KlmIcaMonthlyFile).extractCompletedFlights().flights!!
        println(flights.joinToString("\n"))
        assertEquals(8, flights.size)
        assert(with(flights.last()){
            orig == "PBM" && dest == "AMS"
        })
        val flightAfterMidnight = flights.first{ it.flightNumber == "KL706" }
        val dateOfFLightAfterMidnight = LocalDateTime.ofEpochSecond(flightAfterMidnight.timeOut, 0, ZoneOffset.UTC).dayOfMonth
        assertEquals(25, dateOfFLightAfterMidnight)
    }
}

private val badData = """1-4-2007;;;;LEY;12:00;00:00;EHHV;12:35;00:00;C172;PH-CBN;;SELF;;;;;;;;;;;;;;;35;35;0;0;0;0;0;0;0;0;35;0;35;True;1;0;1;0;0;0;0;;Uitgechecked Ben Air;;;;0;False;0;0;0;;00:00;00:00;0
19-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 1;;;;0;False;0;0;0;;00:00;00:00;0
20-4-2007;;sim;; ;00:00;00:00; ;00:00;00:00;MD11;SIM-MD11;;SELF;;;;;;;;;;;;;;;210;0;0;0;0;0;0;0;0;0;210;0;210;True;0;0;0;0;0;0;0;;TQ 2;;;;0;False;0;0;0;;00:00;00:00;0""".lines()

private val sixFlightsEast = """KLM ID: 00044692
DG LIJNNR ACREG
AANM
TIJD
VERT
TIJD VST
TIJD
V F
CMC
AST SDC
TIJD
V
AANK
TIJD
AFML
TIJD
WC
VLGU
NW
VLGU VDT
AC
RUST DP FDP RECALC
01 JV - Vakantie
02 JV - Vakantie
03 JV - Vakantie
04 JV - Vakantie
05 JV - Vakantie
06 KL 861 PHBQI   17:10 18:37 AMS +1 FO ICN +9 06:13 +1 06:43 +1 11:36 13:33 13:33 13:03 0.036
07 ICN
08 ICN
09 KL 831 PHBHO   05:30 06:31 ICN +9 FO HGH +8 08:55 2:24 46:47
09 KL 832 PHBHO   12:21 HGH +8 FO ICN +9 14:18 14:48 1:57 9:18 9:18 8:48
10 KL 868 PHBHF   14:20 15:29 ICN +9 FO AMS +1 05:15 +1 05:45 +1 13:46 15:25 23:32 15:25 14:55
11 AMS
Totalen:** 29:43 38:16
12 RV - Reisverlof
13 RV - Reisverlof
14 RV - Reisverlof
15 RV - Reisverlof
16 12:15 KS - Keuren schiphol 14:15 2:00
17 KL 595 PHBHM   16:10 17:39 AMS +1 FO CPT +2 04:54 +1 05:24 +1 11:15 13:14 13:14 12:44 -0.397
18 CPT
19 CPT
20 KL 598 PHBVN   22:00 23:07 CPT +2 FO AMS +1 10:21 +1 10:51 +1 11:14 12:51 64:36 12:51 12:21
21 AMS
Totalen:** 22:29 26:05
22 RV - Reisverlof
23 RV - Reisverlof
24 RV - Reisverlof
25 ZBV - Ziek Buiten Verlof
26 ZBV - Ziek Buiten Verlof
27 ZBV - Ziek Buiten Verlof
28 ZBV - Ziek Buiten Verlof
29 EVR - Extra Vry
30 EVR - Extra Vry
31 EVR - Extra Vry
STRALINGSDOSIS: DEC/2022/12 MND 0.16/1.73/1.73 mSv VLIEGUREN IN PERIODE* 52:12
VOOR VRAGEN OVER VLIEGUREN: FLIGHTCREWSUPPORT@KLM.COM VLIEGUREN IN JAAR: 456:30
VOOR INFO OVER STRALING ZIE MYKLM.KLM.COM/NL/WEB/HUMAN-RESOURCES/KLIMAAT-AAN-BOORD-EN-STRALING. VLIEGUREN TOTAAL: 8,636:06
REACTIES OUDER DAN 3 MAANDEN WORDEN NIET MEER IN BEHANDELING GENOMEN. * Vlieguren in periode beginnende op de 1e dag van de maand 00:00
   en eindigend op laatste dag van de maand 24:00 (Local time Homebase)
** Totaal uren van de omloop startende in deze maand
Datum: 08-02-23 08:09
Chronologisch overzicht Vlieguren
Base: AMS
Naam:  WELLE, J 
Posting: FO - 778 Crew Type: Cockpit
Periode: 01-01-2023 t/m 31-01-2023
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ENDROSTER - F_00044692 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<""".lines()

private val eightFlightsWithTraining = """KLM ID: 00044692

DG LIJNNR ACREG
AANM
TIJD
VERT
TIJD VST
TIJD
V F
CMC
AST SDC
TIJD
V
AANK
TIJD
AFML
TIJD
WC
VLGU
NW
VLGU VDT
AC
RUST DP FDP RECALC

01 RV - Reisverlof
02 RVF - Reisverlof
Inneembaar
03 NI - Niet Indeelbaar
04 KL 677 PHBHH 09:20 11:24 AMS +2 FO YYC RIQ -6 19:53 20:23 8:29 11:03 11:03 10:33 -0.497
05 KL 678 PHBHN 20:10 21:19 YYC -6 FO AMS RIQ +2 05:49 +1 06:19 +1 8:30 10:09 23:47 10:09 9:39
06 AMS

Totalen:** 16:59 21:12

07 RV - Reisverlof
08 RV - Reisverlof
09 RV - Reisverlof
10 RV - Reisverlof
11 RVF - Reisverlof
Inneembaar
12 NI - Niet Indeelbaar
13 KL 597 PHBQK 07:35 08:55 AMS +2 SO CPT RIQ +2 20:05 20:35 11:10 13:00 13:00 12:30 0.441
14 CPT
15 KL 598 PHBQN 20:45 21:41 CPT +2 SO AMS RIQ +2 09:19 +1 09:49 +1 11:38 13:04 48:10 13:04 12:34
16 AMS 0.018 H

Totalen:** 22:48 26:04

17 RV - Reisverlof
18 RV - Reisverlof
19 RV - Reisverlof
20 RVF - Reisverlof
Inneembaar
21 NI - Niet Indeelbaar
22 KL 705 PHBHG 09:45 11:37 AMS +2 SO GIG RIQ -3 22:56 23:26 11:19 13:41 13:41 13:11 -0.409
23 GIG
24 KL 706 PHBHC 23:40 00:52 +1 GIG -3 SO AMS RIQ +2 11:55 +1 12:25 +1 11:03 12:45 48:14 12:45 12:15
25 AMS

Totalen:** 22:22 26:26

26 RV - Reisverlof
27 RV - Reisverlof
28 RV - Reisverlof
29 RV - Reisverlof
30 KL 713 PHBVU 09:00 11:34 AMS +1 SO PBM -3 20:48 21:18 9:14 12:18 12:18 11:48 -0.258
31 KL 714 PHBVS 18:30 20:15 PBM -3 SO AMS LCI,
LC
+1 04:50 +1 05:20 +1 8:35 10:50 21:12 10:50 10:20

Totalen:** 17:49 23:08

Datum: 07-11-22 08:56 Chronologisch overzicht Vlieguren
Base: AMS

Naam: WELLE, J
Posting: FO - 778 Crew Type: Cockpit
Periode: 01-10-2022 t/m 31-10-2022

STRALINGSDOSIS: SEP/2022/12 MND 0.19/1.05/1.33 mSv VLIEGUREN IN PERIODE* 74:08
VOOR VRAGEN OVER VLIEGUREN: FLIGHTCREWSUPPORT@KLM.COM VLIEGUREN IN JAAR: 277:39
VOOR INFORMATIE OVER STRALING ZIE KLM4U.COM > HEALTH & WELL-BEING > KLIMAAT AAN BOORD EN STRALING. VLIEGUREN TOTAAL: 8,457:15
REACTIES OUDER DAN 3 MAANDEN WORDEN NIET MEER IN BEHANDELING GENOMEN. * Vlieguren in periode beginnende op de 1e dag van de maand 00:00
en eindigend op laatste dag van de maand 24:00 (Local time Homebase)
** Totaal uren van de omloop startende in deze maand""".lines()