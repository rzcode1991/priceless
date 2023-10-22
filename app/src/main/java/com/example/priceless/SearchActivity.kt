package com.example.priceless

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var lastRequestTimeMillis: Long = 0L
    private val requestCoolDownMillis: Long = 5000L
    private lateinit var coroutineScope: CoroutineScope
    private var getTime: GetTime? = null
    private var dateAndTimePair: Pair<String?, String?>? = null
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var currentUserID: String
    private var followSituation = ""

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
        getTime = GetTime()
        recyclerView = findViewById(R.id.recycler_view_searched_user)

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
                                            FireStoreClass().deleteFollowRequest(this,
                                                currentUserID, userID) { successfully ->
                                                if (successfully){
                                                    hideProgressDialog()
                                                    checkFollowSituation()
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
                                    FireStoreClass().deleteFollowRequest(this, currentUserID,
                                        userID) { successful ->
                                        if (successful){
                                            hideProgressDialog()
                                            checkFollowSituation()
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
            hideProgressDialog()
            if (currentUID.isNotEmpty()){
                currentUserID = currentUID
                layoutUserInfo.visibility = VISIBLE
                btnFollow.visibility = VISIBLE
                checkFollowSituation()
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
                        Toast.makeText(this, "this user does not have profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
                if (userInfo.publicProfile){
                    layoutFirstLastName.visibility = VISIBLE
                    tvFirstName.text = "first name: ${userInfo.firstName}"
                    tvLastName.text = "last name: ${userInfo.lastName}"
                    tvPrivate.visibility = View.GONE
                    loadPosts()
                }else{
                    //hideProgressDialog()
                    layoutFirstLastName.visibility = View.GONE
                    tvPrivate.visibility = VISIBLE
                    //tvPrivate.text = "${userInfo.userName} Is A Private User."
                    recyclerView.visibility = View.GONE
                }
            }else{
                showErrorSnackBar("Authentication Error", true)
            }
        }
    }


    private fun checkFollowSituation(){
        FireStoreClass().checkFollowSituation(currentUserID, userID) { situation ->
            if (situation.isNotEmpty()){
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


    fun createFollowRequestSuccessful(request: FollowRequest){
        hideProgressDialog()
        showErrorSnackBar("Your Request Was Sent Successfully", false)
        checkFollowSituation()
    }


    private fun loadPosts(){
        showProgressDialog()
        FireStoreClass().getPostsRealTimeListener(this, userID) { posts, success ->
            if (success && posts != null && posts.isNotEmpty()){
                Log.d("posts beginning are:", "$posts")
                val visiblePosts = ArrayList(posts.filter { it.visibility })
                Log.d("already visible posts at beginning are:", "$visiblePosts")
                val postsToUpdate = mutableListOf<PostStructure>()
                if (System.currentTimeMillis() > lastRequestTimeMillis + requestCoolDownMillis){
                    lastRequestTimeMillis = System.currentTimeMillis()
                    coroutineScope.launch {
                        getTimeNow()
                        Log.d("getTimeCalled", "date: $dateNow sec: $secondsNow")
                        if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                            for (post in posts){
                                if (!post.visibility){
                                    if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                                        postsToUpdate.add(post)
                                        Log.d("posts to update are:", "$postsToUpdate")
                                    }
                                }
                            }
                        }
                    }
                }
                coroutineScope.launch {
                    delay(1000)
                    if (postsToUpdate.isNotEmpty()){
                        if (postsToUpdate.size == 1) {
                            val postToBeUpdated = postsToUpdate[0]
                            Log.d("1 post t b updated is:", "$postToBeUpdated")
                            val postHashMap = HashMap<String, Any>()
                            postHashMap["visibility"] = true
                            postHashMap["timeCreatedMillis"] = secondsNow
                            FireStoreClass().updatePostOnFireStore(this@SearchActivity,
                                userID, postHashMap, postToBeUpdated.postID) { onComplete ->
                                if (onComplete){
                                    visiblePosts.add(postToBeUpdated)
                                    Log.d("visible posts after adding 1 post for update:", "$visiblePosts")
                                }else{
                                    showErrorSnackBar("failed to update future post", true)
                                }
                            }
                        }else{
                            Log.d("list of posts to be updated is:", "$postsToUpdate")
                            val batchUpdates = mutableMapOf<String, Map<String, Any>>()
                            for (eachPost in postsToUpdate) {
                                val postHashMap = HashMap<String, Any>()
                                postHashMap["visibility"] = true
                                postHashMap["timeCreatedMillis"] = secondsNow
                                batchUpdates[eachPost.postID] = postHashMap
                            }
                            FireStoreClass().batchUpdatePostsOnFireStore(this@SearchActivity,
                                userID, batchUpdates) { successfully ->
                                if (successfully) {
                                    //for (p in postsToUpdate){
                                    //    visiblePosts.add(p)
                                    //}
                                    visiblePosts.addAll(postsToUpdate)
                                    Log.d("visible posts after adding list of posts for update:", "$visiblePosts")
                                }else{
                                    Log.d("batchUpdate failed", "error while updating multiple posts on fireStore")
                                }
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
                    //val currentUserID = FireStoreClass().getUserID()
                    val adapter = RecyclerviewAdapter(this@SearchActivity, visiblePosts, currentUserID)
                    adapter.notifyDataSetChanged()
                    val layoutManager = LinearLayoutManager(this@SearchActivity)
                    recyclerView.layoutManager = layoutManager
                    recyclerView.adapter = adapter
                    hideProgressDialog()
                }
            }else{
                recyclerView.visibility = View.GONE
                hideProgressDialog()
            }
        }
    }


    private suspend fun getTimeNow(){
        dateAndTimePair = getTime?.getCurrentTimeAndDate()
        if (dateAndTimePair != null){
            if (dateAndTimePair!!.first != null && dateAndTimePair!!.second != null){
                dateNow = dateAndTimePair!!.first!!
                secondsNow = dateAndTimePair!!.second!!
            }
        }
    }


    fun deleteFollowRequestSuccessful(){
        hideProgressDialog()
        checkFollowSituation()
        Toast.makeText(this, "Follow Request Deleted", Toast.LENGTH_LONG).show()
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