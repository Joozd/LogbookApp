package nl.joozd.logbookapp.ui.utils

import androidx.fragment.app.viewModels

/**
 * When message in in this fragment has been completed (i.e. bar can be closed), call [onCompleted]
 * [MessageCenter] will take care of
 */
abstract class MessageBarFragment: JoozdlogFragment() {
    private val messageBarFragmentViewModel: MessageBarFragmentViewModel by viewModels()
    fun setOnCompleted(onCompleted: () -> Unit){
        messageBarFragmentViewModel.onCompleted = onCompleted
    }

    protected fun onCompleted() =
        messageBarFragmentViewModel.onCompleted?.let{
            it()
        }

}