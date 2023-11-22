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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.IOException

class ProfileActivity : BaseActivity(), OnClickListener {

    private lateinit var ivImageProfile: ImageView
    private lateinit var etUserName: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbPublic: RadioButton
    private lateinit var rbPrivate: RadioButton
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
        radioGroup = findViewById(R.id.radioGroup_profile)
        rbPublic = findViewById(R.id.rb_public_profile)
        rbPrivate = findViewById(R.id.rb_private_profile)
        btnSave = findViewById(R.id.btn_save_profile)
        toolbarProfile = findViewById(R.id.toolbar_profile)
        btnLogOut = findViewById(R.id.btn_LogOut_profile)

        showProgressDialog()
        val userID = FireStoreClass().getUserID()
        FireStoreClass().getUserInfoFromFireStore(this, userID)

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
        etPhoneNumber.setText(userInfo.phoneNumber.toString())
        if (userInfo.publicProfile){
            rbPublic.isChecked = true
        }else{
            rbPrivate.isChecked = true
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
        val userName = etUserName.text.toString().lowercase().trim { it <= ' ' }
        val firstName = etFirstName.text.toString().trim { it <= ' ' }
        val lastName = etLastName.text.toString().trim { it <= ' ' }
        val phoneNumber = etPhoneNumber.text.toString().trim { it <= ' ' }
        val reservedUsernames = listOf("admin", "priceless", "root", "moderator", "support", "official",
            "anonymous", "system", "bot", "test", "report", "feedback", "contact", "help", "terms",
            "privacy", "security", "register", "login", "logout", "signup", "settings", "profile",
            "account", "user", "users", "blocked", "banned", "spam", "feedback", "master", "invalid",
            "invalidusername", "unavailable", "service", "error", "server", "blockeduser", "guest")
        val allowedRegexForUserName = Regex("^[a-z0-9_-]*$")
        val allowedRegexForName = Regex("^[a-zA-Z0-9_-]*$")
        when{
            TextUtils.isEmpty(phoneNumber) -> {
                showErrorSnackBar("Please Enter Phone Number", true)
                callback(false)
            }
            phoneNumber.length > 30 -> {
                showErrorSnackBar("Phone Number Too Long", true)
                callback(false)
            }
            TextUtils.isEmpty(userName) -> {
                showErrorSnackBar("Please Enter User Name", true)
                callback(false)
            }
            userName.length !in 3..20 -> {
                showErrorSnackBar("User Name Should Be At Least 3 And Max 20 Characters", true)
                callback(false)
            }
            !userName.matches(allowedRegexForUserName) -> {
                showErrorSnackBar("Allowed Characters For UserName Are: letters (a-z), numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
                callback(false)
            }
            userName in reservedUsernames -> {
                showErrorSnackBar("User Name Is Already Taken", true)
                callback(false)
            }
            TextUtils.isEmpty(firstName) -> {
                showErrorSnackBar("Please Enter First Name", true)
                callback(false)
            }
            firstName.length > 30 -> {
                showErrorSnackBar("First Name Too Long", true)
                callback(false)
            }
            !firstName.matches(allowedRegexForName) -> {
                showErrorSnackBar("Allowed Characters For FirstName Are: letters (a-z),(A-Z) numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
                callback(false)
            }
            TextUtils.isEmpty(lastName) -> {
                showErrorSnackBar("Please Enter Last Name", true)
                callback(false)
            }
            lastName.length > 30 -> {
                showErrorSnackBar("Last Name Too Long", true)
                callback(false)
            }
            !lastName.matches(allowedRegexForName) -> {
                showErrorSnackBar("Allowed Characters For LastName Are: letters (a-z),(A-Z) numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
                callback(false)
            }
            else -> {
                // Check if the username is taken
                if (userInfo.userName != etUserName.text.toString().lowercase().trim { it <= ' ' }){
                    showProgressDialog()
                    FireStoreClass().isUserNameTaken(etUserName.text.toString().lowercase().trim { it <= ' ' }) { isTaken ->
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
        val userName = etUserName.text.toString().lowercase().trim { it <= ' ' }
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
        val publicProfile = rbPublic.isChecked
        userHashMap["publicProfile"] = publicProfile

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
            FireStoreClass().deleteImageFromCloudStorage(userInfo.image) { yep ->
                if (yep){
                    imageNameUrl = imageUrl
                    updateUserInfo()
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Error Adding New Image On Cloud.", Toast.LENGTH_SHORT).show()
                }
            }
        }else{
            imageNameUrl = imageUrl
            updateUserInfo()
        }
    }



}