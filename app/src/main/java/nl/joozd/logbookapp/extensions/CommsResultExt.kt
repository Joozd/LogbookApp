/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.logbookapp.extensions

import nl.joozd.comms.CommsResult
import nl.joozd.logbookapp.data.comm.ServerFunctionResult

fun CommsResult.toCloudFunctionResults(): ServerFunctionResult = when(this){
    CommsResult.OK -> ServerFunctionResult.OK
    CommsResult.DATA_ERROR -> ServerFunctionResult.DATA_ERROR
    CommsResult.CLIENT_NOT_ALIVE -> ServerFunctionResult.CLIENT_NOT_ALIVE
    CommsResult.SERVER_ERROR -> ServerFunctionResult.SERVER_ERROR
    CommsResult.CLIENT_ERROR -> ServerFunctionResult.CLIENT_ERROR
    CommsResult.SOCKET_IS_NULL -> ServerFunctionResult.SOCKET_IS_NULL
    CommsResult.UNKNOWN_HOST -> ServerFunctionResult.UNKNOWN_HOST
    CommsResult.IO_ERROR -> ServerFunctionResult.IO_ERROR
    CommsResult.CONNECTION_REFUSED -> ServerFunctionResult.CONNECTION_REFUSED
    CommsResult.SOCKET_ERROR -> ServerFunctionResult.SOCKET_ERROR
}