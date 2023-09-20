package com.example.priceless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class FullScreenPostImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_post_image)

        supportActionBar?.hide()

        if (intent.hasExtra("post_image")){
            val postImage = intent.getStringExtra("post_image")
            val ivFullScreenPostImage: ImageView = findViewById(R.id.iv_full_screen_post_image)
            if (postImage != null){
                GlideLoader(this).loadImageUri(postImage, ivFullScreenPostImage)
            }
        }
    }
}