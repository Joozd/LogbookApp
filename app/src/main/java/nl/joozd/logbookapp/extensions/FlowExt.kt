package nl.joozd.logbookapp.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> Flow<T?>.notNullFlow(): Flow<T> =
    map{ it!! }.also{
        println("Not null flow $it made")
    }