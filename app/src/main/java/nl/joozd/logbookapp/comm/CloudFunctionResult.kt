package nl.joozd.logbookapp.comm

@Suppress("ClassName")
enum class CloudFunctionResult {
    OK,                 // wanted task completed successfully
    CONNECTION_ERROR,   // could not establish a conenction for the wanted task
    SERVER_REFUSED;     // server refused to complete wanted task (e.g. because of bad login data)
                        // Function returning this CLoudFunctionResult must handle the cause of this
                        // result, (e.g. set flag for bad login data so TaskDispatcher can do some-
                        // thing about it)

    fun correspondingServerFunctionResult(): ServerFunctionResult = when(this){
        OK -> ServerFunctionResult.SUCCESS
        CONNECTION_ERROR -> ServerFunctionResult.RETRY
        SERVER_REFUSED -> ServerFunctionResult.FAILURE
    }

    fun isOK(): Boolean = this == OK
}