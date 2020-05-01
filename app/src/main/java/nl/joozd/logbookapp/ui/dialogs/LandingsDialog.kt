package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.picker_landings.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.dialogs.LandingsDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

class LandingsDialog: JoozdlogFragment() {
    private val landingsDialogViewModel: LandingsDialogViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.picker_landings, container, false).apply{

            toDayUpButton.setOnClickListener { landingsDialogViewModel.toDayUpButtonClick() }
            toNightUpButton.setOnClickListener { landingsDialogViewModel.toNightUpButtonClick() }
            ldgDayUpButton.setOnClickListener { landingsDialogViewModel.ldgDayUpButtonClick() }
            ldgNightUpButton.setOnClickListener { landingsDialogViewModel.ldgNightUpButtonClick() }
            autolandUpButton.setOnClickListener { landingsDialogViewModel.autolandUpButtonClick() }

            toDayDownButton.setOnClickListener { landingsDialogViewModel.toDayDownButtonClick() }
            toNightDownButton.setOnClickListener { landingsDialogViewModel.toNightDownButtonClick() }
            ldgDayDownButton.setOnClickListener { landingsDialogViewModel.ldgDayDownButtonClick() }
            ldgNightDownButton.setOnClickListener { landingsDialogViewModel.ldgNightDownButtonClick() }
            autolandDownButton.setOnClickListener { landingsDialogViewModel.autolandDownButtonClick() }


            /**
             * observers
             */
            landingsDialogViewModel.toDay.observe(viewLifecycleOwner, Observer{
                toDayField.setText(it.toString())
            })
            landingsDialogViewModel.toNight.observe(viewLifecycleOwner, Observer{
                toNightField.setText(it.toString())
            })
            landingsDialogViewModel.ldgDay.observe(viewLifecycleOwner, Observer{
                ldgDayField.setText(it.toString())
            })
            landingsDialogViewModel.ldgNight.observe(viewLifecycleOwner, Observer{
                ldgNightField.setText(it.toString())
            })
            landingsDialogViewModel.autoland.observe(viewLifecycleOwner, Observer{
                autolandField.setText(it.toString())
            })

            //catch clicks on empty parts of dialog
            landingCardsWrapper.setOnClickListener {  }

            //set cancel functions
            cancelTextButton.setOnClickListener {
                landingsDialogViewModel.undo()
                closeFragment()
            }
            landingPickerBackground.setOnClickListener {
                landingsDialogViewModel.undo()
                closeFragment()
            }

            saveTextButon.setOnClickListener {
                closeFragment()
            }
        }
    }
}