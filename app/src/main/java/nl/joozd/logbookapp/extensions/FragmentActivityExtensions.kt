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
        setReorderingAllowed(true)
        add<T>(containerViewId, tag = tag, args = args)
        if (addToBackStack) addToBackStack(tag)
    }
}

/**
 * Launch a fragment with less boilerplate.
 * This version takes an instantiated Fragment, in case it needs constructor parameters or an apply block or anything like that:
 * showFragment()
 */
fun <T: Fragment>FragmentActivity.showFragment(
    fragment: T,
    @IdRes containerViewId: Int = android.R.id.content,
    tag: String? = null,
    addToBackStack: Boolean = true)
{
    supportFragmentManager.commit{
        setReorderingAllowed(true)
        add(containerViewId, fragment, tag)
        if (addToBackStack) addToBackStack(tag)
    }
}

/**
 * Launch a fragment with less boilerplate.
 * This version takes an instantiated Fragment, in case it needs constructor parameters or an apply block or anything like that:
 * showFragment()
 */
fun <T: Fragment>FragmentActivity.showFragment(
    fragment: T,
    containerView: View,
    tag: String? = null,
    addToBackStack: Boolean = true)
{
    supportFragmentManager.commit{
        setReorderingAllowed(true)
        add(containerView.id, fragment, tag)
        if (addToBackStack) addToBackStack(tag)
    }
}