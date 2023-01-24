package nl.joozd.logbookapp.core

import android.view.View
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.messages.*
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * MessageCenter keep track of messages to be displayed, and displays them to registered activities
 */
object MessageCenter {
    private val instantMessageFlow = MutableSharedFlow<InstantMessage>(replay = 0)

    /**
     * Register this [activity] as a contyext to display message dialog fragments.
     */
    fun registerActivityForDialogDisplay(activity: JoozdlogActivity){
        with(activity){
            MessagesWaiting.getDialogMessageToDisplayFlow().launchCollectWhileLifecycleStateStarted{
                it?.displayDialog(activity)
            }
            instantMessageFlow.launchCollectWhileLifecycleStateStarted{
                it.displayDialog(activity)
            }
        }
    }

    /**
     * Register this [activity] for displaying message bar messages. Needs a [container] to insert messages into.
     */
    fun registerActivityForMessageBarDisplay(activity: JoozdlogActivity, container: View){
        with(activity){
            MessagesWaiting.getMessageBarMessageToDisplayFlow().launchCollectWhileLifecycleStateStarted{
                it?.displayMessage(activity, container)
            }
        }
    }

    fun scheduleMessage(message: Message){
        when(message) {
            is PersistentMessage -> message.messageNeedsToBeDisplayedFlag(true)
            is InstantMessage -> MainScope().launch { instantMessageFlow.emit(message) }
        }
    }

    // Unscheduling a displayed message will change the output of the combined flow in MessagesWaiting. This will either lead to the current message being recreated, or no message being displayed at all.
    fun unscheduleMessage(message: PersistentMessage){
        message.messageNeedsToBeDisplayedFlag(false)
    }
}