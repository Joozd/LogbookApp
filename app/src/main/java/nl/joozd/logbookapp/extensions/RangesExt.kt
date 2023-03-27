package nl.joozd.logbookapp.extensions

import java.time.Instant

/**
 * Makes a ClosedRange<Long> from the epochMillis of the Instants in this ClosedRange
 */
fun ClosedRange<Instant>.toEpochMilliRange(): ClosedRange<Long> =
    this.start.toEpochMilli()..this.endInclusive.toEpochMilli()

/**
 * Makes a ClosedRange<Long> from the epochSeconds of the Instants in this ClosedRange
 */
fun ClosedRange<Instant>.toEpochSecondRange(): ClosedRange<Long> =
    this.start.epochSecond..this.endInclusive.epochSecond