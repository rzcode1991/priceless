package com.example.priceless

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class BoughtPostsActivity : BaseActivity() {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var currentUserID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bought_posts)

        ivProfilePic = findViewById(R.id.iv_profile_pic_bought_posts)
        tvUserName = findViewById(R.id.tv_username_bought_posts)
        recyclerView = findViewById(R.id.recycler_view_bought_posts)
        progressBar = findViewById(R.id.progress_bar_bought_posts)

        setUserInfo()

    }

    private fun setUserInfo(){
        FireStoreClass().getCurrentUserID { userID ->
            if (userID.isNotEmpty()){
                currentUserID = userID
                FireStoreClass().getUserInfoWithCallback(userID) { currentUser ->
                    if (currentUser != null){
                        if (currentUser.image.isNotEmpty()){
                            GlideLoader(this).loadImageUri(currentUser.image, ivProfilePic)
                        }else{
                            ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
                        }
                        tvUserName.text = currentUser.userName
                    }
                }
                loadBoughtPosts()
            }
        }
    }

    private fun loadBoughtPosts(){
        CoroutineScope(Dispatchers.Main).launch {
            val deferredPosts = async { FireStoreClass().getBoughtPosts(currentUserID) }
            val allPosts = deferredPosts.await()
            if (allPosts.isNullOrEmpty()){
                progressBar.visibility = View.GONE
                Toast.makeText(this@BoughtPostsActivity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val userGroupedPosts = allPosts.groupBy { it.userId }
            val userInfoJobs = userGroupedPosts.map { (userId, groupPosts) ->
                async {
                    val deferredUserInfo = CompletableDeferred<User?>()
                    FireStoreClass().getUserInfoWithCallback(userId) { userInfo ->
                        deferredUserInfo.complete(userInfo)
                    }
                    val userInfo = deferredUserInfo.await()
                    if (userInfo != null) {
                        groupPosts.forEach { postItem ->
                            postItem.profilePicture = userInfo.image
                            postItem.userName = userInfo.userName
                        }
                    }
                }
            }
            userInfoJobs.awaitAll()
            allPosts.sortByDescending { it.timeCreatedMillis.toLong() }
            recyclerView.visibility = VISIBLE
            val adapter = SoldPostsRvAdapter(this@BoughtPostsActivity, allPosts, false)
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(this@BoughtPostsActivity)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            progressBar.visibility = View.GONE
        }
    }



}