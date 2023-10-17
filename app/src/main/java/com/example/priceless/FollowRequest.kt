package com.example.priceless

data class FollowRequest(var senderUserID: String,
                         var senderUserName: String,
                         var receiverUserID: String,
                         var receiverUserName: String,
                         var accepted: Boolean) {
}