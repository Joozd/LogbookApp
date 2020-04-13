package nl.joozd.logbookapp.ui.dialogs.popups

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_names.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.NamesPickerAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.longToast

class NamesDialog(): JoozdlogFragment() {
    // If this is true, we are editing PIC name so only one name allowed
    // if null or false, will return false (null check on different places)
    private val workingOnName1: Boolean
        get() = viewModel.namePickerWorkingOnName1 == true

    var names: String
        get() = if (workingOnName1) flight.name else flight.name2
        set(it) {
            flight = when(viewModel.namePickerWorkingOnName1){
                true -> flight.copy(name = it)
                false -> flight.copy(name2 = it)
                null -> error ("Trying to save $it but which name is not specified (workingOnName1 == null")
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        unchangedFlight = flight

        //
        var selectedName = ""

        return inflater.inflate(R.layout.dialog_names, container, false).apply{
            //set color of top part
            (editAircraftDialogTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN)

            // Set correct button texts if only 1 name can be selected (ie. working on PIC)
            if (workingOnName1){
                addSearchFieldNameButton.text = getString(R.string.select)
                addSelectedNameButton.text = getString(R.string.select)
                removeLastButon.text = getString(R.string.clear)
            }

            // set initial list of selected names
            selectedNames.text = names.replace(", ", "\n")

            //initialize RecyclerView
            val namesPickerAdapter = NamesPickerAdapter(viewModel.allNames) { name -> selectedName = name }  // yes this will crash if no namesworker inserted
            namesPickerList.layoutManager = LinearLayoutManager(context)
            namesPickerList.adapter = namesPickerAdapter

            //Search field changed:
            namesSearchField.onTextChanged {
                namesPickerAdapter.getNames(namesSearchField.text.toString())
            }

            //Buttons OnClickListeners:
            removeLastButon.setOnClickListener {
                if ("\n" in selectedNames.text.toString()){
                    selectedNames.text = selectedNames.text.toString().split("\n").dropLast(1).joinToString(separator="\n")
                }
                else selectedNames.text = ""
            }

            //add name in search field to list, or replace if working on name1
            addSearchFieldNameButton.setOnClickListener {
                selectedNames.text = if (workingOnName1) namesSearchField.text.toString() else listOf(selectedNames.text.toString(), namesSearchField.text.toString()).filter{ x -> x.isNotEmpty()}.joinToString(separator="\n")
                namesSearchField.setText("")
            }

            //add selected name to list, or replace if working on name1
            addSelectedNameButton.setOnClickListener {
                selectedNames.text = if (workingOnName1) selectedName else listOf(selectedNames.text.toString(), selectedName).filter{ x -> x.isNotEmpty()}.joinToString(separator="\n")
            }

            // Save/Cancel onClickListeners:
            saveTextView.setOnClickListener{
                names = selectedNames.text.toString().replace("\n",", ")
                activity?.currentFocus?.clearFocus()
                supportFragmentManager.popBackStack()
            }

            cancelTextView.setOnClickListener {
                unchangedFlight?.let { flight = it }
                viewModel.namePickerWorkingOnName1 = null
                supportFragmentManager.popBackStack()
            }


            //on cancel, revert to previous flight, set viewModel.namePickerWorkingOnName1 to null and close
            editAircraftLayout.setOnClickListener {
                unchangedFlight?.let { flight = it }
                viewModel.namePickerWorkingOnName1 = null
                supportFragmentManager.popBackStack()
            }

            //catch clicks on empty parts of this dialog
            editAircraftDialogLayout.setOnClickListener {  }
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.namePickerWorkingOnName1 == null){
            longToast("Namepicker starting without viewModel.namePickerWorkingOnName1 being set")
            supportFragmentManager.popBackStack()
        }
    }
}