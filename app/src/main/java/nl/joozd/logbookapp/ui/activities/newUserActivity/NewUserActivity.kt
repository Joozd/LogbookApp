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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

class NewUserActivity : JoozdlogActivity() {
    private lateinit var mViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setTheme(R.style.AppTheme)
        ActivityNewUserBinding.inflate(layoutInflater).apply {
            mViewPager = viewPager
            setContentView(root)
        }
    }

    fun continueClicked(page: Int){
        mViewPager.currentItem = page + 1
    }

    fun previousClicked(page: Int){
        mViewPager.currentItem = page - 1
    }
/*
    val viewModel: NewUserActivityViewModel by viewModels()

    private lateinit var mViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setTheme(R.style.AppTheme)
        ActivityNewUserBinding.inflate(layoutInflater).apply {

            /*******************************************************************************************
             * setup viewPager
             *******************************************************************************************/

            mViewPager = viewPager.apply {
                adapter = ScreenSlidePagerAdapter(this@NewUserActivity)
                setPageTransformer(DepthPageTransformer(TRANSFORMER_MIN_SCALE))
                navigationBar.attach(this)
                currentItem = viewModel.lastOpenPageState ?: 0
            }


            /*******************************************************************************************
             * Observers:
             *******************************************************************************************/

            /**
             * Event observers:
             */

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()) {
                    NewUserActivityEvents.FINISHED -> closeAndStartMainActivity()
                    NewUserActivityEvents.NEXT_PAGE -> viewPager.currentItem++
                    NewUserActivityEvents.UPDATE_NAVBAR -> navigationBar.notifyDataChanged()
                }
            }
            setContentView(root)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.lastOpenPageState = mViewPager.currentItem
    }

    override fun onBackPressed() {
        // select previous step if > 0
        mViewPager.currentItem = mViewPager.currentItem.minusOneWithMinimumValue(0)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa), ViewPager2NavigatorBar.Adapter {

        override fun getItemCount(): Int =  PAGE_FINAL + 1

        override fun createFragment(position: Int): Fragment = when(position){
            PAGE_INTRO -> NewUserActivityIntroPage()
            PAGE_CLOUD -> NewUserActivityCloudPage()
            PAGE_EMAIL -> NewUserActivityEmailPage()
            PAGE_CALENDAR -> NewUserActivityCalendarPage()
            PAGE_FINAL -> NewUserActivityFinalPage()
            else -> error("ScreenSlidePagerAdapter asked to provide non-existing page")
        }

        /**
         * Text for the left button. Null for default text, empty string for hidden button.
         */
        override fun previousButtonText(position: Int): String = viewModel.skipButtonText(position) ?: "ERROR: NO CONTEXT"

        /**
         * OnClickListener for the left button. Null for default action.
         * In this case: SKIP
         */
        override fun previousButtonOnClick(position: Int): (ViewPager2) -> Unit = {
            // Delegate this to [viewModel] who will send feedback to [activity] so it will do things with [ViewPager] if needed
            viewModel.skipClicked(position)
        }

        /**
         * Text for the right button. Null for default text, empty string for hidden button.
         */
        override fun nextButtonText(position: Int): String = when(position){
            4 -> getString(R.string.done)
            else -> getString(R.string._continue)
        }

        /**
         * OnClickListener for the right button. Null for default action.
         */
        override fun nextButtonOnClick(position: Int): (ViewPager2) -> Unit = {
            // Delegate this to [viewModel] who will send feedback to [activity] so it will do things with [ViewPager] if needed
            viewModel.continueClicked(position)
        }

        /**
         * if true, button is enabled when navigating to this page, if false, it is disabled.
         * If not implemented it stays the way it was on previous page.
         */
        override fun nextButtonEnabled(position: Int): Boolean = viewModel.isContinueButtonEnabled(position)

        /**
         * Keep track of which page is open so in case of activity recreation (eg rotation) we will continue on that page
         */
        override fun onPageChanged(position: Int) {
            viewModel.lastOpenPageState = position
        }
    }


 */
    companion object{
        private const val TRANSFORMER_MIN_SCALE = 0.75f

        const val PAGE_INTRO = 0
        const val PAGE_CLOUD = 1
        const val PAGE_EMAIL = 2
        const val PAGE_CALENDAR = 3
        const val PAGE_FINAL = 4
    }
}