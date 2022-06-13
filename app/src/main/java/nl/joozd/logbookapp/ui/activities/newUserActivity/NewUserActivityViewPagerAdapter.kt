package nl.joozd.logbookapp.ui.activities.newUserActivity

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class NewUserActivityViewPagerAdapter(actitivy: NewUserActivity): FragmentStateAdapter(actitivy) {
    override fun getItemCount(): Int = NewUserActivity.PAGE_FINAL + 1

    override fun createFragment(position: Int): Fragment = when(position){
        NewUserActivity.PAGE_INTRO -> NewUserActivityIntroPage()
        NewUserActivity.PAGE_CLOUD -> NewUserActivityCloudPage()
        NewUserActivity.PAGE_EMAIL -> NewUserActivityEmailPage()
        NewUserActivity.PAGE_CALENDAR -> NewUserActivityCalendarPage()
        NewUserActivity.PAGE_FINAL -> NewUserActivityFinalPage()
        else -> error("ScreenSlidePagerAdapter asked to provide non-existing page")
    }
}