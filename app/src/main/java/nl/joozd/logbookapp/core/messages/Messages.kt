package nl.joozd.logbookapp.core.messages

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.emailFunctions.EmailCenter

object Messages {
    val unknownOrUnverifiedEmailMessage = UserMessage.Builder().apply{
        titleResource = R.string.email
        descriptionResource = R.string.server_reported_email_not_verified_new_mail_will_be_sent
        setPositiveButton(android.R.string.ok){
            EmailCenter().requestEmailVerificationMail()
        }
    }.build()

    val invalidEmailAddressSentToServer = UserMessage.Builder().apply{
        titleResource = R.string.email
        descriptionResource = R.string.server_not_an_email_address_please_enter_again
        setPositiveButton(android.R.string.ok){
            //consider opening email fragment
        }
    }.build()


}