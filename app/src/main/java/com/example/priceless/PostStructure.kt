package com.example.priceless

data class PostStructure(var profilePicture: String = "",
                         var userId: String = "",
                         var userName: String = "",
                         var postText: String = "",
                         var postImage: String = "",
                         var timeCreated: String = "",
                         var visibility: Boolean = true,
                         var timeToShare: String = "now") {
}