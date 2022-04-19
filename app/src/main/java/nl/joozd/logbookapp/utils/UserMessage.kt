package nl.joozd.logbookapp.utils

import android.app.Activity
import android.app.AlertDialog

open class UserMessage(
    private val titleResource: Int,
    private val descriptionResource: Int,
    private val choice1Resource: Int?,
    private val choice2Resource: Int?,
    private val action1: UserChoiceListener,
    private val action2: UserChoiceListener
) {
    fun interface UserChoiceListener{
        operator fun invoke()
    }

    open class Builder{
        var titleResource: Int? = null
        var descriptionResource: Int? = null
        var choice1Resource: Int? = null
        var choice2Resource: Int? = null
        var action1Listener = UserChoiceListener {  }
        var action2Listener = UserChoiceListener {  }

        fun setAction1(action: UserChoiceListener){
            action1Listener = action
        }

        fun setAction2(action: UserChoiceListener){
            action2Listener = action
        }

        fun setPositiveButton(resource: Int, action: UserChoiceListener){
            choice1Resource = resource
            action1Listener = action
        }

        fun setNegativeButton(resource: Int, action: UserChoiceListener){
            choice2Resource = resource
            action2Listener = action
        }

        open fun build(): UserMessage =
            UserMessage(
                titleResource ?: android.R.string.unknownName,
                descriptionResource ?: android.R.string.unknownName,
                choice1Resource,
                choice2Resource,
                action1Listener,
                action2Listener
            )
    }

    fun toAlertDialog(activity: Activity): AlertDialog =
        AlertDialog.Builder(activity).apply {
            setTitle(titleResource)
            setMessage(descriptionResource)
            choice1Resource?.let{
                setPositiveButton(it){ _, _ ->
                    action1()
                }
            }
            choice2Resource?.let{
                setNegativeButton(it){ _, _ ->
                    action2()
                }
            }
        }.create()

}