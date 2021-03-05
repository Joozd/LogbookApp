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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.viewpager2.widget.ViewPager2
import nl.joozd.logbookapp.R
import kotlin.reflect.KProperty


/**
 * A navigation bar to be used with a ViewPager2.
 * Can be used to show which page is being viewed, and has two customizable buttons (actually TextViews)
 * @params @see [ConstraintLayout]
 * //TODO this does not have protection against huge amounts of dots yet.
 */
@Suppress("unused")
class ViewPager2NavigatorBar(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0): ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    //secondary constructors:
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0,):  this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?):                         this(context, attrs, 0, 0)
    constructor(context: Context):                                               this(context, null, 0, 0)


    private var attachedViewPager2: ViewPager2? = null

    //this will be set to true when initializing is done, so initial setting of all values will not cause view to be recreated a gazillion times
    private var initialized = false

    private val layout
        get() = this


    /****************************************************************************************
     * Public properties
     ****************************************************************************************/

    /**
     * Dots properties
     * @see [Dots]
     */
    var dotsInactiveDotSize: Int by DotsProperty(dpToPixels(Dots.DEFAULT_UNSELECTED_DOT_DIAMETER_DP))

    var dotsActiveDotSize: Int  by DotsProperty(dpToPixels(Dots.DEFAULT_SELECTED_DOT_DIAMETER_DP))

    var dotsSpacing: Int  by DotsProperty(dpToPixels(Dots.DEFAULT_SPACING_DP))

    var dotsActiveColor: Int  by DotsProperty(getAttribute(R.attr.colorAccent))

    var dotsInactiveColor: Int  by DotsProperty(getAttribute(android.R.attr.textColorSecondary))

    var dotsAnimationDuration: Int  by DotsProperty(Dots.DEFAULT_ANIMATION_DURATION_MS)

    /**
     * Text to be shown as "previous" button (usually left side), default "<"
     */
    var textPrevious: String  by TextProperty(DEFAULT_TEXT_PREVIOUS)

    /**
     * Text to be shown as "previous" button (usually left side), default ">"
     */
    var textNext: String = DEFAULT_TEXT_NEXT
        set(it){
            field = it
            if (initialized) recreateText()
        }

    /**
     * Default padding on previous and next text buttons, default is 0dp
     */
    var textPadding: Int = dpToPixels(DEFAULT_TEXT_PADDING_DP)
        set(it){
            val alreadyInitialized = initialized
            initialized = false // don't recreate text 4 times
            field = it
            textPaddingStart = it
            textPaddingEnd = it
            textPaddingTop = it
            textPaddingBottom = it
            initialized = alreadyInitialized
            if(initialized) recreateText()
        }
    // per dimension
    var textPaddingStart: Int = dpToPixels(DEFAULT_TEXT_PADDING_DP)
        set(it){
            field = it
            if (initialized) recreateText()
        }
    var textPaddingEnd: Int by TextProperty(dpToPixels(DEFAULT_TEXT_PADDING_DP))

    var textPaddingTop: Int by TextProperty(dpToPixels(DEFAULT_TEXT_PADDING_DP))

    var textPaddingBottom: Int by TextProperty(dpToPixels(DEFAULT_TEXT_PADDING_DP))

    /**
     *  action to be performed when 'previous' button clicked
     */
    var onPreviousClicked: OnButtonClickedListener? = null


    /**
     * action to be performed when 'next' button clicked
     */
    var onNextClicked: OnButtonClickedListener? = null

    /**
     *  Style for previous and next buttons. Only used for it's setter.
     */
    var textStyle: Int? = null
    set(it){
        val alreadyInitialized = initialized
        initialized = false // don't recreate text 4 times
        field = it
        textStylePrevious = it
        textStyleNext = it
        initialized = alreadyInitialized
        if(initialized && (previousEnabled || nextEnabled)) recreateText()
    }

    /**
     * Style for previous button (overrides [textStyle])
     */
    var textStylePrevious: Int? by TextProperty(null)

    /**
     * Style for next button (overrides [textStyle])
     */
    var textStyleNext: Int? by TextProperty(null)

    /**
     * previous and next all caps (default True)
     */
    var textAllCaps: Boolean by TextProperty(true)

    /**
     *  Style for previous and next buttons. when disabled. Only used for it's setter.
     */
    var textStyleDisabled: Int? = null
        set(it){
            val alreadyInitialized = initialized
            initialized = false // don't recreate text 4 times
            field = it
            textStylePreviousDisabled = it
            textStyleNextDisabled = it
            initialized = alreadyInitialized
            if(initialized && (!previousEnabled || !nextEnabled)) recreateText()
        }

    /**
     * Style for previous button when disabled
     */
    var textStylePreviousDisabled: Int? by TextProperty(null)

    /**
     * Style for next button when disabled
     */
    var textStyleNextDisabled: Int? by TextProperty(null)

    /**
     * true:    [previousButton] button is enabled, meaning [onPreviousClicked] will be executed and style is [textStylePrevious]
     * false:   [previousButton] button is disabled, meaning [onPreviousClicked] will not be executed and style is [textStylePreviousDisabled]
     * Default is true
     */
    var previousEnabled: Boolean by TextProperty(true)

    /**
     * true:    [nextButton] button is enabled, meaning [onNextClicked] will be executed and style is [textStyleNext]
     * false:   [nextButton] button is disabled, meaning [onNextClicked] will not be executed and style is [textStyleNextDisabled]
     * Default is true
     */
    var nextEnabled: Boolean by TextProperty(true)

    /****************************************************************************************
     * Private properties
     ****************************************************************************************/

    // 'previous' button. Private because it can change to another item whenever.
    private var previousButton: TextView? = null

    // 'next' button. Private because it can change to another item whenever.
    private var nextButton: TextView? = null

    //The progress dots.
    private var dots: Dots? = null

    private val onPageChangedCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            loadPage(position)
        }
    }

    private var adapter: Adapter? = null

    /**
     * The number of dots to be shown
     */
    private var amountOfDots: Int by DotsProperty(attachedViewPager2?.adapter?.itemCount ?: 0)

    /**
     * Amount of dots
     */


    /****************************************************************************************
     * Public functions
     ****************************************************************************************/

    /**
     * Attach this to a ViewPager2
     */
    fun attach(viewPager2: ViewPager2): ViewPager2NavigatorBar {
        if(viewPager2.adapter == null) throw IllegalStateException("Attached ViewPager2NavigatorBar before ViewPager2 has an adapter")
        attachedViewPager2 = viewPager2
        viewPager2.registerOnPageChangeCallback(onPageChangedCallback)
        adapter = viewPager2.adapter.takeIf { it is Adapter } as Adapter
        adapter?.let { a->
            val position = viewPager2.currentItem
            a.previousButtonEnabled(position)?.let { previousEnabled = it }
            a.nextButtonEnabled(position)?.let { nextEnabled = it }

            previousButton?.update(a.previousButtonText(position) ?: textPrevious, makeOnButtonClickedListener(a.previousButtonOnClick(position)) ?: onPreviousClicked, previousEnabled)
            nextButton?.update(a.nextButtonText(position) ?: textPrevious, makeOnButtonClickedListener(a.nextButtonOnClick(position)) ?: onPreviousClicked, previousEnabled)
        }
        amountOfDots = viewPager2.adapter!!.itemCount
        return this
    }

    /**
     * Let the navigation bar know that data to be displayed has changed and should be redrawn or updated.
     */
    fun notifyDataChanged(){
        loadPage(attachedViewPager2!!.currentItem)
    }

    /**
     * Set action for a click on 'previous' button
     */
    fun onPreviousClicked(action: OnButtonClickedListener){
        onPreviousClicked = action
    }

    /**
     * Set action for a click on 'next' button
     */
    fun onNextClicked(action: OnButtonClickedListener){
        onNextClicked = action
    }

    /****************************************************************************************
     * Private functions
     ****************************************************************************************/

    /**
     * Creates the ViewPager2NavigatorBar's layout
     */
    private fun createLayout(){
        // clear layout
        removeAllViews()

        // previous button
        previousButton = createTextView(textPrevious).apply {
            isAllCaps = textAllCaps
            textStylePrevious?.let { setTextAppearance(it) }
            setOnClickListener {
                onPreviousClicked?.onButtonClicked(attachedViewPager2!!)
            }
        }.also {
            addView(it)
        }

        // next button
        nextButton = createTextView(textNext).apply {
            textStyleNext?.let { setTextAppearance(it) }
            setOnClickListener{
                onNextClicked?.onButtonClicked(attachedViewPager2!!)
            }
        }.also{
            addView(it)
        }

        // progress dots
        dots = createDots().also { addView(it) }

        createConstraints()
    }

    /**
     * Recreate dots in case of changes to any layout values
     * Only call this after view has been initially created.
     */
    private fun recreateDots(){
        removeView(dots)
        dots = createDots().also { addView(it) }
        createConstraints()
    }

    /**
     * Recreate text in case of changes to any layout values
     * Only call this after view has been initially created.
     */
    fun recreateText(){
        //remove old buttons
        removeView(previousButton)
        removeView(nextButton)

        // previous button
        previousButton = createTextView(textPrevious).apply {
            isAllCaps = textAllCaps
            textStylePrevious?.let { setTextAppearance(it) }
            setOnClickListener {
                onPreviousClicked?.onButtonClicked(attachedViewPager2!!)
            }
        }.also {
            addView(it)
        }

        // next button
        nextButton = createTextView(textNext).apply {
            textStyleNext?.let { setTextAppearance(it) }
            setOnClickListener{
                onNextClicked?.onButtonClicked(attachedViewPager2!!)
            }
        }.also{
            addView(it)
        }
        createConstraints()
    }

    /**
     * (re-) creates all constraints in this view so everything appras where it should
     */
    private fun createConstraints() = with(ConstraintSet()){
        clone(layout)

        connect(previousButton!!.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(previousButton!!.id, ConstraintSet.TOP,  ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(previousButton!!.id, ConstraintSet.BOTTOM,  ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        connect(dots!!.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(dots!!.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(dots!!.id, ConstraintSet.TOP,  ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(dots!!.id, ConstraintSet.BOTTOM,  ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        connect(nextButton!!.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(nextButton!!.id, ConstraintSet.TOP,  ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(nextButton!!.id, ConstraintSet.BOTTOM,  ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        applyTo(layout)
    }






    private fun createTextView(t: String) = TextView(context).apply{
        text = t
        setPadding(textPaddingStart, textPaddingEnd, textPaddingTop, textPaddingBottom)
        id = generateViewId()
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    private fun createDots(): Dots = Dots(context, amountOfDots = amountOfDots, activeDot = 0).apply{
        inactiveDotSize = dotsInactiveDotSize
        activeDotSize = dotsActiveDotSize
        spacing = dotsSpacing
        activeColor = dotsActiveColor
        inactiveColor = dotsInactiveColor
        animationDuration = dotsAnimationDuration

        id = generateViewId()
    }

    private fun TextView.update(newText: String? = null, newListener: OnButtonClickedListener? = null, enabled: Boolean){
        newText?.let{
            visibility = if (it.isEmpty()) View.INVISIBLE else View.VISIBLE
            text = newText
        }
        newListener?.let{l ->
            if (enabled) {
                setOnClickListener {
                    l.onButtonClicked(attachedViewPager2!!)
                }
            } else setOnClickListener { /* do nothing */ }
        }
        (if (enabled) textStyle else textStyleDisabled)?.let{
            setTextAppearance(it)
        }
    }

    private fun enablePrevious(enabled: Boolean){
        if (enabled)
            textStylePrevious?.let { previousButton?.setTextAppearance(it) }

    }

    /****************************************************************************************
     * Initialization on construction
     ****************************************************************************************/

    init{
        context.obtainStyledAttributes(attrs, R.styleable.ViewPager2NavigatorBar, defStyleAttr, defStyleRes).apply{
            //inactive dot size = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize)
            dotsInactiveDotSize = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_dotsInactiveDiameter, dotsInactiveDotSize)

            //active dot size = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize)
            dotsActiveDotSize = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_dotsActiveDiameter, dotsActiveDotSize)

            //spacing = default (converted to px here) or from attrs (converted to Px by getDimsnsionPixelSize) plus largest of active or inactive dot
            dotsSpacing = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_dotsSpacing, dotsSpacing)
            dotsActiveColor = getColor(R.styleable.ViewPager2NavigatorBar_dotsActiveColor, dotsActiveColor)
            dotsInactiveColor = getColor(R.styleable.ViewPager2NavigatorBar_dotsActiveColor, dotsInactiveColor)
            dotsAnimationDuration = getInt(R.styleable.ViewPager2NavigatorBar_dotsAnimationDuration, dotsAnimationDuration)

            textStyle = getResourceId(R.styleable.ViewPager2NavigatorBar_textStyle, -1).takeIf { it != -1 }
            textStylePrevious = getResourceId(R.styleable.ViewPager2NavigatorBar_textStyle, textStyle ?: -1).takeIf { it != -1 }
            textStyleNext = getResourceId(R.styleable.ViewPager2NavigatorBar_textStyle, textStyle ?: -1).takeIf { it != -1 }
            textStyleDisabled = getResourceId(R.styleable.ViewPager2NavigatorBar_textStyleDisabled, -1).takeIf { it != -1 }
            textStylePreviousDisabled = getResourceId(R.styleable.ViewPager2NavigatorBar_textStylePreviousDisabled, textStyle ?: -1).takeIf { it != -1 }
            textStyleNextDisabled = getResourceId(R.styleable.ViewPager2NavigatorBar_textStyleNextDisabled, textStyle ?: -1).takeIf { it != -1 }

            textAllCaps = getBoolean(R.styleable.ViewPager2NavigatorBar_textAllCaps, textAllCaps)

            // Padding for 'previous' and 'next' buttons
            textPadding = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_textPadding, textPadding)                // defaults to DEFAULT_TEXT_PADDING_DP
            textPaddingStart = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_textPaddingStart, textPadding)      // defaults to [textPadding]
            textPaddingEnd = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_textPaddingEnd, textPadding)          // defaults to [textPadding]
            textPaddingTop = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_textPaddingTop, textPadding)          // defaults to [textPadding]
            textPaddingBottom = getDimensionPixelSize(R.styleable.ViewPager2NavigatorBar_textPaddingBottom, textPadding)    // defaults to [textPadding]

            previousEnabled = getBoolean(R.styleable.ViewPager2NavigatorBar_previousButtonEnabled, previousEnabled)
            nextEnabled = getBoolean(R.styleable.ViewPager2NavigatorBar_nextButtonEnabled, previousEnabled)

            //aaaand we're done with this TypedArray
            recycle()
        }
        initialized = true
        createLayout()
    }

    /**
     * Set all data for page at [position]
     */
    private fun loadPage(position: Int){
        println("Loading page $position")
        dots?.activateDot(position)
        adapter?.let { a ->
            a.previousButtonEnabled(position)?.let { previousEnabled = it }
            a.nextButtonEnabled(position)?.let { nextEnabled = it }
            previousButton?.update(a.previousButtonText(position) ?: textPrevious, makeOnButtonClickedListener(a.previousButtonOnClick(position)) ?: onPreviousClicked, previousEnabled)
            nextButton?.update(a.nextButtonText(position) ?: textPrevious, makeOnButtonClickedListener(a.nextButtonOnClick(position)) ?: onPreviousClicked, nextEnabled)
        }
    }

    /**
     * Helper function to make a function into an [OnButtonClickedListener]
     */
    private fun makeOnButtonClickedListener(f: ((ViewPager2) -> Unit)?) = f?.let {
        object : OnButtonClickedListener {
            override fun onButtonClicked(viewPager: ViewPager2) {
                it(viewPager)
            }
        }
    }

    /****************************************************************************************
     * Interfaces
     ****************************************************************************************/

    interface OnButtonClickedListener{
        fun onButtonClicked(viewPager: ViewPager2)
    }



    /**
     * You can implement [ViewPager2NavigatorBar.Adapter] in your ViewPager2's adapter to make different naavigation button behaviours per page.
     */
    interface Adapter {
        /**
         * Text for the left button.
         * Empty string for hidden button.
         * If null or not implemented, will use [textPrevious]
         */
        fun previousButtonText(position: Int): String? = null

        /**
         * OnClickListener for the left button. Null for default action.
         */
        fun previousButtonOnClick(position: Int): ((ViewPager2) -> Unit)? = null

        /**
         * if true, button is enabled when navigating to this page, if false, it is disabled.
         * If not implemented it stays the way it was on previous page.
         */
        fun previousButtonEnabled(position: Int): Boolean? = null

        /**
         * Text for the right button.
         * Empty string for hidden button.
         * If null or not implemented, will use [textNext]
         */
        fun nextButtonText(position: Int): String? = null

        /**
         * * OnClickListener for the right button. Null for default action.
         */
        fun nextButtonOnClick(position: Int): ((ViewPager2) -> Unit)? = null

        /**
         * if true, button is enabled when navigating to this page, if false, it is disabled.
         * If not implemented it stays the way it was on previous page.
         */
        fun nextButtonEnabled(position: Int): Boolean? = null
    }


    /****************************************************************************************
     * Inner classes
     ****************************************************************************************/

    /**
     * If [initialized] this will run redraw text
     */
    private inner class TextProperty<T>(initialValue: T) {
        private var v: T = initialValue

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return v
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            v = value
            if (initialized) recreateText()
        }
    }

    /**
     * If [initialized] this will run redraw dots
     */
    private inner class DotsProperty<T>(initialValue: T) {
        private var v: T = initialValue

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return v
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            v = value
            if (initialized) recreateDots()
        }
    }



        /****************************************************************************************
         * Companion object
         ****************************************************************************************/

        companion object{
        const val DEFAULT_NUMBER_OF_PAGES = 1
        const val DEFAULT_SELECTED_PAGE = 0

        const val DEFAULT_TEXT_PREVIOUS = "<"
        const val DEFAULT_TEXT_NEXT = ">"

        const val DEFAULT_TEXT_PADDING_DP = 0
    }
}