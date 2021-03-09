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

package nl.joozd.logbookapp.ui.utils.customs.viewpagernavigatorbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.utils.fractionAnimator


/**
 * A line of dots, one of which can be active.
 * WIP.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Dots(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int, private val amountOfDots: Int = 0, private var activeDot: Int? = null): ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    //secondary constructors:
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0):  this(context, attrs, defStyleAttr, 0, 0,null)
    constructor(context: Context, attrs: AttributeSet?):                         this(context, attrs, 0, 0, 0, null)
    constructor(context: Context, amountOfDots: Int = 0, activeDot: Int? = 0):   this(context, null, 0, 0, amountOfDots)
    constructor(context: Context):                                               this(context, null, 0, 0)

    private var initialized = true
    /**
     * The size of an inactive dot in px, default is [DEFAULT_UNSELECTED_DOT_DIAMETER_DP]
     */
    var inactiveDotSize: Int = dpToPixels(DEFAULT_UNSELECTED_DOT_DIAMETER_DP)
        set(it){
            field = it
            if (initialized) generateDots()
        }

    /**
     * The size of an active dot in px, default is [DEFAULT_SELECTED_DOT_DIAMETER_DP]
     */
    var activeDotSize: Int = dpToPixels(DEFAULT_SELECTED_DOT_DIAMETER_DP)
        set(it){
            field = it
            if (initialized) generateDots()
        }


    /**
     * Space between two dots in px, default is [DEFAULT_SPACING_DP]
     * This will add [maxDotSize] when getting
     */
    var spacing: Int = dpToPixels(DEFAULT_SPACING_DP)
        set(it){
            field = it
            if (initialized) generateDots()
        }
        get() = field + maxDotSize

    /**
     * Color of an active dot, default is ?attr/colorAccent
     */
    var activeColor: Int = getAttribute(R.attr.colorAccent)
        set(it){
            field = it
            if (initialized) generateDots()
        }

    /**
     * Color of an inactive dot, default is ?attr/android:textColorSecondary
     */
    var inactiveColor: Int = getAttribute(android.R.attr.textColorSecondary)
        set(it){
            field = it
            if (initialized) generateDots()
        }

    /**
     * Duration of animation effects in ms, default is [DEFAULT_ANIMATION_DURATION_MS]
     */
    var animationDuration: Int = DEFAULT_ANIMATION_DURATION_MS
        set(it){
            field = it
            if (initialized) generateDots()
        }


    /**
     * Private values
     */
    private val maxDotSize: Int
        get() = maxOf(inactiveDotSize, activeDotSize)


    // The actual dots we will be displaying
    private var shapes: List<ShapeDrawable> = emptyList()
    private var dots: List<ImageView> = emptyList()

    //The dot that is currently active


    //the currently active animations
    //setters will make sure replaced animations are completed
    private var currentActivatingAnimation: ValueAnimator? = null
        set(it){
            field?.end()
            field = it
        }
    private var currentDeactivatingAnimation: ValueAnimator? = null
        set(it){
            field?.end()
            field = it
        }


    init{
        context.obtainStyledAttributes(attrs, R.styleable.Dots, defStyleAttr, defStyleRes).apply{
            //inactive dot size = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize)
            inactiveDotSize = getDimensionPixelSize(R.styleable.Dots_dotsInactiveDiameter, inactiveDotSize)

            //active dot size = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize)
            activeDotSize = getDimensionPixelSize(R.styleable.Dots_dotsActiveDiameter, activeDotSize)

            //spacing = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize)
            spacing = getDimensionPixelSize(R.styleable.Dots_dotsSpacing, spacing)
            activeColor = getColor(R.styleable.Dots_dotsActiveColor, activeColor)
            inactiveColor = getColor(R.styleable.Dots_dotsActiveColor, inactiveColor)
            animationDuration = getInt(R.styleable.Dots_dotsAnimationDuration, animationDuration)
            //aaaand we're done with this TypedArray
            recycle()
        }
        generateDots()
        activeDot?.let { forceActivatedDot(it) }

        initialized = true
    }

    /**
     * Make a dot active, and all preivous active dot inactive
     */
    fun activateDot(dotIndex: Int){
        activeDot?.let { if (it != dotIndex) deactivateDot(it) }
        activeDot = dotIndex
        currentActivatingAnimation = shapes.getOrNull(dotIndex)?.let { shape ->
            val startSize = shape.intrinsicHeight
            val endSize = activeDotSize
            val startColor = shape.paint.color
            val endColor = activeColor

            val change = endSize - startSize
            fractionAnimator(duration = animationDuration.toLong()) { fraction ->
                val newSize = startSize + (change * fraction).toInt()
                val newColor = ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int
                changeShape(dotIndex, newSize, newColor)
            }
        }
    }

    /**
     * Deactivate a dot. No other dot will be activated.
     * This function will not touch [activeDot]
     */
    fun deactivateDot(index: Int) {
        currentDeactivatingAnimation = shapes.getOrNull(index)?.let { shape ->
            val startSize = shape.intrinsicHeight
            val endSize = inactiveDotSize
            val startColor = shape.paint.color
            val endColor = inactiveColor

            val change = endSize - startSize
            fractionAnimator(duration = animationDuration.toLong()) { fraction ->
                val newSize = startSize + (change * fraction).toInt()
                val newColor = ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int
                changeShape(index, newSize, newColor)
            }
        }
    }


    /**
     * Set dot as activated, no animation
     */
    fun forceActivatedDot(index: Int){
        changeShape(index, activeDotSize, activeColor)
    }

    /**
     * Generate dots and update view to match
     */
    private fun generateDots(){
        shapes = Array(amountOfDots) { generateShape() }.toList()
        dots = shapes.map{ addShapeToImageView(it) }

        updateView()
    }


    /**
     * Draw them dots!
     * All dots will be drawn as Inactive.
     */
    private fun updateView(){
        val totalWidth = if (amountOfDots == 0) 0 else (amountOfDots-1) * spacing + maxDotSize
        println("TOTALWIDTH: $totalWidth, amountOFDots: $amountOfDots, spacing: $spacing, maxDotSize: $maxDotSize")
        removeAllViews()
        layoutParams = LayoutParams(totalWidth, maxDotSize)
        ConstraintSet().apply{
            clone(this@Dots)
            //first add all dots to the view
            dots.forEach { dot ->
                addView(dot)
            }
            //connext all dots to whatever comes before and after
            dots.forEachIndexed{ index, dot ->
                connect(dot.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(dot.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

                if (dot === dots.first())
                    connect(dot.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                else
                    connect(dot.id, ConstraintSet.START, dots[index-1].id, ConstraintSet.END)
                if (dot === dots.last())
                    connect(dot.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                else
                    connect(dot.id, ConstraintSet.END, dots[index+1].id, ConstraintSet.START)
            }
            //if (dots.size >= 2) createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, dots.map{it.id}.toIntArray(),null, ConstraintSet.CHAIN_SPREAD_INSIDE )
            applyTo(this@Dots)
        }
        requestLayout()
    }

    /**
     * Wrap a shape in an imageView
     */
    private fun addShapeToImageView(shape: ShapeDrawable) = ImageView(context).apply{
        setImageDrawable(null) // forces redraw
        setImageDrawable(shape)
        scaleType = ImageView.ScaleType.CENTER
        id = generateViewId()
    }

    /**
     * Generate a shape of a dot
     */
    private fun generateShape() = ShapeDrawable(OvalShape()).apply{
        intrinsicWidth = inactiveDotSize
        intrinsicHeight = inactiveDotSize
        paint.color = inactiveColor
    }

    private fun changeShape(targetIndex: Int, newSize: Int? = null, newColor: Int? = null){
        shapes.getOrNull(targetIndex)?.let { shape ->
            newSize?.let {
                shape.intrinsicHeight = newSize
                shape.intrinsicWidth = newSize
            }
            newColor?.let{
                shape.paint.color = it
            }
            dots.getOrNull(targetIndex)?.let{
                it.setImageDrawable(null) // force redraw
                it.setImageDrawable(shape)
            }
        }

    }

    companion object{
        const val DEFAULT_SPACING_DP = 9
        const val DEFAULT_UNSELECTED_DOT_DIAMETER_DP = 6
        const val DEFAULT_SELECTED_DOT_DIAMETER_DP = 9
        const val DEFAULT_ANIMATION_DURATION_MS = 0 // default is no animation
    }
}
