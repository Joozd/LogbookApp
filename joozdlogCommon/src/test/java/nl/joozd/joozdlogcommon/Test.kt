package nl.joozd.joozdlogcommon

import org.junit.Test
import org.junit.Assert.assertEquals

class Test {
    @Test
    fun test(){
        val metaData = DataFilesMetaData(
            11,
            "location for aircraft types",
            12,
            "location for forced types",
            13,
            "location for airports"
        )
        val json = metaData.toString()
        println("********************")
        println(json)
        println("********************")
        assertEquals(metaData, DataFilesMetaData.fromJSON(json))
        println("test complete!")
    }
}