package nl.joozd.logbookapp.data.room.dao


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nl.joozd.logbookapp.data.dataclasses.Airport


@Dao
interface AirportDao {
    @Query("SELECT * FROM Airport")
    suspend fun requestAllAirports(): List<Airport>

    @Query("SELECT ident FROM Airport")
    suspend fun requestAllIdents(): List<String>

    @Query("SELECT * FROM Airport")
    fun requestLiveAirports(): LiveData<List<Airport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirports(vararg airportData: Airport)

    @Query("DELETE FROM Airport")
    suspend fun clearDb()

    /**
     * Search 1 airport by a specific field, combine them in repository
     */
    @Query("SELECT * FROM Airport WHERE :query LIKE ident LIMIT 1")
    suspend fun searchAirportByIdent(query: String): List<Airport>

    @Query("SELECT * FROM Airport WHERE :query LIKE iata_code LIMIT 1")
    suspend fun searchAirportByIata(query: String): List<Airport>

    @Query("SELECT * FROM Airport WHERE :query LIKE municipality LIMIT 1")
    suspend fun searchAirportByMunicipality(query: String): List<Airport>

    @Query("SELECT * FROM Airport WHERE :query LIKE name LIMIT 1")
    suspend fun searchAirportByName(query: String): List<Airport>

    @Query("SELECT DISTINCT * FROM Airport WHERE UPPER(ident) LIKE :query OR UPPER(iata_code) LIKE :query OR UPPER(municipality) LIKE :query OR UPPER(name) LIKE :query")
    suspend fun searchAirports(query: String): List<Airport>

}
