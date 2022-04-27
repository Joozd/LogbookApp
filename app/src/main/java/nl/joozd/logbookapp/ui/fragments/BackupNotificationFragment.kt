package nl.joozd.logbookapp.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.BackupCenter
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.databinding.FragmentBackupNotificationBinding
import nl.joozd.logbookapp.extensions.atStartOfDay
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.model.viewmodels.status.BackupCenterStatus
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
    ) = FragmentBackupNotificationBinding.bind(inflateLayout(inflater, container)).apply{
        collectFlows()
        setOnClickListeners()
        collectStatus()
    }.root


    private fun FragmentBackupNotificationBinding.collectFlows(){
        launchBackupMessageFlow()
    }

    private fun FragmentBackupNotificationBinding.setOnClickListeners(){
        setIgnoreOnClickListener()
        setBackupNowOnClickListener()
    }

    private fun FragmentBackupNotificationBinding.launchBackupMessageFlow(){
        BackupPrefs.mostRecentBackupFlow.launchCollectWhileLifecycleStateStarted { t ->
            val elapsedTime = Instant.now().epochSecond - t
            backupMessage.text = if (BackupPrefs.mostRecentBackup == 0L)
                getString(R.string.you_have_never_backed_up)
            else
                getString(R.string.you_have_not_backed_up_n_days, elapsedTime / ONE_DAY_IN_SECONDS)
        }
    }

    private fun FragmentBackupNotificationBinding.setIgnoreOnClickListener(){
        ignoreButton.setOnClickListener {
            BackupPrefs.backupIgnoredUntil = Instant.now().atStartOfDay(ZoneOffset.UTC).epochSecond + ONE_DAY_IN_SECONDS
            onCompleted()
        }
    }

    private fun FragmentBackupNotificationBinding.setBackupNowOnClickListener(){
        backupButton.setOnClickListener {
            BackupCenter.putBackupUriInStatus()
        }
    }

    private fun FragmentBackupNotificationBinding.collectStatus(){
        BackupCenter.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { } // do nothing
                is BackupCenterStatus.BuildingCsv -> backupButton.setBackupButtonToBuildingCsv()
                is BackupCenterStatus.SharedUri -> shareCsvAndActivateBackupNowButton(it.uri)
            }
        }
    }

    private fun FragmentBackupNotificationBinding.shareCsvAndActivateBackupNowButton(
        it: Uri
    ) {
        requireActivity().makeCsvSharingIntent(it)
        backupButton.setBackupButtonToActive()
        onCompleted()
    }

    private fun TextView.setBackupButtonToBuildingCsv() {
        setOnClickListener {  }
        setText(R.string.exporting_csv)
    }

    private fun TextView.setBackupButtonToActive() {
        setOnClickListener { BackupCenter.putBackupUriInStatus() }
        setText(R.string.backup_now)
        BackupCenter.reset()
    }

    private fun inflateLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = inflater.inflate(R.layout.fragment_backup_notification, container, false)
}