package nl.joozd.logbookapp.data.sync


import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.FlightsListChecksum
import nl.joozd.joozdlogcommon.IDWithTimeStamp
import nl.joozd.logbookapp.comm.Cloud
import nl.joozd.logbookapp.comm.CloudFunctionResult
import nl.joozd.logbookapp.comm.ServerFunctionResult
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.importing.merging.mergeFlightsLists
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.sharedPrefs.ServerPrefs
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker

/**
 * This has full access to both a [Client] (i.e. direct access to server) and [FlightRepositoryWithDirectAccess]
 * It uses that access to synchronize flights between current repository and server.
 */
class FlightsSynchronizer(
    private val cloud: Cloud = Cloud(),
    private val userManagement: UserManagement = UserManagement(),
    private val repository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance
) {
    suspend fun synchronizeIfNotSynced(): ServerFunctionResult = with(cloud){
        if (!UserManagement().checkIfLoginDataSet()) return ServerFunctionResult.FAILURE

        doInOneClientSession {
            loginToServer().also{ if (!it.isOK()) return@doInOneClientSession it }
            val isSynchronized = checkIfIsSynchronized().let {
                if (it !is CloudFunctionResult.ResultWithPayload<*>) return@doInOneClientSession it
                it.payload as Boolean
            }
            if (isSynchronized) {
                markMostRecentSyncAsNow()
                return@doInOneClientSession CloudFunctionResult.OK
            }
            //If we get here, we need to synchronize!
            performSync()
        }.correspondingServerFunctionResult()
    }

    suspend fun mergeRepoWithServer(): ServerFunctionResult = with(cloud){
        doInOneClientSession {
            loginToServer().also{ if (!it.isOK()) return@doInOneClientSession it }

            val serverIDs = getIDWithTimestampsListFromServer().let {
                if (it !is CloudFunctionResult.ResultWithPayload<*>) return@doInOneClientSession it
                @Suppress("UNCHECKED_CAST")
                it.payload as List<IDWithTimeStamp>
            }.map { it.ID }

            val serversFlights = downloadFlightsFromServer(serverIDs).let {
                if (it !is CloudFunctionResult.ResultWithPayload<*>) return@doInOneClientSession it
                @Suppress("UNCHECKED_CAST")
                it.payload as List<BasicFlight>
            }.map { Flight(it)}
            val repoFlights = repository.getAllFlightsInDB()
            val mergedFlightsList = mergeFlightsLists(serversFlights, repoFlights)
            repository.clear()
            repository.saveDirectToDB(mergedFlightsList.filter { !it.isPlanned })

            // just send entire list. Result of this function will be returned; any earlier failures will be returned from the 'also' blocks
            sendFlights(mergedFlightsList.map { it.copy(unknownToServer = false).toBasicFlight() }).also{
                markFlightsAsKnownToServer(mergedFlightsList)
            }
        }.correspondingServerFunctionResult()
    }

    private suspend fun markFlightsAsKnownToServer(mergedFlightsList: List<Flight>) {
        repository.save(mergedFlightsList.filter { it.unknownToServer }.map { it.copy(unknownToServer = false) })
    }

    suspend fun forceSync(): ServerFunctionResult = with(cloud){
        doInOneClientSession {
            loginToServer().also{ if (!it.isOK()) return@doInOneClientSession it }
            performSync()
        }.correspondingServerFunctionResult()
    }

    // Do this only when logged in to server, or user should get an error message asking them to report this bug.
    private suspend fun Client.performSync(): CloudFunctionResult = with(cloud) {
        FlightRepositoryWithUndo.instance.invalidateInsertedUndoCommands() // undo/redo operations on imports do not play well with synchronizing.
        val serverIDsWithTimestamps = getIDWithTimestampsListFromServer().let {
            if (it !is CloudFunctionResult.ResultWithPayload<*>) return it
            @Suppress("UNCHECKED_CAST")
            it.payload as List<IDWithTimeStamp>
        }
        updateIDsForNewFlights(serverIDsWithTimestamps)
        val correctedRepoIDsWithTimeStamps = getIDsWithTimestampsFromRepository()

        saveNewOrNewerFlightsFromServer(correctedRepoIDsWithTimeStamps, serverIDsWithTimestamps).also { if (!it.isOK()) return it}
        sendNewOrNewerFlightsToServer(correctedRepoIDsWithTimeStamps, serverIDsWithTimestamps).also { if (!it.isOK()) return it}

        markFlightsAsKnownToServerInRepository(correctedRepoIDsWithTimeStamps)

        markMostRecentSyncAsNow()


        CloudFunctionResult.OK
    }

    private fun markMostRecentSyncAsNow() {
        ServerPrefs.mostRecentFlightsSyncEpochSecond(TimestampMaker().now)
    }

    private suspend fun Client.loginToServer(): CloudFunctionResult = with (cloud){
        userManagement.getUsernameWithKey()?.let { usernameWithKey ->
            login(usernameWithKey).also { if (!it.isOK()) return it }
        } ?: return CloudFunctionResult.SERVER_REFUSED
    }

    private suspend fun Client.checkIfIsSynchronized(): CloudFunctionResult {
        with(cloud) {
            val repositoryChecksumAsync = MainScope().async { getChecksumFromRepository() }
            val r = Cloud.Result<FlightsListChecksum>().apply{
                putFlightsListChecksumInResult(this).also { if (!it.isOK()) return it }
            }
            val result = repositoryChecksumAsync.await() == r.value
            return CloudFunctionResult.ResultWithPayload(CloudFunctionResult.OK, result)
        }
    }

    private suspend fun Client.saveNewOrNewerFlightsFromServer(
        repoIDsWithTimeStamps: Collection<IDWithTimeStamp>,
        serverIDsWithTimestamps: Collection<IDWithTimeStamp>
    ): CloudFunctionResult{
        val downloadedFlights = downloadNewOrNewerFlightsFromServer(repoIDsWithTimeStamps, serverIDsWithTimestamps).let{
            if (it !is CloudFunctionResult.ResultWithPayload<*>) return it
            @Suppress("UNCHECKED_CAST")
            it.payload as List<BasicFlight>
        }
        repository.saveDirectToDB(downloadedFlights.map { Flight(it) })
        return CloudFunctionResult.OK
    }


    private suspend fun Client.downloadNewOrNewerFlightsFromServer(
        repoIDsWithTimeStamps: Collection<IDWithTimeStamp>,
        serverIDsWithTimestamps: Collection<IDWithTimeStamp>
    ): CloudFunctionResult = with(cloud) {
        val idsToGet = getFlightsToGetFromServer(repoIDsWithTimeStamps, serverIDsWithTimestamps).map { it.ID }

        return if (idsToGet.isEmpty())
            CloudFunctionResult.OK
        else
            downloadFlightsFromServer(idsToGet)
    }

    private suspend fun Client.sendNewOrNewerFlightsToServer(
        repoIDsWithTimeStamps: Collection<IDWithTimeStamp>,
        serverIDsWithTimestamps: Collection<IDWithTimeStamp>
    ): CloudFunctionResult = with(cloud) {
        val idsToSend = getFlightsToSendToServer(repoIDsWithTimeStamps, serverIDsWithTimestamps).map { it.ID }
        if (idsToSend.isEmpty())
            return CloudFunctionResult.OK

        val flightsToSend = repository.getFlightsByID(idsToSend)
        sendFlights(flightsToSend.map { it.toBasicFlight() })
    }

    private suspend fun getIDsWithTimestampsFromRepository() =
        repository.getUnplannedFlights()
            .map {
                IDWithTimeStamp(
                    it.toBasicFlight()
                )
            }

    private suspend fun getChecksumFromRepository(): FlightsListChecksum =
        FlightsListChecksum(
            repository.getUnplannedFlights()
                .map {
                    it.toBasicFlight()
                }
        )


    private fun getFlightsToGetFromServer(repoIDsWithTimeStamps: Collection<IDWithTimeStamp>, serverIDsWithTimestamps: Collection<IDWithTimeStamp>): List<IDWithTimeStamp> =
        serverIDsWithTimestamps.filter {
            it.isNewOrIsNewerVersion(repoIDsWithTimeStamps)
        }

    private fun getFlightsToSendToServer(repoIDsWithTimeStamps: Collection<IDWithTimeStamp>, serverIDsWithTimestamps: Collection<IDWithTimeStamp>): List<IDWithTimeStamp> =
        repoIDsWithTimeStamps.filter {
            it.isNewOrIsNewerVersion(serverIDsWithTimestamps)
        }

    private fun IDWithTimeStamp.isNewOrIsNewerVersion(
        listToCompareTo: Collection<IDWithTimeStamp>
    ) = listToCompareTo.firstOrNull { listItem -> listItem.ID == this.ID }
        ?.let {
            it.timeStamp < this.timeStamp   // if found return whether this is newer
        } ?: true                           // if not found, this is new so return true

    // Will update flights directly in DB
    private suspend fun updateIDsForNewFlights(serverIDsWithTimestamps: List<IDWithTimeStamp>){
        val newFlights = repository.getAllFlights().filter { it.unknownToServer }
        val highestIDOnServer = serverIDsWithTimestamps.maxOfOrNull { it.ID } ?: -1
        if (newFlights.none { it.flightID <= highestIDOnServer }) return // if no conflicts, do nothing
        val updatedNewFlights = newFlights.map {
            it.copy(flightID = repository.generateAndReserveNewFlightID(highestIDOnServer))
        }
        repository.saveDirectToDB(updatedNewFlights)
        repository.deleteHard(newFlights)
    }

    private suspend fun markFlightsAsKnownToServerInRepository(idsToMark: Collection<IDWithTimeStamp>){
        val flightsToUpdate = repository.getFlightsByID(idsToMark.map { it.ID })
            .filter { it.unknownToServer }
        repository.saveDirectToDB(flightsToUpdate.map { it.copy(unknownToServer = false) })
    }

    private suspend fun FlightRepositoryWithDirectAccess.getUnplannedFlights() = getAllFlights().filter { !it.isPlanned }
}