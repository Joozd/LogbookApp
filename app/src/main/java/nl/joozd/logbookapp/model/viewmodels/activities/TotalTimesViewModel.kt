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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jjoe64.graphview.ValueDependentColor
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.abs

class TotalTimesViewModel: JoozdlogActivityViewModel() {
    /**
     * STUB
     * Fills a bar graph with time per month
     */
    fun fillGraph() = viewModelScope.launch {
        setText("Started")
        val allFlights = flightRepository.getAllFlights()
        setText("got flights")
        val now = Instant.now()
        //latestDate = first day of next month
        val latestDate = LocalDate.now().withDayOfMonth(1)
        setText("latest Date: $latestDate")
        //oldestDate = first day of month in which oldest flight happens
        val oldestDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(allFlights.minBy { it.timeOut }?.timeOut ?: now.epochSecond), ZoneOffset.UTC).toLocalDate().withDayOfMonth(1)
        val monthsToPlot = mutableListOf(oldestDate)

        var i = 1L
        while (monthsToPlot.last() < latestDate){
            monthsToPlot.add(oldestDate.plusMonths(i++))
        }
        val monthWithTimes = mutableMapOf<LocalDate, Long>()
        setText("monthWithTimes: ${monthWithTimes.size}")

        monthsToPlot.forEach{ month ->
            val start = month.atTime(0,0).toInstant(ZoneOffset.UTC).epochSecond
            val end = month.plusMonths(1).minusDays(1).atTime(23,59).toInstant(ZoneOffset.UTC).epochSecond
            monthWithTimes[month] = allFlights
                .filter { it.timeOut in (start..end) }
                .map { it.timeIn - it.timeOut }
                .sum() / 60
        }
        // Now we have a map of Months with matching total flight times in minutes

        val dataPoints = monthsToPlot.mapIndexed{j, month ->
            setText("working at $j/${monthsToPlot.size} ...")
            DataPoint(month.atStartOfDay(ZoneOffset.UTC).toInstant().epochSecond.toDouble(), (monthWithTimes[month] ?: -100).toDouble())}

        val times = monthsToPlot.map { monthWithTimes[it] ?: 0 }
        val firstYear = (0 until if (times.size > 11)11 else times.size).map{
            times.slice(0..it).average()}
        val rest = times.windowed(12, 1).map{it.average()}

        val movingAverage: List<Double> = firstYear + rest
        Log.d("COUNTING", "${firstYear.size} / ${rest.size} / ${movingAverage.size}" )

        val avgDataPoints = monthsToPlot.mapIndexed { j, month ->
            setText("working at $j/${monthsToPlot.size} ...")
            DataPoint(month.atStartOfDay(ZoneOffset.UTC).toInstant().epochSecond.toDouble(), movingAverage[j])}


        barGraphData.value = BarGraphSeries(dataPoints.toTypedArray()).apply{
            color = Color.BLUE
            spacing = 10
            isDrawValuesOnTop = false
            valuesOnTopColor= Color.RED
            setOnDataPointTapListener { _, dataPoint ->
                feedback(FeedbackEvents.GenericEvents.EVENT_1).apply {
                    val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(dataPoint.x.toLong()), ZoneOffset.UTC).plusMinutes(1) // add a minutes fo rrounding errors
                    val dateString = "${date.year.toString().takeLast(2)}/${date.month.value.toString().padStart(2, '0')}"
                    extraData.putDouble("value", dataPoint.y)
                    extraData.putString("date", dateString)
                }
            }
        }

        lineGraphData.value = LineGraphSeries(avgDataPoints.toTypedArray()).apply{
            color = Color.RED
            thickness = 8
            isDrawDataPoints = true
            dataPointsRadius = 16.0f
            setOnDataPointTapListener { _, dataPoint ->
                feedback(FeedbackEvents.GenericEvents.EVENT_1).apply {
                    val date = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(dataPoint.x.toLong()),
                        ZoneOffset.UTC
                    ).plusMinutes(1) // add a minutes fo rrounding errors
                    val dateString =
                        "${date.year.toString().takeLast(2)}/${date.month.value.toString()
                            .padStart(2, '0')}"
                    extraData.putDouble("value", dataPoint.y)
                    extraData.putString("date", dateString)
                }
            }
        }

        setText("Should be done")




    }
    val barGraphData = MutableLiveData<BarGraphSeries<DataPoint>>()
    val lineGraphData = MutableLiveData<LineGraphSeries<DataPoint>>()
    val text = MutableLiveData<String>()
    fun setText(it: String){
        Log.d("Settext", it)
        text.value = it
    }




}



