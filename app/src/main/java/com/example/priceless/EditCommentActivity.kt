package com.example.priceless

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException

class EditCommentActivity : BaseActivity(), OnClickListener {

    private lateinit var tvCommentText: TextView
    private lateinit var etCommentEdit: TextInputEditText
    private lateinit var ivCommentPhoto: ImageView
    private lateinit var tvTimeCreated: TextView
    private lateinit var tvPrivateComment: TextView
    private lateinit var btnEditAndUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button
    private lateinit var comment: CommentStructure
    private var editOrUpdateSituation = "edit"
    private var imageURI: Uri? = null
    private var newImageNameUrl: String = ""
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var ibRemovePhoto: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_comment)

        tvCommentText = findViewById(R.id.tv_comment_text_edit)
        etCommentEdit = findViewById(R.id.et_comment_edit)
        ivCommentPhoto = findViewById(R.id.iv_comment_photo_edit)
        tvTimeCreated = findViewById(R.id.tv_time_created_comment_edit)
        tvPrivateComment = findViewById(R.id.tv_private_comment_edit)
        btnEditAndUpdate = findViewById(R.id.btn_edit_and_update_comment)
        btnCancel = findViewById(R.id.btn_cancel_editing_comment)
        btnDelete = findViewById(R.id.btn_delete_comment_edit)
        ibRemovePhoto = findViewById(R.id.ib_remove_comment_photo_edit)

        if (intent.hasExtra("comment")){
            comment = intent.getParcelableExtra("comment")!!
        }

        setCommentInfo()

        btnEditAndUpdate.setOnClickListener(this)
        btnCancel.setOnClickListener(this)

    }

    private fun setCommentInfo(){
        tvCommentText.text = comment.text
        if (comment.commentPhoto.isNotEmpty()){
            ivCommentPhoto.setOnClickListener(this)
            GlideLoader(this).loadImageUri(comment.commentPhoto, ivCommentPhoto)
        }else{
            ivCommentPhoto.setImageResource(R.drawable.ic_baseline_image_24)
        }
        tvTimeCreated.text = comment.timeCreatedToShow
        if (comment.isPrivate){
            tvPrivateComment.visibility = VISIBLE
        }else{
            tvPrivateComment.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.iv_comment_photo_edit -> {
                    if (comment.commentPhoto.isNotEmpty()){
                        val intent = Intent(this, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", comment.commentPhoto)
                        startActivity(intent)
                    }
                }
                R.id.ib_remove_comment_photo_edit -> {
                    if (imageURI != null && ibRemovePhoto.visibility == VISIBLE){
                        imageURI = null
                        if (comment.commentPhoto.isNotEmpty()){
                            //ivCommentPhoto.setOnClickListener(this)
                            GlideLoader(this).loadImageUri(comment.commentPhoto, ivCommentPhoto)
                        }else{
                            ivCommentPhoto.setImageResource(R.drawable.ic_baseline_image_24)
                        }
                        ibRemovePhoto.visibility = View.GONE
                    }
                }
                R.id.btn_edit_and_update_comment -> {
                    if (editOrUpdateSituation == "edit"){
                        btnEditAndUpdate.text = "Update"
                        showErrorSnackBar("Now You Can Edit Comment Or Change Image.", false)
                        tvCommentText.visibility = View.GONE
                        etCommentEdit.visibility = VISIBLE
                        etCommentEdit.setText(comment.text)
                        btnDelete.visibility = VISIBLE
                        btnDelete.setOnClickListener(this)
                        ivCommentPhoto.setOnClickListener {
                            if (ContextCompat.checkSelfPermission(this,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED){
                                Constants.showImageFromStorage(this@EditCommentActivity)
                            }else{
                                ActivityCompat.requestPermissions(this@EditCommentActivity,
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
                                    "comment_image")
                            }else{
                                updateComment()
                            }
                        }
                    }
                }
                R.id.btn_cancel_editing_comment -> {
                    finish()
                }
                R.id.btn_delete_comment_edit -> {
                    if (btnDelete.visibility == VISIBLE){
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("DELETE COMMENT?")
                        builder.setMessage("comment will be deleted permanently!")
                        builder.setIcon(R.drawable.ic_round_warning_24)
                        builder.setPositiveButton("Yes") { dialog, _ ->
                            showProgressDialog()
                            when{
                                comment.isPrivate -> {
                                    if (comment.topCommentIDForReply.isNotEmpty()){
                                        deletePrivateReply()
                                    }else{
                                        deletePrivateComment()
                                    }
                                }
                                else -> {
                                    if (comment.topCommentIDForReply.isNotEmpty()){
                                        deletePublicReply()
                                    }else{
                                        deletePublicComment()
                                    }
                                }
                            }
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

    fun uploadImageOnCloudSuccess(newImageUrl: String){
        if (comment.commentPhoto.isNotEmpty()){
            FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yep ->
                if (yep){
                    newImageNameUrl = newImageUrl
                    updateComment()
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Error Adding New Image On Cloud.", Toast.LENGTH_SHORT).show()
                }
            }
        }else{
            newImageNameUrl = newImageUrl
            updateComment()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@EditCommentActivity)
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
                            GlideLoader(this).loadImageUri(imageURI!!, ivCommentPhoto)
                            ibRemovePhoto.visibility = VISIBLE
                            ibRemovePhoto.setOnClickListener(this)
                        }else{
                            ibRemovePhoto.visibility = View.GONE
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "image selection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED){
            ibRemovePhoto.visibility = View.GONE
            Log.e("image selection failed", "image has not been selected")
        }
    }

    private fun updateComment(){
        CoroutineScope(Dispatchers.Main).launch {
            val timeJob = async { getTimeNow() }
            timeJob.await()
            if (secondsNow.isEmpty() || dateNow.isEmpty()){
                showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
                hideProgressDialog()
            }else{
                val commentHashMap = HashMap<String, Any>()
                if (comment.text != etCommentEdit.text.toString()){
                    commentHashMap["text"] = etCommentEdit.text.toString()
                }
                if (newImageNameUrl.isNotEmpty()){
                    commentHashMap["commentPhoto"] = newImageNameUrl
                }
                commentHashMap["timeCreated"] = secondsNow
                commentHashMap["edited"] = true
                if (comment.isPrivate){
                    if (comment.topCommentIDForReply.isNotEmpty()){
                        FireStoreClass().updatePrivateReply(comment, commentHashMap) { onSuccess ->
                            if (onSuccess){
                                hideProgressDialog()
                                Toast.makeText(this@EditCommentActivity, "Reply Updated Successfully", Toast.LENGTH_LONG).show()
                                finish()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error While Updating Reply.", true)
                            }
                        }
                        //
                    }else{
                        FireStoreClass().updatePrivateComment(comment.postOwnerUID, comment.postID,
                            comment.writerUID, comment.commentID, commentHashMap) { onSuccess ->
                            if (onSuccess){
                                hideProgressDialog()
                                Toast.makeText(this@EditCommentActivity, "Comment Updated Successfully", Toast.LENGTH_LONG).show()
                                finish()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error While Updating Comment.", true)
                            }
                        }
                    }
                }else{
                    if (comment.topCommentIDForReply.isNotEmpty()){
                        FireStoreClass().updatePublicReply(comment, commentHashMap) { succeed ->
                            if (succeed){
                                hideProgressDialog()
                                Toast.makeText(this@EditCommentActivity, "Reply Updated Successfully", Toast.LENGTH_LONG).show()
                                finish()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error While Updating Reply.", true)
                            }
                        }
                        //
                    }else{
                        FireStoreClass().updatePublicComment(comment.postOwnerUID, comment.postID,
                            comment.commentID, commentHashMap) { succeed ->
                            if (succeed){
                                hideProgressDialog()
                                Toast.makeText(this@EditCommentActivity, "Comment Updated Successfully", Toast.LENGTH_LONG).show()
                                finish()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error While Updating Comment.", true)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun deletePublicReply(){
        FireStoreClass().deletePublicReply(comment) { success ->
            if (success){
                if (comment.commentPhoto.isNotEmpty()){
                    FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yep ->
                        if (yep){
                            hideProgressDialog()
                            Toast.makeText(this, "Reply Deleted.", Toast.LENGTH_LONG).show()
                            finish()
                        }else{
                            hideProgressDialog()
                            showErrorSnackBar("Error While Deleting Reply.", true)
                        }
                    }
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Reply Deleted.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Error While Deleting Reply.", true)
            }
        }
    }

    private fun deletePublicComment(){
        FireStoreClass().deletePublicComment(comment.postOwnerUID,
            comment.postID, comment.commentID) { success ->
            if (success){
                if (comment.commentPhoto.isNotEmpty()){
                    FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { ok ->
                        if (ok){
                            hideProgressDialog()
                            Toast.makeText(this, "Comment Deleted.", Toast.LENGTH_LONG).show()
                            finish()
                        }else{
                            hideProgressDialog()
                            showErrorSnackBar("Error While Deleting Comment.", true)
                        }
                    }
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Comment Deleted.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Error While Deleting Comment.", true)
            }
        }
    }

    private fun deletePrivateComment(){
        FireStoreClass().deletePrivateComment(comment.postOwnerUID,
            comment.postID, comment.writerUID, comment.commentID) { onComplete ->
            if (onComplete){
                if (comment.commentPhoto.isNotEmpty()){
                    FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yep ->
                        if (yep){
                            hideProgressDialog()
                            Toast.makeText(this, "Comment Deleted.", Toast.LENGTH_LONG).show()
                            finish()
                        }else{
                            hideProgressDialog()
                            showErrorSnackBar("Error While Deleting Comment.", true)
                        }
                    }
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Comment Deleted.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Error While Deleting Comment.", true)
            }
        }
    }

    private fun deletePrivateReply(){
        FireStoreClass().deletePrivateReply(comment) { onComplete ->
            if (onComplete){
                if (comment.commentPhoto.isNotEmpty()){
                    FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yes ->
                        if (yes){
                            hideProgressDialog()
                            Toast.makeText(this, "Reply Deleted.", Toast.LENGTH_LONG).show()
                            finish()
                        }else{
                            hideProgressDialog()
                            showErrorSnackBar("Error While Deleting Reply.", true)
                        }
                    }
                }else{
                    hideProgressDialog()
                    Toast.makeText(this, "Reply Deleted.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Error While Deleting Reply.", true)
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

    private fun validateUserInput(): Boolean {
        val commentText = etCommentEdit.text.toString()
        val disallowedPattern = Regex("[\\[\\]#/<\\\\>]")

        return if (TextUtils.isEmpty(commentText)) {
            showErrorSnackBar("Please Enter Comment's Text", true)
            false
        }else if (commentText.length > 2000){
            showErrorSnackBar("Comment Text Too Long.", true)
            false
        }else if (disallowedPattern.containsMatchIn(commentText)) {
            showErrorSnackBar("You Cant Use These Characters: \\[]<>#/ In Comment Text.", true)
            false
        }else if (comment.text == commentText && imageURI == null){
            showErrorSnackBar("You Did Not Change Anything", true)
            false
        }else{
            true
        }
    }


}