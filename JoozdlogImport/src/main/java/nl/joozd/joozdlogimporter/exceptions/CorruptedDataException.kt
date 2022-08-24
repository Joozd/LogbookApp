package nl.joozd.joozdlogimporter.exceptions

class CorruptedDataException(val reason: String): Exception(reason)