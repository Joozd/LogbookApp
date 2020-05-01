package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_names.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.MainViewModel
import nl.joozd.logbookapp.model.viewmodels.dialogs.NamesDialogViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

//TODO if only one name selected in recyclerView, set that as active name if OK pressed
class NamesDialog(): JoozdlogFragment() {
    // If this is true, we are editing PIC name so only one name allowed
    // if null or false, will return false (null check on different places)
    private val mainViewModel: MainViewModel by activityViewModels()
    private val namesDialogViewModel: NamesDialogViewModel by viewModels()

    /*
    var names: String
        get() = if (workingOnName1) flight.name.trim() else flight.name2.trim()
        set(it) {
            flight = when(viewModel.namePickerWorkingOnName1){
                true -> flight.copy(name = it)
                false -> flight.copy(name2 = it)
                null -> error ("Trying to save $it but which name is not specified (workingOnName1 == null")
            }
        }

    private val currentNames: List<String>
        get() = if (workingOnName1) listOf(flight.name) else flight.name2.split(',').map{it.trim()}
    */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_names, container, false).apply{
            //set color of top part
            namesDialogTopHalf.joozdLogSetBackgroundColor()

            //initialize RecyclerView
            val namesPickerAdapter = SelectableStringAdapter() { name ->
                namesDialogViewModel.selectName(name)
            }.also {
                namesPickerList.layoutManager = LinearLayoutManager(context)
                namesPickerList.adapter = it
            }

            //Search field changed:
            namesSearchField.onTextChanged {
                namesDialogViewModel.searchNames(it)
            }

            //Buttons OnClickListeners:
            removeLastButon.setOnClickListener {
                namesDialogViewModel.removeLastClicked()

            }

            //add name in search field to list, or replace if working on name1
            addSearchFieldNameButton.setOnClickListener {
                namesDialogViewModel.addManualNameClicked()
            }

            //add selected name to list, or replace if working on name1
            addSelectedNameButton.setOnClickListener {
                namesDialogViewModel.addSelectedName()

            }

            // Save/Cancel onClickListeners:
            saveTextView.setOnClickListener{
                mainViewModel.namePickerWorkingOnName1 = null
                closeFragment()
            }

            //on cancel, revert to previous flight, set viewModel.namePickerWorkingOnName1 to null and close
            cancelTextView.setOnClickListener {
                mainViewModel.namePickerWorkingOnName1 = null
                namesDialogViewModel.undo()
                closeFragment()
            }
            editAircraftLayout.setOnClickListener {
                mainViewModel.namePickerWorkingOnName1 = null
                namesDialogViewModel.undo()
                closeFragment()
            }

            //catch clicks on empty parts of this dialog
            editAircraftDialogLayout.setOnClickListener {  }

            /**
             * observers:
             */

            namesDialogViewModel.addSearchFieldNameButtonTextResource.observe(viewLifecycleOwner, Observer{
                addSearchFieldNameButton.text = getString(it)
            })
            namesDialogViewModel.addSelectedNameButtonTextResource.observe(viewLifecycleOwner, Observer{
                addSelectedNameButton.text = getString(it)
            })
            namesDialogViewModel.removeLastButonTextResource.observe(viewLifecycleOwner, Observer{
                removeLastButon.text = getString(it)
            })

        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        namesDialogViewModel.workingOnName1 = mainViewModel.namePickerWorkingOnName1
    }
}