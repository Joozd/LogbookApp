package nl.joozd.logbookapp.data.repository.helpers

fun String.findBestHitForRegistration(registrations: List<String>): String?{
    if (this in registrations) return this
    var searchableRegs = registrations.filter{it.length > length}.filter{this in it}

    //we'll want to search from end to start. As soon as wel have one, we're good.
    do {
        searchableRegs.firstOrNull { it.endsWith(this) }?.let {return it}
        if (searchableRegs.none { this in it }) return null
        searchableRegs = searchableRegs.map { it.dropLast(1) }.filter { it.isNotEmpty() }
    } while (searchableRegs.none{it.endsWith(this)})
    return null
}