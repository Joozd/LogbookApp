package nl.joozd.logbookapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.databinding.FragmentGenericNotificationBinding
import nl.joozd.logbookapp.ui.utils.MessageBarFragment

fun makeGenericMessageBarFragment(builder: MessageCenter.MessageFragmentBuilder)= object: MessageBarFragment(){
    override val messageTag = builder.tag

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentGenericNotificationBinding.bind(inflateGenericLayout(inflater, container)).apply{
        setMessage(builder, this)

        if (builder.hasPositiveButton) makePositiveButton(builder, this)
        else positiveButton.visibility = View.GONE

        if (builder.hasNegativeButton) makeNegativeButton(builder, this)
        else negativeButton.visibility = View.GONE
    }.root
}

private fun MessageBarFragment.setMessage(builder: MessageCenter.MessageFragmentBuilder, binding: FragmentGenericNotificationBinding) {
    with(binding.genericNotificationMessage) {
        with(builder) {
            positiveText?.let { text = it }
            positiveTextResource?.let { setText(it) }
            positiveTextFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { s ->
                    text = s
                }
            }
            positiveTextResourceFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { i ->
                    setText(i)
                }
            }
        }
    }
}

private fun MessageBarFragment.makePositiveButton(builder: MessageCenter.MessageFragmentBuilder, binding: FragmentGenericNotificationBinding){
    with (binding.positiveButton){
        with(builder) {
            positiveText?.let { text = it }
            positiveTextResource?.let { setText(it) }
            positiveTextFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { s ->
                    text = s
                }
            }
            positiveTextResourceFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { i ->
                    setText(i)
                }
            }
            setOnClickListener { builder.positiveAction() }
        }
    }
}

private fun MessageBarFragment.makeNegativeButton(builder: MessageCenter.MessageFragmentBuilder, binding: FragmentGenericNotificationBinding){
    with (binding.negativeButton){
        with(builder) {
            negativeText?.let { text = it }
            negativeTextResource?.let { setText(it) }
            negativeTextFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { s ->
                    text = s
                }
            }
            negativeTextResourceFlow?.let {
                it.launchCollectWhileLifecycleStateStarted { i ->
                    setText(i)
                }
            }
            setOnClickListener { builder.negativeAction() }
        }
    }
}