/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.EmptyConstraintLayoutBinding
import nl.joozd.logbookapp.extensions.activity
import nl.joozd.logbookapp.extensions.setBackgroundColor
import kotlin.math.abs

/**
 * Swipe a view left or right
 * Basically a wrapper for a ConstraintLayout
 * @param backgroundLayout the background of the swipable view. This is the part that stays put.
 *  [backgroundLayout] should have a transparent background.
 *  If [backgroundLayout] has a ConstraintView as child, this child wil be taken as swipeable by default
 */
class Swiper(val backgroundLayout: ConstraintLayout) {
    /**
     * @param activity: Activity to get LayoutInflater from. If able, provide a root view for LayoutParams
     */
    @SuppressLint("InflateParams")
    constructor(activity: Activity): this(EmptyConstraintLayoutBinding.bind(activity.layoutInflater.inflate(R.layout.empty_constraint_layout, null)).root)

    /**
     * @param root: ViewGroup for getting LayoutParams from for inflating standard (empty) background
     *              If not attached to an activity, provide an activity to get LayoutInflater from
     */
    constructor(root: ViewGroup): this(EmptyConstraintLayoutBinding.bind(root.activity!!.layoutInflater.inflate(R.layout.empty_constraint_layout, root)).root)

    /**
     * @param root: ViewGroup for getting LayoutParams from for inflating standard (empty) background
     * @param activity: Activity to get LayoutInflater from
     */
    constructor(root: ViewGroup, activity: Activity): this(EmptyConstraintLayoutBinding.bind(activity.layoutInflater.inflate(R.layout.empty_constraint_layout, root)).root)

    /***********************************************************************************************************
     * Private values
     ***********************************************************************************************************/
    private val activity
        get() = backgroundLayout.activity!!

    private val parent
        get() = backgroundLayout.parent

    private val parentView
        get() = parent as View

    private var fingerDownPosX = 0f
    private var fingerDownPosY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var touchIsClick = false
    private var fingersDown = 0

    /**
     * Only one animation running at the same time. Setting a new animation will end the previous
     */
    private var currentAnimation: ValueAnimator? = null
    set(animation){
        field?.end()
        field = animation
    }


    /***********************************************************************************************************
     * Properties
     ***********************************************************************************************************/

    var isOpen: Boolean = false
        private set


    /***********************************************************************************************************
     * Settings
     ***********************************************************************************************************/

    /**
     * The root view of the part that can be swiped left (or right)
     * If a different layout is provided it wil be attached to [backgroundLayout]
     */
    var swipeableView: ConstraintLayout? = (backgroundLayout.children.firstOrNull { it is ConstraintLayout } as ConstraintLayout?)?.apply{
        makeSwipeable()
    }
    set(sv){
        if (sv == null || sv as View in backgroundLayout.children) field = sv
        else {
            field = sv
            sv.addTo(backgroundLayout)
            sv.makeSwipeable()
        }
    }

    /**
     * How wide is 'open' when 'swiped open'
     */
    var openWidth: Float = 112.dpToPixels()

    /**
     * Percentage of [openWidth] which counts as opened
     */
    var openPercentage: Float = 0.85f

    /**
     * Animate snapping to grid
     */
    var animate: Boolean = true

    /**
     * duration of animations in ms
     */
    var animateDuration: Long = 250

    /**
     * Vertical movement required to stop swiping and allow parent to scroll
     * Setter with Int will change DP to PX
     */
    var verticalLimit: Float = 40.dpToPixels()
    fun setVerticalLimit(dp: Int){
        verticalLimit = dp.dpToPixels()
    }

    /**
     * Color background will change to when swiped left
     */
    var backgroundColorLeft: Int = 0xFFFF0000.toInt()
    fun setBackgroundColorLeft(color: Long){
        backgroundColorLeft = color.toInt()
    }

    /**
     * Close if swiped open
     */
    fun close(){
        cancelSwipe()
    }


    /***********************************************************************************************************
     * Utils
     ***********************************************************************************************************/
    /**
     * Conversion DP <-> pixels
     */
    //private fun Float.pixelsToDp() = this / backgroundLayout.resources.displayMetrics.density
    //private fun Float.dpToPixels() = this * backgroundLayout.resources.displayMetrics.density
    //private fun Int.pixelsToDp() = this.toFloat() / backgroundLayout.resources.displayMetrics.density
    private fun Int.dpToPixels() = this.toFloat() * backgroundLayout.resources.displayMetrics.density

    /**
     * Add a ConstraintLayout to another ConstraintLayout with constraints matching parent
     */
    private fun ConstraintLayout.addTo(parent: ConstraintLayout){
        parent.addView(this, 0)
        ConstraintSet().apply{
            clone(parent)
            connect(id, ConstraintSet.START, parent.id, ConstraintSet.START, 0)
            connect(id, ConstraintSet.END, parent.id, ConstraintSet.END, 0)
            connect(id, ConstraintSet.TOP, parent.id, ConstraintSet.TOP, 0)
            connect(id, ConstraintSet.BOTTOM, parent.id, ConstraintSet.BOTTOM, 0)
            applyTo(parent)
        }
    }

    private fun ConstraintLayout.makeSwipeable() {
        setOnTouchListener { _, event ->
            parent.requestDisallowInterceptTouchEvent(true) // disallow parent to scroll or detect clicks
            when (event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchIsClick = true
                    fingerDownPosX = event.rawX
                    fingerDownPosY = event.rawY
                    previousX = fingerDownPosX
                    previousY = fingerDownPosY

                    // Log.d(TAG,"finger down")
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    //get direction of movement:
                    val offsetX = event.rawX - previousX
                    val offsetY = event.rawY - previousY
                    previousX = event.rawX
                    previousY = event.rawY

                    // check if movement is in swiping or scrolling direction
                    if (touchIsClick){ // only finger down detected and no movement yet
                        if (abs(offsetX / 3) > abs(offsetY.toDouble())) {
                            touchIsClick = false
                        }
                        else parent.requestDisallowInterceptTouchEvent(false) // allow parent to scroll
                        return@setOnTouchListener true
                    }
                    else {
                        if (abs(event.rawY-fingerDownPosY) > verticalLimit && abs(offsetY / 2) > abs(offsetX)) cancelSwipe()
                        else {
                            // Log.d(TAG, "translationX is $translationX")
                            moveX(offsetX)
                        }
                    }
                    false
                }

                MotionEvent.ACTION_UP -> {
                    when {
                        touchIsClick -> {
                            if (isOpen) cancelSwipe()
                            else performClick()
                            true
                        }
                        translationX < openWidth * openPercentage * -1 -> {
                            if (animate) {
                                currentAnimation = ValueAnimator.ofInt((translationX + openWidth).toInt(), 0).apply {
                                    isOpen = true
                                    addUpdateListener {
                                        setTX(1.0f * it.animatedValue as Int - openWidth)
                                    }
                                    duration = 250
                                    start()
                                }
                            }
                            else translationX = openWidth
                            true
                        }
                        else -> {
                            cancelSwipe()
                            true
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    // Log.d(TAG,"CANCELLED")
                    fingersDown = 0
                    cancelSwipe()
                    false
                }

                else -> onTouchEvent(event)
            }
        }
    }

    private fun cancelSwipe(animationAllowed: Boolean = true) = swipeableView?.let{ v ->
        parent?.requestDisallowInterceptTouchEvent(false)
        if (animate && animationAllowed) {
            currentAnimation = ValueAnimator.ofInt((v.translationX).toInt(), 0).apply{
                addUpdateListener {
                    v.translationX = 1.0f * it.animatedValue as Int
                    val newAlpha = ((1 - (openWidth + v.translationX) / openWidth) * 255).toInt()
                    backgroundLayout.background.alpha = newAlpha
                }
                duration = animateDuration
                start()
            }
        }
        else {
            v.translationX = 0f
            backgroundLayout.background.alpha = 0
        }
    }.also{
        isOpen = false
    }

    /**
     * Move [swipeableView] left or right
     * TODO: right now it can only move left
     */
    private fun moveX(offsetX: Float){
        swipeableView?.let { v ->
            v.translationX = when{
                v.translationX == 0f -> if (offsetX < 0) v.translationX + offsetX else 0f
                (v.translationX + offsetX) <= 0 -> v.translationX + offsetX
                else -> 0f
            }
            when {
                v.translationX < 0 && v.translationX >= -openWidth -> {
                    backgroundLayout.setBackgroundColor(0xFFFF0000)
                    val newAlpha = ((1 - (openWidth + v.translationX) / openWidth) * 255).toInt()
                    backgroundLayout.background.alpha = newAlpha
                }

                v.translationX < -openWidth ->
                    backgroundLayout.setBackgroundColor(0xFFFF0000)

                v.translationX == 0f -> backgroundLayout.setBackgroundColor(0x00000000)

                else -> backgroundLayout.background.alpha = 0
            }
        }
    }

    private fun setTX(tx: Float){
        swipeableView?.let { v ->
            v.translationX = tx
            when {
                v.translationX < 0 && v.translationX >= -openWidth -> {
                    backgroundLayout.setBackgroundColor(0xFFFF0000)
                    val newAlpha = ((1 - (openWidth + v.translationX) / openWidth) * 255).toInt()
                    backgroundLayout.background.alpha = newAlpha
                }

                v.translationX < -openWidth ->
                    backgroundLayout.setBackgroundColor(0xFFFF0000)

                v.translationX == 0f -> backgroundLayout.setBackgroundColor(0x00000000)

                else -> backgroundLayout.background.alpha = 0
            }
        }
    }

    /**
     * If a view is reused and made into a Swiper again, close it first
     */
    init{
        cancelSwipe(false)
    }



}