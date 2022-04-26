package nl.joozd.logbookapp.model.viewmodels.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.model.viewmodels.status.BackupCenterStatus
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.time.Instant
import java.time.LocalDate

class BackupNotificationFragmentViewModel: ViewModel() {

}