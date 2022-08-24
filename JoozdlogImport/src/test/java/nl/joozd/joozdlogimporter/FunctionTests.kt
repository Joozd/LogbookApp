package nl.joozd.joozdlogimporter

import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionTests {
    @Test
    fun testMinutesFromString(){
        val decimal = "1.50" to 90
        val comma = "1,50" to 90
        val none = "1" to 60
        val colon = "1:30" to 90
        val longer = "10:30" to 630

        assertEquals(decimal.second, getMinutes(decimal.first))
        assertEquals(comma.second, getMinutes(comma.first))
        assertEquals(none.second, getMinutes(none.first))
        assertEquals(colon.second, getMinutes(colon.first))
        assertEquals(longer.second, getMinutes(longer.first))
    }

    private fun getMinutes(s: String): Int{
        if (':' in s) return minutesFromHoursAndMinutesString(s)
        val notANumberRegex = "[^0-9]".toRegex()
        val standardizedTimeString = s.replace(notANumberRegex, ".")
        val hours = standardizedTimeString.toDouble()
        val minutes = hours * 60
        return minutes.toInt()
    }

    private fun minutesFromHoursAndMinutesString(s: String): Int{
        require (":" in s) { "only use minutesFromHoursAndMinutesString for hh:mm"}
        val hmList = s.split(":").map { it.toInt() }
        if (hmList.size != 2) return 0 // only supports Hours:Minutes
        return hmList[1] + hmList[0]*60
    }
}