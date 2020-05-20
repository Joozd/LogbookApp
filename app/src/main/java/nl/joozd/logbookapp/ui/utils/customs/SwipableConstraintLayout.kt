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

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.extensions.setBackgroundColor
import kotlin.math.abs


class SwipableConstraintLayout(ctx: Context, attributes: AttributeSet): ConstraintLayout(ctx, attributes) {
    companion object{
        private const val TAG = "SwipableConstraintLayout"
    }

    // Converts 14 dip into its equivalent px
    val openingWidth = 112f.dpToPixels()
    val snapBackWidth = openingWidth * 2.5

    private var swiping = false
    private lateinit var parentViewHolder: RecyclerView
    private lateinit var parentView: View
    private var fingerDownPosX = 0f
    private var fingerDownPosY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var opened = false
    private var closing = false

    override fun onAttachedToWindow() {
        parentViewHolder = parent.parent as RecyclerView
        parentView = parent as View
        super.onAttachedToWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fingerDownPosX = event.rawX
                fingerDownPosY = event.rawY
                previousX = fingerDownPosX
                previousY = fingerDownPosY

                // Log.d(TAG,"finger down")
                true
            }
            MotionEvent.ACTION_UP -> {
                // Log.d(TAG, "finger UP")
                if (!swiping && event.historySize == 0) performClick()
                else {
                    if (opened){
                        cancelSwipe()
                        true
                    }

                    else {
                        if (translationX < openingWidth * -0.85) {
                            val animator =
                                ValueAnimator.ofInt((translationX + openingWidth).toInt(), 0)
                            animator.addUpdateListener {
                                translationX = 1.0f * it.animatedValue as Int - openingWidth
                                if (translationX >= openingWidth*-2) opened = true
                            }
                            animator.apply {
                                duration = 250
                                start()
                            }
                        } else {
                            opened = false
                            swiping = false
                            parent.requestDisallowInterceptTouchEvent(false)
                            val animator = ValueAnimator.ofInt((translationX).toInt(), 0)
                            animator.addUpdateListener {
                                translationX = 1.0f * it.animatedValue as Int
                            }
                            animator.apply {
                                duration = 250
                                start()
                            }
                        }
                        true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.historySize > 0) {
                    val offsetX = event.rawX - previousX
                    val offsetY = event.rawY - previousY
                    previousX = event.rawX
                    previousY = event.rawY
                    if (!swiping) {
                        if (abs(offsetX / 3) > abs(offsetY.toDouble())) {
                            swiping = true
                        }
                        else parent.requestDisallowInterceptTouchEvent(false)
                    }
                    if (abs(event.rawY-fingerDownPosY) > 40.dpToPixels() && abs(offsetY / 2) > abs(offsetX) && swiping) cancelSwipe()
                    if (swiping){
                        // Log.d(TAG, "translationX is $translationX")
                        if (translationX == 0f){
                            translationX = if (offsetX < 0) translationX + offsetX else 0f
                        }
                        else{
                            if (offsetX + translationX <= 0) translationX += offsetX
                            else translationX = 0f
                        }

                    }

                    // Log.d(TAG, "offsetX is $offsetX, offsetY is $offsetY")
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                // Log.d(TAG,"CANCELLED")
                swiping = false
                true
            }
            else -> {
                // Log.d(TAG, "event is ${event?.actionMasked}")
                super.onTouchEvent(event)
            }
        }
    }


    /*******************
     * Override setTranslationX to change color for background to make nice animations when
     * opening/closing that work with what youn will get
     */
    override fun setTranslationX(translationX: Float) {
        when {
            translationX < 0 && translationX >= -openingWidth -> {
                parentView.setBackgroundColor(0xFFFF0000)
                val newAlpha = ((1 - (openingWidth + translationX) / openingWidth) * 255).toInt()
                parentView.background.alpha = newAlpha
            }

            translationX < -openingWidth ->
                    parentView.setBackgroundColor(0xFFFF0000)

            translationX == 0f -> parentView.setBackgroundColor(0x00000000)

            else ->  parentView.background.alpha = 0


        }
        super.setTranslationX(translationX)
    }

    private fun cancelSwipe(animate: Boolean = true){
        parent.requestDisallowInterceptTouchEvent(false)
        closing = true
        if (animate) {
            val animator = ValueAnimator.ofInt((translationX).toInt(), 0)
            animator.addUpdateListener {
                translationX = 1.0f * it.animatedValue as Int
            }

            animator.apply {
                duration = 250
                start()
            }
        }

        swiping = false
        opened = false
    }

    private fun Float.pixelsToDp() = this / resources.displayMetrics.density
    private fun Float.dpToPixels() = this * resources.displayMetrics.density
    private fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density
    private fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density


    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun closeIfSwiped(){
        cancelSwipe(false)
    }
}