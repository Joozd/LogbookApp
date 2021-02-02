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

package nl.joozd.logbookapp.ui.utils.customs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ViewSnackbarCustomBinding
import nl.joozd.logbookapp.extensions.findSuitableParent

class CustomSnackbar(
    parent: ViewGroup,
    content: CustomSnackbarView
) : BaseTransientBottomBar<CustomSnackbar>(parent, content, content){
    val binding = ViewSnackbarCustomBinding.bind(view)

    init {
        view.setBackgroundColor(ContextCompat.getColor(parent.context, android.R.color.transparent))
        view.setPadding(0, 0, 0, 0)
        duration = 1000*5
    }
    fun setMessage(text: String): CustomSnackbar {
        binding.message.text = text
        return this
    }

    fun setMessage(resource: Int): CustomSnackbar {
        binding.message.setText(resource)
        return this
    }

    fun setActionText(text: String) {
        binding.undoTextView.text=text
    }

    fun setOnAction(f: (Any)->Unit ) {
        binding.undoTextView.setOnClickListener(f)
    }

    fun setOnActionBarShown(f:() -> Unit){
        mCustomSnackBarView?.onBarShown = CustomSnackbarView.OnBarShown(f)
    }

    fun setOnActionBarGone(f:() -> Unit){
        mCustomSnackBarView?.onBarGone = CustomSnackbarView.OnBarGone(f)
    }


    companion object {
        private var mCustomSnackBarView: CustomSnackbarView? = null

        fun make(view: View): CustomSnackbar {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            // We inflate our custom view
            val customView = LayoutInflater.from(view.context).inflate(
                R.layout.layout_snackbar_custom,
                parent,
                false
            ) as CustomSnackbarView
            mCustomSnackBarView = customView

            // We create and return our Snackbar
            return CustomSnackbar(
                parent,
                customView
            )
        }

    }

}