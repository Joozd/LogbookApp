package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

abstract class NewUseractivityPage: JoozdlogFragment() {
    val newUserActivity: NewUserActivity get() = requireActivity() as NewUserActivity

    private fun continueClicked(){
        newUserActivity.continueClicked()
    }
    private fun previousClicked(){
        newUserActivity.previousClicked()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.continueButton)?.setOnClickListener {
            continueClicked()
        }
        view.findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            previousClicked()
        }
    }
}