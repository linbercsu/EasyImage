package com.next.easyloader

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    companion object {
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.image_view)
        val imageView2 = findViewById<ImageView>(R.id.image_view2)
        val imageView3 = findViewById<ImageView>(R.id.image_view3)
        val imageView4 = findViewById<ImageView>(R.id.image_view4)
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

    override fun onResume() {
        super.onResume()

        Log.e("test", "onResume run")
//        job?.cancel()
    }
}

