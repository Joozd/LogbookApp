package nl.joozd.logbookapp.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.background.BackupCenter
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.databinding.FragmentGenericNotificationBinding
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.ui.utils.MessageBarFragment
import java.time.Instant
import java.time.ZoneOffset

/**
 * A small fragment that gives user a choice to backup now or ignore 1 day.
 * Backup shuold run a backup,
 * Ignore should remove the fragment, to be re-displayed the next calendar day.
 */
class BackupNotificationFragment: MessageBarFragment() {
    override val messageTag = "BackupNotificationFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentGenericNotificationBinding.bind(inflateGenericLayout(inflater, container)).apply{
        positiveButton.setText(R.string.backup_now)
        negativeButton.setText(R.string.ignore_for_one_day)
        collectFlows()
        setOnClickListeners()
    }.root


    private fun FragmentGenericNotificationBinding.collectFlows(){
        launchBackupMessageFlow()
    }

    private fun FragmentGenericNotificationBinding.setOnClickListeners(){
        setIgnoreOnClickListener()
        setBackupNowOnClickListener()
    }

    private fun FragmentGenericNotificationBinding.launchBackupMessageFlow(){
        BackupPrefs.mostRecentBackup.flow.launchCollectWhileLifecycleStateStarted { t ->
            genericNotificationMessage.text = makeTimeSinceLastBackupMessage(t)
        }
    }

    private fun makeTimeSinceLastBackupMessage(timeOfLastBackup: Long) = if (timeOfLastBackup == 0L)
        getString(R.string.you_have_never_backed_up)
    else {
        val elapsedTime = Instant.now().epochSecond - timeOfLastBackup
        getString(R.string.you_have_not_backed_up_n_days, elapsedTime / ONE_DAY_IN_SECONDS)
    }

    private fun FragmentGenericNotificationBinding.setIgnoreOnClickListener(){
        negativeButton.setOnClickListener {
            BackupPrefs.backupIgnoredUntil(Instant.now().atStartOfDay(currentLocalZoneOffset()).epochSecond + ONE_DAY_IN_SECONDS)
            onCompleted()
        }
    }

    private fun FragmentGenericNotificationBinding.setBackupNowOnClickListener(){
        positiveButton.setOnClickListener {
            positiveButton.setBackupButtonToBuildingCsv()
            lifecycleScope.launch {
                shareCsvAndActivateBackupNowButton(BackupCenter.makeBackupUri())
            }
        }
    }

    private fun FragmentGenericNotificationBinding.shareCsvAndActivateBackupNowButton(
        it: Uri
    ) {
        requireActivity().makeCsvSharingIntent(it)
        setBackupButtonToActive()
        onCompleted()
    }

    private fun TextView.setBackupButtonToBuildingCsv() {
        setOnClickListener {  }
        setText(R.string.exporting_csv)
    }

    private fun FragmentGenericNotificationBinding.setBackupButtonToActive() {
        setBackupNowOnClickListener()
        positiveButton.setText(R.string.backup_now)
    }

    private fun currentLocalZoneOffset(): ZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
}