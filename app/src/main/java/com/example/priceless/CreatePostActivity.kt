package com.example.priceless

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var newPost: PostStructure
    private var finalTimeInMillis: String = ""
    private var formattedDateTime: String = ""
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var getTime: GetTime? = null
    private lateinit var dateAndTimePair: Pair<String, String>
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)


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

        //getTime = GetTime()

        showProgressDialog()
        val userID = FireStoreClass().getUserID()
        FireStoreClass().getUserInfoFromFireStore(this, userID)

        tvSendPost.setOnClickListener(this@CreatePostActivity)
        ivPostImage.setOnClickListener(this@CreatePostActivity)

        cbSendToFuture.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnSelectDate.visibility = VISIBLE
                btnSelectDate.setOnClickListener(this@CreatePostActivity)
            } else {
                btnSelectDate.visibility = View.GONE
                tvSelectedDate.visibility = View.GONE
                finalTimeInMillis = ""
            }
        }


    }


    private suspend fun createPost() {
        getTimeNow()
        if (secondsNow.isEmpty() || dateNow.isEmpty()){
            showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
            hideProgressDialog()
        }else{
            val profilePicture = userInfo.image
            val userId = userInfo.id
            val userName = userInfo.userName
            val postText = etPostText.text.toString()
            val postImage = imageNameUrl
            val timeCreatedMillis = secondsNow
            val timeCreatedToShow = dateNow
            val timeToShare = finalTimeInMillis.ifEmpty { "now" }
            val visibility = timeToShare == "now"
            val timeTraveler = !visibility
            if (timeTraveler){
                if (timeToShare.toLong() <= secondsNow.toLong()){
                    showErrorSnackBar("Please select a future date", true)
                    hideProgressDialog()
                }else{
                    val postID = ""
                    val edited = false
                    newPost = PostStructure(profilePicture, userId, userName, postText, postImage,
                        timeCreatedMillis, timeCreatedToShow, timeToShare, visibility, timeTraveler, postID, edited)
                    FireStoreClass().createPostOnFireStore(this, newPost)
                }
            }else{
                val postID = ""
                val edited = false
                newPost = PostStructure(profilePicture, userId, userName, postText, postImage,
                    timeCreatedMillis, timeCreatedToShow, timeToShare, visibility, timeTraveler, postID, edited)
                FireStoreClass().createPostOnFireStore(this, newPost)
            }
        }
    }


    fun createPostSuccessful(){
        // updating sortedPosts
        val sortedPosts = Constants.sortedPosts
        sortedPosts[newPost.timeCreatedMillis] = newPost
        hideProgressDialog()
        Toast.makeText(this, "Your Post Was Sent Successfully", Toast.LENGTH_LONG).show()
        val intent = Intent(this@CreatePostActivity, FragmentActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
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
                R.id.btn_select_date -> {
                    if (btnSelectDate.visibility == VISIBLE){
                        coroutineScope.launch {
                            try {
                                showDatePickerDialog()
                            } catch (e: Exception) {
                                Log.d("err showing DatePickerDialog", e.message.toString())
                            }
                        }
                    }
                }
                R.id.tv_send_post_create_post -> {
                    if (validateUserInput()){
                        if (cbSendToFuture.isChecked && finalTimeInMillis.isEmpty()){
                            showErrorSnackBar("please select a date or uncheck the box", true)
                        }else{
                            showProgressDialog()
                            if (imageURI != null) {
                                FireStoreClass().uploadImageToCloudStorage(this, imageURI!!,
                                    "post_image")
                            }else{
                                coroutineScope.launch {
                                    try {
                                        createPost()
                                    } catch (e: Exception) {
                                        Log.d("err calling createPost", e.message.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
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


    private fun validateUserInput(): Boolean {
        val postText = etPostText.text.toString()

        if (TextUtils.isEmpty(postText)) {
            showErrorSnackBar("Please Enter Text", true)
            return false
        }

        if (postText.length > 2000){
            showErrorSnackBar("Post Text Too Long.", true)
            return false
        }

        val disallowedPattern = Regex("[\\[\\]#/<\\\\>]")

        if (disallowedPattern.containsMatchIn(postText)) {
            showErrorSnackBar("You Cant Use These Characters: \\[]<>#/ In Post Text.", true)
            return false
        }

        return true
    }



    fun uploadImageOnCloudSuccess(imageUrl: String){
        imageNameUrl = imageUrl
        coroutineScope.launch {
            try {
                createPost()
            } catch (e: Exception) {
                Log.d("err calling createPost", e.message.toString())
            }
        }
    }


    private suspend fun getTimeNow() {
        val result = GetTime().getCurrentTimeAndDate()

        if (result.isSuccess) {
            val dateAndTimePair = result.getOrNull()
            if (dateAndTimePair != null) {
                dateNow = dateAndTimePair.first
                secondsNow = dateAndTimePair.second
            }
        } else {
            val exception = result.exceptionOrNull()
            if (exception != null) {
                Log.e("Error getting time", exception.message.toString(), exception)
            }
        }
    }


    private suspend fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        showProgressDialog()
        getTimeNow()
        hideProgressDialog()
        if (dateNow.isEmpty() || secondsNow.isEmpty()){
            showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
        }else{
            val onlineTime = dateNow
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            try {
                val onlineDate = sdf.parse(onlineTime)
                if (onlineDate != null) {
                    calendar.time = onlineDate
                }
            } catch (e: ParseException) {
                // Handle the parsing exception, e.g., log it or show an error message
                showErrorSnackBar("Error parsing online time", true)
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            //
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val datePickerDialog = DatePickerDialog(
                this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
                    //
                    selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                    selectedDate.set(Calendar.MINUTE, minute)
                    Log.d("compare", "${selectedDate.timeInMillis/1000} is it < ${secondsNow.toLong()}")
                    if (selectedDate.timeInMillis/1000+20 < secondsNow.toLong()) {
                        showErrorSnackBar("Please select a future date", true)
                    } else {
                        showTimePickerDialog(selectedDate)
                    }
                },
                year, month, day
            )

            // Set a minimum date (e.g., prevent selecting a past date)
            datePickerDialog.datePicker.minDate = secondsNow.toLong()*1000 - 1000
            datePickerDialog.show()
        }
    }

    private fun showTimePickerDialog(selectedDate: Calendar) {
        // if shown time was wrong we should set the hour and minute based on our online time here
        val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
        val minute = selectedDate.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this, { _, selectedHourOfDay, selectedMinute ->
                // Combine date and time
                selectedDate.set(Calendar.HOUR_OF_DAY, selectedHourOfDay)
                selectedDate.set(Calendar.MINUTE, selectedMinute)

                val combinedTimeInMillis = selectedDate.timeInMillis/1000
                Log.d("finalCombinedTimeMillis", "$combinedTimeInMillis")

                if (combinedTimeInMillis <= secondsNow.toLong()) {
                    showErrorSnackBar("Please select a future date", true)
                } else {
                    // Now, you can use combinedTimeInMillis for further processing
                    finalTimeInMillis = combinedTimeInMillis.toString()
                    //val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    formattedDateTime = sdf.format(selectedDate.time)
                    tvSelectedDate.visibility = VISIBLE
                    tvSelectedDate.text = "will be visible at: $formattedDateTime"
                }
            },
            hour, minute, false
        )

        timePickerDialog.show()
    }




}