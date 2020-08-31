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


import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity


/**
 * My own take on how to build an alert dialog.
 * use: JoozdlogAlertDialog(activity).show{
 *    // set buttons and texts here
 * }
 * [title] = title
 * [titleResource] = title, from recource.
 * Same goes for [message] / [messageResource]
 * [setPositiveButton] / [setNegativeButton] / [setNeutralButton] for buttons
 */
class JoozdlogAlertDialog(private val ctx: FragmentActivity): DialogFragment() {
    private var builder = AlertDialog.Builder(ctx)
    private var attachedActivity: FragmentActivity? = null

    var titleResource: Int? = null
        set (res){
            field = res
            res?.let {
                builder.setTitle(it)
            }
        }

    var title: CharSequence = ""
        set (title){
            field = title
            builder.setTitle(title)
        }

    var messageResource: Int? = null
    set(messageID){
        field = messageID
        messageID?.let {
            builder.setMessage(it)
        }
    }

    var message: CharSequence = ""
        set(msg){
            field = msg
            builder.setMessage(msg)
        }

    /**
     * @param resource: String resource for text
     * @param onClick: Onclick action
     */
    fun setPositiveButton(resource: Int, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setPositiveButton(resource, listener?.let{DialogInterface.OnClickListener(listener)})
    }

    fun setPositiveButton(text: CharSequence, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setPositiveButton(text, listener?.let{DialogInterface.OnClickListener(listener)})
    }

    fun setNegativeButton(resource: Int, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setNegativeButton(resource, listener?.let{DialogInterface.OnClickListener(listener)})
    }

    fun setNegativeButton(text: CharSequence, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setNegativeButton(text, listener?.let{DialogInterface.OnClickListener(listener)})
    }
    fun setNeutralButton(resource: Int, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setNeutralButton(resource, listener?.let{DialogInterface.OnClickListener(listener)})
    }

    fun setNeutralButton(text: CharSequence, onClick: ((dialog: DialogInterface) -> Unit)? = null){
        val listener: ((dialog: DialogInterface, id: Int) -> Unit)? = if (onClick == null) null else {dialog, _ ->
            onClick(dialog)
        }
        builder.setNeutralButton(text, listener?.let{DialogInterface.OnClickListener(listener)})
    }
    fun show(tag: String = "bla", block: JoozdlogAlertDialog.() -> Unit = {}): JoozdlogAlertDialog {
        block()
        show(ctx.supportFragmentManager, tag)
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return builder.create()
    }
}

