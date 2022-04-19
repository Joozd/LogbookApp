package nl.joozd.logbookapp.ui.messageCenter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.UserMessage
import java.util.*

object MessageCenter {
    val messageFlow: StateFlow<UserMessage?> = MutableStateFlow(null)
    private var _message by CastFlowToMutableFlowShortcut(messageFlow)

    private val messageQueue = LinkedList<UserMessage>()

    fun pushMessage(message: UserMessage){
        message.addCleanUpAction { nextMessage() }
        addMessageToQueue(message)
    }

    private fun addMessageToQueue(msg: UserMessage){
        messageQueue.add(msg)

        //push message if none active yet
        if (_message == null)
            nextMessage()
    }

    // Will put null if no next message present
    private fun nextMessage(){
        _message = messageQueue.poll()
    }
}