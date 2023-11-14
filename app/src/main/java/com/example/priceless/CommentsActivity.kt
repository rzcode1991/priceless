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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import java.io.IOException

class CommentsActivity : BaseActivity(), OnClickListener {

    private lateinit var post: PostStructure
    private var currentUserID = ""
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvPostText: TextView
    private lateinit var ivPostImage: ImageView
    private lateinit var tvTimeCreatedPost: TextView
    private lateinit var tvPriceLess: TextView
    private lateinit var tvNumberOfLikesOfPost: TextView
    private lateinit var ibLikePost: ImageButton
    private lateinit var etNewComment: TextInputEditText
    private lateinit var ibAddPhotoToComment: ImageButton
    private lateinit var ibRemoveCommentPhoto: ImageButton
    private lateinit var ivCommentPhoto: ImageView
    private lateinit var cbPrivateComment: CheckBox
    private lateinit var btnSendComment: Button
    private lateinit var rvComments: RecyclerView
    private var likeSituation = false
    private var imageNameUrl: String = ""
    private var imageURI: Uri? = null
    private var imageURIForRecyclerView: Uri? = null
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        ivProfilePic = findViewById(R.id.iv_profile_post_item_comments)
        tvUserName = findViewById(R.id.tv_user_name_post_item_comments)
        tvPostText = findViewById(R.id.tv_post_content_comments)
        ivPostImage = findViewById(R.id.iv_post_image_comments)
        tvTimeCreatedPost = findViewById(R.id.tv_time_created_post_item_comments)
        tvPriceLess = findViewById(R.id.tv_priceless_comments)
        tvNumberOfLikesOfPost = findViewById(R.id.tv_likes_of_post_comments)
        ibLikePost = findViewById(R.id.ib_like_post_comments)
        etNewComment = findViewById(R.id.et_new_comment)
        ibAddPhotoToComment = findViewById(R.id.ib_add_photo_to_comment)
        ibRemoveCommentPhoto = findViewById(R.id.ib_remove_comment_photo)
        ivCommentPhoto = findViewById(R.id.iv_comment_photo)
        cbPrivateComment = findViewById(R.id.cb_private_comment)
        btnSendComment = findViewById(R.id.send_comment)
        rvComments = findViewById(R.id.recycler_view_comments)
        progressBar = findViewById(R.id.pb_comments)

        if (intent.hasExtra("post")){
            post = intent.getParcelableExtra("post")!!
        }

        currentUserID = FireStoreClass().getUserID()

        setPostInfo()

        loadComments()

        ibLikePost.setOnClickListener(this@CommentsActivity)
        ibAddPhotoToComment.setOnClickListener(this@CommentsActivity)
        btnSendComment.setOnClickListener(this@CommentsActivity)
        ivProfilePic.setOnClickListener(this@CommentsActivity)

    }

    override fun onResume() {
        super.onResume()
        loadComments()
    }

    private fun setPostInfo(){
        if (post.profilePicture.isNotEmpty()){
            GlideLoader(this).loadImageUri(post.profilePicture, ivProfilePic)
        }else{
            ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        tvUserName.text = post.userName
        tvPostText.text = post.postText
        if (post.postImage.isNotEmpty()){
            ivPostImage.visibility = VISIBLE
            GlideLoader(this).loadImageUri(post.postImage, ivPostImage)
            ivPostImage.setOnClickListener(this@CommentsActivity)
        }else{
            ivPostImage.visibility = View.GONE
        }
        tvTimeCreatedPost.text = post.timeCreatedToShow
        if (post.timeTraveler){
            tvPriceLess.visibility = VISIBLE
        }else{
            tvPriceLess.visibility = View.GONE
        }
        getNumberOfLikes()
        checkLikeSituation()

    }

    private fun getNumberOfLikes(){
        FireStoreClass().getNumberOfLikesForPost(post.postID, post.userId) { number ->
            if (number != null && number != 0){
                tvNumberOfLikesOfPost.visibility = VISIBLE
                tvNumberOfLikesOfPost.text = "$number"
            }else{
                tvNumberOfLikesOfPost.visibility = View.GONE
            }
        }
    }

    private fun checkLikeSituation(){
        FireStoreClass().getLikeSituationForPost(currentUserID, post.postID, post.userId) { yep ->
            if (yep){
                likeSituation = true
                ibLikePost.setImageResource(R.drawable.thumb_liked)
            }else{
                likeSituation = false
                ibLikePost.setImageResource(R.drawable.thumb_like)
            }
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.iv_profile_post_item_comments -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    intent.putExtra("userID", post.userId)
                    startActivity(intent)
                }
                R.id.ib_like_post_comments -> {
                    if (likeSituation){
                        FireStoreClass().unLikePost(currentUserID, post.postID,
                            post.userId) { onSuccess ->
                            if (onSuccess){
                                getNumberOfLikes()
                                checkLikeSituation()
                            }
                        }
                    }else{
                        FireStoreClass().likePost(currentUserID, post.postID,
                            post.userId) { success ->
                            if (success){
                                getNumberOfLikes()
                                checkLikeSituation()
                            }
                        }
                    }
                }
                R.id.iv_post_image_comments -> {
                    if (post.postImage.isNotEmpty() && ivPostImage.visibility == VISIBLE){
                        val intent = Intent(this, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", post.postImage)
                        startActivity(intent)
                    }
                }
                R.id.ib_add_photo_to_comment -> {
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED){
                        Constants.showImageFromStorage(this@CommentsActivity)
                    }else{
                        ActivityCompat.requestPermissions(this@CommentsActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.PermissionExternalStorageCode)
                    }
                }
                R.id.ib_remove_comment_photo -> {
                    if (imageURI != null && ibRemoveCommentPhoto.visibility == VISIBLE
                        && ivCommentPhoto.visibility == VISIBLE){
                        imageURI = null
                        ivCommentPhoto.visibility = View.GONE
                        ibRemoveCommentPhoto.visibility = View.GONE
                    }
                }
                R.id.send_comment -> {
                    if (validateUserInput()){
                        showProgressDialog()
                        if (imageURI != null) {
                            FireStoreClass().uploadImageToCloudStorage(this, imageURI!!,
                                "comment_image")
                        }else{
                            addNewComment()
                        }
                    }
                }
            }
        }
    }


    private fun addNewComment(){
        if (currentUserID.isNotEmpty()){
            CoroutineScope(Dispatchers.Main).launch {
                val timeJob = async { getTimeNow() }
                timeJob.await()
                if (secondsNow.isEmpty() || dateNow.isEmpty()){
                    showErrorSnackBar("Error Getting Time; Check Your Internet Connection", true)
                    hideProgressDialog()
                }else{
                    val text = etNewComment.text.toString()
                    val commentPhoto = imageNameUrl
                    val timeCreated = secondsNow
                    val timeCreatedToShow = dateNow
                    val postID = post.postID
                    val postOwnerUID = post.userId
                    val writerUID = currentUserID
                    val writerUserName = ""
                    val writerProfilePic = ""
                    val isPrivate = cbPrivateComment.isChecked
                    val edited = false
                    val commentID = ""
                    val newComment = CommentStructure(text, commentPhoto, timeCreated, timeCreatedToShow,
                        postID, postOwnerUID, writerUID, writerUserName, writerProfilePic, isPrivate, edited, commentID)
                    if (isPrivate){
                        FireStoreClass().addNewPrivateComment(postID, post.userId,
                            newComment) { success ->
                            if (success){
                                loadComments()
                                etNewComment.setText("")
                                imageURI = null
                                imageNameUrl = ""
                                ivCommentPhoto.visibility = View.GONE
                                ibRemoveCommentPhoto.visibility = View.GONE
                                showErrorSnackBar("New Comment Added Successfully.", false)
                                hideProgressDialog()
                            }else{
                                showErrorSnackBar("Error Adding New Comment.", true)
                                hideProgressDialog()
                            }
                        }
                    }else{
                        FireStoreClass().addNewPublicComment(postID, post.userId,
                            newComment) { success ->
                            if (success){
                                loadComments()
                                etNewComment.setText("")
                                imageURI = null
                                imageNameUrl = ""
                                ivCommentPhoto.visibility = View.GONE
                                ibRemoveCommentPhoto.visibility = View.GONE
                                showErrorSnackBar("New Comment Added Successfully.", false)
                                hideProgressDialog()
                            }else{
                                showErrorSnackBar("Error Adding New Comment.", true)
                                hideProgressDialog()
                            }
                        }
                    }
                }
            }
        }else{
            showErrorSnackBar("Error Getting Current User ID.", true)
            hideProgressDialog()
        }
    }


    private fun loadComments(){
        CoroutineScope(Dispatchers.Main).launch {
            val commentsToShow = ArrayList<CommentStructure>()

            if (currentUserID == post.userId){
                val pubComForOwnerJob = async {
                    val deferredCompletable = CompletableDeferred<ArrayList<CommentStructure>?>()
                    FireStoreClass().getPublicComments(post.userId, post.postID) { publicCommentsForPostOwner ->
                        deferredCompletable.complete(publicCommentsForPostOwner)
                    }
                    val publicCommentsFO = deferredCompletable.await()
                    if (publicCommentsFO != null){
                        commentsToShow.addAll(publicCommentsFO)
                    }
                }
                pubComForOwnerJob.await()



                val uIDList = ArrayList<String>()
                val job = async {
                    val deferredCompletable = CompletableDeferred<ArrayList<String>?>()
                    FireStoreClass().getUIDsOfPrivateCommentsForPostOwner(post.postID, post.userId) { uIDs ->
                        deferredCompletable.complete(uIDs)
                    }
                    val uIDsList = deferredCompletable.await()
                    if (uIDsList != null){
                        uIDList.addAll(uIDsList)
                    }
                }
                job.await()

                for (uID in uIDList){
                    val job2 = async {
                        val deferredCompletable = CompletableDeferred<ArrayList<CommentStructure>?>()
                        FireStoreClass().getPrivateCommentsFromUIDsForPostOwner(post.postID, post.userId,
                            uID) { privateComments ->
                            deferredCompletable.complete(privateComments)
                        }
                        val privateComments = deferredCompletable.await()
                        if (privateComments != null){
                            commentsToShow.addAll(privateComments)
                        }
                    }
                    job2.await()
                }
            }else{
                val pubComJob = async {
                    val deferredCompletable = CompletableDeferred<ArrayList<CommentStructure>?>()
                    FireStoreClass().getPublicComments(post.userId, post.postID) { publicComments ->
                        deferredCompletable.complete(publicComments)
                    }
                    val pubComments = deferredCompletable.await()
                    if (pubComments != null){
                        commentsToShow.addAll(pubComments)
                    }
                }
                pubComJob.await()

                val privateComJob = async {
                    val deferredCompletable4 = CompletableDeferred<ArrayList<CommentStructure>?>()
                    FireStoreClass().getPrivateCommentsForViewerUser(post.postID, post.userId,
                        currentUserID) { privateComments ->
                        deferredCompletable4.complete(privateComments)
                    }
                    val prComments = deferredCompletable4.await()
                    if (prComments != null){
                        commentsToShow.addAll(prComments)
                    }
                }
                privateComJob.await()

            }

            if (commentsToShow.isNotEmpty()){
                val userGroupedComments = commentsToShow.groupBy { it.writerUID }
                val userInfoJobs = userGroupedComments.map { (userId, groupComments) ->
                    async {
                        val deferredUserInfo = CompletableDeferred<User?>()
                        FireStoreClass().getUserInfoWithCallback(userId) { userInfo ->
                            deferredUserInfo.complete(userInfo)
                        }
                        val userInfo = deferredUserInfo.await()
                        if (userInfo != null) {
                            groupComments.forEach { commentItem ->
                                commentItem.writerProfilePic = userInfo.image
                                commentItem.writerUserName = userInfo.userName
                            }
                        }
                    }
                }
                userInfoJobs.awaitAll()

                commentsToShow.sortByDescending { it.timeCreated.toLong() }
                progressBar.visibility = View.GONE
                rvComments.visibility = VISIBLE
                val adapter = CommentsRvAdapter(this@CommentsActivity, commentsToShow,
                    currentUserID, this@CommentsActivity)
                adapter.notifyDataSetChanged()
                val layOutManager = LinearLayoutManager(this@CommentsActivity)
                rvComments.layoutManager = layOutManager
                rvComments.adapter = adapter
            }else{
                rvComments.visibility = View.GONE
                progressBar.visibility = View.GONE
                Toast.makeText(this@CommentsActivity, "There Is No Comment For This Post", Toast.LENGTH_SHORT).show()
            }

        }
    }


    fun uploadImageOnCloudSuccess(imageUrl: String){
        imageNameUrl = imageUrl
        addNewComment()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionExternalStorageCode){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorage(this@CommentsActivity)
            }else{
                Toast.makeText(this, "oops! you didn't gave permission to app for access " +
                        "storage, you can change it in your device's settings", Toast.LENGTH_LONG).show()
            }
        }else if (requestCode == 200){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Constants.showImageFromStorageForRecyclerView(this@CommentsActivity)
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
                            ivCommentPhoto.visibility = VISIBLE
                            GlideLoader(this).loadImageUri(imageURI!!, ivCommentPhoto)
                            ibRemoveCommentPhoto.visibility = VISIBLE
                            ibRemoveCommentPhoto.setOnClickListener(this@CommentsActivity)
                        }else{
                            ivCommentPhoto.visibility = View.GONE
                            ibRemoveCommentPhoto.visibility = View.GONE
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "image selection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }else if (requestCode == 400){
                if (data != null) {
                    try {
                        imageURIForRecyclerView = data.data!!
                        Log.d("image URI for RecyclerView is:", "$imageURIForRecyclerView")
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "image selection For RecyclerView failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED){
            ivCommentPhoto.visibility = View.GONE
            ibRemoveCommentPhoto.visibility = View.GONE
            Log.e("image selection failed", "image has not been selected")
        }
    }

    fun returnImageURIForRecyclerView(): Uri?{
        return imageURIForRecyclerView
    }

    fun setImageUriForRvToNull(){
        imageURIForRecyclerView = null
    }

    private fun validateUserInput(): Boolean {
        val commentText = etNewComment.text.toString()
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
        }else{
            true
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


}