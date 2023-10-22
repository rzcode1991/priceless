package com.example.priceless

data class FollowRequest(var senderUserID: String,
                         var receiverUserID: String,
                         var accepted: Boolean) {
}