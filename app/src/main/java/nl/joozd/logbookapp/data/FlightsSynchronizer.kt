package nl.joozd.logbookapp.data


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
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * This has full access to both a [Client] (i.e. direct access to server) and [FlightRepositoryWithDirectAccess]
 * It uses that access to synchronize flights between current repository and server.
 */
class FlightsSynchronizer(
    private val cloud: Cloud = Cloud(),
    private val repository: FlightRepositoryWithDirectAccess = FlightRepositoryWithDirectAccess.instance
) {
    suspend fun synchronizeIfNotSynced(): ServerFunctionResult = with(cloud){
        if (!UserManagement().checkIfLoginDataSet()) return ServerFunctionResult.FAILURE

        doInOneClientSession {
            login(Prefs.username()!!, Prefs.key()!!).also{ if (!it.isOK()) return@doInOneClientSession it }
            val isSynchronized = Cloud.Result<Boolean>().apply{
                isSynchronized(this).also { if (!it.isOK()) return@doInOneClientSession it }
            }.value!! // if this is null, isSynchronized(this) would not have returned OK
            if (isSynchronized) return@doInOneClientSession CloudFunctionResult.OK

            //If we get here, we need to synchronize!
            performSync()
        }.correspondingServerFunctionResult()
    }

    suspend fun forceSync(): ServerFunctionResult = with(cloud){
        doInOneClientSession {
            login(Prefs.username()!!, Prefs.key()!!).also{ if (!it.isOK()) return@doInOneClientSession it }
            performSync()
        }.correspondingServerFunctionResult()
    }

    // Do this only when logged in to server, or user should get an error message asking them to report this bug.
    private suspend fun Client.performSync(): CloudFunctionResult = with(cloud) {
        val repositoryIDsAndTimestampsAsync = MainScope().async { getIDsWithTimestampsFromRepository() }
        val serverIDsWithTimestamps = Cloud.Result<List<IDWithTimeStamp>>().apply{
            putIDWithTimestampsListInResult(this).also{ if (!it.isOK()) return it }
        }.value!!
        val repoIDsWithTimeStamps = repositoryIDsAndTimestampsAsync.await()

        updateIDsForNewFlights(serverIDsWithTimestamps)

        downloadNewOrNewerFlightsFromServer(repoIDsWithTimeStamps, serverIDsWithTimestamps)
        sendNewOrNewerFlightsToServer(repoIDsWithTimeStamps, serverIDsWithTimestamps)

        CloudFunctionResult.OK
    }

    private suspend fun Client.isSynchronized(result: Cloud.Result<Boolean>): CloudFunctionResult {
        with(cloud) {
            val repositoryChecksumAsync = MainScope().async { getChecksumFromRepository() }
            val r = Cloud.Result<FlightsListChecksum>().apply{
                putFlightsListChecksumInResult(this).also { if (!it.isOK()) return it }
            }
            result.value = repositoryChecksumAsync.await() == r.value
            return CloudFunctionResult.OK
        }
    }

    private suspend fun Client.downloadNewOrNewerFlightsFromServer(
        repoIDsWithTimeStamps: Collection<IDWithTimeStamp>,
        serverIDsWithTimestamps: Collection<IDWithTimeStamp>
    ): CloudFunctionResult = with(cloud) {
        val idsToGet = getFlightsToGetFromServer(repoIDsWithTimeStamps, serverIDsWithTimestamps).map { it.ID }
        if (idsToGet.isEmpty())
            return CloudFunctionResult.OK

        val downloadedFlights = Cloud.Result<List<BasicFlight>>().apply{
            putFlightsFromServerInResult(idsToGet, this).also { if (!it.isOK()) return it }
        }.value!!
        repository.saveDirectToDB(downloadedFlights.map { Flight(it) })
        CloudFunctionResult.OK
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
        repository.getAllFlights()
            .filter { !it.isPlanned }
            .map {
                IDWithTimeStamp(
                    it.toBasicFlight()
                )
            }

    private suspend fun getChecksumFromRepository(): FlightsListChecksum =
        FlightsListChecksum(
            repository.getAllFlights()
                .filter { !it.isPlanned }
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
    ) = listToCompareTo.firstOrNull { repoItem -> repoItem.ID == this.ID }
        ?.let {
            it.timeStamp < this.timeStamp   // if found return whether this is newer
        } ?: true                           // if not found, this is new so return true

    // Will update flights directly in DB
    private suspend fun updateIDsForNewFlights(serverIDsWithTimestamps: List<IDWithTimeStamp>){
        val newFlights = repository.getAllFlights().filter { it.unknownToServer }
        val highestIDOnServer = serverIDsWithTimestamps.maxOf { it.ID }
        if (newFlights.none { it.flightID <= highestIDOnServer }) return // if no conflicts, do nothing
        val updatedNewFlights = newFlights.map {
            it.copy(flightID = repository.generateAndReserveNewFlightID(highestIDOnServer))
        }
        repository.saveDirectToDB(updatedNewFlights)
        repository.deleteHard(newFlights)
    }
}