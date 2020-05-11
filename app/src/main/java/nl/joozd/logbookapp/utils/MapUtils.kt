package nl.joozd.logbookapp.utils

fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap.put(it.value, it.key) }
}