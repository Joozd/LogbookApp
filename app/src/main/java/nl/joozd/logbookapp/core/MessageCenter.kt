package nl.joozd.logbookapp.core


import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.ui.utils.MessageBarFragment
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.UserMessage
import java.util.*

object MessageCenter {
    private val messageQueue = LinkedList<UserMessage>()
    private val messageBarFragmentQueue = LinkedList<MessageBarFragment>()
    private var waitingForNextFragmentPush: Boolean = false

    val messageFlow: StateFlow<UserMessage?> = MutableStateFlow(null)
    private var currentMessage by CastFlowToMutableFlowShortcut(messageFlow)

    val messageBarFragmentFlow: StateFlow<MessageBarFragment?> = MutableStateFlow(null)
    private var currentMessageBarFragment by CastFlowToMutableFlowShortcut(messageBarFragmentFlow)



    fun pushMessage(message: UserMessage){
        message.addCleanUpAction { nextMessage() }
        addMessageToQueue(message)
    }

    fun pushMessageBarFragment(fragment: MessageBarFragment){
        if (notYetInQueue(fragment)) {
            fragment.setOnCompleted {
                waitingForNextFragmentPush = true
                currentMessageBarFragment = null
                MainScope().launch { nextFragmentWithDelay() }
            }
            addFragmentToQueue(fragment)
        }
    }

    //Remove a messageBarFragment from queue if it is in there, by tag.
    fun pullMessageBarFragmentByTag(messageTag: String){
        messageBarFragmentQueue.firstOrNull { it.messageTag == messageTag}?.let{
            messageBarFragmentQueue.remove(it)
        }
    }

    private fun addMessageToQueue(msg: UserMessage){
        messageQueue.add(msg)

        //push message if none active yet
        if (currentMessage == null)
            nextMessage()
    }

    private fun addFragmentToQueue(fragment: MessageBarFragment){
        messageBarFragmentQueue.add(fragment)

        //push fragment if none active yet
        if (currentMessageBarFragment == null && !waitingForNextFragmentPush)
            nextFragment()
    }

    // Will put null if no next message present
    private fun nextMessage(){
        currentMessage = messageQueue.poll()
    }

    // Will put null if no next fragment present
    private fun nextFragment(){
        currentMessageBarFragment = messageBarFragmentQueue.poll()
        waitingForNextFragmentPush = false
    }

    // Will put null if no next fragment present
    private suspend fun nextFragmentWithDelay(){
        delay(1000) // time for short fade out
        nextFragment()
    }

    private fun notYetInQueue(fragment: MessageBarFragment) =
        fragment.messageTag == null ||
                (fragment.messageTag !in messageBarFragmentQueue.map { it.tag }
                        && fragment.messageTag != currentMessageBarFragment?.messageTag)
}