package com.example.priceless

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FireStoreListenerService : Service() {

    private var fireStoreClass: FireStoreClass? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        fireStoreClass = FireStoreClass()
        fireStoreClass?.getCurrentUserID { userID ->
            if (userID.isNotEmpty()){
                startRequestsListener(userID)
                startSoldPostsListener(userID)
                startCommentsListener(userID)
                startRepliesOfCurrentUserCommentsOnOtherUsersPostsListener(userID)
            }
        }
    }

    private fun startRequestsListener(userID: String) {
        val savedNotifications = ArrayList<Int>()
        fireStoreClass?.getReceivedRequestsRealTime(userID) { requests ->
            if (!requests.isNullOrEmpty()) {
                val followRequests = ArrayList<FollowRequest>()
                followRequests.addAll(requests)

                CoroutineScope(Dispatchers.Main).launch {
                    val userGroupedRequests = followRequests.groupBy { it.senderUserID }
                    val userInfoJobs = userGroupedRequests.map { (userId, groupRequests) ->
                        async {
                            val deferredUserInfo = CompletableDeferred<User?>()
                            fireStoreClass?.getUserInfoWithCallback(userId) { userInfo ->
                                deferredUserInfo.complete(userInfo)
                            }
                            val userInfo = deferredUserInfo.await()
                            if (userInfo != null) {
                                groupRequests.forEach { requestItem ->
                                    requestItem.senderProfilePic = userInfo.image
                                    requestItem.senderUserName = userInfo.userName
                                }
                            }
                        }
                    }
                    userInfoJobs.awaitAll()

                    for (notifID in savedNotifications){
                        clearNotifications(notifID)
                    }
                    for (request in followRequests) {
                        if (!request.accepted) {
                            val currentTime = System.currentTimeMillis().toInt()-1000000
                            val notificationID = request.senderUserID.hashCode()+currentTime
                            if (notificationID !in savedNotifications){
                                sendNotification("Follow Request",
                                    "From: ${request.senderUserName}",
                                    notificationID,
                                    request.senderUserID,
                                    null,
                                    null,
                                    UserProfileActivity::class.java)
                                savedNotifications.add(notificationID)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startSoldPostsListener(userID: String){
        fireStoreClass?.getSoldPostsListener(userID) { posts ->
            if (!posts.isNullOrEmpty()){
                val soldPosts = ArrayList<PostStructure>()
                soldPosts.addAll(posts)
                for (postItem in soldPosts){
                    fireStoreClass?.soldPostNotifExists(userID, postItem.postID) { yep ->
                        if (!yep){
                            sendNotification("Post Sold",
                                "You Sold a New Post",
                                postItem.postID.hashCode(),
                                null,
                                null,
                                null,
                                SoldPostsActivity::class.java)
                            fireStoreClass?.saveSoldPostNotif(userID, postItem.postID)
                        }
                    }
                }
            }
        }
    }

    private fun startCommentsListener(userID: String){
        fireStoreClass?.getPostsRealTimeListener(userID) { posts ->
            if (!posts.isNullOrEmpty()){
                for (post in posts){
                    listenToPublicComments(post)
                    listenToPrivateComments(post)
                }
            }
        }
    }

    private fun listenToPublicComments(post: PostStructure){
        fireStoreClass?.publicCommentsListener(post) { publicComments ->
            if (!publicComments.isNullOrEmpty()){
                for (commentItem in publicComments){
                    if (commentItem.writerUID != post.userId){
                        createNotificationForPublicComment(post, commentItem)
                    }
                    fireStoreClass?.getPublicRepliesListener(commentItem) { replies ->
                        if (!replies.isNullOrEmpty()){
                            for (reply in replies){
                                if (reply.writerUID != post.userId){
                                    createNotificationForPublicReply(post.userId, commentItem, reply)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationForPublicComment(post: PostStructure, commentItem: CommentStructure){
        fireStoreClass?.commentNotifExists(post.userId, commentItem.commentID) { yep ->
            if (!yep){
                sendNotification("New Public Comment",
                    "",
                    commentItem.commentID.hashCode(),
                    null,
                    post,
                    null,
                    CommentsActivity::class.java)
                fireStoreClass?.saveCommentNotif(post.userId, commentItem.commentID)
            }
        }
    }

    private fun createNotificationForPublicReply(userID: String, comment: CommentStructure,
                                                 reply: CommentStructure){
        fireStoreClass?.commentNotifExists(userID, reply.commentID) { yep ->
            if (!yep){
                sendNotification("New Public Reply",
                    "",
                    reply.commentID.hashCode(),
                    null,
                    null,
                    comment,
                    ReplyCommentActivity::class.java)
                fireStoreClass?.saveCommentNotif(userID, reply.commentID)
            }
        }
    }

    private fun createNotificationForPublicReplyForCommentOnOtherUsersPosts(userID: String,
                                                                            comment: CommentStructure,
                                                                            reply: CommentStructure){
        fireStoreClass?.commentNotifExists(userID, reply.commentID) { yep ->
            if (!yep){
                sendNotification("New Public Reply To Your Comment",
                    "",
                    reply.commentID.hashCode(),
                    null,
                    null,
                    comment,
                    ReplyCommentActivity::class.java)
                fireStoreClass?.saveCommentNotif(userID, reply.commentID)
            }
        }
    }

    private fun createNotificationForPrivateReply(userID: String, comment: CommentStructure, reply: CommentStructure){
        fireStoreClass?.commentNotifExists(userID, reply.commentID) { yep ->
            if (!yep){
                sendNotification("New Private Reply",
                    "",
                    reply.commentID.hashCode(),
                    null,
                    null,
                    comment,
                    ReplyCommentActivity::class.java)
                fireStoreClass?.saveCommentNotif(userID, reply.commentID)
            }
        }
    }

    private fun createNotificationForPrivateReplyForCommentOnOtherUsersPosts(userID: String,
                                                                                comment: CommentStructure,
                                                                                reply: CommentStructure){
        fireStoreClass?.commentNotifExists(userID, reply.commentID) { yep ->
            if (!yep){
                sendNotification("New Private Reply To Your Comment",
                    "",
                    reply.commentID.hashCode(),
                    null,
                    null,
                    comment,
                    ReplyCommentActivity::class.java)
                fireStoreClass?.saveCommentNotif(userID, reply.commentID)
            }
        }
    }

    private fun listenToPrivateComments(post: PostStructure){
        fireStoreClass?.getUIDsOfPrivateCommentsListener(post.postID, post.userId) { uIDs ->
            if (!uIDs.isNullOrEmpty()){
                for (userID in uIDs){
                    fireStoreClass?.getPrivateCommentsForPostOwnerListener(post.postID, post.userId,
                        userID) { privateComments ->
                        if (!privateComments.isNullOrEmpty()){
                            for (comment in privateComments){
                                if (comment.writerUID != post.userId){
                                    createNotificationForPrivateComment(post, comment)
                                }
                                fireStoreClass?.getPrivateRepliesListener(comment) { replies ->
                                    if (!replies.isNullOrEmpty()){
                                        for (reply in replies){
                                            if (reply.writerUID != post.userId){
                                                createNotificationForPrivateReply(post.userId, comment, reply)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationForPrivateComment(post: PostStructure, commentItem: CommentStructure){
        fireStoreClass?.commentNotifExists(post.userId, commentItem.commentID) { yep ->
            if (!yep){
                sendNotification("New Private Comment",
                    "",
                    commentItem.commentID.hashCode(),
                    null,
                    post,
                    null,
                    CommentsActivity::class.java)
                fireStoreClass?.saveCommentNotif(post.userId, commentItem.commentID)
            }
        }
    }

    private fun startRepliesOfCurrentUserCommentsOnOtherUsersPostsListener(userID: String){
        fireStoreClass?.getAwayCommentsInfoListener(userID) { awayCommentsInfoList ->
            if (!awayCommentsInfoList.isNullOrEmpty()){
                for (commentInfo in awayCommentsInfoList){
                    val commentID = commentInfo["commentID"] as String
                    val isPrivate = commentInfo["isPrivate"] as Boolean
                    val postID = commentInfo["postID"] as String
                    val postOwnerUID = commentInfo["postOwnerUID"] as String
                    if (isPrivate){
                        fireStoreClass?.getSinglePrivateCommentForViewerUserListener(postID,
                            postOwnerUID, commentID, userID) { topComment ->
                            if (topComment != null){
                                fireStoreClass?.getPrivateRepliesListener2(postOwnerUID, postID,
                                    userID, commentID) { privateReplies ->
                                    if (!privateReplies.isNullOrEmpty()){
                                        for (reply in privateReplies){
                                            if (reply.writerUID != userID){
                                                createNotificationForPrivateReplyForCommentOnOtherUsersPosts(userID,
                                                    topComment, reply)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else{
                        fireStoreClass?.getSinglePublicCommentListener(postOwnerUID, postID,
                            commentID) { topComment ->
                            if (topComment != null){
                                fireStoreClass?.getPublicRepliesListener2(topComment) { publicReplies ->
                                    if (!publicReplies.isNullOrEmpty()){
                                        for (reply in publicReplies){
                                            if (reply.writerUID != userID){
                                                createNotificationForPublicReplyForCommentOnOtherUsersPosts(userID,
                                                    topComment, reply)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String, notificationId: Int,
                                 stringExtra: String?,
                                 postExtra: PostStructure?,
                                 commentExtra: CommentStructure?,
                                 targetActivity: Class<*>?) {
        val intent = Intent(this, targetActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (stringExtra != null){
            intent.putExtra("userID", stringExtra)
        }
        if (postExtra != null){
            intent.putExtra("post", postExtra)
        }
        if (commentExtra != null){
            intent.putExtra("com.example.priceless.comment", commentExtra)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }else{
            PendingIntent.getActivity(
                this, notificationId, intent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }

        val channelId = "Your_Channel_ID"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_baseline_account_circle_24)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    private fun clearNotifications(notificationId: Int) {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.cancel(notificationId)
    }

    override fun onDestroy() {
        super.onDestroy()
        fireStoreClass?.stopRequestListener()
        fireStoreClass?.stopSoldPostsListener()
        fireStoreClass?.stopUserPostsListener()
        fireStoreClass?.stopPublicCommentsListener()
        fireStoreClass?.stopListenToPublicReplies()
        fireStoreClass?.stopListenToUIDsOfPrivateComments()
        fireStoreClass?.stopListeningToPrivateCommentsForPostOwner()
        fireStoreClass?.stopListeningToPrivateReplies()
        fireStoreClass?.stopListenToAwayCommentsInfo()
        fireStoreClass?.stopListeningToPrivateReplies2()
        fireStoreClass?.stopListenToSinglePrivateCommentForViewerUser()
        fireStoreClass?.stopListenToSinglePublicComment()
        fireStoreClass?.stopListenToPublicReplies2()
    }
}
