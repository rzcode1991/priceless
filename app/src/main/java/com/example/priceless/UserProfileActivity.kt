package com.example.priceless

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class UserProfileActivity : BaseActivity(), OnClickListener{

    private lateinit var layoutUserInfo: LinearLayout
    private lateinit var layoutFirstLastName: LinearLayout
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var btnFollow: Button
    private lateinit var tvPrivate: TextView
    private lateinit var recyclerView: RecyclerView
    private var userID = ""
    private lateinit var userInfo: User
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var currentUserID = ""
    private lateinit var layoutOtherUser: LinearLayout
    private lateinit var tvOtherUserFollowsYou: TextView
    private lateinit var btnStopOtherUser: Button
    private lateinit var layoutActionRequest: LinearLayout
    private lateinit var btnAcceptRequest: Button
    private lateinit var btnRejectRequest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        layoutUserInfo = findViewById(R.id.layout_user_info_user_profile)
        layoutFirstLastName = findViewById(R.id.layout_first_last_name_user_profile)
        tvFirstName = findViewById(R.id.tv_first_name_user_profile)
        tvLastName = findViewById(R.id.tv_last_name_user_profile)
        ivProfilePic = findViewById(R.id.iv_profile_pic_user_profile)
        tvUserName = findViewById(R.id.tv_username_user_profile)
        btnFollow = findViewById(R.id.btn_follow_user_profile)
        tvPrivate = findViewById(R.id.tv_private_user_user_profile)
        recyclerView = findViewById(R.id.recycler_view_posts_user_profile)
        layoutOtherUser = findViewById(R.id.layout_other_user_follows_you_user_profile)
        tvOtherUserFollowsYou = findViewById(R.id.tv_other_user_follows_you_user_profile)
        btnStopOtherUser = findViewById(R.id.btn_stop_other_user_user_profile)
        layoutActionRequest = findViewById(R.id.layout_action_request_user_profile)
        btnAcceptRequest = findViewById(R.id.btn_accept_request_user_profile)
        btnRejectRequest = findViewById(R.id.btn_reject_request_user_profile)

        if (intent.hasExtra("userID")){
            userID = intent.getStringExtra("userID").toString()
        }

        setUserInfo()

    }


    private fun setUserInfo(){
        showProgressDialog()
        CoroutineScope(Dispatchers.Main).launch {
            val userInfoJob = async {
                val deferredCompletable = CompletableDeferred<User?>()
                FireStoreClass().getUserInfoWithCallback(userID) { user ->
                    deferredCompletable.complete(user)
                }
                val userDetails = deferredCompletable.await()
                if (userDetails != null){
                    userInfo = userDetails
                }else{
                    hideProgressDialog()
                    showErrorSnackBar("Authentication Error", true)
                    return@async
                }
            }
            userInfoJob.await()

            val deferredCurrentUserID = async { FireStoreClass().getUserID() }
            currentUserID = deferredCurrentUserID.await()

            if (currentUserID.isNotEmpty()){
                layoutUserInfo.visibility = VISIBLE
                checkFollowSituation()
                hideProgressDialog()
                checkOtherUserFollowSituation()
                tvUserName.text = userInfo.userName
                if (userInfo.image.isNotEmpty()){
                    GlideLoader(this@UserProfileActivity).loadImageUri(userInfo.image, ivProfilePic)
                    ivProfilePic.setOnClickListener {
                        val intent = Intent(this@UserProfileActivity, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", userInfo.image)
                        startActivity(intent)
                    }
                }else{
                    GlideLoader(this@UserProfileActivity).loadImageUri(R.drawable.ic_baseline_account_circle_24, ivProfilePic)
                    ivProfilePic.setOnClickListener {
                        Toast.makeText(this@UserProfileActivity, "This User Does Not Have Profile Picture", Toast.LENGTH_LONG).show()
                    }
                }
                if (userInfo.publicProfile){
                    layoutFirstLastName.visibility = VISIBLE
                    tvFirstName.text = "first name: ${userInfo.firstName}"
                    tvLastName.text = "last name: ${userInfo.lastName}"
                    tvPrivate.visibility = View.GONE
                    loadPosts(userID)
                }else{
                    FireStoreClass().amIFollowingThatUser(currentUserID, userID) { yep ->
                        if (yep){
                            layoutFirstLastName.visibility = VISIBLE
                            tvFirstName.text = "first name: ${userInfo.firstName}"
                            tvLastName.text = "last name: ${userInfo.lastName}"
                            tvPrivate.visibility = View.GONE
                            loadPosts(userID)
                        }else{
                            layoutFirstLastName.visibility = View.GONE
                            tvPrivate.visibility = VISIBLE
                            recyclerView.visibility = View.GONE
                        }
                    }
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Authentication Error", true)
            }
        }
    }

    private fun loadPosts(UID: String) {
        showProgressDialog()
        CoroutineScope(Dispatchers.Main).launch {
            val deferredAllPosts = async { FireStoreClass().getPostsFromFireStore(UID) }
            val allPostsList = deferredAllPosts.await()
            val allPosts = ArrayList<PostStructure>()

            if (!allPostsList.isNullOrEmpty()) {
                for (post in allPostsList){
                    if (post.postID.isNotEmpty() && post.buyerID.isEmpty()){
                        allPosts.add(post)
                    }
                }
            }else{
                recyclerView.visibility = View.GONE
                hideProgressDialog()
                Toast.makeText(this@UserProfileActivity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.d("--- all posts beginning:", "${allPosts.size}")
            val visiblePosts = ArrayList(allPosts.filter { it.visibility })
            Log.d("--- already visible posts beginning:", "${visiblePosts.size}")
            val postsToUpdate = mutableListOf<PostStructure>()
            val timeJob = async { getTimeNow() }
            timeJob.await()
            if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                for (post in allPosts){
                    if (!post.visibility){
                        if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                            postsToUpdate.add(post)
                            Log.d("--- posts to update are:", "${postsToUpdate.size}")
                        }
                    }
                }
            }else{
                Toast.makeText(this@UserProfileActivity, "Error Getting Time; Check Your Internet Connection", Toast.LENGTH_SHORT).show()
            }
            if (postsToUpdate.isNotEmpty()){
                if (postsToUpdate.size == 1) {
                    val postToBeUpdated = postsToUpdate[0]
                    Log.d("--- 1 post t b updated is:", "$postToBeUpdated")
                    val postHashMap = HashMap<String, Any>()
                    postHashMap["visibility"] = true
                    postHashMap["timeCreatedMillis"] = secondsNow
                    val updatePostJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().updatePostOnFireStore(postToBeUpdated.userId, postHashMap,
                            postToBeUpdated.postID) { onComplete ->
                            deferredCompletable.complete(onComplete)
                        }
                        deferredCompletable.await()
                    }

                    if (updatePostJob.await()) {
                        postToBeUpdated.timeCreatedMillis = secondsNow
                        visiblePosts.add(postToBeUpdated)
                        Log.d("--- visible posts after adding 1 post for update:", "${visiblePosts.size}")
                    }else{
                        Toast.makeText(this@UserProfileActivity, "err during update a post.", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Log.d("--- multiple posts to be updated are:", "${postsToUpdate.size}")
                    val batchUpdates = mutableMapOf<String, Map<String, Any>>()
                    for (eachPost in postsToUpdate) {
                        val postHashMap = HashMap<String, Any>()
                        postHashMap["visibility"] = true
                        postHashMap["timeCreatedMillis"] = secondsNow
                        batchUpdates[eachPost.postID] = postHashMap
                        eachPost.timeCreatedMillis = secondsNow
                    }
                    val batchUpdateJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().batchUpdatePostsOnFireStore(UID, batchUpdates) { successfully ->
                            deferredCompletable.complete(successfully)
                        }
                        deferredCompletable.await()
                    }
                    if (batchUpdateJob.await()) {
                        visiblePosts.addAll(postsToUpdate)
                        Log.d("--- visible posts after adding multiple posts for update:", "${visiblePosts.size}")
                    } else {
                        Toast.makeText(this@UserProfileActivity, "err during batch update posts.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Log.d("user is:", "$userInfo")
            for (i in allPosts){
                i.profilePicture = userInfo.image
                i.userName = userInfo.userName
            }
            allPosts.sortByDescending { it.timeCreatedMillis.toLong() }
            recyclerView.visibility = VISIBLE
            val adapter = RecyclerviewAdapter(this@UserProfileActivity, allPosts, currentUserID)
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(this@UserProfileActivity)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            hideProgressDialog()
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

    private fun checkFollowSituation(){
        FireStoreClass().checkFollowSituation(currentUserID, userID) { situation ->
            if (situation.isNotEmpty()){
                btnFollow.visibility = VISIBLE
                btnFollow.setOnClickListener(this)
                when(situation){
                    "following" -> {
                        btnFollow.text = "Following"
                    }
                    "pending" -> {
                        btnFollow.text = "Pending"
                    }
                    "follow" -> {
                        btnFollow.text = "Follow"
                    }
                }
            }else{
                btnFollow.visibility = View.GONE
                showErrorSnackBar("Error Getting Follow Situation Between You And This User.", true)
            }
        }
    }

    private fun checkOtherUserFollowSituation(){
        FireStoreClass().checkOtherUserFollowSituation(currentUserID, userID) { situation ->
            if (situation.isNotEmpty()){
                layoutOtherUser.visibility = VISIBLE
                tvOtherUserFollowsYou.visibility = VISIBLE
                when(situation){
                    "FollowsYou" -> {
                        // when a user is following me, means that he sent me a request and I've accepted it.
                        tvOtherUserFollowsYou.text = "This User Is Following You."
                        layoutActionRequest.visibility = View.GONE
                        btnStopOtherUser.visibility = VISIBLE
                        btnStopOtherUser.setOnClickListener(this)
                    }
                    "Pending" -> {
                        tvOtherUserFollowsYou.text = "This User Sent You A Follow Request."
                        btnStopOtherUser.visibility = View.GONE
                        layoutActionRequest.visibility = VISIBLE
                        btnAcceptRequest.setOnClickListener(this)
                        btnRejectRequest.setOnClickListener(this)
                    }
                    "IsNotFollowing" -> {
                        tvOtherUserFollowsYou.text = "This User Is Not Following You."
                        btnStopOtherUser.visibility = View.GONE
                        layoutActionRequest.visibility = View.GONE
                    }
                }
            }else{
                layoutOtherUser.visibility = View.GONE
                tvOtherUserFollowsYou.visibility = View.GONE
                btnStopOtherUser.visibility = View.GONE
                layoutActionRequest.visibility = View.GONE
                btnAcceptRequest.visibility = View.GONE
                btnRejectRequest.visibility = View.GONE
                showErrorSnackBar("Error Getting Follow Situation From This User.", true)
            }
        }
    }

    private fun stopOtherUserFromFollowingMe(){
        showProgressDialog()
        // unfollow myself, so I will be considered as other user.
        FireStoreClass().unfollowUser(userID, currentUserID) { success ->
            if (success){
                //hideProgressDialog()
                FireStoreClass().deleteFollowRequest(userID,
                    currentUserID) { succeed ->
                    hideProgressDialog()
                    if (succeed){
                        checkOtherUserFollowSituation()
                    }else{
                        Log.e("+++ error deleting follow request", "")
                    }
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Unfollow User Failed.", true)
            }
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.btn_stop_other_user_user_profile -> {
                    if (btnStopOtherUser.visibility == VISIBLE){
                        stopOtherUserFromFollowingMe()
                    }
                }
                R.id.btn_accept_request_user_profile -> {
                    if (layoutActionRequest.visibility == VISIBLE){
                        showProgressDialog()
                        FireStoreClass().acceptFollowRequest(currentUserID, userID) { successful ->
                            if (successful){
                                hideProgressDialog()
                                checkOtherUserFollowSituation()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Accept Follow Request Failed.", true)
                            }
                        }
                    }
                }
                R.id.btn_reject_request_user_profile -> {
                    if (layoutActionRequest.visibility == VISIBLE){
                        showProgressDialog()
                        FireStoreClass().deleteFollowRequest(userID, currentUserID) { success ->
                            if (success){
                                hideProgressDialog()
                                checkOtherUserFollowSituation()
                            }else{
                                hideProgressDialog()
                                showErrorSnackBar("Error While Rejecting Follow Request.", true)
                            }
                        }
                    }
                }
                R.id.btn_follow_user_profile -> {
                    if (btnFollow.visibility == VISIBLE){
                        when(btnFollow.text){
                            "Following" -> {
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Unfollow User?")
                                builder.setMessage("You Are Following This User, Do You Wanna Unfollow?")
                                builder.setIcon(R.drawable.ic_round_warning_24)
                                builder.setPositiveButton("Yes") { dialog, _ ->
                                    showProgressDialog()
                                    FireStoreClass().unfollowUser(currentUserID, userID) { success ->
                                        if (success){
                                            FireStoreClass().deleteFollowRequest(currentUserID,
                                                userID) { successfully ->
                                                if (successfully){
                                                    hideProgressDialog()
                                                    setUserInfo()
                                                }else{
                                                    hideProgressDialog()
                                                    showErrorSnackBar("Error While Unfollowing User", true)
                                                }
                                            }
                                        }else{
                                            hideProgressDialog()
                                            showErrorSnackBar("Error While Unfollowing User", true)
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                builder.setNeutralButton("No") {dialog, _ ->
                                    dialog.dismiss()
                                }
                                builder.setCancelable(false)
                                builder.create().show()
                            }
                            "Pending" -> {
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Cancel Request?")
                                builder.setMessage("Your Request Is Waiting, " +
                                        "Do You Wanna Cancel It?")
                                builder.setIcon(R.drawable.ic_round_warning_24)
                                builder.setPositiveButton("Yes") { dialog, _ ->
                                    showProgressDialog()
                                    FireStoreClass().deleteFollowRequest(currentUserID,
                                        userID) { successful ->
                                        if (successful){
                                            checkFollowSituation()
                                            hideProgressDialog()
                                            Toast.makeText(this, "Follow Request Deleted", Toast.LENGTH_LONG).show()
                                        }else{
                                            hideProgressDialog()
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                builder.setNeutralButton("No") {dialog, _ ->
                                    dialog.dismiss()
                                }
                                builder.setCancelable(false)
                                builder.create().show()
                            }
                            "Follow" -> {
                                createNewFollowRequest()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createNewFollowRequest(){
        val receiverUserID = userID
        val followRequest = FollowRequest(currentUserID, "", "",
            receiverUserID, false)
        showProgressDialog()
        FireStoreClass().createFollowRequest(this, followRequest)
    }

    fun createFollowRequestSuccessful(){
        checkFollowSituation()
        hideProgressDialog()
        showErrorSnackBar("Your Request Was Sent Successfully", false)
    }

}