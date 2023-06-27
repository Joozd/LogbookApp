package nl.joozd.textscanner.extensions

// Remove control characters from a regex and return it as a string, eg, "CA.?" will become "CA"
fun Regex.toStringWithoutControlCharacters(): String =
    this.toString().filter { it !in CONTROLS_USED_IN_REGEXES }

// This is definitely not complete, but does contain all the controls I am using at the time of writing this comment.
private const val CONTROLS_USED_IN_REGEXES = ".?"