package nl.joozd.logbookapp.ui.dialogs

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_names.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.NamesPickerAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.longToast

//TODO if only one name selected in recyclerView, set that as active name if OK pressed
class NamesDialog(): JoozdlogFragment() {
    // If this is true, we are editing PIC name so only one name allowed
    // if null or false, will return false (null check on different places)
    private val workingOnName1: Boolean
        get() = viewModel.namePickerWorkingOnName1 == true

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var selectedName = ""

        return inflater.inflate(R.layout.dialog_names, container, false).apply{
            //set color of top part
            (namesDialogTopHalf.background as GradientDrawable).colorFilter =
                PorterDuffColorFilter(
                    requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                    PorterDuff.Mode.SRC_IN
                )

            // Set correct button texts if only 1 name can be selected (ie. working on PIC)
            if (workingOnName1){
                addSearchFieldNameButton.text = getString(R.string.select)
                addSelectedNameButton.text = getString(R.string.select)
                removeLastButon.text = getString(R.string.clear)
            }

            // set initial list of selected names
            setViews(this)

            //initialize RecyclerView
            val namesPickerAdapter = NamesPickerAdapter(viewModel.allNames.filter{it !in currentNames}) { name -> selectedName = name }
            namesPickerList.layoutManager = LinearLayoutManager(context)
            namesPickerList.adapter = namesPickerAdapter

            //Search field changed:
            namesSearchField.onTextChanged {
                namesPickerAdapter.getNames(namesSearchField.text.toString())
            }

            //Buttons OnClickListeners:
            removeLastButon.setOnClickListener {
                names = currentNames.dropLast(1).joinToString(",")
                namesPickerAdapter.updateAllNames(viewModel.allNames.filter{it !in currentNames})
            }

            //add name in search field to list, or replace if working on name1
            addSearchFieldNameButton.setOnClickListener {
                names =
                    if (workingOnName1) namesSearchField.text.toString()
                    else (currentNames + listOf(namesSearchField.text.toString())).filter{it.isNotEmpty()}.distinct().joinToString(","){it.trim()}
                namesPickerAdapter.updateAllNames(viewModel.allNames.filter{it !in currentNames})
            }

            //add selected name to list, or replace if working on name1
            addSelectedNameButton.setOnClickListener {
                if (selectedName.isNotEmpty())
                    names =
                        if (workingOnName1) selectedName
                        else (currentNames + listOf(selectedName)).filter{it.isNotEmpty()}.distinct().joinToString(","){it.trim()}
                namesPickerAdapter.updateAllNames(viewModel.allNames.filter{it !in currentNames})
                Log.d("NamesDialog", "adapter has ${namesPickerAdapter.allNames.size} names")

            }

            // Save/Cancel onClickListeners:
            saveTextView.setOnClickListener{
                Log.d(this::class.simpleName, "current names: $names")
                viewModel.namePickerWorkingOnName1 = null
                supportFragmentManager.popBackStack()
            }

            //on cancel, revert to previous flight, set viewModel.namePickerWorkingOnName1 to null and close
            cancelTextView.setOnClickListener {
                unchangedFlight?.let { flight = it }
                viewModel.namePickerWorkingOnName1 = null
                supportFragmentManager.popBackStack()
            }
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

    override fun setViews(v: View?) {
        v?.selectedNames?.text = names.split(',').joinToString("\n") { it.trim() }
        //DEBUG
        Log.d("NamesDialog", "currentNames = $currentNames")
    }
}