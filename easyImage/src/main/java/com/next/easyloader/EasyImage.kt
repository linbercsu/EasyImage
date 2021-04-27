package com.next.easyloader

import android.app.Application
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.next.easyloader.decoder.Decoder
import com.next.easyloader.decoder.DecoderFactory
import com.next.easyloader.decoder.DefaultDecoder
import com.next.easyloader.diskcache.DiskCache
import com.next.easyloader.diskcache.LruDiskCache
import com.next.easyloader.gif.GifDecoderFactory
import com.next.easyloader.internal.MemorySizeCalculator
import com.next.easyloader.internal.TypeDetector
import com.next.easyloader.memorycache.BitmapPool
import com.next.easyloader.memorycache.LruMemoryCache
import com.next.easyloader.memorycache.MemoryCache
import com.next.easyloader.source.*
import com.next.easyloader.transition.FadeInTransition
import com.next.easyloader.transition.Transition
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.io.IOException
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.*
import kotlin.collections.LinkedHashMap

internal const val CONNECT_TIMEOUT = 15_000
internal const val READ_TIMEOUT = 30_000
internal const val TAG = "EasyImage"

object EasyImage : LifecycleEventObserver {
    private lateinit var io: CoroutineDispatcher
    private lateinit var main: CoroutineDispatcher

    private val map: MutableMap<LifecycleOwner, RequestManager> = mutableMapOf()
    private val decoderFactoryMap: MutableMap<String, DecoderFactory> = mutableMapOf()
    internal val requestQueue: ReferenceQueue<Any> = ReferenceQueue()
    internal lateinit var diskCache: DiskCache
    internal lateinit var memoryCache: MemoryCache
    internal lateinit var bitmapPool: BitmapPool
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

        diskCache = LruDiskCache(context)
        val memoryCacheSize: Int
        if (builder.memoryCacheSize == 0)
            memoryCacheSize = MemorySizeCalculator.Builder(context).build().memoryCacheSize
        else
            memoryCacheSize = builder.memoryCacheSize
        memoryCache = LruMemoryCache(memoryCacheSize)
        bitmapPool = memoryCache as BitmapPool

        decoderFactoryMap[TypeDetector.TYPE_GIF] = GifDecoderFactory(gifDispatcher, main)

        val okHttp = OkHttpClient.Builder()
            .connectTimeout((CONNECT_TIMEOUT).toLong(), TimeUnit.MILLISECONDS)
            .readTimeout((READ_TIMEOUT).toLong(), TimeUnit.MILLISECONDS)
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
    fun with(lifecycleOwner: LifecycleOwner): RequestManager {
        val manager = map[lifecycleOwner]
        if (manager != null)
            return manager

        val loaderManager = RequestManager(this, io, main)
        map[lifecycleOwner] = loaderManager

        lifecycleOwner.lifecycle.addObserver(this)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            loaderManager.paused = false
        }

        return loaderManager
    }

    internal fun getDecoder(type: String): Decoder {
        val get = decoderFactoryMap[type] ?: return DefaultDecoder(context.resources, bitmapPool)
        return get.create()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
//            Lifecycle.Event.ON_RESUME -> onResume(source)
//            Lifecycle.Event.ON_PAUSE -> onPause(source)
            Lifecycle.Event.ON_DESTROY -> onDestroy(source)
            else -> doNothing()
        }
    }

    private fun doNothing() {

    }

    /*
    private fun onResume(source: LifecycleOwner) {
//        val loaderManager = map[source]
//        loaderManager?.resume()
    }

    private fun onPause(source: LifecycleOwner) {
//        map[source]?.pause()
    }
     */
    private fun onDestroy(source: LifecycleOwner) {
        map[source]?.release()
        map.remove(source)
    }

    internal fun recycleRequest(request: Request) {
        Log.i(TAG, "try to recycle request 1")
        request.recycle(memoryCache)
    }

    @WorkerThread
    internal fun recycleRequest() {
        while (true) {
            val poll = requestQueue.poll() ?: break

            synchronized(requestQueue) {
                val request = poll as Request
                Log.i(TAG, "try to recycle request 2")
                if (request.recyclable) {
                    if (request.drawable != null) {
                        request.recycle(memoryCache)
                    }
                }
            }
        }
    }
}


class RequestManager(
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
    }

    internal fun onStartRequestOnly(request: Request, imageView: ImageView) {
        request.job = GlobalScope.launch(io) {
            try {
                val drawable = request.load(this)

                launch(main) {
                    request.onLoaded(drawable, true)
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

    internal fun onRequest(request: Request, imageView: ImageView) {
        val previousRequest = requestMap[imageView]
        if (previousRequest != null && previousRequest == request) {
            return
        }

        if (previousRequest != request) {
            previousRequest?.cancel()
        }

        requestMap[imageView] = request

        val drawable = request.loadFromMemoryCache()
        if (drawable != null) {
            request.onLoaded(drawable, false)
            return
        }

        request.onLoading()

        onStartRequestOnly(request, imageView)
    }

}

class RequestBuilder(private val manager: RequestManager) {
    private lateinit var url: String
    private var placeholder: Int = 0
    private var errorResId: Int = 0
    private var diskCacheEnabled = true
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
    private val manager: RequestManager,
    internal val source: Source,
    private val placeholder: Int,
    private val errorResId: Int,
    private val transition: Transition?,
    target: ImageView,
    private val diskCacheEnabled: Boolean,
    private val cornerRadius: Float
) : WeakReference<Any>(target, manager.easyLoader.requestQueue),
    ViewTreeObserver.OnPreDrawListener {

    var ref: WeakReference<ImageView> = WeakReference(target)
    private val hash = source.hashCode() + target.hashCode()
    var job: Job? = null
    private var completed = false

    @Volatile
    var drawable: Drawable? = null
    private var previousDrawable: Drawable? = null
    private var targetW = -1
    private var targetH = -1
    private var listening = false

    internal val recyclable: Boolean
        get() {
            return ref.get() == null
        }

    @MainThread
    fun start() {
        val target: ImageView = ref.get()!!
        val layoutParams = target.layoutParams ?: return

        val view = ref.get()!!
        if (cornerRadius > 0) {
            view.clipToOutline = true
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
        }

        targetW = layoutParams.width
        targetH = layoutParams.height

        val valid: Boolean = targetW != ViewGroup.LayoutParams.MATCH_PARENT && targetH != ViewGroup.LayoutParams.MATCH_PARENT && targetW != 0 && targetH != 0

        if (!valid) {
            listening = true
            target.viewTreeObserver.addOnPreDrawListener(this)
        } else {
            manager.onRequest(this, target)
        }
    }

    @MainThread
    fun cancel() {
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

        if (drawable != null) {
            manager.easyLoader.recycleRequest(this)
        }
    }

    internal fun recycle(memoryCache: MemoryCache) {
        if (drawable != null) {
            val cacheKey = source.getCacheKey()
            val memoryCacheKey = "${cacheKey}-${targetW}-${targetH}"
//            drawable!!.setVisible(false, false)
            memoryCache.put(memoryCacheKey, drawable!!)
        }
    }

    fun loadFromMemoryCache(): Drawable? {
        manager.easyLoader.recycleRequest()
        val cacheKey = source.getCacheKey()
        val memoryCacheKey = "${cacheKey}-${targetW}-${targetH}"
        val memoryCache = manager.easyLoader.memoryCache
        val d = memoryCache.get(memoryCacheKey)
        if (d != null) {
            Log.i(TAG, "memory cache hit on main thread")
//            d.setVisible(true, true)
            drawable = d
            return drawable!!
        }

        return null
    }


    @WorkerThread
    fun load(coroutineScope: CoroutineScope): Drawable {
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

        var cacheKey = source.getCacheKey()
        //load from memory cache
        val memoryCacheKey = "${cacheKey}-${targetW}-${targetH}"
        val memoryCache = manager.easyLoader.memoryCache
        val d = memoryCache.get(memoryCacheKey)
        if (d != null) {
            Log.i(TAG, "memory cache hit")
            drawable = d
            return drawable!!
        }

        checkCancel(coroutineScope)

        cacheKey = generateName(cacheKey)
        //load from disk cache
        val bytes: ByteArray = if (diskCacheEnabled) {

            val diskCache = manager.easyLoader.diskCache
            val cacheFile = diskCache.get(cacheKey)
            if (cacheFile != null) {
                Log.i(TAG, "disk cache hit")
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
        val type = TypeDetector.detect(bytes)
        Log.i(TAG, "image type detected $type")
        val decoder = manager.easyLoader.getDecoder(type)
        drawable = decoder.decode(bytes, targetW, targetH)
        if (drawable == null) {
            throw IOException()
        }

        return drawable!!
    }

    @MainThread
    fun onLoading() {
        if (placeholder > 0) {
            val get = ref.get()
            get?.setImageResource(placeholder)
            previousDrawable = get?.drawable
        }
    }

    @MainThread
    fun onLoaded(drawable: Drawable, transit: Boolean) {
        completed = true
        val view = ref.get() ?: return

        drawable.setVisible(true, true)

        view.setImageDrawable(drawable)
        if (transit)
            transition?.onAfter(view, drawable)
    }

    @MainThread
    fun onError(e: Exception) {
        e.printStackTrace()
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

        if (target.width > 0)
            targetW = target.width
        if (target.height > 0)
            targetH = target.height

        val viewTreeObserver = target.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(this)
        }

        val valid = (targetW > 0 || targetW == ViewGroup.LayoutParams.WRAP_CONTENT) && (targetH > 0 || targetH == ViewGroup.LayoutParams.WRAP_CONTENT)
        if (valid) {
            manager.onRequest(this, target)
        }
        return false
    }
}

internal class LIFOExecutor(private val executorService: ExecutorService) : Executor {
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

            for (run: Runnable in pausedCommandList) {
                execute(run)
            }

            pausedCommandList.clear()
        }
    }
}


internal fun generateName(imageUri: String): String {
    val md5 = getMD5(imageUri.toByteArray())
    val bi = BigInteger(md5).abs()
    return bi.toString(10 + 26)
}

private fun getMD5(data: ByteArray): ByteArray? {
    var hash: ByteArray? = null
    try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(data)
        hash = digest.digest()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }
    return hash
}


