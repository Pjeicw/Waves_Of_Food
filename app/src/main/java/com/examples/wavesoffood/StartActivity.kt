package com.examples.wavesoffood

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private val binding: ActivityStartBinding by lazy {
        ActivityStartBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.nextButton.setOnClickListener {
            // Mark first-time flag as false after the user presses the button
            PrefManager.setFirstTime(this, false)

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close StartActivity so it can't be revisited
        }
    }
}
