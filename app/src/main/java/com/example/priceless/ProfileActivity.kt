package com.example.priceless

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.IOException

class ProfileActivity : BaseActivity(), OnClickListener {

    private lateinit var ivImageProfile: ImageView
    private lateinit var etUserName: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSave: Button
    private lateinit var userInfo: User
    private var imageNameUrl: String = ""
    private var imageURI: Uri? = null
    private lateinit var toolbarProfile: Toolbar
    private lateinit var btnLogOut: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        ivImageProfile = findViewById(R.id.iv_image_profile)
        etUserName = findViewById(R.id.et_UserName_profile)
        etFirstName = findViewById(R.id.et_firstName_profile)
        etLastName = findViewById(R.id.et_lastName_profile)
        etEmail = findViewById(R.id.et_email_profile)
        etPhoneNumber = findViewById(R.id.et_phoneNumber_profile)
        btnSave = findViewById(R.id.btn_save_profile)
        toolbarProfile = findViewById(R.id.toolbar_profile)
        btnLogOut = findViewById(R.id.btn_LogOut_profile)

        showProgressDialog()
        FireStoreClass().getUserInfoFromFireStore(this)

        ivImageProfile.setOnClickListener(this@ProfileActivity)
        btnSave.setOnClickListener(this@ProfileActivity)
        btnLogOut.setOnClickListener(this@ProfileActivity)

    }

    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
        userInfo = user
        setUserInfo()
    }


    private fun setUserInfo(){
        setActionBar()
        etUserName.setText(userInfo.userName)
        etFirstName.setText(userInfo.firstName)
        etLastName.setText(userInfo.lastName)
        etEmail.isEnabled = false
        etEmail.setText(userInfo.email)
        if (userInfo.phoneNumber != 0L){
            etPhoneNumber.setText(userInfo.phoneNumber.toString())
        }
        GlideLoader(this).loadImageUri(userInfo.image, ivImageProfile)
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.iv_image_profile -> {
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                        Constants.showImageFromStorage(this@ProfileActivity)
                    }else{
                        ActivityCompat.requestPermissions(this@ProfileActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.PermissionExternalStorageCode)
                    }
                }
                R.id.btn_save_profile -> {
                    validateUserInput { isValid ->
                        if (isValid) {
                            showProgressDialog()
                            if (imageURI != null) {
                                FireStoreClass().uploadImageToCloudStorage(this, imageURI!!,
                                    "profile_image")
                            } else {
                                updateUserInfo()
                            }
                        }
                    }
                }
                R.id.btn_LogOut_profile -> {
                    Firebase.auth.signOut()
                    val intent = Intent(this, LogIn::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@ProfileActivity)
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
                            GlideLoader(this).loadImageUri(imageURI!!, ivImageProfile)
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


    private fun setActionBar(){
        setSupportActionBar(toolbarProfile)
        val actionBar = supportActionBar
        actionBar?.title = ""
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back)
        }
        toolbarProfile.setNavigationOnClickListener { onBackPressed() }
    }


    private fun validateUserInput(callback: (Boolean) -> Unit){
        when{
            TextUtils.isEmpty(etPhoneNumber.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar("enter phone number", true)
                callback(false)
            }
            TextUtils.isEmpty(etUserName.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar("enter user name", true)
                callback(false)
            }
            else -> {
                // Check if the username is taken
                if (userInfo.userName != etUserName.text.toString().trim { it <= ' ' }){
                    showProgressDialog()
                    FireStoreClass().isUserNameTaken(etUserName.text.toString().trim { it <= ' ' }) { isTaken ->
                        hideProgressDialog()
                        if (isTaken) {
                            showErrorSnackBar("User Name Is Already Taken", true)
                            callback(false)
                        } else {
                            // Username is not taken, continue with registration
                            callback(true)
                        }
                    }
                }else{
                    callback(true)
                }
            }
        }
    }


    private fun updateUserInfo(){
        val userHashMap = HashMap<String, Any>()
        val phoneNumber = etPhoneNumber.text.toString().trim { it <= ' ' }
        if (imageNameUrl.isNotEmpty()){
            userHashMap[Constants.Image] = imageNameUrl
        }
        val userName = etUserName.text.toString().trim { it <= ' ' }
        val firstName = etFirstName.text.toString().trim { it <= ' ' }
        val lastName = etLastName.text.toString().trim { it <= ' ' }

        if (phoneNumber.isNotEmpty() && phoneNumber != userInfo.phoneNumber.toString()){
            userHashMap[Constants.PhoneNumber] = phoneNumber.toLong()
        }
        if (userName != userInfo.userName){
            userHashMap[Constants.UserName] = userName
        }
        if (firstName != userInfo.firstName){
            userHashMap[Constants.FirstName] = firstName
        }
        if (lastName != userInfo.lastName){
            userHashMap[Constants.LastName] = lastName
        }
        userHashMap[Constants.ProfileCompleted] = 1

        FireStoreClass().updateUserInfoOnFireStore(this@ProfileActivity, userHashMap,
            userInfo.userName, userName)

    }


    fun updateUserInfoOnFireStoreSuccess(){
        hideProgressDialog()
        Toast.makeText(this, "update user info success", Toast.LENGTH_LONG).show()
        val intent = Intent(this@ProfileActivity, FragmentActivity::class.java)
        intent.putExtra(Constants.User_Extra_Details, userInfo)
        startActivity(intent)
        finish()
    }

    fun uploadImageOnCloudSuccess(imageUrl: String){
        if (userInfo.image.isNotEmpty()){
            FireStoreClass().deleteImageFromCloudStorage(userInfo.image)
        }
        imageNameUrl = imageUrl
        updateUserInfo()
    }



}