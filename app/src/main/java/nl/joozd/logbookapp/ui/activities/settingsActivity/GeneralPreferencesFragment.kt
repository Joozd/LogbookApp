package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithSpecializedFunctions
import nl.joozd.logbookapp.databinding.ActivitySettingsGeneralBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.model.viewmodels.activities.settingsActivity.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

class GeneralPreferencesFragment: JoozdlogFragment() {
    private val viewModel: SettingsActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivitySettingsGeneralBinding.bind(inflater.inflate(R.layout.activity_settings_general, container, false)).apply {
            initializeViews()
            launchFlowCollectors()
            setOnClickListeners()
            initializeDarkModeSpinner()
        }.root

    private fun ActivitySettingsGeneralBinding.initializeViews(){
        //TODO migrate all views here. Every view should live in its own function so we can easily see everything that happens to it.
        initializeReplaceOwnNameWithSelfSwitch()
        initializeOwnName()
    }

    private fun ActivitySettingsGeneralBinding.initializeReplaceOwnNameWithSelfSwitch() =
        settingsReplaceOwnNameWithSelf.apply {
            /*
            Tell viewModel to set Prefs.replaceOwnNameWithSelf to the setting of this switch when its state gets changed.
            This will probably run the first time it gets set from [viewModel.replaceOwnNameWithSelfFlow] but that is not a problem.
             */
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleReplaceOwnNameWithSelfTo(isChecked)
            }
            /*
            Toggle switch when [viewModel.replaceOwnNameWithSelfFlow], which tracks [Prefs.replaceOwnNameWithSelf] toggles.
            Prefs flows are distinct until changed so the onCheckChangedListener will not get into an endless loop.
            Also onCheckChanged should not trigger when setting the already set value.
             */
            viewModel.replaceOwnNameWithSelfFlow.launchCollectWhileLifecycleStateStarted{
                isChecked = it
            }
        }

    private fun ActivitySettingsGeneralBinding.initializeOwnName(){
        initializeOwnNameTextInputLayout()
        initializeOwnNameEditText()
    }

    private fun ActivitySettingsGeneralBinding.initializeOwnNameTextInputLayout() = ownNameTextInputLayout.apply{
        viewModel.replaceOwnNameWithSelfFlow.launchCollectWhileLifecycleStateStarted{
            // Show "Own Name" entry field if [Prefs.replaceOwnNameWithSelf] is true, or hide it when false.
            visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun ActivitySettingsGeneralBinding.initializeOwnNameEditText() =
        ownNameEditText.apply{
            // display name, send entered name to viewModel to be saved.
            separateDataDisplayAndEntry {
                it?.let{name ->
                    viewModel.updateOwnName(name.toString())
                }
            }
            // Keep text synced with Prefs.ownName
            viewModel.ownNameFlow.launchCollectWhileLifecycleStateStarted{
                setText(it)
            }
        }


    private fun ActivitySettingsGeneralBinding.launchFlowCollectors(){
        viewModel.useIataFlow.launchCollectWhileLifecycleStateStarted {
            setSettingsUseIataSelector(it)
        }

        viewModel.picNameRequiredFlow.launchCollectWhileLifecycleStateStarted{
            settingsPicNameRequiredSwitch.isChecked = it
            picNameRequiredText.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.augmentedTakeoffLandingTimesFlow.launchCollectWhileLifecycleStateStarted{
            augmentedCrewButton.text = activity?.getStringWithMakeup(R.string.standard_augmented_time, it.toString())
        }
    }

    private fun ActivitySettingsGeneralBinding.setOnClickListeners(){
        settingsUseIataSelector.setOnClickListener{
            viewModel.toggleUseIataAirports()
        }

        settingsPicNameRequiredSwitch.setOnClickListener {
            viewModel.toggleRequirePicName()
        }

        augmentedCrewButton.setOnClickListener { showAugmentedTimesNumberPicker() }

        augmentedTakeoffTimeHintButton.setOnClickListener {
            viewModel.showHint(R.string.augmented_crew_time_explanation)
        }

        settingsFixFlightDbButton.setOnClickListener {
            it.isEnabled = false
            fixDB(it)
        }

    }

    private fun ActivitySettingsGeneralBinding.initializeDarkModeSpinner() {
        darkModePickerSpinner.adapter = ArrayAdapter.createFromResource(
            activity ?: App.instance,
            R.array.dark_mode_choices,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        darkModePickerSpinner.setSelection(viewModel.defaultNightMode)

        darkModePickerSpinner.onItemSelectedListener = darkModeSelectedListener
    }

    private val darkModeSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) { }
        override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
            viewModel.darkmodePicked(position)
        }
    }

    private fun ActivitySettingsGeneralBinding.setSettingsUseIataSelector(it: Boolean) {
        settingsUseIataSelector.setText(if (it) R.string.useIataAirports else R.string.useIcaoAirports)
    }

    private fun showAugmentedTimesNumberPicker(){
        AugmentedTakeoffLandingTimesPicker().apply {
            title= App.instance.getString(R.string.time_for_to_ldg)
            wrapSelectorWheel = false
            maxValue = AugmentedTakeoffLandingTimesPicker.EIGHT_HOURS
        }.show(supportFragmentManager, null)
    }

    private fun fixDB(viewToEnableWhenDone: View){
        lifecycleScope.launch {
            val removedFlightsCount = FlightRepositoryWithSpecializedFunctions.instance.removeDuplicates()
            if (removedFlightsCount == 0)
                toast(R.string.no_duplicate_flights_found)
            else
                showDuplicatesFoundDialog(removedFlightsCount)
            viewToEnableWhenDone.isEnabled = true
        }
    }

    private fun showDuplicatesFoundDialog(removedFlightsCount: Int){
        AlertDialog.Builder(activity).apply{
            setTitle(R.string.delete)
            setMessage(getString(R.string.n_duplicates_have_been_removed_long_message, removedFlightsCount))
            setPositiveButton(android.R.string.ok){ _, _ ->
                // intentionally left blank
            }
        }.create().show()
    }
}