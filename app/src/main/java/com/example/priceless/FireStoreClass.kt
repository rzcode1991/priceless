package com.example.priceless

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.*
import org.checkerframework.checker.guieffect.qual.UI
import kotlin.math.log


class FireStoreClass {

    private val mFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var postsListenerRegistration: ListenerRegistration? = null
    private var userListenerRegistration: ListenerRegistration? = null


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


    fun getFollowingListtttt(userID: String, callback: (ArrayList<String>?) -> Unit){
        mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection("following")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty){
                    val followingList = ArrayList<String>()
                    for (document in snapshot.documents){
                        val uID = document.getString("userID")
                        if (uID != null){
                            followingList.add(uID)
                        }
                    }
                    callback(followingList)
                }else{
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
                Log.e("err getting following list", e.message.toString(), e)
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
            //.whereEqualTo("senderUserID", currentUserID)
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


    fun createPostOnFireStore(activity: CreatePostActivity, post: PostStructure) {
        val postCollection = mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)

        // Add the post to FireStore without an ID (FireStore will generate one)
        postCollection.add(post)
            .addOnSuccessListener { documentReference ->
                // Here, documentReference.id gives you the ID of the newly created post
                val postID = documentReference.id

                postCollection.document(postID).update("postID", postID)
                    .addOnSuccessListener {
                        // This block is executed if the postID update is successful
                        activity.createPostSuccessful()
                    }
                    .addOnFailureListener { e ->
                        activity.hideProgressDialog()
                        Log.e(activity.javaClass.simpleName, "Error while updating postID when creating new post", e)
                    }
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating post on FireStore", e)
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

    fun getPrivateCommentsForPostOwner(postID: String, postOwnerUID: String,
                                       onComplete: (ArrayList<CommentStructure>?) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("documents are:", "$documents")
                if (!documents.isEmpty){
                    for (doc in documents){
                        mFireStore.collection(Constants.USERS)
                            .document(postOwnerUID)
                            .collection(Constants.Posts)
                            .document(postID)
                            .collection("UIDs")
                            .document(doc.id)
                            .collection("privateComments")
                            .get()
                            .addOnSuccessListener { docs ->
                                if (!docs.isEmpty){
                                    Log.d("private comments are:", "$docs")
                                    val comments = ArrayList<CommentStructure>()
                                    for (i in docs){
                                        comments.add(i.toObject(CommentStructure::class.java))
                                    }
                                    Log.d("------private comments are:", "$comments")
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
                }else{
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onComplete(null)
                Log.e("error getting private comments for post owner", e.message.toString(), e)
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
        mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("UIDs")
            .document(writerOfCommentUID)
            .collection("privateComments")
            .document(commentID)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error deleting private comment", e.message.toString(), e)
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
                documentUID.collection("privateComments").add(comment)
                    .addOnSuccessListener { documentReference ->
                        val commentID = documentReference.id

                        documentUID.collection("privateComments").document(commentID)
                            .update("commentID", commentID)
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
            .addOnFailureListener { e ->
                onComplete(false)
                Log.e("error adding new private comment", e.message.toString(), e)
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

    fun deletePublicComment(postOwnerUID: String, postID: String, commentID: String,
                            onComplete: (Boolean) -> Unit){
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

    fun addNewPublicComment(postID: String, postOwnerUID: String, comment: CommentStructure,
                             onComplete: (Boolean) -> Unit){
        val publicCommentsCollection = mFireStore.collection(Constants.USERS)
            .document(postOwnerUID)
            .collection(Constants.Posts)
            .document(postID)
            .collection("publicComments")

        publicCommentsCollection.add(comment)
            .addOnSuccessListener { documentReference ->
                val commentID = documentReference.id

                publicCommentsCollection.document(commentID).update("commentID", commentID)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false)
                        Log.e("error adding new public comment", e.message.toString(), e)
                    }
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


    fun updatePostOnFireStore(activity: Activity, userID: String, postHashMap: HashMap<String, Any>,
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
                Log.e(activity.javaClass.simpleName, "update post on fireStore failed", e)
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


    fun batchUpdatePostsForMultipleUsers(userUpdates: Map<String, List<Map<String, Any>>>, onComplete: (Boolean) -> Unit) {
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
                val data = update["data"] as Map<String, Any>
                val documentRef = postsCollectionRef.document(documentId)
                batch.update(documentRef, data)
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


    fun deletePostOnFireStore(activity: Activity, postID: String){
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)
            .document(postID)
            .delete()
            .addOnSuccessListener {
                when(activity){
                    is EditPostActivity -> {
                        activity.deletePostOnFireStoreSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is EditPostActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "delete post on fireStore failed", e)
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
                val user = document.toObject(User::class.java)!!
                //val sharedPreferences: SharedPreferences =
                //    activity.getSharedPreferences(Constants.priceless_PREFERENCES, Context.MODE_PRIVATE)
                //val editor: SharedPreferences.Editor = sharedPreferences.edit()
                //editor.putString(Constants.LOGGEDInUSER, "${user.firstName} ${user.lastName}")
                //editor.apply()
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


    /*
    fun getUserInfoRealtimeListener(activity: Activity, userID: String, listener: (User?, Boolean) -> Unit) {
        userListenerRegistration = mFireStore.collection(Constants.USERS)
            .document(userID)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(activity.javaClass.simpleName, "Error listening to user info", e)
                    listener(null, false)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)!!
                    listener(user, true)
                }
            }
    }

     */



    fun getPostsRealTimeListener(userID: String, listener: (ArrayList<PostStructure>?, Boolean) -> Unit) {
        postsListenerRegistration = mFireStore.collection(Constants.USERS)
            .document(userID)
            .collection(Constants.Posts)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Error listening to posts", "Error listening to posts", e)
                    listener(null, false)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = ArrayList<PostStructure>()

                    for (document in snapshot.documents) {
                        val post = document.toObject(PostStructure::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }

                    listener(posts, true)
                }
            }
    }


    fun removePostsSnapshotListener() {
        postsListenerRegistration?.remove()
    }


    fun removeUsersSnapshotListener() {
        userListenerRegistration?.remove()
    }



    fun getASinglePostFromFireStoreeeeee(userID: String, postId: String, callback: (PostStructure?) -> Unit) {
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
                Log.e("Error getting posts from FireStore", e.message.toString(), e)
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
            Log.i("firebase image Url", TaskSnapshot.metadata!!.reference!!.downloadUrl.toString())
            TaskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { Uri ->
                Log.i("downloadable image Uri", Uri.toString())
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
            }
        }
    }

    fun deleteImageFromCloudStorage(imageUrl: String) {
        val storage = Firebase.storage
        val storageRef = storage.getReferenceFromUrl(imageUrl)

        storageRef.delete()
            .addOnSuccessListener {
                // File deleted successfully
                Log.i("Firebase Storage", "Image deleted successfully")
                // You can perform additional actions here if needed
            }
            .addOnFailureListener { e ->
                // An error occurred while deleting the file
                Log.e("Firebase Storage", "Error deleting image: ${e.message}", e)
                // Handle the error as needed
            }
    }


}