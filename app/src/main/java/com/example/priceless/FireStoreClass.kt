package com.example.priceless

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.firestore.WriteBatch
import kotlin.math.log


class FireStoreClass {

    private val mFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var postsListenerRegistration: ListenerRegistration? = null

    fun registerUserInFireStore(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)
            .document(userInfo.id)
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                // User registration successful, now create the username
                createUserName(userInfo.userName)
                activity.registrationSuccessful()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "error while registering user on fireStore", e)
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
                        Log.e(activity.javaClass.simpleName, "Error while updating postID", e)
                    }
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating post on FireStore", e)
            }

    }



    fun updatePostOnFireStore(activity: Activity, postHashMap: HashMap<String, Any>, postID: String){
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)
            .document(postID)
            .update(postHashMap)
            .addOnSuccessListener {
                when(activity){
                    is EditPostActivity -> {
                        activity.updatePostOnFireStoreSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is EditPostActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "update post on fireStore failed", e)
            }
    }


    fun batchUpdatePostsOnFireStore(activity: Activity, batchUpdates: Map<String, Map<String, Any>>, onComplete: (Boolean) -> Unit) {

        // Create a batch object
        val batch: WriteBatch = mFireStore.batch()

        // Specify the path to the posts collection
        val postsCollectionRef = mFireStore.collection(Constants.USERS)
            .document(getUserID())
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


    private fun createUserName(username: String){
        mFireStore.collection(Constants.UserNames)
            .document(getUserID())
            .set(mapOf("username" to username), SetOptions.merge())
            .addOnSuccessListener {
                // Username added successfully
            }
            .addOnFailureListener { e ->
                Log.e("createUserName error", "error while creating userName", e)
            }
    }


    private fun updateUserName(newUserName: String){
        mFireStore.collection(Constants.UserNames)
            .document(getUserID())
            .update("username", newUserName)
            .addOnSuccessListener {
                // update
            }
            .addOnFailureListener { e ->
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


    fun getUserID(): String {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        var currentUserID = ""
        if (currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getUserInfoFromFireStore(activity: Activity){
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .get()
            .addOnSuccessListener { document ->
                //Log.i(activity.javaClass.simpleName, document.toString())
                val user = document.toObject(User::class.java)!!
                val sharedPreferences: SharedPreferences =
                    activity.getSharedPreferences(Constants.priceless_PREFERENCES, Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString(Constants.LOGGEDInUSER, "${user.firstName} ${user.lastName}")
                editor.apply()
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
                }
                Log.e(activity.javaClass.simpleName, "error while getting user info from fireStore", e)
            }
    }


    fun getUserInfoRealtimeListener(activity: Activity, listener: (User?, Boolean) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
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



    fun getPostsRealTimeListener(activity: Activity, listener: (ArrayList<PostStructure>?, Boolean) -> Unit) {
        postsListenerRegistration = mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(activity.javaClass.simpleName, "Error listening to user info", e)
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



    fun getPostsFromFireStore(activity: Activity) {
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = ArrayList<PostStructure>()
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                //when(activity){
                //    is FragmentActivity -> {
                //        activity.successGettingPostsFromFireStore(posts)
                //    }
                //}
            }
            .addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, "Error getting posts from FireStore", e)
            }
    }


    fun getPostsFromFireStoreAndReturnThem(callback: (ArrayList<PostStructure>) -> Unit) {
        val posts = ArrayList<PostStructure>()
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .collection(Constants.Posts)
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    val post = document.toObject(PostStructure::class.java)
                    if (post != null) {
                        posts.add(post)
                    }
                }
                // Invoke the callback with the retrieved posts when successful
                callback(posts)
            }
            .addOnFailureListener { e ->
                Log.e("err getting posts", "Error getting posts from FireStore", e)
                // Invoke the callback with an empty list or an error indicator if there's a failure
                callback(ArrayList())
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
                            updateUserName(newUserName)
                        }
                        activity.updateUserInfoOnFireStoreSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is ProfileActivity -> {
                        activity.hideProgressDialog()
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