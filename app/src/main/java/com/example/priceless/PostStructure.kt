package com.example.priceless

data class PostStructure(val profilePicture: String,
                         val userId: String,
                         val userName: String,
                         val postText: String,
                         val timeCreated: String,
                         val visibility: Boolean,
                         val timeToShare: String,
                         val postNumber: Int) {
}