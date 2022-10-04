package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.background.BackupCenter
import nl.joozd.logbookapp.databinding.ActivitySettingsBackupBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.model.viewmodels.activities.settingsActivity.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.EmailSetDialog
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class BackupPreferencesFragment: JoozdlogFragment() {
    private val viewModel: SettingsActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivitySettingsBackupBinding.bind(inflater.inflate(R.layout.activity_settings_backup, container, false)).apply {
            // onClickListeners for backupIntervalButton, backupFromCloudSwitch, emailAddressButton and backupNowButton.
            launchFlowCollectors()
            setOnClickListeners()
        }.root


    private fun ActivitySettingsBackupBinding.launchFlowCollectors(){
        viewModel.backupIntervalFlow.launchCollectWhileLifecycleStateStarted{
            backupIntervalButton.text = getBackupIntervalString(it)
        }

        viewModel.sendBackupEmailsFlow.launchCollectWhileLifecycleStateStarted {
            setEmailAddressButtonState(it)
            if (it && !viewModel.emailEntered())
                showEmailDialog()
        }

        backupFromCloudSwitch.bindToFlow(viewModel.sendBackupEmailsFlow)

        viewModel.emailDataFlow.launchCollectWhileLifecycleStateStarted{
            emailAddressButton.text = getEmailAddressButtonString(it)
        }
    }


    private fun ActivitySettingsBackupBinding.setOnClickListeners(){
        backupIntervalButton.setOnClickListener { showBackupIntervalNumberPicker() }

        backupFromCloudSwitch.setOnClickListener { toggleEmailBackup() }

        emailAddressButton.setOnClickListener { showEmailDialog() }

        backupNowButton.setBackupNowButtonToActive()
    }


    private fun showBackupIntervalNumberPicker(){
        BackupIntervalNumberPicker().apply{
            title = App.instance.getString(R.string.pick_backup_interval)
            wrapSelectorWheel = false
            maxValue = 365

        }.show(supportFragmentManager, null)
    }

    // Toggles EmailPrefs.emailBackup. This will receive feedback through flow collector, which will take care of UI setting.
    private fun toggleEmailBackup(){
        viewModel.toggleEmailBackupEnabled()
    }

    /*
     * Show dialog to enter an email address.
     * NOTE Email backup can be scheduled right away, as it will wait for user to confirm email address.
     */
    private fun showEmailDialog() {
        activity?.showFragment<EmailSetDialog>()
    }

    private fun Button.setBackupNowButtonToActive() {
        setOnClickListener {
            setBackupNowButtonToBuildingCsv()
            lifecycleScope.launch {
                //inside launch block because BackupCenter.makeBackupUri() suspends
                shareCsvAndActivateBackupNowButton(BackupCenter.makeBackupUri())
            }
            setText(R.string.backup_now)
        }
    }

    private fun Button.setBackupNowButtonToBuildingCsv() {
        setOnClickListener {  /* Intentionally left blank */ }
        setText(R.string.exporting_csv)
    }


    private fun Button.shareCsvAndActivateBackupNowButton(
        it: Uri
    ) {
        activity?.makeCsvSharingIntent(it)
        setBackupNowButtonToActive()
        viewModel.resetStatus()
    }

    // Buttons text will be updated with emailData through flow collector, whether it is visible or not.
    // This button is visible only when "Auto-send backup emails" is enabled.
    private fun ActivitySettingsBackupBinding.setEmailAddressButtonState(enabled: Boolean){
        if (enabled){
            emailAddressButton.visibility = View.VISIBLE
        }
        else{
            emailAddressButton.visibility = View.GONE
        }
    }

    private fun getBackupIntervalString(it: Int) = activity?.getStringWithMakeup(
        R.string.backup_interval_time, (if (it == 0) getString(
            R.string.never
        ) else getString(R.string.n_days, it.toString()))
    )

    private fun getEmailAddressButtonString(emailData: Pair<String, Boolean>): String{
        val addres= emailData.first
        val verified = emailData.second

        val stringResource = if (verified) R.string.verified_email else R.string.not_verified_email
        if (addres.isBlank()) return getString(R.string.enter_email)

        return getString(stringResource, addres)
    }
}