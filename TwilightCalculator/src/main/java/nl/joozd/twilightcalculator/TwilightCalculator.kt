package nl.joozd.twilightcalculator

import java.time.Instant

interface TwilightCalculator {
    /**
     * @return true if it is day (including civil twilight), false if it is night.
     */
    fun itIsDayAt(time: Instant, latitude: Double, longitude: Double, includeTwilight: Boolean = true): Boolean

    /**
     * Gives the number of minutes this trip was done during the night
     */
    fun minutesOfNightOnTrip(startLat: Double, startLon: Double, endLat: Double, endLong: Double, departureTime: Instant, arrivalTime: Instant): Int

    /**
     * Lists of times at which a sunrise or sunset is expected to be. Usually 0 or 1, but can be higher in theory.
     */
    fun sunrisesSunsets(startLat: Double, startLon: Double, endLat: Double, endLong: Double, departureTime: Instant, arrivalTime: Instant): SunrisesSunsets

    data class SunrisesSunsets(val sunrises: List<Instant>, val sunsets: List<Instant>)

    companion object{
        operator fun invoke() = TwilightCalculatorImpl() // behaves like a constructor!
    }
}