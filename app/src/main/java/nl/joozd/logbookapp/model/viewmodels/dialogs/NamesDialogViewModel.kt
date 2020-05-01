package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.helpers.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class NamesDialogViewModel: JoozdlogDialogViewModel() {
    /**
     * this MUST be set in onActivityCreated in Fragment so feedback event will be observed
     * also, feedbackEvent must be observed. If [won1] == null, things won't work.
     */
    var workingOnName1: Boolean? = null
    set(name1used){
        field = name1used
        when (name1used){
            true -> layoutForName1()
            false -> layoutForName2()
            null -> feedback(FeedbackEvents.NamesDialogEvents.NAME1_OR_NAME2_NOT_SELECTED)
        }
    }

    //Texts for UI elements depending on which name is being edited
    private val _addSearchFieldNameButtonTextResource = MutableLiveData<Int>() // getString(R.string.select)
    val addSearchFieldNameButtonTextResource: LiveData<Int>
        get() = _addSearchFieldNameButtonTextResource
    private val _addSelectedNameButtonTextResource = MutableLiveData<Int>() //getString(R.string.select)
    val addSelectedNameButtonTextResource: LiveData<Int>
        get() = _addSelectedNameButtonTextResource
    private val _removeLastButonTextResource = MutableLiveData<Int>() //getString(R.string.clear)
    val removeLastButonTextResource: LiveData<Int>
        get() = _removeLastButonTextResource

    private val _selectedName = MutableLiveData<String>()
    val selectedName: LiveData<String>
        get() = _selectedName
    fun selectName(name: String){
        _selectedName.value = name
    }
    fun addSelectedName(){
        //TODO do the whole magic stuff
        feedback(FeedbackEvents.NamesDialogEvents.NOT_IMPLEMENTED)
    }

    var manualName: String = ""
    fun searchNames(query: String){
        manualName = query
        //TODO build this
        feedback(FeedbackEvents.NamesDialogEvents.NOT_IMPLEMENTED)
    }
    fun addManualNameClicked(){
        //TODO build this
        feedback(FeedbackEvents.NamesDialogEvents.NOT_IMPLEMENTED)
    }

    fun removeLastClicked(){
        //TODO build this
        feedback(FeedbackEvents.NamesDialogEvents.NOT_IMPLEMENTED)
    }

    private fun layoutForName1(){
        _addSearchFieldNameButtonTextResource.value = R.string.select
        _addSelectedNameButtonTextResource.value = R.string.select
        _removeLastButonTextResource.value = R.string.clear
    }
    private fun layoutForName2(){
        _addSearchFieldNameButtonTextResource.value = R.string.addThis
        _addSelectedNameButtonTextResource.value = R.string.addThis
        _removeLastButonTextResource.value = R.string.remove
    }
}