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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class CreatePostActivity : BaseActivity(), OnClickListener {

    private lateinit var cbSendToFuture: CheckBox
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var etPostText: TextInputEditText
    private lateinit var ivPostImage: ImageView
    private lateinit var tvSendPost: TextView
    private var userInfo: User? = null
    private var imageNameUrl: String = ""
    private var imageURI: Uri? = null
    private lateinit var newPost: PostStructure
    private var finalTimeInMillis: String = ""
    private var formattedDateTime: String = ""
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var layoutPrice: LinearLayout
    private lateinit var cbPrice: CheckBox
    private lateinit var tilPrice: TextInputLayout
    private lateinit var etPrice: TextInputEditText
    private lateinit var tvGoToProfile: TextView
    private var userID = ""


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
        layoutPrice = findViewById(R.id.layout_price)
        cbPrice = findViewById(R.id.cb_price)
        tilPrice = findViewById(R.id.text_input_Layout_price)
        etPrice = findViewById(R.id.et_price)
        tvGoToProfile = findViewById(R.id.tv_go_to_profile_from_create_post)

        //getTime = GetTime()

        showProgressDialog()
        FireStoreClass().getCurrentUserID { uID ->
            if (uID.isNotEmpty()){
                userID = uID
                FireStoreClass().getUserInfoFromFireStore(this, userID)
            }else{
                hideProgressDialog()
                showErrorSnackBar("Error Getting User ID.", true)
            }
        }

        tvSendPost.setOnClickListener(this@CreatePostActivity)
        ivPostImage.setOnClickListener(this@CreatePostActivity)

        cbSendToFuture.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnSelectDate.visibility = VISIBLE
                btnSelectDate.setOnClickListener(this@CreatePostActivity)
                layoutPrice.visibility = VISIBLE
            } else {
                btnSelectDate.visibility = View.GONE
                tvSelectedDate.visibility = View.GONE
                finalTimeInMillis = ""
                layoutPrice.visibility = View.GONE
                cbPrice.isChecked = false
                tilPrice.visibility = View.GONE
                etPrice.visibility = View.GONE
                etPrice.setText("")
            }
        }

        cbPrice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (userInfo != null){
                    if (userInfo!!.wallet.isNotEmpty()){
                        tilPrice.visibility = VISIBLE
                        etPrice.visibility = VISIBLE
                        tvGoToProfile.visibility = View.GONE
                    }else{
                        tilPrice.visibility = View.GONE
                        etPrice.visibility = View.GONE
                        etPrice.setText("")
                        tvGoToProfile.visibility = VISIBLE
                        tvGoToProfile.setOnClickListener(this@CreatePostActivity)
                    }
                }else{
                    showErrorSnackBar("Error Getting User Info.", true)
                    tilPrice.visibility = View.GONE
                    etPrice.visibility = View.GONE
                    tvGoToProfile.visibility = View.GONE
                    etPrice.setText("")
                }
            } else {
                tilPrice.visibility = View.GONE
                etPrice.visibility = View.GONE
                tvGoToProfile.visibility = View.GONE
                etPrice.setText("")
            }
        }


    }


    private suspend fun createPost() {
        if (userInfo != null){
            getTimeNow()
            if (secondsNow.isEmpty() || dateNow.isEmpty()){
                showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
                hideProgressDialog()
            }else{
                val profilePicture = userInfo!!.image
                val userId = userInfo!!.id
                val userName = userInfo!!.userName
                val postText = etPostText.text.toString()
                val postImage = imageNameUrl
                val timeCreatedMillis = secondsNow
                val timeCreatedToShow = dateNow
                val timeToShare = finalTimeInMillis.ifEmpty { "now" }
                val visibility = timeToShare == "now"
                val timeTraveler = !visibility
                val price = etPrice.text.toString().ifEmpty { "" }
                val buyerID = ""
                if (timeTraveler){
                    if (timeToShare.toLong() <= secondsNow.toLong()){
                        showErrorSnackBar("Please select a future date", true)
                        hideProgressDialog()
                    }else{
                        val postID = System.currentTimeMillis().toString()
                        val edited = false
                        newPost = PostStructure(profilePicture, userId, userName, postText, postImage,
                            timeCreatedMillis, timeCreatedToShow, timeToShare, visibility,
                            timeTraveler, price, buyerID, postID, edited)
                        FireStoreClass().createPostOnFireStore(this, userId, newPost)
                    }
                }else{
                    val postID = System.currentTimeMillis().toString()
                    val edited = false
                    newPost = PostStructure(profilePicture, userId, userName, postText, postImage,
                        timeCreatedMillis, timeCreatedToShow, timeToShare, visibility,
                        timeTraveler, price, buyerID, postID, edited)
                    FireStoreClass().createPostOnFireStore(this, userId, newPost)
                }
            }
        }else{
            showErrorSnackBar("Error Getting User Info.", true)
        }
    }

    fun createPostSuccessful(){
        hideProgressDialog()
        Toast.makeText(this, "Your Post Was Sent Successfully", Toast.LENGTH_LONG).show()
        val intent = Intent(this@CreatePostActivity, FragmentActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun createPostFailed(){
        hideProgressDialog()
        showErrorSnackBar("Error while Creating Post", true)
    }

    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
        userInfo = user
        setUserInfo()
    }

    private fun setUserInfo(){
        if (userInfo != null){
            tvUserName.text = userInfo!!.userName
            if (userInfo!!.image.isNotEmpty()){
                GlideLoader(this).loadImageUri(userInfo!!.image, ivProfilePic)
            }else{
                GlideLoader(this).loadImageUri(R.drawable.ic_baseline_account_circle_24, ivProfilePic)
            }
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
                R.id.tv_go_to_profile_from_create_post -> {
                    if (tvGoToProfile.visibility == VISIBLE){
                        val intent = Intent(this@CreatePostActivity, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
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

    @Deprecated("Deprecated in Java")
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
        val amountOfPrice = etPrice.text.toString().trim()
        if (cbPrice.isChecked && amountOfPrice.isEmpty()){
            showErrorSnackBar("Please Set a Price or Uncheck The Box.", true)
            return false
        }
        if (cbPrice.isChecked && amountOfPrice.isNotEmpty()){
            if (amountOfPrice.startsWith(".") || amountOfPrice.endsWith(".")){
                showErrorSnackBar("Invalid price format", true)
                return false
            }
            try {
                val price = BigDecimal(amountOfPrice)
                if (price <= BigDecimal.ZERO) {
                    showErrorSnackBar("Price must be greater than 0", true)
                    return false
                }
            } catch (e: NumberFormatException) {
                showErrorSnackBar("Invalid price format", true)
                return false
            }
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