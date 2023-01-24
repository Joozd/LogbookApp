package nl.joozd.logbookapp.core.messages

import androidx.annotation.CallSuper
import androidx.fragment.app.commit
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * Classes implementing this should also implement either PersistentMessage or InstantMessage
 */
abstract class DialogMessage: Message {
    protected abstract val messageTag: String

    abstract val titleRes: Int
    open val titleFormatArgs: Array<Any> = emptyArray()

    abstract val messageRes: Int
    open val messageFormatArgs: Array<Any> = emptyArray()

    /**
     * Positive button (right). Defaults to "OK" and  { /*do nothing*/ }
     */
    open val positiveButtonTextRes: Int = android.R.string.ok

    /**
     * The action to be performed when positive button is clicked.
     * Call super to make sure dialog gets closed.
     */
    @CallSuper
    open fun positiveButtonAction(activity: JoozdlogActivity) { clearMessage(activity) }

    /**
     * Negative button (left). Defaults to null which means not there.
     */
    open val negativeButtonTextRes: Int? = null // can be null, negative button not required. If not null, negativeButtonAction must not be null

    /**
     * The action to be performed when negative button is clicked.
     * Call super to make sure dialog gets closed.
     */
    @CallSuper
    open fun negativeButtonAction(activity: JoozdlogActivity) { clearMessage(activity) }

    open fun displayDialog(activity: JoozdlogActivity) {
        //If a dialog is already opened when this gets executed, destroy the old dialog.
        clearMessage(activity)
        if (this !is Message.NoMessage) {
            val dialogMessageFragment = DialogMessageFragment(this)
            activity.showFragment(dialogMessageFragment, tag = messageTag, addToBackStack = true)
        }
    }

    fun clearMessage(activity: JoozdlogActivity) {
        println("QQQQQQQQQQQQQ clearMessage called")
        activity.supportFragmentManager.findFragmentByTag(messageTag)?.let {
            activity.supportFragmentManager.commit { remove(it) }
        }
    }
}