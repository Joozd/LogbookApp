package nl.joozd.joozdlogcommon

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class TestBackupEmailData {
    private val f1 = BasicFlight.PROTOTYPE.copy(remarks = "one")
    private val f2 = BasicFlight.PROTOTYPE.copy(remarks = "two", signature = makeSignature())



    val flights = listOf(f1, f2)

    val csv = buildCsvString(flights).toByteArray(Charsets.UTF_8)

    private val email = "one@two.com"
    private val username = 123L

    @Test
    fun testBackupEmailData(){
        val data = EmailData(username, email, csv)
        val serializedData = data.serialize()
        assertEquals(data, EmailData.deserialize(serializedData))
        val grabbedflights = flightsFromCsv(EmailData.deserialize(serializedData).attachment.toString(Charsets.UTF_8))
        assertEquals(flights, grabbedflights)
        println(grabbedflights[1].signature)
        println("yolo")
    }

    private fun buildCsvString(flights: List<BasicFlight>): String =
        BasicFlight.CSV_IDENTIFIER_STRING + "\n" + flights.joinToString("\n") { it.toCsv() }

    private fun makeSignature(): String{
        val encodedSignature = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB2ZXJzaW9uPSIxLjIiIGJhc2VQcm9maWxlPSJ0aW55IiBoZWlnaHQ9IjM4MCIgd2lkdGg9IjExNDAiIHZpZXdCb3g9IjAgMCAxMTQwIDM4MCI+PGcgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBmaWxsPSJub25lIiBzdHJva2U9ImJsYWNrIj48cGF0aCBzdHJva2Utd2lkdGg9IjIyIiBkPSJNMTE3LDMzMmMwLDAgMCwwIDAsMCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjIyIiBkPSJNMTE3LDMzMmMwLDAgMCwwIDAsMCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjIxIiBkPSJNMTE3LDMzMmM0LC0xNyAxLC0xOCA4LC0zNCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjIxIiBkPSJNMTE3LDMzMmM0LC0xNyAxLC0xOCA4LC0zNCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjEzIiBkPSJNMTI1LDI5OGM5LC0yNSA5LC0yNiAyNSwtNDcgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMyIgZD0iTTEyNSwyOThjOSwtMjUgOSwtMjYgMjUsLTQ3ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTE1MCwyNTFjMjMsLTMwIDI0LC0zMCA1MywtNTYgMzAsLTI2IDMwLC0yNyA2NCwtNDkgMzEsLTE5IDMyLC0xNyA2NSwtMzQgMzIsLTE2IDMyLC0xNyA2NSwtMzEgMjcsLTExIDI4LC0xMSA1NiwtMTkgMjMsLTYgMjQsLTggNDcsLTkgMTQsMCAxNiwxIDI3LDkgOSw2IDEyLDggMTQsMTggMiwxNiAtMiwxNyAtNiwzNCAtNSwyMCAtNSwyMCAtMTMsMzkgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTE1MCwyNTFjMjMsLTMwIDI0LC0zMCA1MywtNTYgMzAsLTI2IDMwLC0yNyA2NCwtNDkgMzEsLTE5IDMyLC0xNyA2NSwtMzQgMzIsLTE2IDMyLC0xNyA2NSwtMzEgMjcsLTExIDI4LC0xMSA1NiwtMTkgMjMsLTYgMjQsLTggNDcsLTkgMTQsMCAxNiwxIDI3LDkgOSw2IDEyLDggMTQsMTggMiwxNiAtMiwxNyAtNiwzNCAtNSwyMCAtNSwyMCAtMTMsMzkgLTgsMjEgLTksMjEgLTE4LDQxICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3IC0zLDEwIC0zLDEyIDEsMjAgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTE1MCwyNTFjMjMsLTMwIDI0LC0zMCA1MywtNTYgMzAsLTI2IDMwLC0yNyA2NCwtNDkgMzEsLTE5IDMyLC0xNyA2NSwtMzQgMzIsLTE2IDMyLC0xNyA2NSwtMzEgMjcsLTExIDI4LC0xMSA1NiwtMTkgMjMsLTYgMjQsLTggNDcsLTkgMTQsMCAxNiwxIDI3LDkgOSw2IDEyLDggMTQsMTggMiwxNiAtMiwxNyAtNiwzNCAtNSwyMCAtNSwyMCAtMTMsMzkgLTgsMjEgLTksMjEgLTE4LDQxIC05LDE5IC0xMSwxOCAtMTYsMzcgLTMsMTAgLTMsMTIgMSwyMCAzLDYgNiw5IDEzLDkgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTE1MCwyNTFjMjMsLTMwIDI0LC0zMCA1MywtNTYgMzAsLTI2IDMwLC0yNyA2NCwtNDkgMzEsLTE5IDMyLC0xNyA2NSwtMzQgMzIsLTE2IDMyLC0xNyA2NSwtMzEgMjcsLTExIDI4LC0xMSA1NiwtMTkgMjMsLTYgMjQsLTggNDcsLTkgMTQsMCAxNiwxIDI3LDkgOSw2IDEyLDggMTQsMTggMiwxNiAtMiwxNyAtNiwzNCAtNSwyMCAtNSwyMCAtMTMsMzkgLTgsMjEgLTksMjEgLTE4LDQxIC05LDE5IC0xMSwxOCAtMTYsMzcgLTMsMTAgLTMsMTIgMSwyMCAzLDYgNiw5IDEzLDkgMTgsLTEgMTksLTUgMzcsLTExICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3IC0zLDEwIC0zLDEyIDEsMjAgMyw2IDYsOSAxMyw5IDE4LC0xIDE5LC01IDM3LC0xMSAyNiwtOSAyNiwtOCA1MCwtMTkgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTE1MCwyNTFjMjMsLTMwIDI0LC0zMCA1MywtNTYgMzAsLTI2IDMwLC0yNyA2NCwtNDkgMzEsLTE5IDMyLC0xNyA2NSwtMzQgMzIsLTE2IDMyLC0xNyA2NSwtMzEgMjcsLTExIDI4LC0xMSA1NiwtMTkgMjMsLTYgMjQsLTggNDcsLTkgMTQsMCAxNiwxIDI3LDkgOSw2IDEyLDggMTQsMTggMiwxNiAtMiwxNyAtNiwzNCAtNSwyMCAtNSwyMCAtMTMsMzkgLTgsMjEgLTksMjEgLTE4LDQxIC05LDE5IC0xMSwxOCAtMTYsMzcgLTMsMTAgLTMsMTIgMSwyMCAzLDYgNiw5IDEzLDkgMTgsLTEgMTksLTUgMzcsLTExIDI2LC05IDI2LC04IDUwLC0xOSAzMCwtMTMgMjksLTE0IDU4LC0yOCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNMTUwLDI1MWMyMywtMzAgMjQsLTMwIDUzLC01NiAzMCwtMjYgMzAsLTI3IDY0LC00OSAzMSwtMTkgMzIsLTE3IDY1LC0zNCAzMiwtMTYgMzIsLTE3IDY1LC0zMSAyNywtMTEgMjgsLTExIDU2LC0xOSAyMywtNiAyNCwtOCA0NywtOSAxNCwwIDE2LDEgMjcsOSA5LDYgMTIsOCAxNCwxOCAyLDE2IC0yLDE3IC02LDM0IC01LDIwIC01LDIwIC0xMywzOSAtOCwyMSAtOSwyMSAtMTgsNDEgLTksMTkgLTExLDE4IC0xNiwzNyAtMywxMCAtMywxMiAxLDIwIDMsNiA2LDkgMTMsOSAxOCwtMSAxOSwtNSAzNywtMTEgMjYsLTkgMjYsLTggNTAsLTE5IDMwLC0xMyAyOSwtMTQgNTgsLTI4IDI4LC0xMyAyOCwtMTUgNTYsLTI2ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3IC0zLDEwIC0zLDEyIDEsMjAgMyw2IDYsOSAxMyw5IDE4LC0xIDE5LC01IDM3LC0xMSAyNiwtOSAyNiwtOCA1MCwtMTkgMzAsLTEzIDI5LC0xNCA1OCwtMjggMjgsLTEzIDI4LC0xNSA1NiwtMjYgMjAsLTggMjAsLTggNDEsLTEyICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3IC0zLDEwIC0zLDEyIDEsMjAgMyw2IDYsOSAxMyw5IDE4LC0xIDE5LC01IDM3LC0xMSAyNiwtOSAyNiwtOCA1MCwtMTkgMzAsLTEzIDI5LC0xNCA1OCwtMjggMjgsLTEzIDI4LC0xNSA1NiwtMjYgMjAsLTggMjAsLTggNDEsLTEyIDEyLC0zIDE0LC01IDI2LC0zICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik0xNTAsMjUxYzIzLC0zMCAyNCwtMzAgNTMsLTU2IDMwLC0yNiAzMCwtMjcgNjQsLTQ5IDMxLC0xOSAzMiwtMTcgNjUsLTM0IDMyLC0xNiAzMiwtMTcgNjUsLTMxIDI3LC0xMSAyOCwtMTEgNTYsLTE5IDIzLC02IDI0LC04IDQ3LC05IDE0LDAgMTYsMSAyNyw5IDksNiAxMiw4IDE0LDE4IDIsMTYgLTIsMTcgLTYsMzQgLTUsMjAgLTUsMjAgLTEzLDM5IC04LDIxIC05LDIxIC0xOCw0MSAtOSwxOSAtMTEsMTggLTE2LDM3IC0zLDEwIC0zLDEyIDEsMjAgMyw2IDYsOSAxMyw5IDE4LC0xIDE5LC01IDM3LC0xMSAyNiwtOSAyNiwtOCA1MCwtMTkgMzAsLTEzIDI5LC0xNCA1OCwtMjggMjgsLTEzIDI4LC0xNSA1NiwtMjYgMjAsLTggMjAsLTggNDEsLTEyIDEyLC0zIDE0LC01IDI2LC0zICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTIiIGQ9Ik03NzAsMTYxYzQsMCA0LDIgNSw2ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTIiIGQ9Ik03NzAsMTYxYzQsMCA0LDIgNSw2ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTMiIGQ9Ik03NzUsMTY3YzIsNSAxLDYgMSwxMiAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjEzIiBkPSJNNzc1LDE2N2MyLDUgMSw2IDEsMTIgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMiIgZD0iTTc3NiwxNzljMCw2IC0xLDYgLTEsMTMgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMiIgZD0iTTc3NiwxNzljMCw2IC0xLDYgLTEsMTMgMCw3IC0zLDggMCwxNCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjEyIiBkPSJNNzc2LDE3OWMwLDYgLTEsNiAtMSwxMyAwLDcgLTMsOCAwLDE0ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTc3NSwyMDZjMyw2IDQsNyAxMSwxMCAxMiw2IDEzLDggMjYsNyAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNNzc1LDIwNmMzLDYgNCw3IDExLDEwIDEyLDYgMTMsOCAyNiw3IDI0LC0xIDI1LC0yIDQ5LC05ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3IC0xMSwtOCAtMTQsLTkgLTI5LC0xMCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNNzc1LDIwNmMzLDYgNCw3IDExLDEwIDEyLDYgMTMsOCAyNiw3IDI0LC0xIDI1LC0yIDQ5LC05IDI3LC04IDI3LC05IDUzLC0yMiAyMiwtMTEgMjIsLTEzIDQyLC0yOCAxNywtMTEgMjIsLTEwIDMyLC0yNSA0LC02IDIsLTEyIC0zLC0xNyAtMTEsLTggLTE0LC05IC0yOSwtMTAgLTM5LC0yIC00MCwxIC03OSw0ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3IC0xMSwtOCAtMTQsLTkgLTI5LC0xMCAtMzksLTIgLTQwLDEgLTc5LDQgLTUyLDUgLTUzLDQgLTEwNCwxMyAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNNzc1LDIwNmMzLDYgNCw3IDExLDEwIDEyLDYgMTMsOCAyNiw3IDI0LC0xIDI1LC0yIDQ5LC05IDI3LC04IDI3LC05IDUzLC0yMiAyMiwtMTEgMjIsLTEzIDQyLC0yOCAxNywtMTEgMjIsLTEwIDMyLC0yNSA0LC02IDIsLTEyIC0zLC0xNyAtMTEsLTggLTE0LC05IC0yOSwtMTAgLTM5LC0yIC00MCwxIC03OSw0IC01Miw1IC01Myw0IC0xMDQsMTMgLTcxLDEyIC03MCwxMyAtMTQwLDI5ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3IC0xMSwtOCAtMTQsLTkgLTI5LC0xMCAtMzksLTIgLTQwLDEgLTc5LDQgLTUyLDUgLTUzLDQgLTEwNCwxMyAtNzEsMTIgLTcwLDEzIC0xNDAsMjkgLTc1LDE4IC03NSwxOCAtMTQ5LDM3ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3IC0xMSwtOCAtMTQsLTkgLTI5LC0xMCAtMzksLTIgLTQwLDEgLTc5LDQgLTUyLDUgLTUzLDQgLTEwNCwxMyAtNzEsMTIgLTcwLDEzIC0xNDAsMjkgLTc1LDE4IC03NSwxOCAtMTQ5LDM3IC03NCwyMCAtNzQsMjEgLTE0Nyw0MiAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNNzc1LDIwNmMzLDYgNCw3IDExLDEwIDEyLDYgMTMsOCAyNiw3IDI0LC0xIDI1LC0yIDQ5LC05IDI3LC04IDI3LC05IDUzLC0yMiAyMiwtMTEgMjIsLTEzIDQyLC0yOCAxNywtMTEgMjIsLTEwIDMyLC0yNSA0LC02IDIsLTEyIC0zLC0xNyAtMTEsLTggLTE0LC05IC0yOSwtMTAgLTM5LC0yIC00MCwxIC03OSw0IC01Miw1IC01Myw0IC0xMDQsMTMgLTcxLDEyIC03MCwxMyAtMTQwLDI5IC03NSwxOCAtNzUsMTggLTE0OSwzNyAtNzQsMjAgLTc0LDIxIC0xNDcsNDIgLTY5LDE5IC02OSwxOSAtMTM3LDM5ICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTEiIGQ9Ik03NzUsMjA2YzMsNiA0LDcgMTEsMTAgMTIsNiAxMyw4IDI2LDcgMjQsLTEgMjUsLTIgNDksLTkgMjcsLTggMjcsLTkgNTMsLTIyIDIyLC0xMSAyMiwtMTMgNDIsLTI4IDE3LC0xMSAyMiwtMTAgMzIsLTI1IDQsLTYgMiwtMTIgLTMsLTE3IC0xMSwtOCAtMTQsLTkgLTI5LC0xMCAtMzksLTIgLTQwLDEgLTc5LDQgLTUyLDUgLTUzLDQgLTEwNCwxMyAtNzEsMTIgLTcwLDEzIC0xNDAsMjkgLTc1LDE4IC03NSwxOCAtMTQ5LDM3IC03NCwyMCAtNzQsMjEgLTE0Nyw0MiAtNjksMTkgLTY5LDE5IC0xMzcsMzkgLTQ4LDEzIC00NywxNCAtOTUsMjcgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTc3NSwyMDZjMyw2IDQsNyAxMSwxMCAxMiw2IDEzLDggMjYsNyAyNCwtMSAyNSwtMiA0OSwtOSAyNywtOCAyNywtOSA1MywtMjIgMjIsLTExIDIyLC0xMyA0MiwtMjggMTcsLTExIDIyLC0xMCAzMiwtMjUgNCwtNiAyLC0xMiAtMywtMTcgLTExLC04IC0xNCwtOSAtMjksLTEwIC0zOSwtMiAtNDAsMSAtNzksNCAtNTIsNSAtNTMsNCAtMTA0LDEzIC03MSwxMiAtNzAsMTMgLTE0MCwyOSAtNzUsMTggLTc1LDE4IC0xNDksMzcgLTc0LDIwIC03NCwyMSAtMTQ3LDQyIC02OSwxOSAtNjksMTkgLTEzNywzOSAtNDgsMTMgLTQ3LDE0IC05NSwyNyAtMzEsOCAtMzEsOCAtNjMsMTYgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTc3NSwyMDZjMyw2IDQsNyAxMSwxMCAxMiw2IDEzLDggMjYsNyAyNCwtMSAyNSwtMiA0OSwtOSAyNywtOCAyNywtOSA1MywtMjIgMjIsLTExIDIyLC0xMyA0MiwtMjggMTcsLTExIDIyLC0xMCAzMiwtMjUgNCwtNiAyLC0xMiAtMywtMTcgLTExLC04IC0xNCwtOSAtMjksLTEwIC0zOSwtMiAtNDAsMSAtNzksNCAtNTIsNSAtNTMsNCAtMTA0LDEzIC03MSwxMiAtNzAsMTMgLTE0MCwyOSAtNzUsMTggLTc1LDE4IC0xNDksMzcgLTc0LDIwIC03NCwyMSAtMTQ3LDQyIC02OSwxOSAtNjksMTkgLTEzNywzOSAtNDgsMTMgLTQ3LDE0IC05NSwyNyAtMzEsOCAtMzEsOCAtNjMsMTYgLTQsMSAtOCwyIC04LDEgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTc3NSwyMDZjMyw2IDQsNyAxMSwxMCAxMiw2IDEzLDggMjYsNyAyNCwtMSAyNSwtMiA0OSwtOSAyNywtOCAyNywtOSA1MywtMjIgMjIsLTExIDIyLC0xMyA0MiwtMjggMTcsLTExIDIyLC0xMCAzMiwtMjUgNCwtNiAyLC0xMiAtMywtMTcgLTExLC04IC0xNCwtOSAtMjksLTEwIC0zOSwtMiAtNDAsMSAtNzksNCAtNTIsNSAtNTMsNCAtMTA0LDEzIC03MSwxMiAtNzAsMTMgLTE0MCwyOSAtNzUsMTggLTc1LDE4IC0xNDksMzcgLTc0LDIwIC03NCwyMSAtMTQ3LDQyIC02OSwxOSAtNjksMTkgLTEzNywzOSAtNDgsMTMgLTQ3LDE0IC05NSwyNyAtMzEsOCAtMzEsOCAtNjMsMTYgLTQsMSAtOCwyIC04LDEgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMyIgZD0iTTM0LDMyMGMwLC0xIDMsLTQgNywtNSAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjEzIiBkPSJNMzQsMzIwYzAsLTEgMywtNCA3LC01IDI5LC0xMyAyOSwtMTIgNTgsLTIzICIvPjxwYXRoIHN0cm9rZS13aWR0aD0iMTMiIGQ9Ik0zNCwzMjBjMCwtMSAzLC00IDcsLTUgMjksLTEzIDI5LC0xMiA1OCwtMjMgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTk5LDI5MmM0OCwtMTggNDgsLTE4IDk2LC0zNCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNOTksMjkyYzQ4LC0xOCA0OCwtMTggOTYsLTM0IDY3LC0yMyA2OCwtMjMgMTM2LC00NCAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNOTksMjkyYzQ4LC0xOCA0OCwtMTggOTYsLTM0IDY3LC0yMyA2OCwtMjMgMTM2LC00NCA4MiwtMjUgODEsLTI2IDE2NCwtNDcgIi8+PHBhdGggc3Ryb2tlLXdpZHRoPSIxMSIgZD0iTTk5LDI5MmM0OCwtMTggNDgsLTE4IDk2LC0zNCA2NywtMjMgNjgsLTIzIDEzNiwtNDQgODIsLTI1IDgxLC0yNiAxNjQsLTQ3IDg2LC0yMiA4NywtMjIgMTc0LC0zOSAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNOTksMjkyYzQ4LC0xOCA0OCwtMTggOTYsLTM0IDY3LC0yMyA2OCwtMjMgMTM2LC00NCA4MiwtMjUgODEsLTI2IDE2NCwtNDcgODYsLTIyIDg3LC0yMiAxNzQsLTM5IDg4LC0xNyA4OCwtMTcgMTc3LC0yOSAiLz48cGF0aCBzdHJva2Utd2lkdGg9IjExIiBkPSJNOTksMjkyYzQ4LC0xOCA0OCwtMTggOTYsLTM0IDY3LC0yMyA2OCwtMjMgMTM2LC00NCA4MiwtMjUgODEsLTI2IDE2NCwtNDcgODYsLTIyIDg3LC0yMiAxNzQsLTM5IDg4LC0xNyA4OCwtMTcgMTc3LC0yOSA5OSwtMTIgMTAwLC04IDE5OSwtMTggIi8+PC9nPjwvc3ZnPg=="
        return Base64.getDecoder().decode(encodedSignature).toString(Charsets.UTF_8)
    }

    private fun flightsFromCsv(csv: String): List<BasicFlight> =
        csv.split("\n").drop(1).map {BasicFlight.ofCsv (it) }
}