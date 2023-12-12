package com.example.priceless

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.util.*

object Constants {
    const val USERS: String = "users"
    const val User_Extra_Details: String = "User_Extra_Details"
    const val PermissionExternalStorageCode = 2
    const val ImageIntentCode = 4
    const val PhoneNumber = "phoneNumber"
    const val Image = "image"
    const val ProfileCompleted = "profileCompleted"
    const val FirstName = "firstName"
    const val LastName = "lastName"
    const val UserName = "userName"
    const val UserNames = "usernames"
    const val Posts = "posts"


    val sortedPosts = TreeMap<String, PostStructure>()

    fun showImageFromStorage(activity: Activity){
        val imageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(imageIntent, ImageIntentCode)
    }

    fun showImageFromStorageForRecyclerView(activity: Activity){
        val imageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(imageIntent, 400)
    }


    fun getExtensionFromFile(activity: Activity, imageUri: Uri): String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(activity.contentResolver.getType(imageUri))
    }

}