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
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import java.io.IOException

class ReplyCommentActivity : BaseActivity(), OnClickListener {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvMainCommentText: TextView
    private lateinit var ivMainCommentPhoto: ImageView
    private lateinit var tvTimeCreatedMainComment: TextView
    private lateinit var tvPrivateMainComment: TextView
    private lateinit var tvNumberOfLikes: TextView
    private lateinit var ibLikeMainComment: ImageButton
    private lateinit var etNewReply: TextInputEditText
    private lateinit var ibAddPhotoToReply: ImageButton
    private lateinit var ibRemovePhotoFromReply: ImageButton
    private lateinit var ivReplyPhoto: ImageView
    private lateinit var tvAllRepliesArePrivate: TextView
    private lateinit var btnSendReply: Button
    private lateinit var rvReplies: RecyclerView
    private lateinit var progressBarReplies: ProgressBar
    private lateinit var comment: CommentStructure
    private var userInfo: User? = null
    private var likeSituationForPublicComment = false
    private var likeSituationForPrivateComment = false
    private var currentUserID = ""
    private var imageURI: Uri? = null
    private var imageNameUrl = ""
    private var dateNow = ""
    private var secondsNow = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reply_comment)

        ivProfilePic = findViewById(R.id.iv_profile_main_comment_reply)
        tvUserName = findViewById(R.id.tv_user_name_main_comment_reply)
        tvMainCommentText = findViewById(R.id.tv_main_comment_text_reply)
        ivMainCommentPhoto = findViewById(R.id.iv_main_comment_photo_reply)
        tvTimeCreatedMainComment = findViewById(R.id.tv_time_created_main_comment_reply)
        tvPrivateMainComment = findViewById(R.id.tv_private_main_comment_reply)
        tvNumberOfLikes = findViewById(R.id.tv_likes_of_main_comment_reply)
        ibLikeMainComment = findViewById(R.id.ib_like_main_comment_reply)
        etNewReply = findViewById(R.id.et_new_reply_comment)
        ibAddPhotoToReply = findViewById(R.id.ib_add_photo_to_reply_comment)
        ibRemovePhotoFromReply = findViewById(R.id.ib_remove_photo_reply_comment)
        ivReplyPhoto = findViewById(R.id.iv_reply_comment_photo)
        tvAllRepliesArePrivate = findViewById(R.id.tv_replies_are_private)
        btnSendReply = findViewById(R.id.send_reply_comment)
        rvReplies = findViewById(R.id.recycler_view_replies)
        progressBarReplies = findViewById(R.id.pb_replies)

        if (intent.hasExtra("com.example.priceless.comment")){
            comment = intent.getParcelableExtra("com.example.priceless.comment")!!
        }

        setCommentInfo()

        loadReplies()

        ivProfilePic.setOnClickListener(this@ReplyCommentActivity)
        ibLikeMainComment.setOnClickListener(this@ReplyCommentActivity)
        ibAddPhotoToReply.setOnClickListener(this@ReplyCommentActivity)
        btnSendReply.setOnClickListener(this@ReplyCommentActivity)

    }

    override fun onResume() {
        super.onResume()
        loadReplies()
    }


    private fun setCommentInfo(){
        CoroutineScope(Dispatchers.Main).launch {
            val userInfoJob = async {
                val deferredCompletable = CompletableDeferred<User?>()
                FireStoreClass().getUserInfoWithCallback(comment.writerUID) { user ->
                    deferredCompletable.complete(user)
                }
                userInfo = deferredCompletable.await()
            }
            userInfoJob.await()

            if (userInfo != null){
                comment.writerProfilePic = userInfo!!.image
                tvUserName.text = userInfo!!.userName
            }else{
                showErrorSnackBar("Error Getting User Info.", true)
            }
            if (comment.writerProfilePic.isNotEmpty()){
                GlideLoader(this@ReplyCommentActivity).loadImageUri(comment.writerProfilePic, ivProfilePic)
            }else{
                ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
            }
            tvMainCommentText.text = comment.text
            if (comment.commentPhoto.isNotEmpty()){
                ivMainCommentPhoto.visibility = VISIBLE
                GlideLoader(this@ReplyCommentActivity).loadImageUri(comment.commentPhoto,
                    ivMainCommentPhoto)
                ivMainCommentPhoto.setOnClickListener(this@ReplyCommentActivity)
            }else{
                ivMainCommentPhoto.visibility = View.GONE
            }
            tvTimeCreatedMainComment.text = comment.timeCreatedToShow
            if (comment.isPrivate){
                tvPrivateMainComment.visibility = VISIBLE
                tvAllRepliesArePrivate.visibility = VISIBLE
            }else{
                tvPrivateMainComment.visibility = View.GONE
                tvAllRepliesArePrivate.visibility = View.GONE
            }
            getNumberOfLikesForMainComment()
            val deferredCurrentUserID = async { FireStoreClass().getUserID() }
            currentUserID = deferredCurrentUserID.await()
            getLikeSituation()
        }
    }

    private fun getNumberOfLikesForMainComment(){
        if (comment.isPrivate){
            FireStoreClass().getNumberOfLikesForPrivateComment(comment.postOwnerUID, comment.postID,
                comment.commentID, comment.writerUID) { number ->
                if (number != null && number != 0){
                    tvNumberOfLikes.visibility = VISIBLE
                    tvNumberOfLikes.text = "$number"
                }else{
                    tvNumberOfLikes.visibility = View.GONE
                }
            }
        }else{
            FireStoreClass().getNumberOfLikesForPublicComment(comment.postOwnerUID, comment.postID,
                comment.commentID) { number ->
                if (number != null && number != 0){
                    tvNumberOfLikes.visibility = VISIBLE
                    tvNumberOfLikes.text = "$number"
                }else{
                    tvNumberOfLikes.visibility = View.GONE
                }
            }
        }
    }

    private fun getLikeSituation(){
        if (currentUserID.isNotEmpty()){
            if (comment.isPrivate){
                FireStoreClass().getLikeSituationForPrivateComment(comment.postOwnerUID, comment.postID,
                    comment.commentID, comment.writerUID, currentUserID) { yep ->
                    if (yep){
                        likeSituationForPrivateComment = true
                        ibLikeMainComment.setImageResource(R.drawable.thumb_liked)
                    }else{
                        likeSituationForPrivateComment = false
                        ibLikeMainComment.setImageResource(R.drawable.thumb_like)
                    }
                }
            }else{
                FireStoreClass().getLikeSituationForPublicComment(comment.postOwnerUID, comment.postID,
                    comment.commentID, currentUserID) { yep ->
                    if (yep){
                        likeSituationForPublicComment = true
                        ibLikeMainComment.setImageResource(R.drawable.thumb_liked)
                    }else{
                        likeSituationForPublicComment = false
                        ibLikeMainComment.setImageResource(R.drawable.thumb_like)
                    }
                }
            }
        }else{
            showErrorSnackBar("Error Getting Current User ID, Check Your Internet Connection.", true)
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.ib_like_main_comment_reply -> {
                    if (comment.isPrivate){
                        if (likeSituationForPrivateComment){
                            FireStoreClass().unLikePrivateComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, comment.writerUID, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    getLikeSituation()
                                    getNumberOfLikesForMainComment()
                                }
                            }
                        }else{
                            FireStoreClass().likePrivateComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, comment.writerUID, currentUserID) { success ->
                                if (success){
                                    getLikeSituation()
                                    getNumberOfLikesForMainComment()
                                }
                            }
                        }
                    }else{
                        if (likeSituationForPublicComment){
                            FireStoreClass().unLikePublicComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    getLikeSituation()
                                    getNumberOfLikesForMainComment()
                                }
                            }
                        }else{
                            FireStoreClass().likePublicComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, currentUserID) { success ->
                                if (success){
                                    getLikeSituation()
                                    getNumberOfLikesForMainComment()
                                }
                            }
                        }
                    }
                }
                R.id.iv_profile_main_comment_reply -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    intent.putExtra("userID", comment.writerUID)
                    startActivity(intent)
                }
                R.id.iv_main_comment_photo_reply -> {
                    if (comment.commentPhoto.isNotEmpty() && ivMainCommentPhoto.visibility == VISIBLE){
                        val intent = Intent(this, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", comment.commentPhoto)
                        startActivity(intent)
                    }
                }
                R.id.ib_add_photo_to_reply_comment -> {
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                        Constants.showImageFromStorage(this@ReplyCommentActivity)
                    }else{
                        ActivityCompat.requestPermissions(this@ReplyCommentActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.PermissionExternalStorageCode)
                    }
                }
                R.id.ib_remove_photo_reply_comment -> {
                    if (imageURI != null && ibRemovePhotoFromReply.visibility == VISIBLE
                        && ivReplyPhoto.visibility == VISIBLE){
                        imageURI = null
                        ivReplyPhoto.visibility = View.GONE
                        ibRemovePhotoFromReply.visibility = View.GONE
                    }
                }
                R.id.send_reply_comment -> {
                    if (validateUserInput()){
                        showProgressDialog()
                        if (imageURI != null) {
                            FireStoreClass().uploadImageToCloudStorage(this, imageURI!!,
                                "reply_image")
                        }else{
                            addNewReplyComment()
                        }
                    }
                }
            }
        }
    }

    fun uploadImageOnCloudSuccess(imageUrl: String){
        imageNameUrl = imageUrl
        addNewReplyComment()
    }

    private fun addNewReplyComment(){
        if (currentUserID.isNotEmpty()){
            CoroutineScope(Dispatchers.Main).launch {
                val timeJob = async { getTimeNow() }
                timeJob.await()
                if (secondsNow.isEmpty() || dateNow.isEmpty()){
                    showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
                    hideProgressDialog()
                }else{
                    val text = etNewReply.text.toString()
                    val commentPhoto = imageNameUrl
                    val timeCreated = secondsNow
                    val timeCreatedToShow = dateNow
                    val postID = comment.postID
                    val topCommentIDForReply = comment.commentID
                    val writerOfTopCommentUID = comment.writerUID
                    val postOwnerUID = comment.postOwnerUID
                    val writerOfReplyUID = currentUserID
                    val writerUserName = ""
                    val writerProfilePic = ""
                    val isPrivate = comment.isPrivate
                    val edited = false
                    val replyID = ""
                    val newReplyComment = CommentStructure(text, commentPhoto, timeCreated,
                        timeCreatedToShow, postID, topCommentIDForReply, writerOfTopCommentUID, postOwnerUID,
                        writerOfReplyUID, writerUserName, writerProfilePic, isPrivate, edited, replyID)
                    if (isPrivate){
                        FireStoreClass().addPrivateReply(newReplyComment) { onSuccess ->
                            if (onSuccess){
                                loadReplies()
                                etNewReply.setText("")
                                imageURI = null
                                imageNameUrl = ""
                                ivReplyPhoto.visibility = View.GONE
                                ibRemovePhotoFromReply.visibility = View.GONE
                                hideProgressDialog()
                                showErrorSnackBar("New Reply Added Successfully.", false)
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error Adding New Reply.", true)
                            }
                        }
                    }else{
                        FireStoreClass().addPublicReply(newReplyComment) { succeed ->
                            if (succeed){
                                loadReplies()
                                etNewReply.setText("")
                                imageURI = null
                                imageNameUrl = ""
                                ivReplyPhoto.visibility = View.GONE
                                ibRemovePhotoFromReply.visibility = View.GONE
                                hideProgressDialog()
                                showErrorSnackBar("New Reply Added Successfully.", false)
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error Adding New Reply.", true)
                            }
                        }
                    }
                }
            }
        }else{
            showErrorSnackBar("Error Getting Current User ID, Check Your Internet Connection.", true)
            hideProgressDialog()
        }
    }

    private fun loadReplies(){
        CoroutineScope(Dispatchers.Main).launch {
            val allReplies = ArrayList<CommentStructure>()
            val replies = ArrayList<CommentStructure>()
            if (comment.isPrivate){
                val privateRepliesJob = async {
                    val deferredCompletable = CompletableDeferred<ArrayList<CommentStructure>?>()
                    FireStoreClass().getPrivateReplies(comment) { privateRepliesList ->
                        deferredCompletable.complete(privateRepliesList)
                    }
                    val privateReplies = deferredCompletable.await()
                    if (privateReplies != null){
                        allReplies.addAll(privateReplies)
                    }
                }
                privateRepliesJob.await()
            }else{
                val publicRepliesJob = async {
                    val deferredCompletable = CompletableDeferred<ArrayList<CommentStructure>?>()
                    FireStoreClass().getPublicReplies(comment) { publicRepliesList ->
                        deferredCompletable.complete(publicRepliesList)
                    }
                    val publicReplies = deferredCompletable.await()
                    if (publicReplies != null){
                        allReplies.addAll(publicReplies)
                    }
                }
                publicRepliesJob.await()
            }

            for (reply in allReplies){
                if (reply.commentID.isNotEmpty()){
                    replies.add(reply)
                }
            }
            if (replies.isNotEmpty()){
                val userGroupedReplies = replies.groupBy { it.writerUID }
                val userInfoJobs = userGroupedReplies.map { (userId, groupReplies) ->
                    async {
                        val deferredUserInfo = CompletableDeferred<User?>()
                        FireStoreClass().getUserInfoWithCallback(userId) { userInfo ->
                            deferredUserInfo.complete(userInfo)
                        }
                        val userInfo = deferredUserInfo.await()
                        if (userInfo != null) {
                            groupReplies.forEach { replyItem ->
                                replyItem.writerProfilePic = userInfo.image
                                replyItem.writerUserName = userInfo.userName
                            }
                        }
                    }
                }
                userInfoJobs.awaitAll()

                replies.sortByDescending { it.timeCreated.toLong() }
                progressBarReplies.visibility = View.GONE
                rvReplies.visibility = VISIBLE
                val adapter = CommentsRvAdapter(this@ReplyCommentActivity, replies,
                    currentUserID)
                adapter.notifyDataSetChanged()
                val layOutManager = LinearLayoutManager(this@ReplyCommentActivity)
                rvReplies.layoutManager = layOutManager
                rvReplies.adapter = adapter
            }else{
                rvReplies.visibility = View.GONE
                progressBarReplies.visibility = View.GONE
                Toast.makeText(this@ReplyCommentActivity, "There Is No Reply For This Comment", Toast.LENGTH_SHORT).show()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@ReplyCommentActivity)
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
                            ivReplyPhoto.visibility = VISIBLE
                            GlideLoader(this).loadImageUri(imageURI!!, ivReplyPhoto)
                            ibRemovePhotoFromReply.visibility = VISIBLE
                            ibRemovePhotoFromReply.setOnClickListener(this@ReplyCommentActivity)
                        }else{
                            ivReplyPhoto.visibility = View.GONE
                            ibRemovePhotoFromReply.visibility = View.GONE
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "image selection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED){
            ivReplyPhoto.visibility = View.GONE
            ibRemovePhotoFromReply.visibility = View.GONE
        }
    }

    private fun validateUserInput(): Boolean {
        val replyText = etNewReply.text.toString()
        val disallowedPattern = Regex("[\\[\\]#/<\\\\>]")

        return if (TextUtils.isEmpty(replyText)) {
            showErrorSnackBar("Please Enter Text For Reply.", true)
            false
        }else if (replyText.length > 2000){
            showErrorSnackBar("Reply Text Too Long.", true)
            false
        }else if (disallowedPattern.containsMatchIn(replyText)) {
            showErrorSnackBar("You Cant Use These Characters: \\[]<>#/ In Reply Text.", true)
            false
        }else{
            true
        }
    }


}