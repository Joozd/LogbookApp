package nl.joozd.logbookapp.core.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogPersistantDialogBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

class DialogMessageFragment(): Fragment() {
    constructor(message: DialogMessage): this()
    { this.message = message }
    private var message: DialogMessage? = null


    private val joozdlogActivity get() = activity as JoozdlogActivity

    private val viewModel: MessageFragmentViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPersistantDialogBinding.bind(inflater.inflate(R.layout.dialog_persistant_dialog, container, false)).apply {
            //I could combine this into a single "viewmodel.message.apply",
            // but I am only using it as a null check and I am not applying anything to message
            // so I think this is easier to understand.

            message?.let {
                viewModel.message = it
            }

            viewModel.message?.let { message ->
                with(message as DialogMessage) { // can cast because it is added only as a MessageBarMessage (in secondary constructor)
                    persistantDialogTitleTextView.text = if (titleFormatArgs.isEmpty()) getString(titleRes) else getString(titleRes, *titleFormatArgs)
                    persistantDialogMessage.text = if (messageFormatArgs.isEmpty()) getString(messageRes) else getString(messageRes, *messageFormatArgs)
                    persistantDialogPositiveButton.setText(positiveButtonTextRes)
                    persistantDialogPositiveButton.setOnClickListener {
                        clearMessage(joozdlogActivity)
                        positiveButtonAction(joozdlogActivity)
                    }
                    if (negativeButtonTextRes != null) {
                        persistantDialogNegativeButton.setText(
                            negativeButtonTextRes ?: android.R.string.ok
                        ) // this elvis operator is probably never used but better than forced not null
                        persistantDialogNegativeButton.setOnClickListener {
                            clearMessage(joozdlogActivity)
                            negativeButtonAction(joozdlogActivity)
                        }
                    } else {
                        persistantDialogNegativeButton.visibility = View.GONE
                    }
                }
            }
        }.root
}