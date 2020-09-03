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

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.PopupTextLayoutBinding

/**
 * Pops up a text window anchored on a view.
 * Needs an anchor, even when not used as such
 * It can use all the functions of a PopupWindow, but it needs to do [initialize] first
 * which is already incorporated in [showAsDropDown]
 */
open class PopupText(protected var anchor: View): PopupWindow() {
    constructor(anchor: View, message: String): this(anchor){
        text = message
    }

    constructor(anchor: View, messageResource: Int): this(anchor){
        textRes = messageResource
    }
    /**********************************************************************************************
     * Protected parts
     **********************************************************************************************/

    protected var presetText: CharSequence? = null
    protected var presetTextRes: Int? = null

    protected val view: ViewGroup?
        get() = binding?.root

    protected val binding: PopupTextLayoutBinding?
        get() = if (anchor.isAttachedToWindow) _binding else null


    /**
     * When [binding] is created, also set other params
     */
    private val _binding: PopupTextLayoutBinding? by lazy{
        inflater?.let { i ->
            PopupTextLayoutBinding.bind(i.inflate(R.layout.popup_text_layout,(anchor.parent as ViewGroup))).apply {
                root.setOnClickListener { onClick()}

                // Set text to preset text
                popupWindowTextView.text = presetText ?: presetTextRes?.let {root.context.getString(it) } ?: ""
            }.also {
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                isFocusable = true
            }
        }
    }





    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/

    /**
     * Check requirements
     */
    init {
        require (anchor.parent is ViewGroup) {"Anchor cannot be root view!" }
    }

    private val inflater: LayoutInflater?
        get() = try {
            LayoutInflater.from(anchor.context)
        } catch(e: AssertionError){
            Log.w("PopupText", "Could not get layoutInflater")
            null
        }



    /**********************************************************************************************
     * Public parts
     **********************************************************************************************/

    /**
     * Set text if binding is already made,
     */
    var text: CharSequence?
        get() = binding?.let {
            binding?.popupWindowTextView?.text
        } ?: presetText

        set(text) {
            binding?.let {
                binding?.popupWindowTextView?.text = text
            } ?: run { presetText = text }
        }

    var textRes: Int?
        get() = presetTextRes
        set(res) {
            binding?.let{
                text = res?.let { r -> it.root.context.getString(r) }
            } ?: run {presetTextRes = res}

        }

    /**
     *
     */
    fun initialize(){
        contentView = binding?.root
    }

    open fun onClick() = dismiss()

    open fun showAsDropDown(){
        initialize()
        showAsDropDown(null)
    }

    override fun showAsDropDown(newAnchor: View?) {
        anchor?.let{a ->
            anchor = a
        }
        initialize()
        super.showAsDropDown(anchor)
    }

}