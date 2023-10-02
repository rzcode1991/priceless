package com.example.priceless

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class EditPostActivity : BaseActivity(), OnClickListener {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var etPostText: EditText
    private lateinit var ivPostImage: ImageView
    private lateinit var tvTimeCreated: TextView
    private lateinit var btnEditAndUpdatePost: Button
    private lateinit var btnCancelEditing: Button
    private lateinit var btnDeletePost: Button
    private lateinit var post: PostStructure
    private var editOrUpdateSituation = "edit"
    private var imageURI: Uri? = null
    private var newImageNameUrl: String = ""
    private var newTimeCreatedMillis: String = ""
    private var newTimeCreatedToShow: String = ""
    private lateinit var newPost: PostStructure
    private var newPostText: String = ""
    private var newPostImage: String = ""
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var getTime: GetTime? = null
    private var dateAndTimePair: Pair<String?, String?>? = null
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)

        ivProfilePic = findViewById(R.id.iv_profile_pic_edit_post)
        tvUserName = findViewById(R.id.tv_user_name_edit_post)
        etPostText = findViewById(R.id.et_post_text_edit_post)
        ivPostImage = findViewById(R.id.iv_post_image_edit_post)
        tvTimeCreated = findViewById(R.id.tv_time_created_edit_post)
        btnEditAndUpdatePost = findViewById(R.id.btn_edit_and_update_post)
        btnCancelEditing = findViewById(R.id.btn_cancel_editing_post)
        btnDeletePost = findViewById(R.id.btn_delete_post)

        getTime = GetTime()

        if (intent.hasExtra("entire_post")){
            post = intent.getParcelableExtra("entire_post")!!
        }

        setPost()
        btnEditAndUpdatePost.setOnClickListener(this@EditPostActivity)
        btnCancelEditing.setOnClickListener(this@EditPostActivity)
        ivPostImage.setOnClickListener(this@EditPostActivity)
        btnDeletePost.setOnClickListener(this@EditPostActivity)

    }

    private fun setPost(){
        if (post.profilePicture.isNotEmpty()){
            GlideLoader(this).loadImageUri(post.profilePicture, ivProfilePic)
        }else{
            ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        tvUserName.text = post.userName
        etPostText.setText(post.postText)
        etPostText.isEnabled = false
        // set color for etPostText
        if (post.postImage.isNotEmpty()){
            GlideLoader(this).loadImageUri(post.postImage, ivPostImage)
        }else{
            ivPostImage.setImageResource(R.drawable.ic_baseline_image_24)
        }
        if (post.timeCreatedToShow.isNotEmpty()){
            tvTimeCreated.text = "created at ${post.timeCreatedToShow}"
        }else{
            tvTimeCreated.text = "err getting time online"
        }

    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.btn_edit_and_update_post -> {
                    if (editOrUpdateSituation == "edit"){
                        Toast.makeText(this, "now you can edit the text or change the image", Toast.LENGTH_LONG).show()
                        btnEditAndUpdatePost.text = "Update"
                        btnCancelEditing.visibility = VISIBLE
                        btnDeletePost.visibility = VISIBLE
                        etPostText.isEnabled = true
                        ivPostImage.setOnClickListener {
                            if (ContextCompat.checkSelfPermission(this,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED){
                                Constants.showImageFromStorage(this@EditPostActivity)
                            }else{
                                ActivityCompat.requestPermissions(this@EditPostActivity,
                                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                                    Constants.PermissionExternalStorageCode)
                            }
                        }
                        editOrUpdateSituation = "update"
                    }else if (editOrUpdateSituation == "update"){
                        if (validateUserInput()){
                            showProgressDialog()
                            if (imageURI != null){
                                FireStoreClass().uploadImageToCloudStorage(this, imageURI!!,
                                    "post_image")
                            }else{
                                coroutineScope.launch {
                                    try {
                                        updatePost()
                                    } catch (e: Exception) {
                                        Log.d("err calling createPost", e.message.toString())
                                    }
                                }
                            }
                        }
                    }
                }
                R.id.btn_cancel_editing_post -> {
                    if (btnCancelEditing.visibility == VISIBLE){
                        finish()
                    }
                }
                R.id.iv_post_image_edit_post -> {
                    if (post.postImage.isNotEmpty()){
                        val intent = Intent(this, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", post.postImage)
                        startActivity(intent)
                    }
                }
                R.id.btn_delete_post -> {
                    if (btnDeletePost.visibility == VISIBLE){
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("DELETE POST?")
                        builder.setMessage("post will be deleted permanently!")
                        builder.setIcon(R.drawable.ic_round_warning_24)
                        builder.setPositiveButton("Yes") { dialog, _ ->
                            showProgressDialog()
                            FireStoreClass().deletePostOnFireStore(this@EditPostActivity, post.postID)
                            dialog.dismiss()
                        }
                        builder.setNeutralButton("Cancel") {dialog, _ ->
                            dialog.dismiss()
                        }
                        builder.setCancelable(false)
                        builder.create().show()
                    }
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@EditPostActivity)
            }else{
                Toast.makeText(this, "oops! you didn't gave permission to app for access " +
                        "storage, you can change it in your device's settings", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == Constants.ImageIntentCode){
                if (data != null) {
                    try {
                        imageURI = data.data!!
                        if (imageURI != null){
                            GlideLoader(this).loadImageUri(imageURI!!, ivPostImage)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "image selection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED){
            Log.e("image selection failed", "image has not been selected")
        }
    }


    private fun validateUserInput(): Boolean{
        return if (TextUtils.isEmpty(etPostText.text.toString())){
            showErrorSnackBar("please enter text", true)
            false
        }else if (post.postText == etPostText.text.toString() && imageURI == null){
            showErrorSnackBar("you did not change anything", true)
            false
        }else{
            true
        }
    }

    fun uploadImageOnCloudSuccess(newImageUrl: String){
        if (post.postImage.isNotEmpty()){
            FireStoreClass().deleteImageFromCloudStorage(post.postImage)
        }
        newImageNameUrl = newImageUrl
        coroutineScope.launch {
            try {
                updatePost()
            } catch (e: Exception) {
                Log.d("err calling createPost", e.message.toString())
            }
        }
    }


    private suspend fun updatePost(){
        getTimeNow()
        if (secondsNow.isEmpty() || dateNow.isEmpty()){
            showErrorSnackBar("error getting time online, try again", true)
            hideProgressDialog()
        }else{
            val postHashMap = HashMap<String, Any>()
            val postID = post.postID
            if (post.postText != etPostText.text.toString()){
                newPostText = etPostText.text.toString()
                postHashMap["postText"] = newPostText
            }else{
                // just for adjusting the sortedPosts,
                newPostText = post.postText
            }
            if (newImageNameUrl.isNotEmpty()){
                newPostImage = newImageNameUrl
                postHashMap["postImage"] = newPostImage
            }else{
                newPostImage = post.postImage
            }
            newTimeCreatedMillis = secondsNow
            newTimeCreatedToShow = dateNow
            postHashMap["timeCreatedMillis"] = newTimeCreatedMillis
            postHashMap["timeCreatedToShow"] = newTimeCreatedToShow
            postHashMap["edited"] = true
            FireStoreClass().updatePostOnFireStore(this@EditPostActivity, postHashMap, postID)
        }
    }


    private suspend fun getTimeNow(){
        dateAndTimePair = getTime?.getCurrentTimeAndDate()
        if (dateAndTimePair != null){
            if (dateAndTimePair!!.first != null && dateAndTimePair!!.second != null){
                dateNow = dateAndTimePair!!.first!!
                secondsNow = dateAndTimePair!!.second!!
            }
        }
    }

    fun updatePostOnFireStoreSuccess(){
        val sortedPosts = Constants.sortedPosts
        sortedPosts.remove(post.timeCreatedMillis)
        // assuming that only visible posts are editable
        newPost = PostStructure(post.profilePicture, post.userId, post.userName, newPostText,
            newPostImage, newTimeCreatedMillis, newTimeCreatedToShow, "now", true, post.postID, true)
        sortedPosts[newTimeCreatedMillis] = newPost
        hideProgressDialog()
        Toast.makeText(this, "post updated successfully", Toast.LENGTH_LONG).show()
        val intent = Intent(this@EditPostActivity, FragmentActivity::class.java)
        startActivity(intent)
        finish()
    }


    fun deletePostOnFireStoreSuccess(){
        if (post.postImage.isNotEmpty()){
            FireStoreClass().deleteImageFromCloudStorage(post.postImage)
        }
        val sortedPosts = Constants.sortedPosts
        sortedPosts.remove(post.timeCreatedMillis)
        hideProgressDialog()
        Toast.makeText(this, "Post Deleted", Toast.LENGTH_LONG).show()
        val intent = Intent(this@EditPostActivity, FragmentActivity::class.java)
        startActivity(intent)
        finish()
    }

}