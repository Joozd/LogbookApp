package nl.joozd.logbookapp.data.repository.helpers

import kotlinx.coroutines.flow.flow
import nl.joozd.logbookapp.data.dataclasses.Airport
import java.util.*

object FlowingAirportSearcher{
    /**
     * Makes a Flow that emits lists of matching Airports to query in order:
     * - Iata_code
     * - Ident
     * - City
     * - Airport name
     * @param fullList: Complete list of Airports to search
     * @param query: query to search by
     * @return Flow<List<Airport>> with found airports untill search is finished
     */
    fun makeFlow(fullList: List<Airport>,  query: String?) = flow {
        if (query == null) {
            emit(fullList)
        } else {
            val upperQuery = query.toUpperCase(Locale.ROOT)
            val result = mutableListOf<Airport>()
            var currentPosition = 0
            // pass1
            fullList.forEach {
                if (upperQuery in it.iata_code.toUpperCase(Locale.ROOT)) result += it
                emit(result)
            }
            //pass2
            fullList.forEach {
                if (upperQuery in it.ident.toUpperCase(Locale.ROOT)) result += it
                emit(result)
            }
            //pass3
            fullList.forEach {
                if (upperQuery in it.municipality.toUpperCase(Locale.ROOT)) result += it
                emit(result)
            }
            //pass4
            fullList.forEach {
                if (upperQuery in it.name.toUpperCase(Locale.ROOT)) result += it
                emit(result)
            }
        }
    }
}