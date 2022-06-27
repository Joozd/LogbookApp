package nl.joozd.logbookapp.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import androidx.activity.ComponentActivity

open class UserMessage(
    private val titleResource: Int,
    private val descriptionResource: Int,
    private val choice1Resource: Int?,
    private val choice2Resource: Int?,
    private val action1: UserChoiceListener,
    private val action2: UserChoiceListener,
    private val cleanUp: MutableList<() -> Unit> = ArrayList()
) {
    override operator fun equals(other: Any?): Boolean =
        other is UserMessage
                && other.titleResource == titleResource
                && other.descriptionResource == descriptionResource
                && other.choice1Resource == choice1Resource
                && other.choice2Resource == choice2Resource

    fun addCleanUpAction(action: () -> Unit){
        cleanUp.add(action)
    }

    override fun hashCode(): Int =
        titleResource + descriptionResource + (choice1Resource ?: 0) + (choice2Resource ?: 0)




    fun interface UserChoiceListener{
        fun ComponentActivity.execute()
    }

    open class Builder{
        var titleResource: Int? = null
        var descriptionResource: Int? = null
        var choice1Resource: Int? = null
        var choice2Resource: Int? = null
        var action1Listener = UserChoiceListener {  }
        var action2Listener = UserChoiceListener {  }

        fun setAction1(action: UserChoiceListener): Builder{
            action1Listener = action
            return this
        }

        fun setAction2(action: UserChoiceListener): Builder{
            action2Listener = action
            return this
        }

        fun setPositiveButton(resource: Int, action: UserChoiceListener): Builder{
            choice1Resource = resource
            action1Listener = action
            return this
        }

        fun setNegativeButton(resource: Int, action: UserChoiceListener): Builder{
            choice2Resource = resource
            action2Listener = action
            return this
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

    fun toAlertDialog(activity: ComponentActivity): AlertDialog =
        AlertDialog.Builder(activity).apply {
            setTitle(titleResource)
            setMessage(descriptionResource)
            choice1Resource?.let{
                setPositiveButton(it){ _, _ ->
                    with (action1) { activity.execute() }
                    cleanUp.forEach {
                        it()
                    }
                }
            }
            choice2Resource?.let{
                setNegativeButton(it){ _, _ ->
                    with (action2) { activity.execute() }
                    cleanUp.forEach {
                        it()
                    }
                }
            }
        }.create()



}