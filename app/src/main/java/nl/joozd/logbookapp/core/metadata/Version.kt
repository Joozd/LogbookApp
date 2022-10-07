package nl.joozd.logbookapp.core.metadata

object Version {
    const val NEW_INSTALL = -1
    const val ALPACA = 0 // version before this Version object was introduced; 1.1
    const val BEAVER = 1 // First version with this object; 1.2

    val currentVersion get() = BEAVER
}