package com.examples.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.R
class Splash_Screen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if it's the first time opening the app
        if (!PrefManager.isFirstTime(this)) {
            // Skip Splash_Screen and go directly to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close the Splash_Screen
        } else {
            // If it's the first time, show the splash screen layout
            setContentView(R.layout.activity_splash_screen)

            Handler(Looper.getMainLooper()).postDelayed({
                // After showing the splash screen, go to StartActivity
                val intent = Intent(this, StartActivity::class.java)
                startActivity(intent)
                finish() // Close the Splash_Screen activity
            }, 2000) // 2 second delay
        }
    }
}

