package com.example.priceless

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.IOException

class GlideLoader(val context: Context){
    fun loadImageUri(imageUri: Any, imageView: ImageView){
        try {
            Glide.with(context).load(imageUri).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.empty_profile_pic).into(imageView)
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun loadImageUriFitCenter(imageUri: Any, imageView: ImageView){
        try {
            Glide.with(context).load(imageUri).fitCenter().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.empty_profile_pic).into(imageView)
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun loadImageUriCircleCrop(imageUri: Any, imageView: ImageView){
        try {
            Glide.with(context).load(imageUri).circleCrop().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.empty_profile_pic).into(imageView)
        }catch (e: IOException){
            e.printStackTrace()
        }
    }


}