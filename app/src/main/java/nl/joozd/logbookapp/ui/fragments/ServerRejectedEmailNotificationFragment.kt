package nl.joozd.logbookapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.databinding.FragmentGenericNotificationBinding
import nl.joozd.logbookapp.ui.utils.MessageBarFragment

class ServerRejectedEmailNotificationFragment: MessageBarFragment() {
    override val messageTag = "ServerRejectedEmailNotificationFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentGenericNotificationBinding.bind(inflateGenericLayout(inflater, container)).apply {
        genericNotificationMessage.setText(R.string.email_address_rejected)
        negativeButton.setText(R.string.delete)

    }.root

    fun FragmentGenericNotificationBinding.makePositiveButton(){
        with(positiveButton){
            setText(R.string.confirm)
            setOnClickListener{
                TaskFlags.verifyEmail = true
            }
        }
    }
}