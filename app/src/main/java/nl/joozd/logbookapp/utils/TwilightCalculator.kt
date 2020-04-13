/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.utils

import android.util.Log
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.extensions.roundToMinutes
import nl.joozd.logbookapp.extensions.toDegrees
import nl.joozd.logbookapp.extensions.toRadians
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.*


class TwilightCalculator(calculateDate: LocalDateTime) { // will know ALL the daylight an night data on calculateDate!
    private val day = Duration.between(solstice2000, calculateDate).toDays()
    private val date= calculateDate.toLocalDate()


    companion object{
        private val solstice2000 = LocalDateTime.of(2000, 12, 21, 12, 37) // future solsices are always n*365,25 days later
        private const val j = Math.PI / 182.625
        private const val axis = 23.439*Math.PI/180
        private const val twilightAngle = 6.0*Math.PI/180
        private val fiveDegrees = (-175..180 step 5).map{it.toDouble()}
        private const val LEFT = 1
        private const val RIGHT = 2

    }
    fun dayTime(lat: Double, lon: Double): ClosedRange<LocalDateTime> {
        val n = 1- tan(lat.toRadians()) *tan(
            axis *cos(
                j *day))+ twilightAngle /cos(lat.toRadians())
        val lengthOfDay = acos(1-n)/Math.PI
        // Log.d("latLon:", "$lat, $lon")
        // Log.d("lengthOfDay:", (lengthOfDay*24).toString())
        val utcOffset= ((lon/15)*60).toLong()
        val sunriseInZ = LocalDateTime.of(date,LocalTime.of(12,0)).minusMinutes((lengthOfDay*12*60).toLong()+utcOffset)
        val uncorrectedSunset = LocalDateTime.of(date,LocalTime.of(12,0)).plusMinutes((lengthOfDay*12*60).toLong()-utcOffset)
        val sunsetInZ = if (uncorrectedSunset < sunriseInZ) uncorrectedSunset.plusDays(1) else uncorrectedSunset
        // Log.d("sunrise:", "${sunriseInZ.toDateString()}, ${sunriseInZ.toTimeString()}")
        // Log.d("sunset:", "${sunsetInZ.toDateString()}, ${sunsetInZ.toTimeString()}")
        return (sunriseInZ..sunsetInZ)
    }
    fun itIsDayAt(airport: Airport, time: LocalTime): Boolean{
        // Log.d("requested time", "$time")
        return (dayTime(airport.latitude_deg, airport.longitude_deg).contains(LocalDateTime.of(date,time)))
    }
    fun itIsDayAt(lat: Double, lon: Double, time: LocalDateTime): Boolean{
        // Log.d("requested time", "$time")
        return (dayTime(lat, lon).contains(LocalDateTime.of(date,time.toLocalTime())))
    }

    fun minutesOfNight(orig: Airport?, dest: Airport?, departureTime: LocalDateTime, arrivalTime: LocalDateTime): Int {
        /****************************************************************************************
         * Will return the number of minutes of nighttime given an origin, a destination        *
         * and the times of departure / arrival. Will assume a rough great-circle track         *
         * is followed by going in a constant track to the next 5 degree longitude intersection *
         * this assumes flights never go "the long way around" (ie max 180 degrees lat diff.    *
         ****************************************************************************************/

        if (orig == null || dest == null) return 0

        Log.d("Requested locations", "orig latlon: ${orig.latitude_deg}, ${orig.longitude_deg}")
        Log.d("Requested locations", "dest latlon: ${dest.latitude_deg}, ${dest.longitude_deg}")

        val duration = Duration.between(departureTime, arrivalTime).toMinutes()
        Log.d("TwilightCalculator", "duration = $duration")

        // distance in degrees (*60 gives miles)
        fun getDistance(origLatLon: Pair<Double, Double>, destLatLon:Pair<Double, Double>) = acos(cos((90-origLatLon.first).toRadians())*cos((90-destLatLon.first).toRadians()) + sin((90-origLatLon.first).toRadians()) * sin((90-destLatLon.first).toRadians()) * cos((destLatLon.second-origLatLon.second).toRadians())).toDegrees()
        val distance=getDistance(Pair(orig.latitude_deg, orig.longitude_deg), Pair(dest.latitude_deg, dest.longitude_deg))
        Log.d ("TwilightCalculator", "Distance is $distance")

        // find the vertex of the great circle:
//        val sinSin = sin(abs(orig.longitude_deg-dest.longitude_deg).toRadians())/sin(distance.toRadians())
//        val depTrack = asin(sinSin * sin((90-orig.latitude_deg).toRadians())).toDegrees()
        //val depTrack = acos((cos((90-dest.latitude_deg).toRadians()) - cos((90-orig.latitude_deg).toRadians()) * cos(distance.toRadians())) / (sin(distance.toRadians() * sin((90-orig.latitude_deg).toRadians())))).toDegrees()
        val cosDepTrack = (sin(dest.latitude_deg.toRadians()) - sin(orig.latitude_deg.toRadians()) * cos(distance.toRadians())) / (cos (orig.latitude_deg.toRadians()) * sin(distance.toRadians()))
        val depTrack = acos(cosDepTrack).toDegrees()

        val departureTrack =    if (orig.longitude_deg < dest.longitude_deg) depTrack else 360.0 - depTrack
        // val departureTrack =    if (orig.longitude_deg < dest.longitude_deg) acos((sin(dest.latitude_deg.toRadians())-sin(orig.latitude_deg.toRadians())*cos(distance.toRadians())) / cos(orig.latitude_deg.toRadians()) * cos(distance.toRadians())).toDegrees()
        //                        else 360.0-acos((sin(dest.latitude_deg.toRadians())-sin(orig.latitude_deg.toRadians())*cos(distance.toRadians())) / cos(orig.latitude_deg.toRadians()) * cos(distance.toRadians())).toDegrees()

        Log.d ("TwilightCalculator", "departure track is $departureTrack")

        val deltaOrigToVertex = atan(1/(tan(departureTrack.toRadians()) * sin(orig.latitude_deg.toRadians()))).toDegrees()
        val vertexLongitude = orig.longitude_deg + deltaOrigToVertex
        val vertexLatitude = atan(tan(orig.latitude_deg.toRadians())/cos(deltaOrigToVertex.toRadians())).toDegrees()

        // a list of all "5 degrees" that are past, start and end.
        // not crossing dateline
        val left = minOf(orig.longitude_deg, dest.longitude_deg)
        val right = maxOf(orig.longitude_deg, dest.longitude_deg)
        val direction = if (left-right < 180) if (orig.longitude_deg < dest.longitude_deg) RIGHT else LEFT
                    else if (orig.longitude_deg < dest.longitude_deg) LEFT else RIGHT
        val longitudesPassed: List<Double> = if (abs(left - right) <180)
            if (direction == RIGHT) (listOf(left) + fiveDegrees.filter{x -> x in (left..right)} + listOf(right))
                else (listOf(right) + fiveDegrees.filter{x -> x in (left..right)}.reversed() + listOf(left))
            else if (direction == LEFT) (listOf(left) + fiveDegrees.filter{x -> x !in (left..right)}.reversed() + listOf(right))
                else (listOf(right) + fiveDegrees.filter{x -> x !in (left..right)} + listOf(left))
        // make sure people dont go the long way from 359 to 001; 361 degrees works just fine
        // Log.d("longitudes passed", "$longitudesPassed")

        // a list of potisions (Pair(lat,long))
        val positionsPassed: List<Pair<Double, Double>> = longitudesPassed.map{longitude ->
            val lat = atan(tan(vertexLatitude.toRadians()) * cos((vertexLongitude-longitude).toRadians())).toDegrees()
            Pair(lat, longitude)
        }
        // Log.d("positions passed", "$positionsPassed")

        fun shortStretch(outLatLon: Pair<Double, Double>, inLatLon: Pair<Double, Double>, departTime: LocalDateTime, arrivalTime: LocalDateTime): Int {
            val depTime = departTime.roundToMinutes()
            val arrTime = arrivalTime.roundToMinutes()
            val elapsedTime = Duration.between(depTime, arrTime).toMinutes()
            Log.d("elapsedTime", "$elapsedTime")
            val latIncrement = (inLatLon.first - outLatLon.first) / elapsedTime
            val longIncrement = (inLatLon.second - outLatLon.second) / elapsedTime
            var day = 0
            var night = 0
            for (minute: Long in (0 until elapsedTime)){
                if (itIsDayAt(outLatLon.first+latIncrement*minute, outLatLon.second+longIncrement*minute, depTime.plusMinutes(minute))) day++ else night++
            }
            Log.d("TwiLightCalc", "day: $day, night: $night (on this stretch) from $outLatLon to $inLatLon")
            return night
        }

        //Now, get total distance
        var totalDist = 0.0
        for (l in 0 until (positionsPassed.size -1)){
            totalDist += getDistance(positionsPassed[l], if (positionsPassed[l+1].first == 180.0) -180.0 to positionsPassed[l+1].second else(positionsPassed[l+1])) * 60
        }

        //Then times for the positions
        val timePerMile: Double = duration / (totalDist)
        Log.d("TwilightCalendar", "duration: $duration, distance: $totalDist, timePerMile: $timePerMile")

        val posPlusTimes: MutableList<Pair<Pair<Double, Double>, LocalDateTime>> = mutableListOf()
        posPlusTimes.add (positionsPassed[0] to departureTime)

        for (l in 0 until positionsPassed.size -1){
            val distance = getDistance(positionsPassed[l], if (positionsPassed[l+1].first == 180.0) -180.0 to positionsPassed[l+1].second else(positionsPassed[l+1])) * 60
            val elapsedSeconds = (distance * timePerMile *60).toLong()
            Log.d("TwilightCalculator", "distance = $distance")
            posPlusTimes.add(positionsPassed[l+1] to posPlusTimes[l].second.plusSeconds(elapsedSeconds))
            Log.d("TwilightCalculator", "added a stretch of $elapsedSeconds seconds!")
        }
        var darkMinutes = 0

        // finally, get the night times!!! Yaaay!
        for (l in 0 until posPlusTimes.size -1) {
            darkMinutes += shortStretch(
                posPlusTimes[l].first,
                posPlusTimes[l + 1].first,
                posPlusTimes[l].second,
                posPlusTimes[l + 1].second
            )
            Log.d("TwilightCalculator", "cumulative night minutes: $darkMinutes")
        }

        return darkMinutes

    }

}