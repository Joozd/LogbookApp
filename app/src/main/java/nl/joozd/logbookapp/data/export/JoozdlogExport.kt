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

package nl.joozd.logbookapp.data.export

import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.*

object JoozdlogExport {
    private val context
        get() = App.instance.ctx
    suspend fun shareCsvExport(fileName: String): Uri = withContext(Dispatchers.Main) {
        val cachePath = File(context.cacheDir, "files").apply{
            mkdirs()
        }
        val name = if (fileName.uppercase(Locale.ROOT).endsWith(".CSV")) fileName else "$fileName.csv"
        val file = File(cachePath, name)
        OutputStreamWriter(file.outputStream()).use{
            async(Dispatchers.IO) {
                it.write(FlightsRepositoryExporter(FlightRepository.getInstance()).buildCsvString())
            }.await()
        }
        FileProvider.getUriForFile(context, "nl.joozd.joozdlog.fileprovider", file)
    }
}