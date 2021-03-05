package nl.joozd.logbookapp.ui.utils

import android.animation.ValueAnimator

/**
 * Creates a [ValueAnimator] with a fraction of its completeness from 0 to 1
 * @param duration: Duration of the animation
 * @param start: if true, starts the animation straight away
 * @param onUpdate: Function to run every frame of the animation
 */
fun fractionAnimator(duration: Long = 0, start: Boolean = true, onUpdate: (Float) -> Unit) = ValueAnimator.ofFloat(0f, 1f).apply{
    addUpdateListener {
        onUpdate(it.animatedValue as Float)
    }
    this.duration = duration
    if (start) start()
}