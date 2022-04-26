package nl.joozd.logbookapp.ui.utils

import androidx.lifecycle.ViewModel

class MessageBarFragmentViewModel: ViewModel() {
    var onCompleted: (() -> Unit)? = null


}