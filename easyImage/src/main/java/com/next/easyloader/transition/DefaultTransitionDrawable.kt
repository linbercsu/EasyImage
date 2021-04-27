package com.next.easyloader.transition

import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.SystemClock
import android.util.Log
import com.next.easyloader.internal.DrawableHelper
import com.next.easyloader.memorycache.CacheSizeProvider

class DefaultTransitionDrawable(private val drawable: Drawable): LayerDrawable(arrayOf(drawable)), Animatable, CacheSizeProvider {
    private var running = false
    private var lastFrameTime = 0L
    private var currentAlpha = 255

    override fun draw(canvas: Canvas) {
        if (!running) {
            drawable.draw(canvas)
            return
        }

        if (lastFrameTime == 0L) {
            lastFrameTime = SystemClock.elapsedRealtime()
        }

        val now = SystemClock.elapsedRealtime()
        var elapsed = now - lastFrameTime

        val duration = 400
        if (elapsed > duration)
            elapsed = 400

        val alpha = currentAlpha * elapsed/400
        Log.e("test", "alpha $alpha")
        drawable.alpha = alpha.toInt()

        drawable.draw(canvas)
        drawable.alpha = 255

        if (alpha.toInt() == 255) {
            running = false
        } else {
            invalidateSelf()
        }
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)

        currentAlpha = alpha
    }

    override fun start() {
        if (running)
            return

        lastFrameTime = 0
        running = true
        invalidateSelf()
    }

    override fun stop() {
        if (!running)
            return

        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        drawable.setVisible(visible, restart)
        if (visible) {
//            start()
        } else {
            stop()
        }

        return changed
    }

    override fun objectSize(): Int {
        return DrawableHelper.calculateDrawableSize(drawable)
    }

}