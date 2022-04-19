package nl.joozd.logbookapp.data.importing

import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight

suspend fun Collection<Flight>.autocomplete(
    airportRepository: AirportRepository,
    aircraftRepository: AircraftRepository
): List<Flight> {
    val apDataCache = airportRepository.getAirportDataCache()
    val acDataCache = aircraftRepository.getAircraftDataCache()
    return this.map {
        ModelFlight.ofFlightAndDataCaches(it, apDataCache, acDataCache)
            .autoValues()
            .toFlight()
    }
}