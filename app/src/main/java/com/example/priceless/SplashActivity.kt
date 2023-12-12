package com.example.priceless

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        CoroutineScope(Dispatchers.Main).launch {
            delay(2500)
            val currentUserID = FireStoreClass().getUserID()
            if (currentUserID.isNotEmpty()){
                startActivity(Intent(this@SplashActivity, FragmentActivity::class.java))
            }else{
                val intent = Intent(this@SplashActivity, LogIn::class.java)
                startActivity(intent)
            }
            finish()
        }

    }
}