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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserBinding
import nl.joozd.logbookapp.extensions.minusOneWithFloor
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.customs.viewpagernavigatorbar.ViewPager2NavigatorBar
import nl.joozd.logbookapp.ui.utils.viewPagerTransformers.DepthPageTransformer

class NewUserActivity : JoozdlogActivity() {
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
                adapter = ScreenSlidePagerAdapter(this@NewUserActivity, viewModel.openPagesState ?: 1)
                setPageTransformer(DepthPageTransformer(TRANSFORMER_MIN_SCALE))
                navigationBar.attach(this)
                /*
                TabLayoutMediator(tabLayout, this) { _, _ ->
                    // empty for now
                }.attach()
                */
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
                    NewUserActivityEvents.FINISHED -> closeAndstartMainActivity()
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
        mViewPager.currentItem = mViewPager.currentItem.minusOneWithFloor(0)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, private var availablePages: Int) : FragmentStateAdapter(fa), ViewPager2NavigatorBar.Adapter {

        fun openPages(newHighest: Int){
            availablePages = maxOf(availablePages, newHighest)
        }
        override fun getItemCount(): Int = availablePages

        override fun createFragment(position: Int): Fragment = when(position){
            /**
             * This order must match the order in [NewUserActivityViewModel.Companion]
             */
            0 -> NewUserActivityIntroPage()
            1 -> NewUserActivityEmailPage()
            2 -> NewUserActivityCloudPage()
            3 -> NewUserActivityPage3()
            4 -> NewUserActivityPage4()
            else -> error("ScreenSlidePagerAdapter asked to provide non-existing page")
        }

        /**
         * Text for the left button. Null for default text, empty string for hidden button.
         */
        override fun previousButtonText(position: Int): String = when(position){
            4 -> ""
            else -> getString(R.string.skip)
        }

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
         * * OnClickListener for the right button. Null for default action.
         */
        override fun nextButtonOnClick(position: Int): (ViewPager2) -> Unit = {
            // Delegate this to [viewModel] who will send feedback to [activity] so it will do things with [ViewPager] if needed
            viewModel.continueClicked(position)
        }

        override fun nextButtonEnabled(position: Int): Boolean? = viewModel.nextButtonEnabled(position)
    }

    companion object{
        private const val TRANSFORMER_MIN_SCALE = 0.75f
    }
}