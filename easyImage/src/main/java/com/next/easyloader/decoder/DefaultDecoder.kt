package com.next.easyloader.decoder

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log

internal object DefaultDecoder : Decoder {

    override fun decode(bytes: ByteArray, w: Int, h: Int): Drawable? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inJustDecodeBounds = false
        options.inSampleSize = decideSampleSize(w, h, options.outWidth, options.outHeight)
        Log.e("test", "decode: ${options.inSampleSize}")
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (bitmap != null)
            return BitmapDrawable(bitmap)

        return null
    }

    private fun decideSampleSize(w: Int, h: Int, sourceW: Int, sourceH: Int): Int {
        var sampleSizeW = 1
        while (w > 0 && sourceW > w * sampleSizeW) {
            sampleSizeW *= 2
        }

        var sampleSizeH = 1
        while (h > 0 && sourceH > h * sampleSizeH) {
            sampleSizeH *= 2
        }

        return sampleSizeH.coerceAtLeast(sampleSizeW)
    }

}