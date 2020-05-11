package nl.joozd.logbookapp.data.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.model.dataclasses.Flight

object FlightsListFunctions {
    fun makeListOfNamesOld(flights: List<Flight>): List<String> =
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString(",").split(",")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()

    /*
    suspend fun makeListOfNamesAsync (flights: List<Flight>) = withContext(Dispatchers.Default){
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString(",").split(",")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()
    }
    */

    suspend fun makeListOfNamesAsync (flights: List<Flight>) = withContext(Dispatchers.Default){
        flights.asSequence().map{listOfNotNull(it.name.nullIfEmpty(), it.name2).joinToString("|").split("|")}
            .flatten()
            .map{it.trim().nullIfEmpty()}
            .filterNotNull()
            .distinct().toList()
    }
}