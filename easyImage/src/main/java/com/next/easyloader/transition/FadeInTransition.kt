package com.next.easyloader.transition

import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.widget.ImageView

internal class FadeInTransition : Transition {
    override fun transition(previousDrawable: Drawable?, drawable: Drawable): Drawable {
        if (previousDrawable == null)
            return drawable

        val transitionDrawable = DefaultTransitionDrawable(drawable)
//        transitionDrawable.isCrossFadeEnabled = true
        return transitionDrawable
    }

    override fun onAfter(view: ImageView, drawable: Drawable) {
        if (drawable is DefaultTransitionDrawable) {
            drawable.start()
        }
    }

}