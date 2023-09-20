package com.example.priceless

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostStructure(var profilePicture: String = "",
                         var userId: String = "",
                         var userName: String = "",
                         var postText: String = "",
                         var postImage: String = "",
                         var timeCreated: String = "",
                         var visibility: Boolean = true,
                         var timeToShare: String = "now",
                         var postID: String = "",
                         var edited: Boolean = false): Parcelable