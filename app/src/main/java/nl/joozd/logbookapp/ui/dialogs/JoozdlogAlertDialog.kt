/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogJoozdlogFourButtonBinding
import nl.joozd.logbookapp.databinding.DialogJoozdlogTwoButtonBinding


/**
 * My own take on how to build an alert dialog.
 * use: JoozdlogAlertDialog().show(activity){
 *    // set buttons and texts here
 * }
 * [title] = title
 * [titleResource] = title, from recource.
 * Same goes for [message] / [messageResource]
 * [setPositiveButton] / [setNegativeButton] / [setNeutralButton] for buttons
 */
class JoozdlogAlertDialog: Fragment() {
    private val ctx: Context
        get() = requireActivity()

    private val supportFragmentManager: FragmentManager
        get() = requireActivity().supportFragmentManager

    var title: CharSequence? = null
    var message: CharSequence = "Dialog."

    var titleResource: Int? = null

    var messageResource: Int? = null

    fun title() = titleResource?.let {ctx.getString(it)} ?: title
    fun message() = messageResource?.let {ctx.getString(it)} ?: message

    private var _positiveButton: ButtonDescriptor? = null
    private var _negativeButton: ButtonDescriptor? = null
    private var _neutralButton: ButtonDescriptor? = null
    private var _bonusButton: ButtonDescriptor? = null



    /**
     * @param resource: String resource for text
     * @param onClick: Onclick action
     */
    fun setPositiveButton(resource: Int,onClick: View.OnClickListener? = null){
        _positiveButton = ButtonDescriptor(null, resource, onClick)
    }

    fun setPositiveButton(text: CharSequence, onClick: View.OnClickListener? = null){
        _positiveButton = ButtonDescriptor(text, null, onClick)
    }

    fun setNegativeButton(resource: Int,onClick: View.OnClickListener? = null){
        _negativeButton = ButtonDescriptor(null, resource, onClick)
    }

    fun setNegativeButton(text: CharSequence, onClick: View.OnClickListener? = null){
        _negativeButton = ButtonDescriptor(text, null, onClick)
    }

    fun setNeutralButton(resource: Int,onClick: View.OnClickListener? = null){
        _neutralButton = ButtonDescriptor(null, resource, onClick)
    }

    fun setNeutralButton(text: CharSequence, onClick: View.OnClickListener? = null){
        _neutralButton = ButtonDescriptor(text, null, onClick)
    }

    fun setBonusButton(resource: Int,onClick: View.OnClickListener? = null){
        _bonusButton = ButtonDescriptor(null, resource, onClick)
    }

    fun setBonusButton(text: CharSequence, onClick: View.OnClickListener? = null){
        _bonusButton = ButtonDescriptor(text, null, onClick)
    }

    fun show(context: FragmentActivity, tag: String = "bla", block: JoozdlogAlertDialog.() -> Unit = {}): JoozdlogAlertDialog {
        this.block()

        context.supportFragmentManager.commit{
            add(android.R.id.content,this@JoozdlogAlertDialog, JOOZDLOG_DIALOG_TAG)
        }
        return this
    }

    fun dismiss(){
        closeFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        getCorrectBinding(inflater, container).root


    private fun getCorrectBinding(inflater: LayoutInflater, container: ViewGroup?) = if (_neutralButton == null && _bonusButton == null)
        DialogJoozdlogTwoButtonBinding.bind(inflater.inflate(R.layout.dialog_joozdlog_two_button, container, false) ).apply{
            var forceOkButton = false
                title()?.let { dialogTitle.text = title() } ?: run {
                    dialogTitle.visibility = View.GONE
                }

                dialogMessage.text = message()

                /**
                 * Make sure at least an "ok" button is shown
                 */
                if (listOf(_positiveButton, _negativeButton, _bonusButton, _neutralButton).all{it == null}){
                    positiveButton.setText(android.R.string.ok)
                    positiveButton.setOnClickListener { removeFragment() }
                    forceOkButton = true
                }

                _positiveButton?.let{ bd ->
                    positiveButton.text = bd.text
                    positiveButton.setOnClickListener { v ->
                        bd.onClick?.onClick(v)
                        removeFragment()
                    }
                }

                _negativeButton?.let{bd ->
                    negativeButton.text = bd.text
                    negativeButton.setOnClickListener { v ->
                        bd.onClick?.onClick(v)
                        removeFragment()
                    }
                }

                if (_positiveButton == null && !forceOkButton) positiveButton.visibility = View.GONE
                if (_negativeButton == null) negativeButton.visibility = View.GONE
        }
            else
        DialogJoozdlogFourButtonBinding.bind(inflater.inflate(R.layout.dialog_joozdlog_four_button, container, false) ).apply{
            var forceOkButton = false
            title()?.let { dialogTitle.text = title() } ?: run {
                dialogTitle.visibility = View.GONE
            }

            dialogMessage.text = message()

            /**
             * Make sure at least an "ok" button is shown
             */
            if (listOf(_positiveButton, _negativeButton, _bonusButton, _neutralButton).all{it == null}){
                positiveButton.setText(android.R.string.ok)
                positiveButton.setOnClickListener { removeFragment() }
                forceOkButton = true
            }

            _positiveButton?.let{ bd ->
                positiveButton.text = bd.text
                positiveButton.setOnClickListener { v ->
                    bd.onClick?.onClick(v)
                    removeFragment()
                }
            }

            _negativeButton?.let{bd ->
                negativeButton.text = bd.text
                negativeButton.setOnClickListener { v ->
                    bd.onClick?.onClick(v)
                    removeFragment()
                }
            }
            _neutralButton?.let{bd ->
                neutralButton.text = bd.text
                neutralButton.setOnClickListener { v ->
                    bd.onClick?.onClick(v)
                    removeFragment()
                }
            }
            _bonusButton?.let{ bd ->
                bonusButton.text = bd.text
                bonusButton.setOnClickListener { v ->
                    bd.onClick?.onClick(v)
                    removeFragment()
                }
            }

            if (_positiveButton == null && !forceOkButton) positiveButton.visibility = View.GONE
            if (_negativeButton == null) negativeButton.visibility = View.GONE
            if (_neutralButton == null && _bonusButton != null) neutralButton.visibility = View.GONE
            if (_bonusButton == null && _neutralButton != null) bonusButton.visibility = View.GONE
        }


    private fun removeFragment() = supportFragmentManager.commit { remove(this@JoozdlogAlertDialog) }

    private fun closeFragment() = supportFragmentManager.popBackStack()


    private inner class ButtonDescriptor(val t: CharSequence?, val r: Int?, val onClick: View.OnClickListener?){
        val text: CharSequence
            get() = r?.let{ ctx.getString(it) } ?: t ?: ""
    }

    companion object{
        const val JOOZDLOG_DIALOG_TAG = "JOOZDLOG_DIALOG_TAG"
    }

}

