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
            "file:///sdcard/test/test.jpeg",
            "file:///sdcard/test/test7.gif",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201611%2F04%2F20161104110413_XzVAk.thumb.700_0.gif&refer=http%3A%2F%2Fb-ssl.duitang.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621752418&t=e2f8cbf44ad8616d6c4bf936b5713400",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fi2.sinaimg.cn%2Fty%2Fk%2Fp%2F2009-10-28%2FU3144P6T12D4667860F168DT20091028123558.jpg&refer=http%3A%2F%2Fi2.sinaimg.cn&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996238&t=df3110fb17db54c3d030395c8f509091",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fimage-7.verycd.com%2Fccb3f249509bcaac82dd4cb6ef5a7257104641%2F5.jpg&refer=http%3A%2F%2Fimage-7.verycd.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996264&t=e1870dbccce3514561131144953e53d2",
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fycyima.yccar.com%2FUploadfile%2Fother%2F2011%2F01%2F11%2F106612718.jpg&refer=http%3A%2F%2Fycyima.yccar.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1621996282&t=e7c986421df6dceb70ab4e8b688ce2bf"
        )
        val elapsedRealtime = SystemClock.elapsedRealtime()

        val nextInt = elapsedRealtime % 6

        EasyImage
            .with(activity)
            .load(arrayOf[nextInt.toInt()])
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

