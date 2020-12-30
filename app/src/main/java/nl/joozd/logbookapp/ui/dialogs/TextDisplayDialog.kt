/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogTextDisplayBinding
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

/**
 * Fragment for displaying text
 */
class TextDisplayDialog() : JoozdlogFragment() {
    constructor(title: String = "", text: String = ""): this(){
        _text = text
        _title = title
    }
    constructor(titleLiveData: LiveData<String>, textLiveData: LiveData<String>): this() {
        _titleLiveData = titleLiveData
        _textLiveData = textLiveData
    }
    constructor(titleResource: Int, textLiveData: LiveData<String>): this(){
        _titleResource = titleResource
        _textLiveData = textLiveData
    }
    //TODO add more constructors as needed


    private var _title = ""
    private var _text = ""
    private var _titleResource: Int? = null
    private var _textResource: Int? = null

    private var _titleLiveData: LiveData<String>? = null
    private var _textLiveData: LiveData<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = DialogTextDisplayBinding.bind(inflater.inflate(R.layout.dialog_text_display, container, false)).apply{

        textDialogTopHalf.joozdLogSetBackgroundColor()

        // Set title and text if constructor with ResID was used
        _titleResource?.let{
            _title = requireActivity().getString(it)
        }
        _textResource?.let{
            _text = requireActivity().getString(it)
        }

        // Get title and text if recreated
        savedInstanceState?.let{ bundle ->
            bundle.getString(TEXT_TAG)?.let {_text = it }
            bundle.getString(TITLE_TAG)?.let { _title = it }
        }
        displayTextDialogTitle.text = _title
        displayTextDialogTextview.text = _text

        _titleLiveData?.observe(requireActivity()){
            displayTextDialogTitle.text = it
        }
        _textLiveData?.observe(requireActivity()){
            displayTextDialogTextview.text = it
        }

        rootLayout.setOnClickListener {  } // catch clicks

        okButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }.root

    override fun onSaveInstanceState(outState: Bundle) {
        with(outState) {
            putString(TEXT_TAG, _text)
            putString(TITLE_TAG, _title)
        }
        super.onSaveInstanceState(outState)

    }

    companion object{
        const val TEXT_TAG = "TEXT_TAG"
        const val TITLE_TAG = "TITLE_TAG"
    }

}