package nl.joozd.logbookapp.ui.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R

/**
 * When message in in this fragment has been completed (i.e. bar can be closed), call [onCompleted]
 * If [messageTag] is set this will not be inserted if the same message is already in MessageCenter queue or active.
 */
abstract class MessageBarFragment: JoozdlogFragment() {
    open val messageTag: String? = null // this will be lost on recreate but that is OK, only used for checking duplicates at insertion in MessageCenter.
    private var onCompleted: (() -> Unit)? = null

    private val messageBarFragmentViewModel: MessageBarFragmentViewModel by viewModels()
    fun setOnCompleted(onCompleted: () -> Unit){
        this.onCompleted = onCompleted
    }

    protected fun onCompleted() =
        messageBarFragmentViewModel.onCompleted?.let{
            it()
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onCompleted?.let { messageBarFragmentViewModel.onCompleted = it }
    }

    protected fun inflateGenericLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View = inflater.inflate(R.layout.fragment_generic_notification, container, false)

}