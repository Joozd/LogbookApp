package nl.joozd.logbookapp.extensions

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit


/**
 * Launch a fragment with less boilerplate.
 * Works fine for dialogs:
 * showFragment<SomeFragment>()
 */
inline fun <reified  T: Fragment> FragmentActivity.showFragment(
    @IdRes containerViewId: Int = android.R.id.content,
    tag: String? = null,
    args: Bundle? = null,
    addToBackStack: Boolean = true)
{
    supportFragmentManager.commit{
        println("COMMITTING")
        setReorderingAllowed(true)
        add<T>(containerViewId, tag = tag, args = args)
        println("containrs: ${findViewById<View>(containerViewId)}")
        if (addToBackStack) addToBackStack(null)
    }
}