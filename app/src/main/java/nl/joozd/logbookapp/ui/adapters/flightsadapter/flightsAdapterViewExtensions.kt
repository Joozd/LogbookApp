package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import nl.joozd.logbookapp.extensions.getColorFromAttr

fun ViewGroup.makePlannedColorIfNeeded(planned: Boolean){
    val normalColor = context.getColorFromAttr(android.R.attr.textColorSecondary)
    val plannedColor = context.getColorFromAttr(android.R.attr.textColorHighlight)
    for (c in 0 until this.childCount) {
        val v: View = getChildAt(c)
        if (v is TextView) {
            v.setTextColor(if (planned) plannedColor else normalColor)
        }
    }
}

fun View.shouldBeVisible(vis: Boolean){
    this.visibility = if (vis) View.VISIBLE else View.GONE
}




