package com.next.easyloader.interfaces

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

interface Transition {
    @WorkerThread
    fun transition(previousDrawable: Drawable?, drawable: Drawable): Drawable

    @MainThread
    fun onAfter(view: ImageView, drawable: Drawable)
}