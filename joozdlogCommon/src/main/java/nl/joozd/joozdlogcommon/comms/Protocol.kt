package nl.joozd.joozdlogcommon.comms

object Protocol {
    val VERSION_1 = 1 // First protocol version

    val CURRENTVERSION = VERSION_1

    val KEY_SIZE = 16 // bytes

    val SERVER_URL = "joozd.nl"
    val SERVER_PORT = 1337

    val DATAFILES_METADATA_FILENAME = "datafiles_metadata.json"
    val DATAFILES_URL_PREFIX = "joozdlog/"
}