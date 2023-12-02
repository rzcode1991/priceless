package com.example.priceless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class SoldPostsActivity : AppCompatActivity() {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var currentUserID = ""
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sold_posts)

        ivProfilePic = findViewById(R.id.iv_profile_pic_sold_posts)
        tvUserName = findViewById(R.id.tv_username_sold_posts)
        recyclerView = findViewById(R.id.recycler_view_sold_posts)
        progressBar = findViewById(R.id.progress_bar_sold_posts)

        setUserInfo()

    }

    private fun setUserInfo(){
        FireStoreClass().getCurrentUserID { userID ->
            if (userID.isNotEmpty()){
                currentUserID = userID
                FireStoreClass().getUserInfoWithCallback(userID) { currentUserInfo ->
                    if (currentUserInfo != null){
                        currentUser = currentUserInfo
                        if (currentUserInfo.image.isNotEmpty()){
                            GlideLoader(this).loadImageUri(currentUserInfo.image, ivProfilePic)
                        }else{
                            ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
                        }
                        tvUserName.text = currentUserInfo.userName
                        loadSoldPosts()
                    }
                }
            }
        }
    }


    private fun loadSoldPosts(){
        CoroutineScope(Dispatchers.Main).launch {
            val deferredPosts = async { FireStoreClass().getSoldPosts(currentUserID) }
            val allPosts = deferredPosts.await()
            if (allPosts.isNullOrEmpty()){
                progressBar.visibility = View.GONE
                Toast.makeText(this@SoldPostsActivity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val userGroupedPosts = allPosts.groupBy { it.buyerID }
            val userInfoJobs = userGroupedPosts.map { (buyerID, groupPosts) ->
                async {
                    val deferredUserInfo = CompletableDeferred<User?>()
                    FireStoreClass().getUserInfoWithCallback(buyerID) { userInfo ->
                        deferredUserInfo.complete(userInfo)
                    }
                    val userInfo = deferredUserInfo.await()
                    if (userInfo != null) {
                        groupPosts.forEach { postItem ->
                            // using buyerID as a place holder
                            postItem.buyerID = userInfo.userName
                        }
                    }
                }
            }
            userInfoJobs.awaitAll()

            for (i in allPosts){
                i.profilePicture = currentUser!!.image
                i.userName = currentUser!!.userName
            }
            allPosts.sortByDescending { it.timeCreatedMillis.toLong() }
            recyclerView.visibility = View.VISIBLE
            val adapter = SoldPostsRvAdapter(this@SoldPostsActivity, allPosts, true)
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(this@SoldPostsActivity)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            progressBar.visibility = View.GONE
        }
    }


}