/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.utils.customs

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.item_custom_autocomplete.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.onTextChanged
import java.util.*
import kotlin.NoSuchElementException

/**
 * ISSUES:
 * -Doesnt close itself onFocusChanged if onFocusChangeListener is overwritten after connecting
 */


/**
 * Constructor:
 * @param items: the list of values to be checked against entered data in the connected EditText
 * @param defaultItems: The list of items to be shown when EditText is empty
 * @param boxLayout: Layout XML of the box view containing the list. Needs to be a LinearLayout.
 * @param itemLayout: Layout XML of the items in the list. Needs to be a LinearLayout.
 */

open class CustomAutoComplete (var items: List<String> = emptyList(), var defaultItems: List<String> = emptyList(), var boxLayout: Int = R.layout.box_custom_autocomplete, var itemLayout: Int = R.layout.item_custom_autocomplete){
    companion object{
        const val TAG = "CustomAutoComplete"
    }

    /**
     * maxItems is the maximum length of the popup list
     * If there are more, an ellipse will show as item # [maxItems]+1
     * If set to / left at 0 it will make the list as long as it can find items
     */
    var maxItems = 0

    /**
     * If set to false, will not show ellipsis after [maxItems] items
     */
    var showEllipsis = true

    /**
     * difference between left edge of [EditText] and left edge of list
     */
    var leftMargin = 50

    /**
     * difference between right edge of [EditText] and left edge of list
     * Negative value means list ends before [EditText]
     */
    var rightMargin = 50

    /**
     * Vertical space between [EditText] and top of list
     */
    var verticalMargin = 10






    private var editText: EditText? = null
    private var parentLayout: ConstraintLayout? = null
    private var rootLayout: ViewGroup? = null
    private var viewToAlignTo: View? = null
    private var valueInserted = false
    // can connect only once. Don't reuse for multiple views, make a new one.
    private var connected = false
    private var box: LinearLayout? = null

    //flag to see if we are done drawing


    /**
     * Connects the [CustomAutoComplete] to the [EditText]
     * Also looks for a ConstraintLayout to constrain the view.
     * Maybe later I'll make it with an absolute position in case of not ConstraintLayout
     * Maybe not. Who knows?
     */
    fun connectToEditText(et: EditText): CustomAutoComplete {
        require(!connected)
        connected = true
        Log.d(TAG, "Connecting to EditText $et")

        /**
         * KNOWN ISSUE: This will be gone if listener is changed after connecting
         */
        val oldOnFocusChangeListener = et.onFocusChangeListener
        et.setOnFocusChangeListener { v, hasFocus ->
            valueInserted = true
            oldOnFocusChangeListener.onFocusChange(v, hasFocus)
            if (!hasFocus)  removeList()
            valueInserted = false
        }

        et.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener{
            override fun onViewDetachedFromWindow(v: View?) { removeList() }
            override fun onViewAttachedToWindow(v: View?) {
                // do nothing
            }
        })

        rootLayout = findTopViewGroup(et)
        Log.d(TAG, "rootLayout = $rootLayout")
        //keep going up to find a constraintLayout
        var prnt = et.parent
        viewToAlignTo = et
        while (prnt !is ConstraintLayout && prnt != null) {
            prnt = prnt.parent

        }
        // throw error if no constraintlayout found
        if (prnt !is ConstraintLayout) throw (NoSuchElementException("No constraintlayout found"))
        parentLayout = prnt

        val activity = (if (prnt.context is Activity) prnt.context as Activity else null)
            ?: throw (Exception("Could not find activity from $et"))

        editText = et
        Log.d(TAG, "Set editText to $editText")
        box = activity.layoutInflater.inflate(boxLayout, parentLayout, false) as LinearLayout
        box?.id = View.generateViewId()

        rootLayout?.let { rootLayout ->
            rootLayout.addView(box)
            viewToAlignTo?.let { viewToAlignTo ->
                editText?.onTextChanged { text ->
                    box?.removeAllViews()

                    if (editText?.hasFocus() == true) { // only put stuff in box if editText has focus
                        //find position to place list
                        val topLeftPosition = IntArray(2)
                        val rootTopLeftPosition = IntArray(2)
                        viewToAlignTo.getLocationInWindow(topLeftPosition)
                        rootLayout.getLocationOnScreen(rootTopLeftPosition)

                        // get positions relative to root layout's position
                        val top =
                            topLeftPosition[1] + viewToAlignTo.height + verticalMargin - rootTopLeftPosition[0]
                        val left = topLeftPosition[0] + leftMargin - rootTopLeftPosition[1]
                        val right =
                            topLeftPosition[1] + viewToAlignTo.width + rightMargin - rootTopLeftPosition[1]

                        //val layoutParams = box?.layoutParams
                        val layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply{
                            setMargins(left, top, 0,0)
                        }
                        //if (layoutParams !is ViewGroup.MarginLayoutParams) return@onTextChanged // or throw an exception or whatever
                        Log.d(TAG, "top: $top")
                        Log.d(TAG, "left: $left")
                        //layoutParams.leftMargin = left
                        layoutParams.marginStart = left
                        layoutParams.topMargin = top
                        box?.layoutParams = layoutParams // TODO is this necessary? check!
                        Log.d(TAG, "$layoutParams")
//                        box?.translationX = left.toFloat()
//                        box?.translationY = top.toFloat()

                        val foundItems = if (text.isNotEmpty()) filter(text) else defaultItems

                        for (counter in foundItems.indices) {
                            // check for maxItems
                            if (counter >= maxItems && maxItems != 0) {
                                if (showEllipsis) {
                                    val listItem = activity.layoutInflater.inflate(
                                        itemLayout,
                                        box,
                                        false
                                    ) as LinearLayout
                                    listItem.textView.text = "${Typography.ellipsis}"
                                    listItem.id = View.generateViewId()
                                    box?.addView(listItem)
                                }
                                break
                            }
                            val it = foundItems[counter]


                            val listItem = activity.layoutInflater.inflate(
                                itemLayout,
                                box,
                                false
                            ) as LinearLayout
                            listItem.textView.text = it
                            listItem.id = View.generateViewId()

                            listItem.setOnClickListener { _ ->
                                //enclosed in "valueInserted" to make sure list closes on click and will open again on changing text
                                valueInserted = true
                                listItemClicked(it)
                                if (editText?.nextFocusForwardId is Int) {
                                    val nextField =
                                        activity.findViewById<View>(editText!!.nextFocusForwardId)
                                    nextField.requestFocus()
                                }
                                valueInserted = false
                            }

                            box?.addView(listItem)


                        }
                    }
                }
                viewToAlignTo.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    if (box?.childCount ?: 0 > 0){
                        val topLeftPosition = IntArray(2)
                        val rootTopLeftPosition = IntArray(2)
                        viewToAlignTo.getLocationInWindow(topLeftPosition)
                        rootLayout.getLocationOnScreen(rootTopLeftPosition)

                        // get positions relative to root layout's position
                        val top =
                            topLeftPosition[1] + viewToAlignTo.height + verticalMargin - rootTopLeftPosition[0]
                        val left = topLeftPosition[0] + leftMargin - rootTopLeftPosition[1]
                        val right =
                            topLeftPosition[1] + viewToAlignTo.width + rightMargin - rootTopLeftPosition[1]

                        val layoutParams = box?.layoutParams
                        if (layoutParams !is ViewGroup.MarginLayoutParams) return@addOnLayoutChangeListener // or throw an exception or whatever
                        Log.d(TAG, "top: $top")
                        Log.d(TAG, "left: $left")
                        layoutParams.leftMargin = left
                        layoutParams.topMargin = top
                        box?.translationX = left.toFloat()
                        box?.translationY = top.toFloat() - 24.dpToPixels()
                        box?.layoutParams = layoutParams // TODO is this necessary? check!
                    }
                }
            }
        }
        return this
    }

    open fun filter(query: String): List<String> = items.filter {query.toUpperCase(Locale.ROOT) in it.toUpperCase(Locale.ROOT)}

    /**
     * Will insert the value into the view.
     * Can be overridden.
     * Value: The clicked value passed from the string
     * view: The EditText that will recieve the text
     */
    open fun insert(value: String, view: EditText?){

        view?.setText(value)
    }

    /**
     * What will happen if a list item is clicked
     * value: the value to be entered into the field (ie passed to CustomAutoComplete.insert())
     */
    open fun listItemClicked(value: String){
        insert(value, editText)
    }

    fun removeList(){
        box?.removeAllViews()
    }

    private fun findTopViewGroup(v: View): ViewGroup?{

        var p = v.parent
        while (p.parent is ViewGroup) p = p.parent
        return if (p is ViewGroup) p else null
    }

    private fun Int.dpToPixels() = this.toFloat() * (editText?.resources?.displayMetrics?.density?: 0.toFloat())

}