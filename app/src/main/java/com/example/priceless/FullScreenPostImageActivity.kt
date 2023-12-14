package com.example.priceless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView

@Suppress("DEPRECATION")
class FullScreenPostImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_post_image)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        if (intent.hasExtra("post_image")){
            val postImage = intent.getStringExtra("post_image")
            val ivFullScreenPostImage: ImageView = findViewById(R.id.iv_full_screen_post_image)
            if (postImage != null){
                GlideLoader(this).loadImageUriFitCenter(postImage, ivFullScreenPostImage)
            }
        }
    }
}