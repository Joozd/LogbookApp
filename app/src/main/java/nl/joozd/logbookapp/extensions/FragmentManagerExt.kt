package nl.joozd.logbookapp.extensions

import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit

// Not compatible with backstack (which it doesn't use)
fun FragmentManager.removeByTagAnimated(tag: String, animation: Int, actionAfter: () -> Unit = {}){
    findFragmentByTag(tag)?.let { fragToRemove ->
        val anim = AnimationUtils.loadAnimation(fragToRemove.context, animation).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    commit {
                        remove(fragToRemove)
                        runOnCommit {
                            actionAfter()
                        }
                    }
                }
            })
        }
        fragToRemove.view?.startAnimation(anim)
    }
}
