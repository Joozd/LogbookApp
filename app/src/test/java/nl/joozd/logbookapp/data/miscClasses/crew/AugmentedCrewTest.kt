package nl.joozd.logbookapp.data.miscClasses.crew

import junit.framework.TestCase.assertEquals
import org.junit.Test

class AugmentedCrewTest {
    private val normalCrew = AugmentedCrew()
    private val coco = AugmentedCrew.coco(TAKEOFF_LANDING_TIME)
    private val fixedRest = AugmentedCrew(isFixedTime = true, times = FIXED_REST_TIME)
    private val fourManOnlyTakeoff = AugmentedCrew(size = 4, takeoff = true, landing = false, times = TAKEOFF_LANDING_TIME)
    private val fourManNoTakeoffLanding = AugmentedCrew(size = 4, takeoff = false, landing = false, times = TAKEOFF_LANDING_TIME)

    @Test
    fun testAugmentedCrewTimes(){
        println(1)
        assertEquals(EXPECTED_NORMAL, normalCrew.getLogTime(FOUR_HOURS, false))
        println(2)
        assertEquals(EXPECTED_COCO, coco.getLogTime(FOUR_HOURS, false))
        println(3)
        assertEquals(EXPECTED_FIXED, fixedRest.getLogTime(FOUR_HOURS, false))
        println(4)
        assertEquals(EXPECTED_FOUR_MAN_CREW_NO_TAKEOFF_LANDING, fourManNoTakeoffLanding.getLogTime(FOUR_HOURS, false))
        println(5)
        assertEquals(EXPECTED_FOUR_MAN_CREW_ONLY_TAKEOFF, fourManOnlyTakeoff.getLogTime(FOUR_HOURS, false))
        println(6)
        assertEquals(EXPECTED_FOUR_MAN_CREW_ONLY_TAKEOFF_PIC, fourManNoTakeoffLanding.getLogTime(FOUR_HOURS, true))
    }

    @Test
    fun testToFromInt(){
        val normalInt = normalCrew.toInt()
        val cocoInt = coco.toInt()
        val fixedRestInt = fixedRest.toInt()
        val fourManOnlyTakeoffInt = fourManOnlyTakeoff.toInt()
        val fourManNoTakeoffLandingInt = fourManNoTakeoffLanding.toInt()

        assertEquals(AugmentedCrew.fromInt(normalInt), normalCrew)
        assertEquals(AugmentedCrew.fromInt(cocoInt), coco)
        assertEquals(AugmentedCrew.fromInt(fixedRestInt), fixedRest)
        assertEquals(AugmentedCrew.fromInt(fourManOnlyTakeoffInt), fourManOnlyTakeoff)
        assertEquals(AugmentedCrew.fromInt(fourManNoTakeoffLandingInt), fourManNoTakeoffLanding)
    }


    companion object{
        private const val FOUR_HOURS = 4*60 // minutes
        private const val TAKEOFF_LANDING_TIME = 30
        private const val FIXED_REST_TIME = 90

        private const val EXPECTED_NORMAL = FOUR_HOURS
        private const val EXPECTED_COCO = (FOUR_HOURS - 2* TAKEOFF_LANDING_TIME) / 3 * 2
        private const val EXPECTED_FIXED = FOUR_HOURS - FIXED_REST_TIME
        private const val EXPECTED_FOUR_MAN_CREW_ONLY_TAKEOFF = (FOUR_HOURS - TAKEOFF_LANDING_TIME * 2) / 2 + TAKEOFF_LANDING_TIME
        private const val EXPECTED_FOUR_MAN_CREW_NO_TAKEOFF_LANDING = (FOUR_HOURS - TAKEOFF_LANDING_TIME * 2) / 2
        private const val EXPECTED_FOUR_MAN_CREW_ONLY_TAKEOFF_PIC = FOUR_HOURS
    }


}