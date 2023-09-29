package com.example.priceless

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostStructure(var profilePicture: String = "",
                         var userId: String = "",
                         var userName: String = "",
                         var postText: String = "",
                         var postImage: String = "",
                         var timeCreatedMillis: String = "",
                         var timeCreatedToShow: String = "",
                         var timeToShare: String = "now",
                         var visibility: Boolean = true,
                         var postID: String = "",
                         var edited: Boolean = false): Parcelable