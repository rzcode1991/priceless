package com.example.priceless

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentStructure(var text: String = "",
                            var commentPhoto: String = "",
                            var timeCreated: String = "",
                            var timeCreatedToShow: String = "",
                            var postID: String = "",
                            var postOwnerUID: String = "",
                            var writerUID: String = "",
                            var writerUserName: String = "",
                            var writerProfilePic: String = "",
                            var isPrivate: Boolean = false,
                            var edited: Boolean = false,
                            var commentID: String = ""): Parcelable