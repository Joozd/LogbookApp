package nl.joozd.logbookapp.comm

@Suppress("ClassName")
sealed class CloudFunctionResult {
    object OK: CloudFunctionResult()                 // wanted task completed successfully

    object CONNECTION_ERROR: CloudFunctionResult()   // could not establish a conenction for the wanted task

    object SERVER_REFUSED: CloudFunctionResult()     // server refused to complete wanted task (e.g. because of bad login data)
                        // Function returning this CLoudFunctionResult must handle the cause of this
                        // result, (e.g. set flag for bad login data so TaskDispatcher can do some-
                        // thing about it)
    class ResultWithPayload<T>(val result: CloudFunctionResult, val payload: T): CloudFunctionResult()

    fun correspondingServerFunctionResult(): ServerFunctionResult = when(this){
        OK -> ServerFunctionResult.SUCCESS
        CONNECTION_ERROR -> ServerFunctionResult.RETRY
        SERVER_REFUSED -> ServerFunctionResult.FAILURE
        is ResultWithPayload<*> -> this.result.correspondingServerFunctionResult()
    }

    fun isOK() = this == OK
}