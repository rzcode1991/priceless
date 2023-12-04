package com.example.priceless

data class FollowRequest(var senderUserID: String = "",
                         var senderUserName: String = "",
                         var senderProfilePic: String = "",
                         var receiverUserID: String = "",
                         var accepted: Boolean = false) {
}