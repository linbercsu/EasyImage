package com.next.easyloader

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import kotlin.random.Random

class MyAdapter(private val activity: AppCompatActivity) : RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val arrayOf = arrayOf(
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201611%2F04%2F20161104110413_XzVAk.thumb.700_0.gif&refer=http%3A%2F%2Fb-ssl.duitang.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621752418&t=e2f8cbf44ad8616d6c4bf936b5713400",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fc-ssl.duitang.com%2Fuploads%2Fitem%2F201201%2F06%2F20120106163751_hFXjw.jpg&refer=http%3A%2F%2Fc-ssl.duitang.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621998956&t=16bc91971e7ed0cc70f130397778a63c",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fi2.sinaimg.cn%2Fty%2Fk%2Fp%2F2009-10-28%2FU3144P6T12D4667860F168DT20091028123558.jpg&refer=http%3A%2F%2Fi2.sinaimg.cn&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996238&t=df3110fb17db54c3d030395c8f509091",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fattach.bbs.miui.com%2Fforum%2F201407%2F16%2F112213a5pttpy167sttw00.jpeg&refer=http%3A%2F%2Fattach.bbs.miui.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622005896&t=d93254d244d211686e63d52cc3e42682",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fhbimg.b0.upaiyun.com%2F604ee06b87644053f29b4e2674f09a15beb2bcfa275e2-BqiFyV_fw658&refer=http%3A%2F%2Fhbimg.b0.upaiyun.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622001775&t=64531c37111896b83473dad700aca6fa",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimage-7.verycd.com%2Fccb3f249509bcaac82dd4cb6ef5a7257104641%2F5.jpg&refer=http%3A%2F%2Fimage-7.verycd.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996264&t=e1870dbccce3514561131144953e53d2",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fhbimg.b0.upaiyun.com%2Fc670c39bb3d259b835f621fbeddd166e94233121b677d-GLh4mQ_fw658&refer=http%3A%2F%2Fhbimg.b0.upaiyun.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622005924&t=63f5548fdb3e41af564e5ee763ce6331",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fycyima.yccar.com%2FUploadfile%2Fother%2F2011%2F01%2F11%2F106612718.jpg&refer=http%3A%2F%2Fycyima.yccar.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996282&t=e7c986421df6dceb70ab4e8b688ce2bf",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2F5b0988e595225.cdn.sohucs.com%2Fimages%2F20180510%2Fc861c0e9509546f98c25ef09419f1b81.gif&refer=http%3A%2F%2F5b0988e595225.cdn.sohucs.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621998484&t=aedb85b4a7a7199419f0914da9dcf903",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fbenyouhuifile.it168.com%2Fforum%2Fday_100124%2F20100124_9709b2f5aa84728f755cmxD7h4CyWGcu.jpeg&refer=http%3A%2F%2Fbenyouhuifile.it168.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622005908&t=09fd904555faf9082bd01d23bb478170",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fwww.17qq.com%2Fimg_biaoqing%2F82468534.jpeg&refer=http%3A%2F%2Fwww.17qq.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621998526&t=86af106a88f814b7c1f7448cf6ac90d0",
            "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3883829124,816187355&fm=26&gp=0.jpg",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fcdn.lizhi.fm%2Faudio_cover%2F2015%2F12%2F17%2F25069069931308679.jpg&refer=http%3A%2F%2Fcdn.lizhi.fm&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622005936&t=1cf7ac8dc263ede343bc21235b8ce494",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fwww.17qq.com%2Fimg_biaoqing%2F22426019.jpeg&refer=http%3A%2F%2Fwww.17qq.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621998551&t=57129c8e0b2b87428393fe0f094e34e6",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fblog%2F201401%2F08%2F20140108195435_5meu3.thumb.700_0.gif&refer=http%3A%2F%2Fb-ssl.duitang.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1622002388&t=ae50a1fcbc990df1d5c1cbd0c7cbe968"
        )

        val nextInt = position % arrayOf.size

        EasyImage
            .with(activity)
            .load(arrayOf[nextInt])
            .placeholder(R.drawable.dark_mode_icon)
            .error(R.drawable.dailycheck_dialog)
            .fadeIn()
            .rounded(20f)
            .into(holder.image)



    }

    override fun getItemCount(): Int {
        return 1000
    }

}

class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image: ImageView = itemView.findViewById(R.id.image)
}

class MainActivity : AppCompatActivity() {
    val referenceQueue: ReferenceQueue<Any> = ReferenceQueue()
    var ref: WeakReference<Any>? = null
    var imageView: ImageView? = null
    private lateinit var recyclerView: RecyclerView
    companion object {
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.list)
        val linearLayoutManager = LinearLayoutManager(this)

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = MyAdapter(this)

        imageView = ImageView(this)


        ref = WeakReference(imageView!!, referenceQueue)
//        val imageView = findViewById<ImageView>(R.id.image_view)
//        val imageView2 = findViewById<ImageView>(R.id.image_view2)
//        val imageView3 = findViewById<ImageView>(R.id.image_view3)
//        val imageView4 = findViewById<ImageView>(R.id.image_view4)
        /*
        EasyImage
            .with(this)
            .load("file:///sdcard/test/test.jpeg")
            .placeholder(R.drawable.dark_mode_icon)
            .error(R.drawable.dailycheck_dialog)
            .fadeIn()
            .rounded(20f)
            .into(imageView)

        EasyImage
            .with(this)
            .load("http://5b0988e595225.cdn.sohucs.com/images/20180723/36df6793a0084d3abc93aa7308bbd31e.gif")
            .placeholder(R.drawable.dark_mode_icon)
            .error(R.drawable.dailycheck_dialog)
            .fadeIn()
            .rounded(20f)
            .into(imageView2)

         */
//        EasyLoader
//            .with(this)
//            .load("file:///sdcard/test/test.gif")
//            .into(imageView3)
//
//        EasyLoader
//            .with(this)
//            .load("file:///sdcard/test/test.gif")
//            .into(imageView4)

//        val findViewById = findViewById<TextView>(R.id.text)
//        t.initialize(findViewById)
//        println(t())
//        println(word)
//
//        name = "hello"

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        val view = binding.root
//        setContentView(view)

//        setContentView(R.layout.activity_main)


//        job = GlobalScope.launch {
//
//            yield()
//            Log.e("test", "global scope run ${Thread.currentThread().id}")
//
//            launch(Dispatchers.Main) {
//                Log.e("test", "global scope 1 run ${Thread.currentThread().id}")
//            }
////            delay(5000)
//            Log.e("test", "global scope run2 ${Thread.currentThread().id}")
//        }
//
//        val launch = scope.launch {
//            Log.e("test", "scope run")
//        }
//
//        launch.isActive

        Log.e("test", "onCreate run")
    }

    override fun onStart() {
        super.onStart()

//        if (this::name.isInitialized) {
//            println(this.name)
//        }
//
//        println(name)

        Log.e("test", "onStart run")

    }

    override fun onStop() {
        super.onStop()

        imageView = null

        val get = ref?.get()
        Log.e("test", "get $get")
        while (true) {
            val poll = referenceQueue.poll()
            if (poll == null)
                break

            Log.e("test", "recycle")
        }
    }

    override fun onResume() {
        super.onResume()

        Log.e("test", "onResume run")
//        job?.cancel()
    }
}

