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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserBinding
import nl.joozd.logbookapp.extensions.minusOneWithFloor
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
import nl.joozd.logbookapp.ui.activities.LoginActivity
import nl.joozd.logbookapp.ui.utils.viewPagerTransformers.DepthPageTransformer

class NewUserActivity : JoozdlogActivity() {
    val viewModel: NewUserActivityViewModel by viewModels()

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)
        val binding = ActivityNewUserBinding.inflate(layoutInflater)

        /*******************************************************************************************
         * setup viewPager
         *******************************************************************************************/

        viewPager = binding.viewPager.apply{
            adapter = ScreenSlidePagerAdapter(this@NewUserActivity, viewModel.openPagesState ?: 1)
            setPageTransformer(DepthPageTransformer(TRANSFORMER_MIN_SCALE))
            TabLayoutMediator(binding.tabLayout, this) { _, _ ->
                // empty for now
            }.attach()
            currentItem = viewModel.lastOpenPageState ?: 0
        }


        /*******************************************************************************************
         * Observers:
         *******************************************************************************************/

        /**
         * Event observers:
         */

        viewModel.feedbackEvent.observe(this, Observer {
            when(it.getEvent()){
                NewUserActivityEvents.FINISHED -> closeAndstartMainActivity()
                NewUserActivityEvents.SHOW_SIGN_IN_DIALOG -> startActivity(Intent(this, LoginActivity::class.java))
                NewUserActivityEvents.NEXT_PAGE -> {
                    it.getInt().let{nextPage ->
                        viewModel.openPagesState = maxOf(viewModel.openPagesState ?: 1, nextPage + 1)
                        (viewPager.adapter as ScreenSlidePagerAdapter).openPages(nextPage+1)
                        viewPager.apply {
                            adapter = adapter
                            TabLayoutMediator(binding.tabLayout, this) { _, _ ->
                                // empty for now
                            }.attach()
                        }
                        viewPager.currentItem = nextPage
                    }
                }
            }
        })

        setContentView(binding.root)

    }

    override fun onStop() {
        super.onStop()
        viewModel.lastOpenPageState = viewPager.currentItem
    }

    override fun onBackPressed() {
        // select previous step if > 0
        viewPager.currentItem = viewPager.currentItem.minusOneWithFloor(0)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, private var availablePages: Int) : FragmentStateAdapter(fa) {

        fun openPages(newHighest: Int){
            availablePages = maxOf(availablePages, newHighest)
        }
        override fun getItemCount(): Int = availablePages

        override fun createFragment(position: Int): Fragment = when(position){
            0 -> NewUserActivityPage1()
            1 -> NewUserActivityPage2()
            2 -> NewUserActivityPage3()
            3 -> NewUserActivityPage4()
            else -> Fragment().also{ Log.w(this::class.simpleName, "PageViewer asked to provide non-existing page")}
        }
    }
    companion object{
        private const val TRANSFORMER_MIN_SCALE = 0.75f
    }
}