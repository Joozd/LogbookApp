package nl.joozd.logbookapp.core.messages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate

object MessagesWaiting: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.MESSAGES_WAITING_KEY"

    /**
     * Dialog messages
     */

    private const val NO_VERIFICATION_CODE_SAVED_BUG = "NO_VERIFICATION_CODE_SAVED_BUG"

    private const val EMAIL_CONFIRMATION_SCHEDULED = "EMAIL_CONFIRMATION_SCHEDULED"
    private const val EMAIL_CONFIRMED = "EMAIL_CONFIRMED"
    private const val NO_EMAIL_ENTERED = "NO_EMAIL_ENTERED"
    private const val SERVER_REJECTED_EMAIL = "SERVER_REJECTED_EMAIL"
    private const val BAD_VERIFICATION_CODE_CLICKED = "BAD_VERIFICATION_CODE_CLICKED"
    private const val INVALID_EMAIL_ADDRESS = "INVALID_EMAIL_ADDRESS"
    private const val BACKUP_NEEDED = "BACKUP_NEEDED"



    private const val TEST_MESSAGE_BAR = "TEST_MESSAGE_BAR"
    private const val TEST_DIALOG_MESSAGE = "TEST_DIALOG_MESSAGE"

    private const val NO_DIALOG_MESSAGE_SCHEDULED = "NO_DIALOG_MESSAGE_SCHEDULED" // this gets added last and is always true
    private const val NO_MESSAGEBAR_MESSAGE_SCHEDULED = "NO_MESSAGEBAR_MESSAGE_SCHEDULED" // this gets added last and is always true

    val emailConfirmationScheduled by JoozdlogSharedPreferenceDelegate(EMAIL_CONFIRMATION_SCHEDULED, false)
    val emailConfirmed by JoozdlogSharedPreferenceDelegate(EMAIL_CONFIRMED, false)
    val noEmailEntered by JoozdlogSharedPreferenceDelegate(NO_EMAIL_ENTERED, false)

    val noVerificationCodeSavedBug by JoozdlogSharedPreferenceDelegate(NO_VERIFICATION_CODE_SAVED_BUG, false)
    val badVerificationCodeClicked by JoozdlogSharedPreferenceDelegate(BAD_VERIFICATION_CODE_CLICKED, false)
    val invalidEmailAddressStored by JoozdlogSharedPreferenceDelegate(INVALID_EMAIL_ADDRESS, false)
    val backupNeeded by JoozdlogSharedPreferenceDelegate(BACKUP_NEEDED, false)
    val serverRejectedEmail by JoozdlogSharedPreferenceDelegate(SERVER_REJECTED_EMAIL, false)

    val testDialogMessage by JoozdlogSharedPreferenceDelegate(TEST_DIALOG_MESSAGE, false)
    val testMessageBarMessage by JoozdlogSharedPreferenceDelegate(TEST_MESSAGE_BAR, false)

    val noDialog by JoozdlogSharedPreferenceDelegate(NO_DIALOG_MESSAGE_SCHEDULED, true) // value gets ignored, noDialogMessageFlow will always emit PersistentDialogMessage.NO_DIALOG
    val noMessageBar by JoozdlogSharedPreferenceDelegate(NO_MESSAGEBAR_MESSAGE_SCHEDULED, true) // value gets ignored, noMessageBarMessageFlow will always emit MessageBarMessage.NO_MESSAGE


    /*
     * flags transformed to their messages. This way we can used the sealed class to prevent forgetting to add them here
     */


    private val noVerificationCodeSavedBugMessageFlow = noVerificationCodeSavedBug.flow.map { if (it) PersistentDialogMessage.NO_VERIFICATION_CODE_SAVED_BUG else null}
    private val testDialogMessageMessageFlow = testDialogMessage.flow.map { if (it) PersistentDialogMessage.TEST_DIALOG else null}


    fun getDialogMessageToDisplayFlow(): Flow<PersistentDialogMessage?> = combine(
        noVerificationCodeSavedBugMessageFlow,
        testDialogMessageMessageFlow,
    ){ messagesWaiting ->
        messagesWaiting.filterNotNull().firstOrNull() ?: PersistentDialogMessage.NO_DIALOG
    }

    /**
     * Message bar messages
     */

    /*
     * flags transformed to their messages. This way we can used the sealed class to prevent forgetting to add them here
     */
    private val backupNeededMessageFlow = backupNeeded.flow.map { if (it) MessageBarMessage.BACKUP_NEEDED else null}
    private val testMessageBarMessageMessageFlow = testMessageBarMessage.flow.map { if (it) MessageBarMessage.TEST_MESSAGE_BAR else null}
    private val emailConfirmationScheduledMessageFlow =emailConfirmationScheduled.flow.map { if (it) MessageBarMessage.EMAIL_CONFIRMATION_SCHEDULED else null}
    private val emailConfirmedMessageFlow = emailConfirmed.flow.map { if (it) MessageBarMessage.EMAIL_CONFIRMED else null}
    private val noEmailEnteredMessageFlow = noEmailEntered.flow.map { if (it) MessageBarMessage.NO_EMAIL_ENTERED_FOR_AUTO_BACKUP else null}
    private val serverRejectedEmailMessageFlow = serverRejectedEmail.flow.map { if (it) MessageBarMessage.UNKNOWN_OR_UNVERIFIED_EMAIL else null}
    private val badVerificationCodeClickedMessageFlow = badVerificationCodeClicked.flow.map { if (it) MessageBarMessage.BAD_VERIFICATION_CODE_CLICKED else null}
    private val invalidEmailAddressStoredMessageFlow = invalidEmailAddressStored.flow.map { if (it) MessageBarMessage.INVALID_EMAIL_ADDRESS else null}

    fun getMessageBarMessageToDisplayFlow(): Flow<MessageBarMessage?> = combine(
        backupNeededMessageFlow,
        testMessageBarMessageMessageFlow,
        emailConfirmationScheduledMessageFlow,
        emailConfirmedMessageFlow,
        noEmailEnteredMessageFlow,
        serverRejectedEmailMessageFlow,
        badVerificationCodeClickedMessageFlow,
        invalidEmailAddressStoredMessageFlow
    ){ messagesWaiting ->
        messagesWaiting.filterNotNull().firstOrNull() ?: MessageBarMessage.NO_MESSAGE
    }


}