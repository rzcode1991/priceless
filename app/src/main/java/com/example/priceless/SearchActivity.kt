package com.example.priceless

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class SearchActivity : BaseActivity(), OnClickListener {

    private lateinit var toolbarSearch: Toolbar
    private lateinit var etSearchUserName: TextInputEditText
    private lateinit var btnSearchUserName: Button
    private lateinit var layoutUserInfo: LinearLayout
    private lateinit var layoutFirstLastName: LinearLayout
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var btnFollow: Button
    private lateinit var tvPrivate: TextView
    private lateinit var recyclerView: RecyclerView
    private var userID: String = ""
    private lateinit var userInfo: User
    private lateinit var coroutineScope: CoroutineScope
    private var getTime: GetTime? = null
    private lateinit var dateAndTimePair: Pair<String, String>
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var currentUserID: String
    private var followSituation = ""
    private lateinit var layoutOtherUser: LinearLayout
    private lateinit var tvOtherUserFollowsYou: TextView
    private lateinit var btnStopOtherUser: Button
    private lateinit var layoutActionRequest: LinearLayout
    private lateinit var btnAcceptRequest: Button
    private lateinit var btnRejectRequest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        toolbarSearch = findViewById(R.id.toolbar_search)
        etSearchUserName = findViewById(R.id.et_search_username)
        btnSearchUserName = findViewById(R.id.btn_search_username)
        layoutUserInfo = findViewById(R.id.layout_user_info_search)
        layoutFirstLastName = findViewById(R.id.layout_first_last_name_search)
        tvFirstName = findViewById(R.id.tv_first_name_search)
        tvLastName = findViewById(R.id.tv_last_name_search)
        ivProfilePic = findViewById(R.id.iv_profile_pic_searched_user)
        tvUserName = findViewById(R.id.tv_username_searched_user)
        btnFollow = findViewById(R.id.btn_follow_searched)
        tvPrivate = findViewById(R.id.tv_private_user_searched)
        coroutineScope = CoroutineScope(Dispatchers.Main)
        //getTime = GetTime()
        recyclerView = findViewById(R.id.recycler_view_searched_user)
        layoutOtherUser = findViewById(R.id.layout_other_user)
        tvOtherUserFollowsYou = findViewById(R.id.tv_other_user_follows_you)
        btnStopOtherUser = findViewById(R.id.btn_stop_other_user)
        layoutActionRequest = findViewById(R.id.layout_action_request_search)
        btnAcceptRequest = findViewById(R.id.btn_accept_request_search)
        btnRejectRequest = findViewById(R.id.btn_reject_request_search)

        setActionBar()

        btnSearchUserName.setOnClickListener(this)

    }


    private fun setActionBar(){
        setSupportActionBar(toolbarSearch)
        val actionBar = supportActionBar
        actionBar?.title = "       Search By UserName"
        //actionBar?.isHideOnContentScrollEnabled = true
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back)
        }
        toolbarSearch.setNavigationOnClickListener { onBackPressed() }
    }


    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.btn_search_username -> {
                    val userName = etSearchUserName.text.toString().lowercase().trim { it <= ' ' }
                    if (validUserNameInput()){
                        showProgressDialog()
                        FireStoreClass().getUserIDByUsername(userName) { uID ->
                            if (uID != null) {
                                userID = uID
                                FireStoreClass().getUserInfoFromFireStore(this@SearchActivity, userID)
                            } else {
                                hideProgressDialog()
                                showErrorSnackBar("User $userName Does Not Exist", true)
                                layoutUserInfo.visibility = View.GONE
                                btnFollow.visibility = View.GONE
                                tvPrivate.visibility = View.GONE
                                recyclerView.visibility = View.GONE
                            }
                        }
                    }
                }
                R.id.btn_follow_searched -> {
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
                                                    checkFollowSituation()
                                                    hideProgressDialog()
                                                    if (userInfo.publicProfile){
                                                        layoutFirstLastName.visibility = VISIBLE
                                                        tvFirstName.text = "first name: ${userInfo.firstName}"
                                                        tvLastName.text = "last name: ${userInfo.lastName}"
                                                        recyclerView.visibility = VISIBLE
                                                        tvPrivate.visibility = View.GONE
                                                    }else{
                                                        layoutFirstLastName.visibility = View.GONE
                                                        recyclerView.visibility = View.GONE
                                                        tvPrivate.visibility = VISIBLE
                                                    }
                                                    Toast.makeText(this, "You Are Not Following This User Anymore", Toast.LENGTH_LONG).show()
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


    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
        userInfo = user
        setUserInfo()
    }


    private fun setUserInfo(){
        showProgressDialog()
        FireStoreClass().getCurrentUserID { currentUID ->
            //hideProgressDialog()
            if (currentUID.isNotEmpty()){
                currentUserID = currentUID
                layoutUserInfo.visibility = VISIBLE
                checkFollowSituation()
                hideProgressDialog()
                checkOtherUserFollowSituation()
                tvUserName.text = userInfo.userName
                if (userInfo.image.isNotEmpty()){
                    GlideLoader(this).loadImageUri(userInfo.image, ivProfilePic)
                    ivProfilePic.setOnClickListener {
                        val intent = Intent(this, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", userInfo.image)
                        startActivity(intent)
                    }
                }else{
                    GlideLoader(this).loadImageUri(R.drawable.ic_baseline_account_circle_24, ivProfilePic)
                    ivProfilePic.setOnClickListener {
                        Toast.makeText(this, "This User Does Not Have Profile Picture", Toast.LENGTH_LONG).show()
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


    private fun stopOtherUserFromFollowingMe(){
        showProgressDialog()
        // unfollow myself, so I will be considered as other user.
        FireStoreClass().unfollowUser(userID, currentUserID) { success ->
            if (success){
                hideProgressDialog()
                FireStoreClass().deleteFollowRequest(userID,
                    currentUserID) { succeed ->
                    if (succeed){
                        tvOtherUserFollowsYou.text = "This User Is Not Following You."
                        btnStopOtherUser.visibility = View.GONE
                        layoutActionRequest.visibility = View.GONE
                    }
                }
            }else{
                hideProgressDialog()
                showErrorSnackBar("Unfollow User Failed.", true)
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
                        btnStopOtherUser.setOnClickListener {
                            stopOtherUserFromFollowingMe()
                        }
                    }
                    "Pending" -> {
                        tvOtherUserFollowsYou.text = "This User Sent You A Follow Request."
                        btnStopOtherUser.visibility = View.GONE
                        layoutActionRequest.visibility = VISIBLE
                        btnAcceptRequest.setOnClickListener {
                            showProgressDialog()
                            FireStoreClass().acceptFollowRequest(currentUserID, userID) { successful ->
                                if (successful){
                                    tvOtherUserFollowsYou.text = "This User Is Following You."
                                    layoutActionRequest.visibility = View.GONE
                                    btnStopOtherUser.visibility = VISIBLE
                                    hideProgressDialog()
                                    showErrorSnackBar("This User Started Following You.", false)
                                    btnStopOtherUser.setOnClickListener {
                                        stopOtherUserFromFollowingMe()
                                    }
                                }else{
                                    hideProgressDialog()
                                    showErrorSnackBar("Accept Follow Request Failed.", true)
                                }
                            }
                        }
                        btnRejectRequest.setOnClickListener {
                            showProgressDialog()
                            FireStoreClass().deleteFollowRequest(userID, currentUserID) { success ->
                                if (success){
                                    tvOtherUserFollowsYou.text = "This User Is Not Following You."
                                    layoutActionRequest.visibility = View.GONE
                                    btnStopOtherUser.visibility = View.GONE
                                    hideProgressDialog()
                                    Toast.makeText(this, "You Deleted This Follow Request.", Toast.LENGTH_LONG).show()
                                }else{
                                    hideProgressDialog()
                                    showErrorSnackBar("Error While Rejecting Follow Request.", true)
                                }
                            }
                        }
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


    private fun createNewFollowRequest(){
        val receiverUserID = userID
        val followRequest = FollowRequest(currentUserID, receiverUserID, false)
        showProgressDialog()
        FireStoreClass().createFollowRequest(this, followRequest)
    }


    fun createFollowRequestSuccessful(){
        checkFollowSituation()
        hideProgressDialog()
        showErrorSnackBar("Your Request Was Sent Successfully", false)
    }


    private fun loadPosts(UID: String) {
        showProgressDialog()
        coroutineScope.launch {
            val deferredAllPosts = async { FireStoreClass().getPostsFromFireStore(UID) }
            val allPosts = deferredAllPosts.await()

            if (allPosts.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                hideProgressDialog()
                Toast.makeText(this@SearchActivity, "all posts isNullOrEmpty", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@SearchActivity, "Error Getting Time; Check Your Internet Connection", Toast.LENGTH_SHORT).show()
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
                        FireStoreClass().updatePostOnFireStore(this@SearchActivity,
                            postToBeUpdated.userId, postHashMap, postToBeUpdated.postID) { onComplete ->
                            deferredCompletable.complete(onComplete)
                        }
                        deferredCompletable.await()
                    }

                    if (updatePostJob.await()) {
                        postToBeUpdated.timeCreatedMillis = secondsNow
                        visiblePosts.add(postToBeUpdated)
                        Log.d("--- visible posts after adding 1 post for update:", "${visiblePosts.size}")
                    }else{
                        Toast.makeText(this@SearchActivity, "err during update a post.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@SearchActivity, "err during batch update posts.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            Log.d("user is:", "$userInfo")
            for (i in visiblePosts){
                i.profilePicture = userInfo.image
                i.userName = userInfo.userName
            }
            visiblePosts.sortByDescending { it.timeCreatedMillis.toLong() }
            Log.d("final visible posts are:", "$visiblePosts")
            recyclerView.visibility = VISIBLE
            val adapter = RecyclerviewAdapter(this@SearchActivity, visiblePosts, currentUserID)
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(this@SearchActivity)
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


    private fun validUserNameInput(): Boolean{
        val userName = etSearchUserName.text.toString().lowercase().trim { it <= ' ' }
        val allowedRegexForUserName = Regex("^[a-z0-9_-]*$")
        val disallowedPattern = Regex("[\\[\\]#/<\\\\>]")
        return if (userName.isEmpty()){
            showErrorSnackBar("Please Enter UserName", true)
            false
        }else if (disallowedPattern.containsMatchIn(userName)){
            showErrorSnackBar("UserName Is Not Valid", true)
            false
        }else if (!userName.matches(allowedRegexForUserName)){
            showErrorSnackBar("UserName Is Not Valid", true)
            false
        }else if (userName.length !in 3..20){
            showErrorSnackBar("UserName Is Not Valid", true)
            false
        }else{
            true
        }
    }


}