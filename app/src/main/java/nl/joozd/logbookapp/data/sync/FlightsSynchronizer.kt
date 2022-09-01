package nl.joozd.logbookapp.data.sync

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.FlightsListChecksum
import nl.joozd.joozdlogcommon.IDWithTimeStamp
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.CloudFunctionResult
import nl.joozd.logbookapp.comm.ServerFunctionResult
import nl.joozd.logbookapp.comm.ServerPrefs
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.importing.merging.mergeFlightsLists
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker

class FlightsSynchronizer(
    private val cloud: Cloud,
    private val userManagement: UserManagement,
    private val repository: FlightRepositoryWithDirectAccess
) {

    /*
     * This part makes sure we
     */
    private var repositoryWasChanged: Boolean = false

    private val repositoryChangedListener = FlightRepository.OnDataChangedListener{
        if (it.isNotEmpty()) repositoryWasChanged = true
    }

    suspend fun synchronizeIfNotSynced(): ServerFunctionResult = with(cloud) {
        if (!UserManagement().checkIfLoginDataSet())
            ServerFunctionResult.FAILURE
        else {
            registerRepositoryChangedListener()
            try {
                performSyncIfNotSynced()
            }
            finally { unregisterRepositoryChangedListener() }
        }
    }

    suspend fun mergeRepoWithServer(): ServerFunctionResult = with(cloud){
        doInOneClientSession {
            try {
                login()
                val serversFlights = getAllFlightsFromServer().map { Flight(it) }
                repository.doLocked {
                    val repoFlights = getAllFlightsInDB() // note: This contains planned flights which must not go to server
                    val mergedFlightsList = mergeFlightsLists(serversFlights, repoFlights)
                    replaceAllFlightsWith(mergedFlightsList)
                }
                performSync()
                ServerFunctionResult.SUCCESS
            }
            catch (e: CloudException){
                e.cloudFunctionResult.correspondingServerFunctionResult()
            }
        }
    }

    private suspend fun performSyncIfNotSynced(): ServerFunctionResult = with(cloud){
        try {
            doInOneClientSession {
                login()
                if(TaskFlags.killDuplicates.flow.first()){
                    killDuplicatesOnServer()
                }
                if (!checkIfIsSynchronized())
                    performSync()
            }
            markMostRecentSyncAsNow()
            return ServerFunctionResult.SUCCESS
        }
        catch (e: CloudException){
            return e.cloudFunctionResult.correspondingServerFunctionResult()
        }
    }



    private suspend fun Client.performSync(): Unit = with(cloud) {
        val serverIdsAndTimestamps = getIDWithTimestampsListFromServer()
        resetRepositoryInterruptionCheck()
        correctIDsForNewFlights(serverIdsAndTimestamps) // No need to remember, they get fetched again.
        downloadUpdatedFlightsFromServer(serverIdsAndTimestamps)
        sendUpdatedFlightsToServer(serverIdsAndTimestamps)
        if (repositoryWasChanged) return performSync() // if somebody touched repository, sync again.
    }

    private suspend fun Client.killDuplicatesOnServer(): Unit = with(cloud){
        killDuplicates()
        TaskFlags.killDuplicates(false) // if killDuplicates() fails, it will throw exception and we will not get here
    }

    private suspend fun Client.login() = with (cloud){
        loginOrThrowException(
            userManagement.getUsernameWithKey()
                ?: throw(CloudException(CloudFunctionResult.SERVER_REFUSED))
        )
    }

    //Throws a CloudException if unable to get server checksum
    private suspend fun Client.checkIfIsSynchronized(): Boolean {
        val serverChecksumAsync = getFLightsListChecksumFromServerAsync()
        val repositoryChecksumAsync = getFlightsListChecksumFromRepositoryAsync()
        return repositoryChecksumAsync.await() == serverChecksumAsync.await()
    }

    private fun getFlightsListChecksumFromRepositoryAsync() =
        MainScope().async { FlightsListChecksum(repository.getSyncableBasicFlights()) }

    private fun Client.getFLightsListChecksumFromServerAsync() = with (cloud) {
        MainScope().async { getFlightsListChecksum() }
    }

    private suspend fun correctIDsForNewFlights(takenIDs: List <IDWithTimeStamp>): List<Flight> =
        repository.doLocked { // locked so these flights won't change while being updated, which could lead to duplicate entries{
            val newFlightsUncheckedIDs = getSyncableFlights().filter { it.unknownToServer }
            val highestTakenID = takenIDs.maxOfOrNull { it.ID } ?: -1
            fixIDs(newFlightsUncheckedIDs, highestTakenID)
        }

    private suspend fun Client.downloadUpdatedFlightsFromServer(serverIdWithTimestamps: List<IDWithTimeStamp>) = with(cloud) {
        repository.doLocked {
            val localIDWithTimeStamps = getSyncableBasicFlights().map { IDWithTimeStamp(it) }
            val idsToDownload = getNewOrNewerIDsInSecondParam(localIDWithTimeStamps, serverIdWithTimestamps)
            val downloadedFlights = downloadFlightsFromServer(idsToDownload).map { Flight(it) }
            repository.saveDirectToDB(downloadedFlights)
        }
    }

    private suspend fun Client.getAllFlightsFromServer(): List<BasicFlight> = with(cloud) {
        val ids = getIDWithTimestampsListFromServer().map { it.ID }
        downloadFlightsFromServer(ids)
    }

    private suspend fun Client.sendUpdatedFlightsToServer(serverIdWithTimestamps: List<IDWithTimeStamp>) = with(cloud){
        repository.doLocked {
            val localIDWithTimeStamps = getSyncableBasicFlights().map { IDWithTimeStamp(it) }
            val idsToUpload = getNewOrNewerIDsInSecondParam(serverIdWithTimestamps, localIDWithTimeStamps)
            val flightsToUpload = getFlightsByID(idsToUpload)
            sendFlightsToServer(flightsToUpload)
        }
    }

    private fun getNewOrNewerIDsInSecondParam(
        idsToCheckTo: List<IDWithTimeStamp>,
        listWithPossibleNewerIDs: List<IDWithTimeStamp>)
            : List<Int> {
        val map = idsToCheckTo.toMap()
        return listWithPossibleNewerIDs.filter { it.timeStamp > (map[it.ID] ?: Long.MIN_VALUE) }.map { it.ID }
    }


    private suspend fun FlightRepositoryWithDirectAccess.fixIDs(flightsToFix: List<Flight>, highestTakenID: Int): List<Flight>{
        if (flightsToFix.none { it.flightID <= highestTakenID}) return flightsToFix
        val highestTakenIdIncludingFlightsToFix = maxOf(highestTakenID, flightsToFix.maxOf { it.flightID })

        // This is done flight-by-flight to minimize damage done if app gets killed/crashes/device dies.
        val fixedFlights = buildList {
            flightsToFix.forEach { old ->
                val new = old.copy(flightID = generateAndReserveNewFlightID(highestTakenIdIncludingFlightsToFix))
                saveDirectToDB(new)
                deleteHard(old)
                add(new)
            }
        }
        return fixedFlights
    }


    private suspend fun FlightRepositoryWithDirectAccess.getSyncableBasicFlights() =
        getSyncableFlights()
            .map {
                it.toBasicFlight()
            }

    private suspend fun FlightRepositoryWithDirectAccess.getSyncableFlights() =
        getAllFlightsInDB()
            .filter { !it.isPlanned }

    private suspend fun Client.sendFlightsToServer(flightsToSend: Collection<Flight>) = with(cloud) {
        val fff = flightsToSend
            .map { it.toBasicFlight()
                .copy(unknownToServer = false)
            }
        sendFlights(fff)
        repository.markFlightsAsKnownToServer(flightsToSend)
    }

    private suspend fun FlightRepositoryWithDirectAccess.markFlightsAsKnownToServer(flights: Collection<Flight>){
        val flightsToMark = flights.filter { it.unknownToServer }
        val markedFlights = flightsToMark.map  { it.copy(unknownToServer = false) }
        saveDirectToDB(markedFlights)
    }

    suspend fun replaceAllFlightsWith(newFlights: Collection<Flight>){
        repository.clear()
        repository.saveDirectToDB(newFlights)
    }

    private fun registerRepositoryChangedListener() {
        repository.registerOnDataChangedListener(repositoryChangedListener)
    }

    private fun unregisterRepositoryChangedListener() {
        repository.unregisterOnDataChangedListener(repositoryChangedListener)
    }

    private fun resetRepositoryInterruptionCheck(){
        repositoryWasChanged = false
    }

    private fun markMostRecentSyncAsNow() {
        ServerPrefs.mostRecentFlightsSyncEpochSecond(TimestampMaker().now)
    }

    private fun List<IDWithTimeStamp>.toMap() = associate { it.ID to it.timeStamp }
}