package com.example.komiscope.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.komiscope.databinding.ActivitySplashBinding
import com.example.komiscope.ui.main.MainActivity

/**
 * Splash screen displayed when app launches
 * Shows app logo and transitions to MainActivity
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for full screen splash
        supportActionBar?.hide()

        // Navigate to MainActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startMainActivity()
        }, SPLASH_DELAY)
    }

    /**
     * Navigate to MainActivity and finish splash
     */
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()

        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}