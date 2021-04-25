package com.next.easyloader.gif

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import com.next.easyloader.memorycache.CacheSizeProvider
import kotlinx.coroutines.*

class GifDrawable(
    private val gifDecoder: GifDecoder,
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher
) : Drawable(), Animatable, CacheSizeProvider {

    private var nextFrame: Bitmap?
    private val paint = Paint()
    private var currentAlpha: Int = 255
    private var running = false
    private var job: Job? = null
    private var nextDelay: Int
    private val width = gifDecoder.width
    private val height = gifDecoder.height

    @Volatile
    private var lastFrameTime: Long = 0

    init {
        gifDecoder.advance()
        nextFrame = gifDecoder.nextFrame
        nextDelay = gifDecoder.nextDelay
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun draw(canvas: Canvas) {
        if (nextFrame == null)
            return

        val bounds = bounds
        canvas.drawBitmap(nextFrame!!, null, bounds, paint)

        if (running)
            invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        if (currentAlpha == alpha)
            return
        paint.alpha = alpha

        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun start() {
        if (running) {
            stop()
        }

        lastFrameTime = SystemClock.elapsedRealtime()
        running = true
        next()
        invalidateSelf()
    }

    private fun next() {
        if (!running)
            return

        job = GlobalScope.launch(io) {
            gifDecoder.advance()
            val nextDelay = gifDecoder.nextDelay
            val nextFrame = gifDecoder.nextFrame

            launch(main) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastFrameTime
                if (elapsed < this@GifDrawable.nextDelay) {
                    delay(this@GifDrawable.nextDelay - elapsed)
                }

                lastFrameTime = SystemClock.elapsedRealtime()

                onNextFrame(nextFrame, nextDelay)

                next()
            }
        }
    }

    private fun onNextFrame(nextFrame: Bitmap, nextDelay: Int) {
        this.nextFrame = nextFrame
        this.nextDelay = nextDelay
//        invalidateSelf()
    }

    override fun stop() {
        if (!running)
            return

        running = false
        job?.cancel()
        job = null
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)

        if (visible && !running) {
            start()
        }

        if (!visible)
            stop()

        return changed
    }

    override fun objectSize(): Int {
        return gifDecoder.frameCount * width * height
    }

}