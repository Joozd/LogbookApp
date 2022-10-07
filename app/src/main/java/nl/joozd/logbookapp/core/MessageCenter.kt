package nl.joozd.logbookapp.core


import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.UserMessage
import nl.joozd.logbookapp.ui.fragments.makeGenericMessageBarFragment
import nl.joozd.logbookapp.ui.utils.MessageBarFragment
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.util.*

object MessageCenter {
    private val messageQueue = LinkedList<UserMessage>()
    private val messageBarFragmentQueue = LinkedList<MessageBarFragment>()
    private var readyToDisplayNextFragment: Boolean = false

    val messageFlow: StateFlow<UserMessage?> = MutableStateFlow(null)
    private var currentMessage by CastFlowToMutableFlowShortcut(messageFlow)

    val messageBarFragmentFlow: StateFlow<MessageBarFragment?> = MutableStateFlow(null)
    private var currentMessageBarFragment by CastFlowToMutableFlowShortcut(messageBarFragmentFlow)



    fun pushMessage(message: UserMessage){
        message.addCleanUpAction { nextMessage() }
        addMessageToQueue(message)
    }

    fun commitMessage(message: UserMessage.Builder.() -> Unit){
        UserMessage.Builder().apply{
            message()
            pushMessage(this.build())
        }
    }

    fun pushMessageBarFragment(fragment: MessageBarFragment){
        println("DEBUG: PushMessageBarFragment called with ${fragment.messageTag}")
        if (notYetInQueue(fragment)) {
            println("DEBUG: 1")
            fragment.setOnCompleted {
                println("DEBUG: 2")
                currentMessageBarFragment = null
                readyToDisplayNextFragment = true
                MainScope().launch { nextFragmentWithDelay() }
            }
            println("DEBUG: 6")
            addFragmentToQueue(fragment)
            println("DEBUG: 7")
        }
        println("DEBUG: messageBarFragmentQueue.size = ${messageBarFragmentQueue.size}")
    }

    //Remove a messageBarFragment from queue if it is in there, by tag.
    fun pullMessageBarFragmentByTag(messageTag: String){
        messageBarFragmentQueue.firstOrNull { it.messageTag == messageTag}?.let{
            messageBarFragmentQueue.remove(it)
        }
    }

    fun pushGenericError(messageResource: Int){
        val message = UserMessage.Builder().apply{
            titleResource = R.string.error
            descriptionResource = messageResource
            setPositiveButton(android.R.string.ok){ }
        }.build()
        pushMessage(message)
    }

    private fun addMessageToQueue(msg: UserMessage){
        if (msg !in messageQueue)
            messageQueue.add(msg)

        //push message if none active yet
        if (currentMessage == null)
            nextMessage()
    }

    private fun addFragmentToQueue(fragment: MessageBarFragment){
        messageBarFragmentQueue.add(fragment)
        println("added (proof: ${messageBarFragmentQueue.size}")

        //push fragment if none active yet
        if (currentMessageBarFragment == null && !readyToDisplayNextFragment)
            nextFragment()
    }

    // Will put null if no next message present
    private fun nextMessage(){
        currentMessage = messageQueue.poll()
    }

    // Will put null if no next fragment present
    private fun nextFragment(){
        currentMessageBarFragment = messageBarFragmentQueue.poll()
        readyToDisplayNextFragment = false
    }

    // Will put null if no next fragment present
    private suspend fun nextFragmentWithDelay(){
        delay(600) // time for short fade out
        nextFragment()
    }

    class MessageFragmentBuilder {
        var tag: String? = null

        var message: String? = null
        var messageResource: Int? = null
        var messageFlow: Flow<String>? = null
        var messageResourceFlow: Flow<Int>? = null

        val hasPositiveButton get() = positiveText != null && positiveTextResource != null && positiveTextFlow!= null && positiveTextResource != null
        var positiveText: String? = null
        var positiveTextResource: Int? = null
        var positiveTextFlow: Flow<String>? = null
        var positiveTextResourceFlow: Flow<Int>? = null
        var positiveAction: Executable = Executable{ }
        fun positiveAction(action: Executable){ positiveAction = action}
        fun setPositiveButton(text: Int, action: Executable){
            positiveTextResource = text
            positiveAction = action
        }
        fun setPositiveButton(text: String, action: Executable){
            positiveText = text
            positiveAction = action
        }

        val hasNegativeButton get() = negativeText != null && negativeTextResource != null && negativeTextFlow!= null && negativeTextResourceFlow != null
        var negativeText: String? = null
        var negativeTextResource: Int? = null
        var negativeTextFlow: Flow<String>? = null
        var negativeTextResourceFlow: Flow<Int>? = null
        var negativeAction: Executable = Executable {  }
        fun negativeAction(action: Executable){ negativeAction = action}
        fun setNegativeButton(text: Int, action: Executable){
            negativeTextResource = text
            negativeAction = action
        }
        fun setNegativeButton(text: String, action: Executable){
            negativeText = text
            negativeAction = action
        }

        fun build(): MessageBarFragment = makeGenericMessageBarFragment(this)

        fun buildAndPush(){
            pushMessageBarFragment(build())
        }

        fun commit(block: MessageFragmentBuilder.() -> Unit){
            block()
            buildAndPush()
        }


        fun interface Executable{
            operator fun invoke()
        }
    }

    private fun notYetInQueue(fragment: MessageBarFragment): Boolean {
        println("Called notYetInQueue. tag: ${fragment.messageTag}, queue: ${messageBarFragmentQueue.map { it.tag }} ")
        return (fragment.messageTag == null
                || (fragment.messageTag !in messageBarFragmentQueue.map { it.tag }
                && fragment.messageTag != currentMessageBarFragment?.messageTag)).also { println ( "Returned $it" )}
    }
}