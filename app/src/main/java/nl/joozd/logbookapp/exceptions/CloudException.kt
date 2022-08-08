package nl.joozd.logbookapp.exceptions

import nl.joozd.logbookapp.comm.CloudFunctionResult

class CloudException(val cloudFunctionResult: CloudFunctionResult): Exception()