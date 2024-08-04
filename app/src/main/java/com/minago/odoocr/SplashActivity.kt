package com.minago.odoocr

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.Animation

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val titleTextView: TextView = findViewById(R.id.titleTextView)

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        val animationSet = android.view.animation.AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(slideUp)
            duration = 1500 // 1.5 seconds for the animation
        }

        titleTextView.startAnimation(animationSet)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, EnterInvoiceActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }
}