package com.example.priceless

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlin.math.log


class FireStoreClass {

    private val mFireStore: FirebaseFirestore = FirebaseFirestore.getInstance()

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

    // maybe we should setOptions.merge()
    private fun createUserName(username: String){
        mFireStore.collection("usernames")
            .document(username)
            .set(mapOf("taken" to true), SetOptions.merge())
            .addOnSuccessListener {
                // Username added successfully
            }
            .addOnFailureListener { e ->
                Log.e("createUserName error", "error while creating userName", e)
            }
    }


    fun updateUserName(oldUsername: String, newUserName: String){
        mFireStore.collection("usernames")
            .document(oldUsername)
            .delete()
            .addOnSuccessListener {
                createUserName(newUserName)
            }
            .addOnFailureListener { e ->
                Log.e("updateUserName error", "error while updating userName", e)
            }
    }


    fun isUserNameTaken(userName: String, callback: (Boolean) -> Unit) {
        mFireStore.collection("usernames")
            .document(userName)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists() && documentSnapshot.getBoolean("taken") == true) {
                    // Username is taken
                    callback(true)
                } else {
                    // Username is available
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("isUserNameTaken error", "error while checking for if userName is taken or not", e)
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
                Log.i(activity.javaClass.simpleName, document.toString())
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
                }
            }
            .addOnFailureListener { e ->
                when(activity){
                    is LogIn -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "error while getting user info from fireStore", e)
            }
    }


    fun getUserInfoRealtimeListener(activity: Activity, listener: (User) -> Unit) {
        mFireStore.collection(Constants.USERS)
            .document(getUserID())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(activity.javaClass.simpleName, "Error listening to user info", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)!!
                    listener(user)
                }
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
                            updateUserName(oldUserName, newUserName)
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

    fun uploadImageToCloudStorage(activity: Activity, imageUri: Uri){

        val sREF: StorageReference = Firebase.storage.reference.child(Constants.User_Profile_Image +
                System.currentTimeMillis() + "." + Constants.getExtensionFromFile(activity, imageUri))
        sREF.putFile(imageUri).addOnSuccessListener { TaskSnapshot ->
            Log.i("firebase image Url", TaskSnapshot.metadata!!.reference!!.downloadUrl.toString())
            TaskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { Uri ->
                Log.i("downloadable image Uri", Uri.toString())
                when(activity){
                    is ProfileActivity -> {
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
            }
        }
    }

}