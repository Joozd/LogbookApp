package nl.joozd.twilightcalculator

import nl.joozd.twilightcalculator.utils.toDegrees
import nl.joozd.twilightcalculator.utils.toRadians
import org.shredzone.commons.suncalc.SunPosition
import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class TwilightCalculatorImpl: TwilightCalculator {
    /**
     * @return true if it is day (including civil twilight), false if it is night.
     */
    override fun itIsDayAt(time: Instant, latitude: Double, longitude: Double, includeTwilight: Boolean): Boolean =
        itIsDayAt(time, Location(latitude, longitude), includeTwilight)

    private fun itIsDayAt(time: Instant, location: Location, includeTwilight: Boolean = true): Boolean =
        SunPosition.compute()
            .at(location.latitude, location.longitude)
            .on(time)
            .execute()
            .altitude >= if (includeTwilight) TWILIGHT_MINIMUM_ANGLE else 0.0


    /**
     * Gives the number of minutes this trip was done during the night
     */
    override fun minutesOfNightOnTrip(startLat: Double, startLon: Double, endLat: Double, endLong: Double, departureTime: Instant, arrivalTime: Instant): Int {
        val positionsOnRoute = routeToPositionsPerMinute(departureTime, arrivalTime, startLat, startLon, endLat, endLong)
        return positionsOnRoute.indices.count { i ->
            !itIsDayAt(departureTime + i.minutes.toJavaDuration(), positionsOnRoute[i])
        }
    }

    /**
     * List of times at which a sunrise is expected to be. Usually 0 or 1, but can be higher in theory.
     */
    override fun sunrisesSunsets(startLat: Double, startLon: Double, endLat: Double, endLong: Double, departureTime: Instant, arrivalTime: Instant): TwilightCalculator.SunrisesSunsets {
        val sunrises = ArrayList<Instant>()
        val sunsets = ArrayList<Instant>()
        val positions = routeToPositionsPerMinute(departureTime, arrivalTime, startLat, startLon, endLat, endLong)
        var itIsDay = itIsDayAt(departureTime, positions.first(), includeTwilight = false)

        // Iterate over all positions and add positions where sun changes from below to above horizon or VV to the respective list
        positions.indices.forEach { i ->
            val time = departureTime.plusSeconds(60L*i  )
            val isDayAtPosition = itIsDayAt(time, positions[i], includeTwilight = false)
            if (isDayAtPosition == itIsDay) return@forEach // no sunrise/sunset event
            if(isDayAtPosition)
                sunrises.add(time)
            else
                sunsets.add(time)
            itIsDay = isDayAtPosition
        }
        return TwilightCalculator.SunrisesSunsets(sunrises, sunsets)
    }

    private fun routeToPositionsPerMinute(
        departureTime: Instant,
        arrivalTime: Instant,
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLong: Double
    ): List<Location> {
        val duration = Duration.between(departureTime, arrivalTime).toMinutes().toInt()
        val startLocation = Location(startLat, startLon)
        val endLocation = Location(endLat, endLong)
        return getPointsAlongRoute(startLocation, endLocation, duration)
    }

    /**
     * Get a List of [amountOfPoints] points along a route from [origin] to [destination]
     */
    private fun getPointsAlongRoute(origin: Location, destination: Location, amountOfPoints: Int) = buildList{
        // Convert latitudes and longitudes from degrees to radians
        val lat1 = origin.latitude.toRadians()
        val lon1 = origin.longitude.toRadians()
        val lat2 = destination.latitude.toRadians()
        val lon2 = destination.longitude.toRadians()

        // calculates cosines and sines so we only have to do that once.
        val cosLat1 = cos(lat1)
        val cosLat2 = cos(lat2)
        val sinLat1 = sin(lat1)
        val sinLat2 = sin(lat2)
        val deltaLon = lon2 - lon1

        // Calculate the initial bearing
        val initialBearing = atan2(sin(deltaLon) * cosLat2, cosLat1 * sinLat2 - sinLat1 * cosLat2 * cos(deltaLon))

        // Calculate the total distance (d) between the points using the Haversine formula
        val d = 2 * asin(sqrt(haversine(lat1, lat2) + cosLat1 * cosLat2 * sin(deltaLon / 2).pow(2.0)))
        println("OVERALL Distance: $d")

        // calculate and add each intermediate point
        repeat(amountOfPoints) { i ->
            val fraction = i.toDouble() / amountOfPoints
            val aDist = fraction * d // angular distance, in radians
            val lat = asin(sinLat1 * cos(aDist) + cosLat1 * sin(aDist) * cos(initialBearing))
            val lon = lon1 + atan2(sin(initialBearing) * sin(aDist) * cosLat1, cos(aDist) - sinLat1 * sin(lat))

            // Add the intermediate point to the waypoints list
            add(Location(lat.toDegrees(), lon.toDegrees()))
        }
    }

    private fun haversine(lat1: Double, lat2: Double): Double =
        sin((lat2 - lat1) / 2).pow(2.0)

    private data class Location(val latitude: Double, val longitude: Double){
        override fun toString() = "(${String.format("%.3f", latitude)}, ${String.format("%.3f", longitude)})"
    }



    companion object{
        private const val TWILIGHT_MINIMUM_ANGLE = -6.0
    }
}