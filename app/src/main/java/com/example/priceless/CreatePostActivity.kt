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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException

class CreatePostActivity : BaseActivity(), OnClickListener {

    private lateinit var cbSendToFuture: CheckBox
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var etPostText: EditText
    private lateinit var ivPostImage: ImageView
    private lateinit var tvSendPost: TextView
    private lateinit var userInfo: User
    private var imageNameUrl: String = ""
    private var imageURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        cbSendToFuture = findViewById(R.id.cb_send_to_future)
        btnSelectDate = findViewById(R.id.btn_select_date)
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        ivProfilePic = findViewById(R.id.iv_profile_pic_create_post)
        tvUserName = findViewById(R.id.tv_user_name_create_post)
        etPostText = findViewById(R.id.et_main_post_text_create_post)
        ivPostImage = findViewById(R.id.iv_post_image_create_post)
        tvSendPost = findViewById(R.id.tv_send_post_create_post)

        FireStoreClass().getUserInfoFromFireStore(this)

        tvSendPost.setOnClickListener(this@CreatePostActivity)
        ivPostImage.setOnClickListener(this@CreatePostActivity)



    }


    private fun createPost(){
        val profilePicture = userInfo.image
        val userId = userInfo.id
        val userName = userInfo.userName
        val postText = etPostText.text.toString()
        val postImage = imageNameUrl
        val timeCreated = System.currentTimeMillis().toString()
        val visibility = true
        val timeToShare = "now"
        val newPost = PostStructure(profilePicture, userId, userName, postText, postImage,
            timeCreated, visibility, timeToShare)
        FireStoreClass().createPostOnFireStore(this, newPost)
    }


    fun createPostSuccessful(){
        hideProgressDialog()
        Toast.makeText(this, "your post was sent successfully", Toast.LENGTH_LONG).show()
        val intent = Intent(this@CreatePostActivity, FragmentActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun successGettingUserInfoFromFireStore(user: User){
        userInfo = user
        setUserInfo()
    }

    private fun setUserInfo(){
        tvUserName.text = userInfo.userName
        if (userInfo.image.isNotEmpty()){
            GlideLoader(this).loadImageUri(userInfo.image, ivProfilePic)
        }else{
            GlideLoader(this).loadImageUri(R.drawable.ic_baseline_account_circle_24, ivProfilePic)
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.iv_post_image_create_post -> {
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                        Constants.showImageFromStorage(this@CreatePostActivity)
                    }else{
                        ActivityCompat.requestPermissions(this@CreatePostActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.PermissionExternalStorageCode)
                    }
                }
                R.id.tv_send_post_create_post -> {
                    if (validateUserInput()){
                        showProgressDialog()
                        if (imageURI != null) {
                            FireStoreClass().uploadImageToCloudStorage(this, imageURI!!)
                        } else {
                            createPost()
                        }
                    }
                }
            }
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@CreatePostActivity)
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
        }else{
            true
        }
    }


    fun uploadImageOnCloudSuccess(imageUrl: String){
        imageNameUrl = imageUrl
        createPost()
    }


}