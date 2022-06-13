package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import androidx.fragment.app.Fragment

abstract class NewUseractivityPage: Fragment() {
    val newUserActivity: NewUserActivity get() = requireActivity() as NewUserActivity

    abstract var pageNumber: Int?

    fun continueClicked(){
        newUserActivity.continueClicked(pageNumber!!)
    }

    fun previousClicked(){
        newUserActivity.previousClicked(pageNumber!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pageNumber?.let { outState.putInt(NEW_USER_PAGE_PAGE_NUMBER, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        pageNumber = savedInstanceState?.getInt(NEW_USER_PAGE_PAGE_NUMBER)
    }

    companion object {
        const val NEW_USER_PAGE_PAGE_NUMBER = "NEW_USER_PAGE_PAGE_NUMBER"
    }
}