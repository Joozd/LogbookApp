package nl.joozd.logbookapp.extensions


/**
 * Returns original value if amove minVal, or minVal if it is less
 */
fun Long.withMinimumValue(minVal: Long) = if (this > minVal) this else minVal