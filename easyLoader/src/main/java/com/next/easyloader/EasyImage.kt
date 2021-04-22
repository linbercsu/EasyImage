package com.next.easyloader

import android.app.Application
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.collection.LruCache
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.next.easyloader.gif.GifDecoderFactory
import com.next.easyloader.source.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.lang.Runnable
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.*
import kotlin.collections.LinkedHashMap

class LIFOExecutor(private val executorService: ExecutorService) : Executor {
    private val commandList: LinkedList<Runnable> = LinkedList()
    private val pausedCommandList: LinkedList<Runnable> = LinkedList()
    @Volatile
    private var paused = false

    override fun execute(command: Runnable) {
        synchronized(pausedCommandList) {
            if (paused) {
                pausedCommandList.add(command)
                return
            }
        }

        synchronized(commandList) {
            commandList.add(command)
        }
        executorService.execute {
            val last: Runnable
            synchronized(commandList) {
                last = commandList.removeLast()
            }

            last.run()
        }
    }

    fun pause() {
        synchronized(pausedCommandList) {
            paused = true
        }
    }

    fun resume() {
        synchronized(pausedCommandList) {
            paused = false

            for (run : Runnable in pausedCommandList) {
                execute(run)
            }

            pausedCommandList.clear()
        }
    }
}

object EasyImage : LifecycleEventObserver {
    private lateinit var io: CoroutineDispatcher
    private lateinit var main: CoroutineDispatcher

    private val map: MutableMap<Any, LoaderManager> = mutableMapOf()
    private val decoderFactoryMap: MutableMap<String, DecoderFactory> = mutableMapOf()
    private val allCompletedRequests = LinkedHashMap<Request, Request>()
    internal lateinit var diskCache: DiskCache
    internal lateinit var memoryCache: MemoryCache
    private lateinit var context: Application
    lateinit var sourceFactory: SourceFactory
    private val gifDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    private lateinit var ioExecutor: LIFOExecutor

    @MainThread
    fun install(builder: EasyImageBuilder) {
        this.context = builder.application
        this.main = builder.main ?: Dispatchers.Main
        if (builder.io != null) {
            ioExecutor = LIFOExecutor(builder.io)
            this.io = ioExecutor.asCoroutineDispatcher()
        } else {
            ioExecutor = LIFOExecutor(Executors.newFixedThreadPool(4))
            this.io = ioExecutor.asCoroutineDispatcher()
        }

        diskCache = DefaultDiskCache(context)
        val memorySizeCalculator = MemorySizeCalculator.Builder(context).build()
        memoryCache = LruMemoryCache(context, memorySizeCalculator.memoryCacheSize)

        decoderFactoryMap["gif"] = GifDecoderFactory(gifDispatcher, main)

        val okHttp = OkHttpClient.Builder()
            .connectTimeout((15 * 1000).toLong(), TimeUnit.MILLISECONDS)
            .readTimeout((30 * 1000).toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .build()

        sourceFactory = NormalSourceFactory(okHttp)
    }

    fun install(context: Application) {
        EasyImageBuilder(context).build()
    }

    fun pause() {
        ioExecutor.pause()
    }

    fun resume() {
        ioExecutor.resume()
    }

    @MainThread
    fun with(lifecycleOwner: LifecycleOwner): LoaderManager {
        val manager = map[lifecycleOwner]
        if (manager != null)
            return manager

        val loaderManager = LoaderManager(this, io, main)
        map[lifecycleOwner] = loaderManager

        lifecycleOwner.lifecycle.addObserver(this)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            loaderManager.paused = false
        }

        return loaderManager
    }

    internal fun getDecoder(type: String): Decoder {
        val get = decoderFactoryMap[type] ?: return DefaultDecoder
        return get.create()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume(source)
            Lifecycle.Event.ON_PAUSE -> onPause(source)
            Lifecycle.Event.ON_DESTROY -> onDestroy(source)
            else -> doNothing()
        }
    }

    private fun doNothing() {

    }

    private fun onResume(source: LifecycleOwner) {
        val loaderManager = map[source]
//        loaderManager?.resume()
    }

    private fun onPause(source: LifecycleOwner) {
//        map[source]?.pause()
    }

    private fun onDestroy(source: LifecycleOwner) {
        map[source]?.release()
        map.remove(source)
    }

    @WorkerThread
    internal fun onRequestSuccessful(request: Request) {
        synchronized(allCompletedRequests) {
            allCompletedRequests[request] = request
        }
    }

    @WorkerThread
    internal fun recycleRequest() {
        synchronized(allCompletedRequests) {
            val iterator = allCompletedRequests.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key.recyclable) {
                    if (next.key.drawable != null) {
                        next.key.recycle(memoryCache)
                    }
                    iterator.remove()
                }
            }
        }
    }

}

class EasyImageBuilder(
    internal val application: Application,
    internal val io: ExecutorService? = null,
    internal val main: CoroutineDispatcher? = null
) {
    fun build() {
        EasyImage.install(this)
    }
}


class LoaderManager(
    val easyLoader: EasyImage,
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher
) {
    internal var paused: Boolean = true
    private val requestMap: WeakHashMap<ImageView, Request> = WeakHashMap()

    fun load(url: String): RequestBuilder {
        return RequestBuilder(this).load(url)
    }

    internal fun release() {
        for ((_, value) in requestMap) {
            value.cancel()
        }

        requestMap.clear()
//        requestList.clear()
    }

    internal fun onStartRequestOnly(request: Request, imageView: ImageView) {
        request.job = GlobalScope.launch(io) {
            try {
                var drawable = request.load(this)

                launch(main) {
                    request.onLoaded(drawable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(main) {
                    request.onError(e)
                }

                GlobalScope.launch(main) {
                    requestMap.remove(imageView)
                }

                return@launch
            }
        }
    }

    internal fun onRequest(request: Request, imageView: ImageView, start: Boolean) {
        val previousRequest = requestMap[imageView]
        if (previousRequest != null && previousRequest == request) {
            return
        }

        if (previousRequest != request) {
            previousRequest?.cancel()
        }

        requestMap[imageView] = request

        request.onLoading()

        if (!start)
            return

        onStartRequestOnly(request, imageView)
    }

}

class RequestBuilder(private val manager: LoaderManager) {
    private lateinit var url: String
    private var placeholder: Int = 0
    private var errorResId: Int = 0
    private var diskCacheEnabled = false
    private var transition: Transition? = null
    private var cornerRadius: Float = 0f

    @MainThread
    fun load(url: String): RequestBuilder {
        this.url = url
        return this
    }

    @MainThread
    fun transition(transition: Transition): RequestBuilder {
        this.transition = transition
        return this
    }

    fun fadeIn(): RequestBuilder {
        return transition(FadeInTransition())
    }

    fun rounded(cornerRadius: Float): RequestBuilder {
        this.cornerRadius = cornerRadius
        return this
    }

    @MainThread
    fun placeholder(resId: Int): RequestBuilder {
        placeholder = resId
        return this
    }

    @MainThread
    fun error(resId: Int): RequestBuilder {
        errorResId = resId
        return this
    }

    @MainThread
    fun into(imageView: ImageView) {
        val request = Request(
            manager,
            manager.easyLoader.sourceFactory.create(url),
            placeholder,
            errorResId,
            transition,
            imageView,
            diskCacheEnabled,
            cornerRadius
        )
        request.start()
    }
}

class Request(
    private val manager: LoaderManager,
    private val source: Source,
    private val placeholder: Int,
    private val errorResId: Int,
    private val transition: Transition?,
    target: ImageView,
    private val diskCacheEnabled: Boolean,
    private val cornerRadius: Float
) : ViewTreeObserver.OnPreDrawListener {

    private var ref: WeakReference<ImageView> = WeakReference(target)
    private val hash = source.hashCode() + target.hashCode()
    var job: Job? = null
    var completed = false

    @Volatile
    var drawable: Drawable? = null
    var previousDrawable: Drawable? = null
    private var targetW = -1
    private var targetH = -1
    private var listening = false

    internal val recyclable: Boolean
        get() {
            return ref.get() == null
        }

    @MainThread
    fun start() {
        Log.e("test", "start ${source.getCacheKey()}")
        val target: ImageView = ref.get()!!
        val layoutParams = target.layoutParams ?: return

        if (target.width > 0) {
            targetW = target.width
        } else if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            targetW = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        if (target.height > 0) {
            targetH = target.height
        } else if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            targetH = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        Log.e("test", "start: $targetW $targetH ${layoutParams.width} ${layoutParams.height}")

        val valid: Boolean = targetW != -1 && targetH != -1 && targetW != 0 && targetH != 0

        if (!valid) {
            listening = true
            target.viewTreeObserver.addOnPreDrawListener(this)
        }

        manager.onRequest(this, target, valid)

    }

    @MainThread
    fun cancel() {
        Log.e("test", "cancel")
        job?.cancel()
        job = null

        if (listening) {
            listening = false
            val target = ref.get() ?: return

            val viewTreeObserver = target.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(this)
            }
        }

        ref = WeakReference<ImageView>(null)
    }

    internal fun recycle(memoryCache: MemoryCache) {
        if (drawable != null) {
            val cacheKey = source.getCacheKey()
            val memoryCacheKey = "${cacheKey}-${targetW}-${targetH}"
            memoryCache.put(memoryCacheKey, drawable!!)
        }
    }

    @WorkerThread
    fun load(coroutineScope: CoroutineScope): Drawable {
        Log.e("test", "load ${source.getCacheKey()}")
        var drawable = doLoad(coroutineScope)

        drawable = transition?.transition(previousDrawable, drawable) ?: drawable
        return drawable
    }

    private fun checkCancel(coroutineScope: CoroutineScope) {
        if (!coroutineScope.isActive)
            throw kotlinx.coroutines.CancellationException()
    }

    private fun doLoad(coroutineScope: CoroutineScope): Drawable {
        checkCancel(coroutineScope)
        manager.easyLoader.recycleRequest()

        val cacheKey = source.getCacheKey()
        Log.e("test", "load1 $cacheKey $diskCacheEnabled")
        //load from memory cache
        val memoryCacheKey = "${cacheKey}-${targetW}-${targetH}"
        val memoryCache = manager.easyLoader.memoryCache
        val d = memoryCache.get(memoryCacheKey)
        if (d != null) {
            drawable = d
            manager.easyLoader.onRequestSuccessful(this)
            return drawable!!
        }

        Log.e("test", "load2 $memoryCacheKey")

        checkCancel(coroutineScope)

        //load from disk cache
        val bytes: ByteArray = if (diskCacheEnabled) {

            val diskCache = manager.easyLoader.diskCache
            val cacheFile = diskCache.get(cacheKey)
            if (cacheFile != null) {
                val fileSource = FileSource(cacheFile)
                val bytes = fileSource.getBytes()
                bytes
            } else {
                val bytes = source.getBytes()
                diskCache.put(cacheKey, bytes)
                bytes
            }

        } else {
            source.getBytes()
        }

        checkCancel(coroutineScope)
        Log.e("test", "load3")

        val decoder = manager.easyLoader.getDecoder(source.type)
        drawable = decoder.decode(bytes, targetW, targetH)
        if (drawable == null) {
            throw IOException()
        }

        manager.easyLoader.onRequestSuccessful(this)
        return drawable!!
//        memoryCache.put(memoryCacheKey, drawable!!)
    }

    @MainThread
    fun onLoading() {
        Log.e("test", "onLoading")
        if (placeholder > 0) {
            val get = ref.get()
            get?.setImageResource(placeholder)
            previousDrawable = get?.drawable
        }
    }

    @MainThread
    fun onLoaded(drawable: Drawable) {
        Log.e("test", "callback ${ref.get()}")
        completed = true
        val view = ref.get() ?: return

        view.setImageDrawable(drawable)
        transition?.onAfter(view, drawable)
        if (cornerRadius > 0) {
            view.clipToOutline = true
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    Log.e("test", "getOutline: ${view.width} ${view.height} $cornerRadius")
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
        }
    }

    @MainThread
    fun onError(e: Exception) {
        e.printStackTrace()
        Log.e("test", "onError")
        if (errorResId > 0) {
            val get = ref.get()
            get?.setImageResource(errorResId)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Request)
            return false

        if (source != other.source)
            return false

        if (ref.get() != other.ref.get())
            return false

        return true
    }

    override fun hashCode(): Int {
        return hash
    }

    override fun onPreDraw(): Boolean {
        val target = ref.get()!!

        targetW = target.width
        targetH = target.height

        Log.e("test", "onPreDraw $targetW $targetH")

        val viewTreeObserver = target.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(this)
        }

        manager.onStartRequestOnly(this, target)
        return false
    }
}

interface Transition {
    @WorkerThread
    fun transition(previousDrawable: Drawable?, drawable: Drawable): Drawable

    @MainThread
    fun onAfter(view: ImageView, drawable: Drawable)
}

interface MemoryCache {
    fun put(key: String, drawable: Drawable)
    fun get(key: String): Drawable?
}

interface DiskCache {
    fun put(key: String, data: ByteArray)
    fun get(key: String): File?
}

interface Decoder {
    fun decode(bytes: ByteArray, w: Int, h: Int): Drawable?
}

interface DecoderFactory {
    fun create(): Decoder
}

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


internal class DefaultDiskCache(private val context: Context) : DiskCache {

    private val dir: File

    init {
        var root = context.externalCacheDir
        if (root == null || !root.exists()) {
            root = context.cacheDir
        }

        dir = File(root, "easy-l")
        dir.mkdirs()
    }


    override fun put(key: String, data: ByteArray) {

        val file = File(dir, key)
        val tempFile = File(dir, "${key}.tmp")
        tempFile.writeBytes(data)
        tempFile.renameTo(file)
    }

    override fun get(key: String): File? {
        val file = File(dir, key)
        return if (file.exists())
            file
        else
            null
    }
}

interface CacheSizeProvider {
    fun objectSize(): Int
}

internal class LruMemoryCache(private val context: Context, private val maxSize: Int) :
    MemoryCache {
    private val cache: LruCache<String, Drawable> = object : LruCache<String, Drawable>(maxSize) {
        override fun sizeOf(key: String, value: Drawable): Int {
            return when (value) {
                is CacheSizeProvider -> {
                    value.objectSize()
                }
                is BitmapDrawable, is RoundedBitmapDrawable -> {
                    value.intrinsicWidth * value.intrinsicHeight
                }
                else -> {
                    1
                }
            }
        }
    }

    @Synchronized
    override fun put(key: String, drawable: Drawable) {
        cache.put(key, drawable)
    }

    @Synchronized
    override fun get(key: String): Drawable? {
        return cache.remove(key)
    }
}

internal class FadeInTransition : Transition {
    override fun transition(previousDrawable: Drawable?, drawable: Drawable): Drawable {
        if (previousDrawable == null)
            return drawable

        val transitionDrawable = TransitionDrawable(arrayOf(previousDrawable, drawable))
        transitionDrawable.isCrossFadeEnabled = true
        return transitionDrawable
    }

    override fun onAfter(view: ImageView, drawable: Drawable) {
        if (drawable is TransitionDrawable) {
            drawable.startTransition(400)
        }
    }

}

