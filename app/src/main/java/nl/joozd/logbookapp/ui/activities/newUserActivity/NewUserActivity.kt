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
import android.os.PersistableBundle
import androidx.viewpager2.widget.ViewPager2
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.viewPagerTransformers.DepthPageTransformer

class NewUserActivity : JoozdlogActivity() {

    private lateinit var mViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setTheme(R.style.AppTheme)
        ActivityNewUserBinding.inflate(layoutInflater).apply {
            mViewPager = viewPager.apply {
                adapter = NewUserActivityViewPagerAdapter(this@NewUserActivity)
                setPageTransformer(DepthPageTransformer(TRANSFORMER_MIN_SCALE))
                currentItem = savedInstanceState?.getInt(SAVED_INSTANCE_STATE_PAGE_KEY) ?: 0
            }
            setContentView(root)
        }
    }

    fun continueClicked(){
        mViewPager.currentItem++
    }

    fun previousClicked(){
        mViewPager.currentItem--
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt(SAVED_INSTANCE_STATE_PAGE_KEY, mViewPager.currentItem)
    }

    companion object{
        private const val TRANSFORMER_MIN_SCALE = 0.75f

        const val PAGE_INTRO = 0
        const val PAGE_CLOUD = 1
        const val PAGE_CALENDAR = 2
        const val PAGE_FINAL = 3

        private const val SAVED_INSTANCE_STATE_PAGE_KEY = "PAGE"
    }
}