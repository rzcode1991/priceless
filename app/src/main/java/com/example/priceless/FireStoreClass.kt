package com.example.priceless

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


class FireStoreClass {

    private val mFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    //private var userListenerRegistration: ListenerRegistration? = null


    fun registerUserInFireStore(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)
            .document(userInfo.id)
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                // User registration successful, now create the username
                createUserName(userInfo.userName, activity)
                //activity.registrationSuccessful()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "error while registering user on fireStore", e)
            }
    }


    fun amIFollowingThatUser(currentUserID: String, otherUserID: String, callback: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(currentUserID)
            .collection("following")
            .document(otherUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    callback(true)
                }else{
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("err checking if I follow that user", e.message.toString(), e)
            }
    }


    fun acceptFollowRequest(receiverUserID: String, senderUserID: String, callback: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(receiverUserID)
            .collection("followers")
            .document(senderUserID)
            .set(mapOf("userID" to senderUserID))
            .addOnSuccessListener {
                mFireStore.collection(Constants.USERS)
                    .document(senderUserID)
                    .collection("following")
                    .document(receiverUserID)
                    .set(mapOf("userID" to receiverUserID))
                    .addOnSuccessListener {
                        updateFollowRequest(receiverUserID, senderUserID){ success ->
                            if (success){
                                callback(true)
                            }else{
                                callback(false)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(false)
                        Log.e("err adding new following user", e.message.toString(), e)
                    }
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("err adding new follower", e.message.toString(), e)
            }
    }


    fun unfollowUser(currentUserID: String, otherUserID: String, callback: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(otherUserID)
            .collection("followers")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                mFireStore.collection(Constants.USERS)
                    .document(currentUserID)
                    .collection("following")
                    .document(otherUserID)
                    .delete()
                    .addOnSuccessListener {
                        callback(true)
                    }
                    .addOnFailureListener { e ->
                        callback(false)
                        Log.e("err unfollowing user", e.message.toString(), e)
                    }
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("err unfollowing user", e.message.toString(), e)
            }
    }


    suspend fun getFollowingList(userID: String): List<String>? = coroutineScope {
        return@coroutineScope try {
            val snapshot = mFireStore.collection(Constants.USERS)
                .document(userID)
                .collection("following")
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val followingList = ArrayList<String>()
                for (document in snapshot.documents) {
                    val uID = document.getString("userID")
                    uID?.let { followingList.add(it) }
                }
                followingList
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("err getting following list", e.message.toString(), e)
            null
        }
    }

    private var requestListener: ListenerRegistration? = null
    fun stopRequestListener(){
        requestListener?.remove()
    }

    fun getReceivedRequestsRealTime(userID: String, callback: (ArrayList<FollowRequest>?) -> Unit) {
        val requests = ArrayList<FollowRequest>()
        val requestRef = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("receivedRequests")

        requestListener = requestRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("error getReceivedRequestsRealTime", e.message.toString(), e)
                callback(null)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                for (document in snapshot.documents) {
                    val request = document.toObject(FollowRequest::class.java)
                    if (request != null) {
                        requests.add(request)
                    }
                }
                callback(requests)
                // Clear the requests list to ensure only new data is added next time
                requests.clear()
            } else {
                callback(null)
            }
        }
    }


    /*
    fun getReceivedRequests(userID: String, callback: (ArrayList<FollowRequest>?) -> Unit){
        val requests = ArrayList<FollowRequest>()
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("receivedRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    val request = document.toObject(FollowRequest::class.java)
                    if (request != null) {
                        requests.add(request)
                    }
                }
                callback(requests)
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("err getting received requests", e.message.toString(), e)
            }
    }

     */


    fun createFollowRequest(activity: Activity, request: FollowRequest){
        mFireStore.collection(Constants.USERS)
            .document(request.receiverUserID)
            .collection("receivedRequests")
            .document(request.senderUserID)
            .set(request, SetOptions.merge())
            .addOnSuccessListener {
                addRequestForSender(activity, request)
            }
            .addOnFailureListener { e ->
                when(activity){
                    is SearchActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(e.message.toString(), true)
                    }
                    is UserProfileActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(e.message.toString(), true)
                    }
                }
                Log.e(activity.javaClass.simpleName, "error creating follow request", e)
            }
    }


    private fun addRequestForSender(activity: Activity, request: FollowRequest){
        mFireStore.collection(Constants.USERS)
            .document(request.senderUserID)
            .collection("sentRequests")
            .document(request.receiverUserID)
            .set(request, SetOptions.merge())
            .addOnSuccessListener {
                when(activity){
                    is SearchActivity -> {
                        activity.createFollowRequestSuccessful()
                    }
                    is UserProfileActivity -> {
                        activity.createFollowRequestSuccessful()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is SearchActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(e.message.toString(), true)
                    }
                    is UserProfileActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(e.message.toString(), true)
                    }
                }
                Log.e(activity.javaClass.simpleName, "error creating follow request", e)
            }
    }


    private fun updateFollowRequest(receiverUserID: String, senderUserID: String, onSuccess: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(receiverUserID)
            .collection("receivedRequests")
            .document(senderUserID)
            .update("accepted", true)
            .addOnSuccessListener {
                mFireStore.collection(Constants.USERS)
                    .document(senderUserID)
                    .collection("sentRequests")
                    .document(receiverUserID)
                    .update("accepted", true)
                    .addOnSuccessListener {
                        onSuccess(true)
                    }
                    .addOnFailureListener { e ->
                        onSuccess(false)
                        Log.e("err updating follow request", e.message.toString(), e)
                    }
            }
            .addOnFailureListener { e ->
                onSuccess(false)
                Log.e("err updating follow request", e.message.toString(), e)
            }
    }


    fun deleteFollowRequest(currentUserID: String, otherUserID: String, callback: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(otherUserID)
            .collection("receivedRequests")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                mFireStore.collection(Constants.USERS)
                    .document(currentUserID)
                    .collection("sentRequests")
                    .document(otherUserID)
                    .delete()
                    .addOnSuccessListener {
                        callback(true)
                    }
                    .addOnFailureListener { e ->
                        callback(false)
                        Log.e("deleting followRequest failed", "error while deleting followRequest", e)
                    }
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("deleting followRequest failed", "error while deleting followRequest", e)
            }
    }


    fun checkFollowSituation(currentUserID: String, otherUserID: String, situation: (String) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(currentUserID)
            .collection("sentRequests")
            .document(otherUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    if (document.get("accepted") as Boolean){
                        situation("following")
                    }else{
                        situation("pending")
                    }
                }else{
                    // there is no request between two users
                    situation("follow")
                }
            }
            .addOnFailureListener { e ->
                Log.e("checkFollowSituation Err", "Error while checking follow situation", e)
                situation("")
            }
    }


    fun checkOtherUserFollowSituation(currentUserID: String, otherUserID: String, situation: (String) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(currentUserID)
            .collection("receivedRequests")
            .document(otherUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    if (document.get("accepted") as Boolean){
                        situation("FollowsYou")
                    }else{
                        situation("Pending")
                    }
                }else{
                    // there is no request between two users
                    situation("IsNotFollowing")
                }
            }
            .addOnFailureListener { e ->
                Log.e("checkFollowSituation Err", "Error while checking follow situation", e)
                situation("")
            }
    }

    fun saveCommentNotif(userID: String, commentID: String){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("commentsNotif")
            .add(mapOf("commentID" to commentID))
            .addOnSuccessListener {
                //
            }
            .addOnFailureListener { e ->
                //
                Log.e("error saveCommentNotif", e.message.toString(), e)
            }
    }

    fun commentNotifExists(userID: String, commentID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("commentsNotif")
            .whereEqualTo("commentID", commentID)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error commentNotifExists", e.message.toString(), e)
            }
    }

    fun saveSoldPostNotif(userID: String, postID: String){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("soldPostsNotif")
            .add(mapOf("postID" to postID))
            .addOnSuccessListener {
                //
            }
            .addOnFailureListener { e ->
                //
                Log.e("error saveSoldPostNotif", e.message.toString(), e)
            }
    }

    fun soldPostNotifExists(userID: String, postID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("soldPostsNotif")
            .whereEqualTo("postID", postID)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error soldPostNotifExists", e.message.toString(), e)
            }
    }

    fun saveTransaction(transactionHash: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection("transactions")
            .add(mapOf("transaction" to transactionHash))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error saveTransactions", e.message.toString(), e)
            }
    }

    fun transactionExists(transactionHash: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection("transactions")
            .whereEqualTo("transaction", transactionHash)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error transactionExists", e.message.toString(), e)
            }
    }

    fun createPostOnFireStore(activity: CreatePostActivity, userID: String, post: PostStructure) {
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(post.postID)
            .set(post)
            .addOnSuccessListener {
                activity.createPostSuccessful()
            }
            .addOnFailureListener { e ->
                activity.createPostFailed()
                Log.e(activity.javaClass.simpleName, "Error while creating post on FireStore", e)
            }

    }

    fun addPostToSoldPosts(userID: String, post: PostStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("soldPosts")
            .document(post.timeCreatedMillis)
            .set(post)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error addPostToSoldPosts", e.message.toString(), e)
            }
    }

    fun addPostToBoughtPosts(userID: String, post: PostStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("boughtPosts")
            .document(post.timeCreatedMillis)
            .set(post)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error addPostToBoughtPosts", e.message.toString(), e)
            }
    }

    fun likePrivateReply(reply: CommentStructure, currentUserID: String,
                         onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .set(mapOf("userID" to currentUserID))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error likePrivateReply", e.message.toString(), e)
            }
    }

    fun unlikePrivateReply(reply: CommentStructure, currentUserID: String,
                           onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun getLikeSituationForPrivateReply(reply: CommentStructure, currentUserID: String,
                                        onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error getLikeSituationForPrivateReply", e.message.toString(), e)
            }
    }

    fun getNumberOfLikesForPrivateReply(reply: CommentStructure, onComplete: (Int?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    onComplete(documents.size())
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getNumberOfLikesForPrivateReply", e.message.toString(), e)
            }
    }

    fun updatePrivateReply(reply: CommentStructure, commentHashMap: HashMap<String, Any>,
                           onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .update(commentHashMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun deletePrivateReply(reply: CommentStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deletePrivateReply", e.message.toString(), e)
            }
    }

    private var privateRepliesListenerRegistration: ListenerRegistration? = null
    fun stopListeningToPrivateReplies(){
        privateRepliesListenerRegistration?.remove()
    }
    fun getPrivateRepliesListener(comment: CommentStructure,
                                  onComplete: (ArrayList<CommentStructure>?) -> Unit){
        val repliesPath = mFireStore.collection(Constants.USERS)
            .document(comment.postOwnerUID)
            .collection(Constants.Posts)
            .document(comment.postID)
            .collection("UIDs")
            .document(comment.writerUID)
            .collection("privateComments")
            .document(comment.commentID)
            .collection("privateReplies")
        privateRepliesListenerRegistration = repliesPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getPrivateRepliesListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val replies = ArrayList<CommentStructure>()
                for (doc in snapshot.documents){
                    val reply = doc.toObject(CommentStructure::class.java)
                    if (reply != null){
                        replies.add(reply)
                    }
                }
                onComplete(replies)
                replies.clear()
            }else{
                onComplete(null)
            }
        }
    }

    private var privateRepliesListenerRegistration2: ListenerRegistration? = null
    fun stopListeningToPrivateReplies2(){
        privateRepliesListenerRegistration2?.remove()
    }
    fun getPrivateRepliesListener2(postOwnerUID: String, postID: String, writerOfCommentUID: String,
                                   commentID: String, onComplete: (ArrayList<CommentStructure>?) -> Unit){
        val repliesPath = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("privateReplies")
        privateRepliesListenerRegistration2 = repliesPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getPrivateRepliesListener2", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val replies = ArrayList<CommentStructure>()
                for (doc in snapshot.documents){
                    val reply = doc.toObject(CommentStructure::class.java)
                    if (reply != null){
                        replies.add(reply)
                    }
                }
                onComplete(replies)
                replies.clear()
            }else{
                onComplete(null)
            }
        }
    }

    fun getPrivateReplies(comment: CommentStructure, onComplete: (ArrayList<CommentStructure>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(comment.postOwnerUID)
            .collection(Constants.Posts)
            .document(comment.postID)
            .collection("UIDs")
            .document(comment.writerUID)
            .collection("privateComments")
            .document(comment.commentID)
            .collection("privateReplies")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty){
                    val replies = ArrayList<CommentStructure>()
                    for (i in docs){
                        replies.add(i.toObject(CommentStructure::class.java))
                    }
                    onComplete(replies)
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getPrivateReplies", e.message.toString(), e)
            }
    }

    fun addPrivateReply(reply: CommentStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("UIDs")
            .document(reply.writerOfTopCommentUID)
            .collection("privateComments")
            .document(reply.topCommentIDForReply)
            .collection("privateReplies")
            .document(reply.commentID)
            .set(reply)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error addPrivateReplyForPrivateComment", e.message.toString(), e)
            }
    }

    private var uIDsListenerRegistration: ListenerRegistration? = null
    fun stopListenToUIDsOfPrivateComments(){
        uIDsListenerRegistration?.remove()
    }
    fun getUIDsOfPrivateCommentsListener(postID: String, postOwnerUID: String,
                                         onComplete: (ArrayList<String>?) -> Unit){
        val uIDsPath = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
        uIDsListenerRegistration = uIDsPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getUIDsOfPrivateCommentsListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val uIDs = ArrayList<String>()
                for (doc in snapshot.documents){
                    uIDs.add(doc.id)
                }
                onComplete(uIDs)
                uIDs.clear()
            }else{
                onComplete(null)
            }
        }
    }

    fun getUIDsOfPrivateCommentsForPostOwner(postID: String, postOwnerUID: String,
                                             onComplete: (ArrayList<String>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    val uIDs = ArrayList<String>()
                    for (doc in documents){
                        uIDs.add(doc.id)
                    }
                    onComplete(uIDs)
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getUIDsOfPrivateCommentsForPostOwner", e.message.toString(), e)
            }
    }

    private var privateCommentsForPostOwnerRegistration: ListenerRegistration? = null
    fun stopListeningToPrivateCommentsForPostOwner(){
        privateCommentsForPostOwnerRegistration?.remove()
    }
    fun getPrivateCommentsForPostOwnerListener(postID: String, postOwnerUID: String, uID: String,
                                               onComplete: (ArrayList<CommentStructure>?) -> Unit){
        val commentsPath = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uID)
            .collection("privateComments")
        privateCommentsForPostOwnerRegistration = commentsPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getPrivateCommentsForPostOwnerListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val comments = ArrayList<CommentStructure>()
                for (doc in snapshot.documents){
                    val comment = doc.toObject(CommentStructure::class.java)
                    if (comment != null){
                        comments.add(comment)
                    }
                }
                onComplete(comments)
                comments.clear()
            }else{
                onComplete(null)
            }
        }
    }

    fun getPrivateCommentsFromUIDsForPostOwner(postID: String, postOwnerUID: String, uID: String,
                                               onComplete: (ArrayList<CommentStructure>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uID)
            .collection("privateComments")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty){
                    val comments = ArrayList<CommentStructure>()
                    for (i in docs){
                        comments.add(i.toObject(CommentStructure::class.java))
                    }
                    onComplete(comments)
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getting private comments for post owner", e.message.toString(), e)
            }
    }

    private var singlePrivateCommentForViewerUserRegistration: ListenerRegistration? = null
    fun stopListenToSinglePrivateCommentForViewerUser(){
        singlePrivateCommentForViewerUserRegistration?.remove()
    }
    fun getSinglePrivateCommentForViewerUserListener(postID: String, postOwnerUID: String,
                                                commentID: String, writerOfCommentUID: String,
                                        onComplete: (CommentStructure?) -> Unit){
        val singleCommentPath = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
        singlePrivateCommentForViewerUserRegistration = singleCommentPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getSinglePrivateCommentForViewerUserListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()){
                val comment = snapshot.toObject(CommentStructure::class.java)
                if (comment != null){
                    onComplete(comment)
                }
            }else{
                onComplete(null)
            }
        }
    }

    fun getPrivateCommentsForViewerUser(postID: String, postOwnerUID: String, currentUserID: String,
                           onComplete: (ArrayList<CommentStructure>?) -> Unit){
        val documentUID = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(currentUserID)

        documentUID.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()){
                    documentUID.collection("privateComments")
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty){
                                val comments = ArrayList<CommentStructure>()
                                for (doc in documents){
                                    comments.add(doc.toObject(CommentStructure::class.java))
                                }
                                onComplete(comments)
                            }else{
                                onComplete(null)
                            }
                        }
                        .addOnFailureListener { e ->
                            onComplete(null)
                            Log.e("error getting private comments level 2", e.message.toString(), e)
                        }
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getting private comments level 1", e.message.toString(), e)
            }
    }

    fun getNumberOfLikesForPrivateComment(postOwnerUID: String, postID: String, commentID: String,
                                          writerOfCommentUID: String, callback: (Int?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    callback(documents.size())
                }else{
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("error getting number of likes for private comment", e.message.toString(), e)
            }
    }

    fun getLikeSituationForPrivateComment(postOwnerUID: String, postID: String, commentID: String,
                                          writerOfCommentUID: String, currentUserID: String,
                                          onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error getting like situation for private comment", e.message.toString(), e)
            }
    }

    fun unLikePrivateComment(postOwnerUID: String, postID: String, commentID: String,
                           writerOfCommentUID: String, currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while UnLiking private comment", e.message.toString(), e)
            }
    }

    fun likePrivateComment(postOwnerUID: String, postID: String, commentID: String,
                           writerOfCommentUID: String, currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .set(mapOf("userID" to currentUserID))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while like private comment", e.message.toString(), e)
            }
    }

    fun deletePrivateComment(postOwnerUID: String, postID: String, writerOfCommentUID: String,
                             commentID: String, onComplete: (Boolean) -> Unit){

        val privateCommentsCollection = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")

        deleteAwayCommentInfo(writerOfCommentUID, commentID) { done ->
            if (done){
                deleteSubCollectionLikesOfPrivateComment(postOwnerUID, postID, writerOfCommentUID,
                    commentID) { successful ->
                    if (successful){
                        deleteSubCollectionRepliesOfPrivateComment(postOwnerUID, postID, writerOfCommentUID,
                            commentID) { successfully ->
                            if (successfully){
                                privateCommentsCollection.document(commentID)
                                    .delete()
                                    .addOnSuccessListener {
                                        privateCommentsCollection.get()
                                            .addOnSuccessListener { privateComments ->
                                                if (privateComments.isEmpty){
                                                    mFireStore.collection(Constants.USERS)
                                                        .document(postOwnerUID)
                                                        .collection(Constants.Posts)
                                                        .document(postID)
                                                        .collection("UIDs")
                                                        .document(writerOfCommentUID)
                                                        .delete()
                                                        .addOnSuccessListener {
                                                            onComplete(true)
                                                        }
                                                        .addOnFailureListener { e ->
                                                            onComplete(false)
                                                            Log.e("error deleting private comment", e.message.toString(), e)
                                                        }
                                                }else{
                                                    onComplete(true)
                                                }
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error deleting private comment", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }else{
                        onComplete(false)
                    }
                }
            }else{
                onComplete(false)
            }
        }
    }

    private fun deleteSubCollectionLikesOfPrivateComment(postOwnerUID: String, postID: String,
                                                         writerOfCommentUID: String, commentID: String,
                                                         onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteSubCollectionLikesOfPrivateComment", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionLikesOfPrivateComment", e.message.toString(), e)
            }
    }

    private fun deleteSubCollectionRepliesOfPrivateComment(postOwnerUID: String, postID: String,
                                                         writerOfCommentUID: String, commentID: String,
                                                           onComplete: (Boolean) -> Unit){
        val privateRepliesCollection = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("privateReplies")

            privateRepliesCollection.get()
            .addOnSuccessListener { replies ->
                if (!replies.isEmpty){
                    var repliesToDelete = replies.size()
                    for (reply in replies){
                        val replyItem = reply.toObject(CommentStructure::class.java)
                        deleteLikesOfSubCollectionRepliesOfPrivateComment(postOwnerUID, postID,
                            writerOfCommentUID, commentID, reply.id) { yep ->
                            if (yep){
                                reply.reference.delete()
                                    .addOnSuccessListener {
                                        if (replyItem.commentPhoto.isNotEmpty()){
                                            deleteImageFromCloudStorage(replyItem.commentPhoto) { onSuccess ->
                                                if (onSuccess){
                                                    repliesToDelete --
                                                    if (repliesToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }else{
                                                    onComplete(false)
                                                }
                                            }
                                        }else{
                                            repliesToDelete --
                                            if (repliesToDelete == 0){
                                                onComplete(true)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error deleteSubCollectionRepliesOfPrivateComment", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionRepliesOfPrivateComment", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfSubCollectionRepliesOfPrivateComment(postOwnerUID: String, postID: String,
                                                                  writerOfCommentUID: String, commentID: String,
                                                                  replyID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .collection("privateReplies")
            .document(replyID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfSubCollectionRepliesOfPrivateComment", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfSubCollectionRepliesOfPrivateComment", e.message.toString(), e)
            }
    }

    fun updatePrivateComment(postOwnerUID: String, postID: String, writerOfCommentUID: String,
                             commentID: String, commentHashMap: HashMap<String, Any>,
                             onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .update(commentHashMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error updating private comment", e.message.toString(), e)
            }
    }

    fun addNewPrivateComment(postID: String, postOwnerUID: String,
                             comment: CommentStructure, onComplete: (Boolean) -> Unit){
        val documentUID = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(comment.writerUID)

        documentUID.set(mapOf("userID" to comment.writerUID))
            .addOnSuccessListener {
                documentUID.collection("privateComments")
                    .document(comment.commentID)
                    .set(comment)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false)
                        Log.e("error adding new private comment", e.message.toString(), e)
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error adding new private comment", e.message.toString(), e)
            }
    }

    fun saveAwayCommentsInfo(userID: String, commentID: String, isPrivate: Boolean,
                             postID: String, postOwnerUID: String){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("awayCommentsInfo")
            .document(commentID)
            .set(mapOf("commentID" to commentID, "isPrivate" to isPrivate, "postID" to postID,
                "postOwnerUID" to postOwnerUID))
            .addOnSuccessListener {
                //
            }
            .addOnFailureListener { e ->
                //
                Log.e("error saveAwayCommentsInfo", e.message.toString(), e)
            }
    }

    private var awayCommentsInfoListenerRegistration: ListenerRegistration? = null
    fun stopListenToAwayCommentsInfo(){
        awayCommentsInfoListenerRegistration?.remove()
    }
    fun getAwayCommentsInfoListener(userID: String, onComplete: (ArrayList<Map<String, Any>>?) -> Unit){
        val awayCommentsPath = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("awayCommentsInfo")
        awayCommentsInfoListenerRegistration = awayCommentsPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getAwayCommentsInfoListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val awayCommentsList = ArrayList<Map<String, Any>>()
                for (doc in snapshot.documents){
                    val commentID = doc["commentID"]
                    val isPrivate = doc["isPrivate"]
                    val postID = doc["postID"]
                    val postOwnerUID = doc["postOwnerUID"]
                    if (commentID != null && isPrivate != null && postID != null && postOwnerUID != null){
                        val commentInfo = mapOf("commentID" to commentID as String,
                            "isPrivate" to isPrivate as Boolean,
                            "postID" to postID as String,
                            "postOwnerUID" to postOwnerUID as String)
                        awayCommentsList.add(commentInfo)
                    }
                }
                onComplete(awayCommentsList)
                awayCommentsList.clear()
            }else{
                onComplete(null)
            }
        }
    }

    private fun deleteAwayCommentInfo(userID: String, commentID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("awayCommentsInfo")
            .document(commentID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteAwayCommentInfo", e.message.toString(), e)
            }
    }

    private var publicCommentsRegistration: ListenerRegistration? = null
    fun stopPublicCommentsListener(){
        publicCommentsRegistration?.remove()
    }
    fun publicCommentsListener(post: PostStructure, onComplete: (ArrayList<CommentStructure>?) -> Unit){
        val publicCommentsPath = mFireStore.collection(Constants.USERS)
            .document(post.userId)
            .collection(Constants.Posts)
            .document(post.postID)
            .collection("publicComments")

        publicCommentsRegistration = publicCommentsPath.addSnapshotListener { value, error ->
            if (error != null){
                onComplete(null)
                Log.e("error publicCommentsListener", error.message.toString(), error)
                return@addSnapshotListener
            }
            if (value != null && !value.isEmpty){
                val comments = ArrayList<CommentStructure>()
                for (doc in value.documents){
                    val comment = doc.toObject(CommentStructure::class.java)
                    if (comment != null){
                        comments.add(comment)
                    }
                }
                onComplete(comments)
                comments.clear()
            }else{
                onComplete(null)
            }
        }
    }

    private var singlePublicCommentListenerRegistration: ListenerRegistration? = null
    fun stopListenToSinglePublicComment(){
        singlePublicCommentListenerRegistration?.remove()
    }
    fun getSinglePublicCommentListener(postOwnerUID: String, postID: String, commentID: String,
                                       onComplete: (CommentStructure?) -> Unit){
        val singleCommentPath = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
        singlePublicCommentListenerRegistration = singleCommentPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                onComplete(null)
                Log.e("error getSinglePublicCommentListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()){
                val comment = snapshot.toObject(CommentStructure::class.java)
                if (comment != null){
                    onComplete(comment)
                }
            }else{
                onComplete(null)
            }
        }
    }

    fun getPublicComments(postOwnerUID: String, postID: String,
                          onComplete: (ArrayList<CommentStructure>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    val comments = ArrayList<CommentStructure>()
                    for (doc in documents){
                        comments.add(doc.toObject(CommentStructure::class.java))
                    }
                    onComplete(comments)
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getting public comments", e.message.toString(), e)
            }
    }

    fun deletePublicComment(writerOfCommentUID: String, postOwnerUID: String, postID: String,
                            commentID: String, onComplete: (Boolean) -> Unit){

        deleteAwayCommentInfo(writerOfCommentUID, commentID) { done ->
            if (done){
                deleteSubCollectionLikesForPublicComments(postOwnerUID, postID, commentID) { ok ->
                    if (ok){
                        deleteSubCollectionRepliesForPublicComments(postOwnerUID, postID, commentID) { yes ->
                            if (yes){
                                mFireStore.collection(Constants.USERS)
                                    .document(postOwnerUID)
                                    .collection(Constants.Posts)
                                    .document(postID)
                                    .collection("publicComments")
                                    .document(commentID)
                                    .delete()
                                    .addOnSuccessListener {
                                        onComplete(true)
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error deleting public comment", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }else{
                        onComplete(false)
                    }
                }
            }else{
                onComplete(false)
            }
        }
    }

    private fun deleteSubCollectionLikesForPublicComments(postOwnerUID: String, postID: String,
                                                          commentID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteSubCollectionLikesForPublicComments", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionLikesForPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteSubCollectionRepliesForPublicComments(postOwnerUID: String, postID: String,
                                                          commentID: String, onComplete: (Boolean) -> Unit){
        val repliesCollection = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("replies")

            repliesCollection.get()
            .addOnSuccessListener { replies ->
                if (!replies.isEmpty){
                    var repliesToDelete = replies.size()
                    for (reply in replies){
                        val replyItem = reply.toObject(CommentStructure::class.java)
                        deleteLikesOfSubCollectionRepliesForPublicComments(postOwnerUID, postID,
                            commentID, reply.id) { yep ->
                            if (yep){
                                reply.reference.delete()
                                    .addOnSuccessListener {
                                        if (replyItem.commentPhoto.isNotEmpty()){
                                            deleteImageFromCloudStorage(replyItem.commentPhoto) { onSuccess ->
                                                if (onSuccess){
                                                    repliesToDelete --
                                                    if (repliesToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }else{
                                                    onComplete(false)
                                                }
                                            }
                                        }else{
                                            repliesToDelete --
                                            if (repliesToDelete == 0){
                                                onComplete(true)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error deleteSubCollectionRepliesForPublicComments", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionRepliesForPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfSubCollectionRepliesForPublicComments(postOwnerUID: String, postID: String,
                                                                   commentID: String, replyID: String,
                                                                   onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("replies")
            .document(replyID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfSubCollectionRepliesForPublicComments", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfSubCollectionRepliesForPublicComments", e.message.toString(), e)
            }
    }

    fun updatePublicComment(postOwnerUID: String, postID: String, commentID: String,
                            commentHashMap: HashMap<String, Any>,
                            onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .update(commentHashMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error updating public comment", e.message.toString(), e)
            }
    }

    fun likePublicReply(reply: CommentStructure, currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .set(mapOf("userID" to currentUserID))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error likePublicReply", e.message.toString(), e)
            }
    }

    fun unlikePublicReply(reply: CommentStructure, currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error unlikePublicReply", e.message.toString(), e)
            }
    }

    fun getLikeSituationForPublicReply(reply: CommentStructure, currentUserID: String,
                                       onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .collection("likes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error getLikeSituationForPublicReply", e.message.toString(), e)
            }
    }

    fun getNumberOfLikesForPublicReply(reply: CommentStructure, onComplete: (Int?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    onComplete(documents.size())
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getNumberOfLikesForPublicReply", e.message.toString(), e)
            }
    }

    fun updatePublicReply(reply: CommentStructure, commentHashMap: HashMap<String, Any>,
                          onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .update(commentHashMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun deletePublicReply(reply: CommentStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private var publicRepliesListener2: ListenerRegistration? = null
    fun stopListenToPublicReplies2(){
        publicRepliesListener2?.remove()
    }
    fun getPublicRepliesListener2(comment: CommentStructure,
                                 callback: (ArrayList<CommentStructure>?) -> Unit){
        val repliesPath = mFireStore.collection(Constants.USERS)
            .document(comment.postOwnerUID)
            .collection(Constants.Posts)
            .document(comment.postID)
            .collection("publicComments")
            .document(comment.commentID)
            .collection("replies")
        publicRepliesListener2 = repliesPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                callback(null)
                Log.e("error getPublicRepliesListener2", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val replies = ArrayList<CommentStructure>()
                for (doc in snapshot.documents){
                    val reply = doc.toObject(CommentStructure::class.java)
                    if (reply != null){
                        replies.add(reply)
                    }
                }
                callback(replies)
                replies.clear()
            }else{
                callback(null)
            }
        }
    }

    private var publicRepliesListener: ListenerRegistration? = null
    fun stopListenToPublicReplies(){
        publicRepliesListener?.remove()
    }
    fun getPublicRepliesListener(comment: CommentStructure,
                                 callback: (ArrayList<CommentStructure>?) -> Unit){
        val repliesPath = mFireStore.collection(Constants.USERS)
            .document(comment.postOwnerUID)
            .collection(Constants.Posts)
            .document(comment.postID)
            .collection("publicComments")
            .document(comment.commentID)
            .collection("replies")
        publicRepliesListener = repliesPath.addSnapshotListener { snapshot, e ->
            if (e != null){
                callback(null)
                Log.e("error getPublicRepliesListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty){
                val replies = ArrayList<CommentStructure>()
                for (doc in snapshot.documents){
                    val reply = doc.toObject(CommentStructure::class.java)
                    if (reply != null){
                        replies.add(reply)
                    }
                }
                callback(replies)
                replies.clear()
            }else{
                callback(null)
            }
        }
    }

    fun getPublicReplies(comment: CommentStructure, onComplete: (ArrayList<CommentStructure>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(comment.postOwnerUID)
            .collection(Constants.Posts)
            .document(comment.postID)
            .collection("publicComments")
            .document(comment.commentID)
            .collection("replies")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    val replies = ArrayList<CommentStructure>()
                    for (doc in documents){
                        replies.add(doc.toObject(CommentStructure::class.java))
                    }
                    onComplete(replies)
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getPublicReplies", e.message.toString(), e)
            }
    }

    fun addPublicReply(reply: CommentStructure, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(reply.postOwnerUID)
            .collection(Constants.Posts)
            .document(reply.postID)
            .collection("publicComments")
            .document(reply.topCommentIDForReply)
            .collection("replies")
            .document(reply.commentID)
            .set(reply)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error addReplyForPublicComment", e.message.toString(), e)
            }
    }

    fun addNewPublicComment(postID: String, postOwnerUID: String, comment: CommentStructure,
                            onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(comment.commentID)
            .set(comment)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error adding new public comment", e.message.toString(), e)
            }
    }

    fun likePublicComment(postOwnerUID: String, postID: String, commentID: String,
                          currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .set(mapOf("userID" to currentUserID))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while like public comment", e.message.toString(), e)
            }
    }

    fun unLikePublicComment(postOwnerUID: String, postID: String, commentID: String,
                          currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while unLiking public comment", e.message.toString(), e)
            }
    }

    fun getLikeSituationForPublicComment(postOwnerUID: String, postID: String, commentID: String,
                                         currentUserID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error getting like situation for public comment", e.message.toString(), e)
            }
    }

    fun getNumberOfLikesForPublicComment(postOwnerUID: String, postID: String, commentID: String,
                                         callback: (Int?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    callback(documents.size())
                }else{
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("error getting number of likes for public comment", e.message.toString(), e)
            }
    }

    fun likePost(currentUserID: String, postID: String, postOwnerUID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("likes")
            .document(currentUserID)
            .set(mapOf("userID" to currentUserID))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while like a post", e.message.toString(), e)
            }
    }

    fun unLikePost(currentUserID: String, postID: String, postOwnerUID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("likes")
            .document(currentUserID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error while UnLike a post", e.message.toString(), e)
            }
    }

    fun getLikeSituationForPost(currentUserID: String, postID: String, postOwnerUID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("likes")
            .document(currentUserID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    onComplete(true)
                }else{
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error getting like situation", e.message.toString(), e)
            }
    }

    fun getNumberOfLikesForPost(postID: String, postOwnerUID: String, callback: (Int?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("likes")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty){
                    callback(documents.size())
                }else{
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("error getting number of likes", e.message.toString(), e)
            }
    }


    fun updatePostOnFireStore(userID: String, postHashMap: HashMap<String, Any>,
                              postID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .update(postHashMap)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("update post on fireStore failed", e.message.toString(), e)
            }
    }


    fun batchUpdatePostsOnFireStore(userID: String,
                                    batchUpdates: Map<String, Map<String, Any>>,
                                    onComplete: (Boolean) -> Unit) {

        // Create a batch object
        val batch: WriteBatch = mFireStore.batch()

        // Specify the path to the posts collection
        val postsCollectionRef = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)

        // Iterate through the updates and add them to the batch
        for ((documentId, data) in batchUpdates) {
            val documentRef = postsCollectionRef.document(documentId)
            batch.update(documentRef, data)
        }

        // Commit the batch updates
        batch
            .commit()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("FireStoreClass", "Error updating posts in batch: ${e.message}")
            }
    }


    fun batchUpdatePostsForMultipleUsers(userUpdates: Map<String, List<Map<String, Any>>>,
                                         onComplete: (Boolean) -> Unit) {
        // Create a batch object
        val batch: WriteBatch = mFireStore.batch()

        for ((userID, batchUpdates) in userUpdates) {
            // Specify the path to the posts collection for the user
            val postsCollectionRef = mFireStore.collection(Constants.USERS)
                .document(userID)
                .collection(Constants.Posts)

            // Iterate through the updates for this user and add them to the batch
            for (update in batchUpdates) {
                val documentId = update["documentId"] as String
                //val data = update["data"] as Map<String, Any>
                val data = update["data"] as Map<*, *>
                val typedData = data.mapKeys { it.key as String }.mapValues { it.value as Any }
                val documentRef = postsCollectionRef.document(documentId)
                batch.update(documentRef, typedData)
            }
        }

        // Commit the batch updates
        batch.commit()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("err batch update posts", "Error updating posts in batch: ${e.message}")
            }
    }

    fun deletePostOnFireStoreWithCallback(userID: String, postID: String, onComplete: (Boolean) -> Unit){

        deleteSubCollectionPublicComments(userID, postID) { yep ->
            if (yep){
                deleteSubCollectionPrivateComments(userID, postID) { ok ->
                    if (ok){
                        deleteSubCollectionLikesOfPost(userID, postID) { success ->
                            if (success){
                                mFireStore.collection(Constants.USERS)
                                    .document(userID)
                                    .collection(Constants.Posts)
                                    .document(postID)
                                    .delete()
                                    .addOnSuccessListener {
                                        onComplete(true)
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error delete post with callback", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }else{
                        onComplete(false)
                    }
                }
            }else{
                onComplete(false)
            }
        }
    }

    private fun deleteSubCollectionPublicComments(userID: String, postID: String, onComplete: (Boolean) -> Unit) {
        val publicCommentsCollection = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")

            publicCommentsCollection.get()
            .addOnSuccessListener { publicComments ->
                if (!publicComments.isEmpty){
                    var commentsToDelete = publicComments.size()
                    for (comment in publicComments) {
                        val publicCommentItem = comment.toObject(CommentStructure::class.java)
                        deleteLikesOfSubPublicComments(userID, comment.id, postID) { onSuccess ->
                            if (onSuccess){
                                deleteRepliesOfSubPublicComments(userID, postID, comment.id) { succeed ->
                                    if (succeed){
                                        comment.reference.delete()
                                            .addOnSuccessListener {
                                                if (publicCommentItem.commentPhoto.isNotEmpty()){
                                                    deleteImageFromCloudStorage(publicCommentItem.commentPhoto) { yep ->
                                                        if (yep){
                                                            commentsToDelete --
                                                            if (commentsToDelete == 0){
                                                                onComplete(true)
                                                            }
                                                        }else{
                                                            onComplete(false)
                                                        }
                                                    }
                                                }else{
                                                    commentsToDelete --
                                                    if (commentsToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                onComplete(false)
                                                Log.e("error deleteSubCollectionPublicComments", e.message.toString(), e)
                                            }
                                    }else{
                                        onComplete(false)
                                    }
                                }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteRepliesOfSubPublicComments(userID: String, postID: String, commentID: String,
                                                 onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("replies")
            .get()
            .addOnSuccessListener { replies ->
                if (!replies.isEmpty){
                    var repliesToDelete = replies.size()
                    for (reply in replies){
                        val replyItem = reply.toObject(CommentStructure::class.java)
                        deleteLikesOfRepliesOfSubPublicComments(userID, postID, commentID, reply.id) { onSuccess ->
                            if (onSuccess){
                                reply.reference.delete()
                                    .addOnSuccessListener {
                                        if (replyItem.commentPhoto.isNotEmpty()){
                                            deleteImageFromCloudStorage(replyItem.commentPhoto) { yep ->
                                                if (yep){
                                                    repliesToDelete --
                                                    if (repliesToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }else{
                                                    onComplete(false)
                                                }
                                            }
                                        }else{
                                            repliesToDelete --
                                            if (repliesToDelete == 0){
                                                onComplete(true)
                                            }
                                        }
                                    }
                                    .addOnFailureListener{ e ->
                                        onComplete(false)
                                        Log.e("error deleteRepliesOfSubPublicComments", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteRepliesOfSubPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfRepliesOfSubPublicComments(userID: String, postID: String, commentID: String,
                                                        replyID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("replies")
            .document(replyID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likesForReplies ->
                if (!likesForReplies.isEmpty){
                    var likesToDelete = likesForReplies.size()
                    for (likeForReply in likesForReplies){
                        likeForReply.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfRepliesOfSubPublicComments", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfRepliesOfSubPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfSubPublicComments(userID: String, commentID: String, postID: String,
                                               onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDeleteCount = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDeleteCount--
                                if (likesToDeleteCount == 0) {
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfSubPublicComments", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfSubPublicComments", e.message.toString(), e)
            }
    }

    private fun deleteSubCollectionLikesOfPost(userID: String, postID: String, onComplete: (Boolean) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes) {
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteSubCollectionLikesOfPost", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionLikesOfPost", e.message.toString(), e)
            }
    }

    private fun deleteSubCollectionPrivateComments(userID: String, postID: String, onComplete: (Boolean) -> Unit) {
        val uIDsCollection = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")

            uIDsCollection.get()
            .addOnSuccessListener { uIDs ->
                if (!uIDs.isEmpty){
                    var uIDsToDelete = uIDs.size()
                    for (uID in uIDs) {
                        deletePrivateCommentsForUIDsOfSubPrivateComments(userID, postID, uID.id) { yep ->
                            if (yep){
                                uID.reference.delete()
                                    .addOnSuccessListener {
                                        uIDsToDelete --
                                        if (uIDsToDelete == 0){
                                            onComplete(true)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false)
                                        Log.e("error deleteSubCollectionPrivateComments", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteSubCollectionPrivateComments", e.message.toString(), e)
            }
    }

    private fun deletePrivateCommentsForUIDsOfSubPrivateComments(userID: String, postID: String, uiDiD: String,
                                                                 onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uiDiD)
            .collection("privateComments")
            .get()
            .addOnSuccessListener { privateComments ->
                if (!privateComments.isEmpty){
                    var commentsToDelete = privateComments.size()
                    for (comment in privateComments){
                        val privateCommentItem = comment.toObject(CommentStructure::class.java)
                        deleteLikesOfPrivateCommentsOfUIDsOfSub(userID, postID, uiDiD, comment.id) { yep ->
                            if (yep){
                                deleteRepliesOfPrivateCommentsOfUIDsOfSub(userID, postID, uiDiD, comment.id) { ok ->
                                    if (ok){
                                        comment.reference.delete()
                                            .addOnSuccessListener {
                                                if (privateCommentItem.commentPhoto.isNotEmpty()){
                                                    deleteImageFromCloudStorage(privateCommentItem.commentPhoto) { onSuccess ->
                                                        if (onSuccess){
                                                            commentsToDelete --
                                                            if (commentsToDelete == 0){
                                                                onComplete(true)
                                                            }
                                                        }else{
                                                            onComplete(false)
                                                        }
                                                    }
                                                }else{
                                                    commentsToDelete --
                                                    if (commentsToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                onComplete(false)
                                                Log.e("error deletePrivateCommentsForUIDsOfSubPrivateComments", e.message.toString(), e)
                                            }
                                    }else{
                                        onComplete(false)
                                    }
                                }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deletePrivateCommentsForUIDsOfSubPrivateComments", e.message.toString(), e)
            }
    }

    private fun deleteRepliesOfPrivateCommentsOfUIDsOfSub(userID: String, postID: String, uiDiD: String, commentID: String,
                                                          onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uiDiD)
            .collection("privateComments")
            .document(commentID)
            .collection("privateReplies")
            .get()
            .addOnSuccessListener { privateReplies ->
                if (!privateReplies.isEmpty){
                    var repliesToDelete = privateReplies.size()
                    for (reply in privateReplies){
                        val replyItem = reply.toObject(CommentStructure::class.java)
                        deleteLikesOfRepliesOfPrivateCommentsOfUIDsOfSub(userID, postID, uiDiD, commentID,
                            reply.id) { yep ->
                            if (yep){
                                reply.reference.delete()
                                    .addOnSuccessListener {
                                        if (replyItem.commentPhoto.isNotEmpty()){
                                            deleteImageFromCloudStorage(replyItem.commentPhoto) { ok ->
                                                if (ok){
                                                    repliesToDelete --
                                                    if (repliesToDelete == 0){
                                                        onComplete(true)
                                                    }
                                                }else{
                                                    onComplete(false)
                                                }
                                            }
                                        }else{
                                            repliesToDelete --
                                            if (repliesToDelete == 0){
                                                onComplete(true)
                                            }
                                        }
                                    }
                                    .addOnFailureListener{ e ->
                                        onComplete(false)
                                        Log.e("error deleteRepliesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
                                    }
                            }else{
                                onComplete(false)
                            }
                        }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteRepliesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfRepliesOfPrivateCommentsOfUIDsOfSub(userID: String, postID: String, uiDiD: String,
                                                                 commentID: String, replyID: String,
                                                                 onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uiDiD)
            .collection("privateComments")
            .document(commentID)
            .collection("privateReplies")
            .document(replyID)
            .collection("likes")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty){
                    var docsToDelete = docs.size()
                    for (doc in docs){
                        doc.reference.delete()
                            .addOnSuccessListener {
                                docsToDelete --
                                if (docsToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfRepliesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfRepliesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
            }
    }

    private fun deleteLikesOfPrivateCommentsOfUIDsOfSub(userID: String, postID: String, uiDiD: String,
                                                        commentID: String, onComplete: (Boolean) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(uiDiD)
            .collection("privateComments")
            .document(commentID)
            .collection("likes")
            .get()
            .addOnSuccessListener { likes ->
                if (!likes.isEmpty){
                    var likesToDelete = likes.size()
                    for (like in likes){
                        like.reference.delete()
                            .addOnSuccessListener {
                                likesToDelete --
                                if (likesToDelete == 0){
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false)
                                Log.e("error deleteLikesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
                            }
                    }
                }else{
                    onComplete(true)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleteLikesOfPrivateCommentsOfUIDsOfSub", e.message.toString(), e)
            }
    }

    private fun createUserName(username: String, activity: SignUpActivity){
        val userID = getUserID()
        val data = mapOf("username" to username, "userID" to userID)

        mFireStore.collection(Constants.UserNames)
            .document(userID)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                // Username added successfully
                activity.registrationSuccessful()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e("createUserName error", "error while creating userName", e)
            }
    }



    private fun updateUserName(newUserName: String, callback: (Boolean) -> Unit){
        mFireStore.collection(Constants.UserNames)
            .document(getUserID())
            .update("username", newUserName)
            .addOnSuccessListener {
                // update
                callback(true)
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.e("updateUserName error", "error while updating userName", e)
            }
    }


    fun isUserNameTaken(userName: String, callback: (Boolean) -> Unit) {
        mFireStore.collection(Constants.UserNames)
            .whereEqualTo("username", userName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // The username is taken by another user
                    callback(true)
                } else {
                    // Username is available
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("isUserNameTaken error", "error while checking if userName is taken or not", e)
                callback(false)
            }
    }


    /*
    fun getUsernameByUserID(userID: String, callback: (String?) -> Unit) {
        mFireStore.collection(Constants.UserNames)
            .whereEqualTo("userID", userID)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    callback(documents.documents[0].get("username") as String)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("getUsernameByUserID error", "error while getting username by userID", e)
                callback(null)
            }
    }

     */


    fun getUserIDByUsername(usernameToSearch: String, callback: (String?) -> Unit) {
        mFireStore.collection(Constants.UserNames)
            .whereEqualTo("username", usernameToSearch)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    callback(documents.documents[0].get("userID") as String)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("getUserIDByUsername error", "error while getting userID by username", e)
                callback(null)
            }
    }


    fun getCurrentUserID(callback: (String) -> Unit) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val currentUserID = currentUser.uid
            callback(currentUserID)
        } else {
            callback("")
        }
    }

    // same

    fun getUserID(): String {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        var currentUserID = ""
        if (currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }


    fun getUserInfoWithCallback(userID: String, callback: (User?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)!!
                callback(user)
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("err getting user info with callback", e.message.toString(), e)
            }
    }


    fun getUserInfoFromFireStore(activity: Activity, userID: String){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    val user = document.toObject(User::class.java)!!
                    when(activity){
                        is LogIn -> {
                            activity.successGettingUserInfoFromFireStore(user)
                        }
                        is MainActivity -> {
                            activity.successGettingUserInfoFromFireStore(user)
                        }
                        is ProfileActivity -> {
                            activity.successGettingUserInfoFromFireStore(user)
                        }
                        is CreatePostActivity -> {
                            activity.successGettingUserInfoFromFireStore(user)
                        }
                        is SearchActivity -> {
                            activity.successGettingUserInfoFromFireStore(user)
                        }
                    }
                }else{
                    when(activity){
                        is LogIn -> {
                            activity.hideProgressDialog()
                        }
                        is MainActivity -> {
                            activity.hideProgressDialog()
                        }
                        is ProfileActivity -> {
                            activity.hideProgressDialog()
                        }
                        is CreatePostActivity -> {
                            activity.hideProgressDialog()
                        }
                        is SearchActivity -> {
                            activity.hideProgressDialog()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is LogIn -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is ProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                    is CreatePostActivity -> {
                        activity.hideProgressDialog()
                    }
                    is SearchActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "error getting user info from fireStore", e)
            }
    }

    private var userPostsListener: ListenerRegistration? = null
    fun stopUserPostsListener(){
        userPostsListener?.remove()
    }
    fun getPostsRealTimeListener(userID: String, callback: (ArrayList<PostStructure>?) -> Unit) {
        val postsPath = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
        userPostsListener = postsPath.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    callback(null)
                    Log.e("Error getPostsRealTimeListener", e.message.toString(), e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val posts = ArrayList<PostStructure>()
                    for (document in snapshot.documents) {
                        val post = document.toObject(PostStructure::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }
                    callback(posts)
                    posts.clear()
                }else{
                    callback(null)
                }
            }
    }



    fun getASinglePostFromFireStore(userID: String, postId: String, callback: (PostStructure?) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        callback(post)
                    }else{
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("Error getting a post from FireStore", e.message.toString(), e)
            }
    }

    private var postListener: ListenerRegistration? = null
    fun stopPostListener(){
        postListener?.remove()
    }

    fun listenForSinglePostChanges(userID: String, postId: String, callback: (PostStructure?) -> Unit) {
        val postRef = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .document(postId)

        postListener = postRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Log.e("error listening for post changes", e.message.toString(), e)
                callback(null)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val post = documentSnapshot.toObject(PostStructure::class.java)
                if (post != null){
                    callback(post)
                }
            } else {
                callback(null)
            }
        }
    }



    suspend fun getPostsFromFireStore(userID: String): ArrayList<PostStructure>? = coroutineScope {
        return@coroutineScope try {
            val snapshot = mFireStore.collection(Constants.USERS)
                .document(userID)
                .collection(Constants.Posts)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val posts = ArrayList<PostStructure>()
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                posts
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Error getting posts from FireStore", "Error getting posts from FireStore", e)
            null
        }
    }

    suspend fun getBoughtPosts(userID: String): ArrayList<PostStructure>? = coroutineScope {
        return@coroutineScope try {
            val snapshot = mFireStore.collection(Constants.USERS)
                .document(userID)
                .collection("boughtPosts")
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val posts = ArrayList<PostStructure>()
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                posts
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Error getting bought posts from FireStore", e.message.toString(), e)
            null
        }
    }

    private var soldPostsListener: ListenerRegistration? = null
    fun stopSoldPostsListener(){
        soldPostsListener?.remove()
    }

    fun getSoldPostsListener(userID: String, callback: (ArrayList<PostStructure>?) -> Unit) {
        val soldPosts = ArrayList<PostStructure>()
        val soldPostsCollection = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("soldPosts")

        soldPostsListener = soldPostsCollection.addSnapshotListener { snapshot, e ->
            if (e != null){
                callback(null)
                Log.e("error getSoldPostsListener", e.message.toString(), e)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        soldPosts.add(post)
                    }
                }
                callback(soldPosts)

                soldPosts.clear()
            } else {
                callback(null)
            }
        }
    }

    suspend fun getSoldPosts(userID: String): ArrayList<PostStructure>? = coroutineScope {
        return@coroutineScope try {
            val snapshot = mFireStore.collection(Constants.USERS)
                .document(userID)
                .collection("soldPosts")
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val posts = ArrayList<PostStructure>()
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                posts
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Error getting sold posts from FireStore", e.message.toString(), e)
            null
        }
    }


    fun updateUserInfoOnFireStore(activity: Activity, userHashMap: HashMap<String, Any>,
                                  oldUserName: String, newUserName: String){
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .update(userHashMap)
            .addOnSuccessListener {
                when(activity){
                    is ProfileActivity -> {
                        if (oldUserName != newUserName){
                            updateUserName(newUserName) { success ->
                                if (success){
                                    activity.updateUserInfoOnFireStoreSuccess()
                                }else{
                                    activity.hideProgressDialog()
                                    activity.showErrorSnackBar("Error While Updating UserName", true)
                                }
                            }
                        }else{
                            activity.updateUserInfoOnFireStoreSuccess()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is ProfileActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar("Error While Updating User Info", true)
                    }
                }
                Log.e(activity.javaClass.simpleName, "update user info on FireStore failed", e)
            }
    }

    fun uploadImageToCloudStorage(activity: Activity, imageUri: Uri, pathString: String){

        val sREF: StorageReference = Firebase.storage.reference.child(pathString +
                System.currentTimeMillis() + "." + Constants.getExtensionFromFile(activity, imageUri))
        sREF.putFile(imageUri).addOnSuccessListener { TaskSnapshot ->
            TaskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { Uri ->
                when(activity){
                    is ProfileActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                    is CreatePostActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                    is EditPostActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                    is CommentsActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                    is EditCommentActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                    is ReplyCommentActivity -> {
                        activity.uploadImageOnCloudSuccess(Uri.toString())
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e(activity.javaClass.simpleName, e.message.toString(), e)
            when(activity){
                is ProfileActivity -> {
                    activity.hideProgressDialog()
                }
                is CreatePostActivity -> {
                    activity.hideProgressDialog()
                }
                is EditPostActivity -> {
                    activity.hideProgressDialog()
                }
                is CommentsActivity -> {
                    activity.hideProgressDialog()
                }
                is EditCommentActivity -> {
                    activity.hideProgressDialog()
                }
                is ReplyCommentActivity -> {
                    activity.hideProgressDialog()
                }
            }
        }
    }

    fun deleteImageFromCloudStorage(imageUrl: String, onComplete: (Boolean) -> Unit) {
        val storage = Firebase.storage
        val storageRef = storage.getReferenceFromUrl(imageUrl)

        storageRef.delete()
            .addOnSuccessListener {
                onComplete(true)
                Log.i("Firebase Storage", "Image deleted successfully")
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("Firebase Storage", "Error deleting image: ${e.message}", e)
            }
    }


}