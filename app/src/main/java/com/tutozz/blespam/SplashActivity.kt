package com.tutozz.blespam

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val gifImageView: ImageView = findViewById(R.id.gifImageView)
        val versionTextView: TextView = findViewById(R.id.versionTextView)

        // Загружаем и отображаем GIF
        Glide.with(this)
            .asGif()
            .load(R.drawable.anim)
            .into(gifImageView)

        val version = "v2.3 by ars2nb"
        versionTextView.text = version

        Handler().postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1400)
    }
}
