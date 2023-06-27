package nl.joozd.textscanner.analyzer

import com.google.mlkit.vision.text.Text
import nl.joozd.textscanner.extensions.toStringWithoutControlCharacters

/**
 * ScannedTextProcessor that processes crew names from scanned text.
 * Outputs two lists: Names and Ranks, in that order. These lists have the same indices, so name[3] corresponds with rank[3].
 */
class CrewNamesCollector: ScannedTextProcessor {
    private val foundNamesWithFrequencies = HashMap<Name, Int>()
    private var scansDone = 0

    override val progress: Int
        get() = 100 * scansDone / TOTAL_SCANS_TO_DO

    override val isDone: Boolean get() = scansDone >= TOTAL_SCANS_TO_DO
    private val foundNames: Set<Name> get() = foundNamesWithFrequencies.filterValues{
        it > scansDone / MINIMUM_RATIO_FOR_VALID_RESULT
    }.keys

    // first is names, second is the ranks going with those names, in the same order
    override val results: Iterable<List<String>>
        get() = foundNames.toList().let { names ->// Making a list because order matters
            listOf(names.map { it.name}, names.map { it.rank })
        }


    /**
     * This runs for [TOTAL_SCANS_TO_DO] times and puts every found [Name] in [foundNamesWithFrequencies]
     */
    override fun addScan(scannedText: Text) {
        scansDone++

        val relevantBlocks = scannedText.textBlocks.filter {
            it.lines.size == 2 && validFunctions.any { rank -> rank.containsMatchIn(it.lines[1].text )}
        }

        relevantBlocks.forEach {
            val name = Name.fromValidBlock(it)
            foundNamesWithFrequencies[name] = (foundNamesWithFrequencies[name] ?: 0) + 1
        }
    }

    data class Name(val name: String, val rank: String){

        companion object{
            fun fromValidBlock(block: Text.TextBlock): Name {

                require(block.lines.size == 2 ) { "Invalid block, needs 2 lines, has ${block.lines.size} lines"}

                //Rank is the second line in a block.
                val rank = validFunctions.first { re ->
                    re.containsMatchIn(block.lines[1].text)
                }.toStringWithoutControlCharacters()

                return Name(block.lines[0].text, rank)
            }
        }
    }

    companion object{
        // determines how many scans the app does to determine names. Higher number takes longer but will probably achieve more accurate results.
        const val TOTAL_SCANS_TO_DO = 10
        const val MINIMUM_RATIO_FOR_VALID_RESULT = 3 // 1/this, so 3 means more than 1/3 of all results must have a name for it to appear in results. 3 Worked in my single RL test.
        private const val CA = "CA"

        // These functions are in order of rank.
        val validFunctions = listOf(
            "Captain",
            "First officer",
            "Second officer",
            "Senior purser.?",
            "Purser.?",
            "$CA.?"
        ).map { it.toRegex() }

        // Map of rank index to it's string value that will be reported in [results], eg "First Officer" is [1]
        val functionOrder: Map<String, Int> = validFunctions.indices.map {i ->
            validFunctions[i].toStringWithoutControlCharacters() to i
        }.toMap()
    }
}