package nl.joozd.logbookapp.ui.utils.customs


import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_progressbar.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getActivity

class JoozdlogProgressBar(private val target: ViewGroup) {
    private val activity = target.getActivity()!!
    private val progBarParent = activity.layoutInflater.inflate(R.layout.item_progressbar, target, false)
    private val progressBar = progBarParent.progressBar

    var text: String
        get() = progBarParent.progressBarText.text.toString()
        set(t) {progBarParent.progressBarText.text = t}

    var progress: Int
        get() = progressBar.progress
        set(p) { progressBar.progress = p}

    var backgroundColor: Int?
        get() = null
        set(color) {
            color?.let{
                progBarParent.setBackgroundColor(it)
            }
        }

    fun show(): JoozdlogProgressBar {
        target.addView(progBarParent)
        return this
    }
    fun remove() = target.removeView(progBarParent)

}