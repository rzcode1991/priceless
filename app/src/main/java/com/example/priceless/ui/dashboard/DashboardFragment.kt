package com.example.priceless.ui.dashboard

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.priceless.*
import com.example.priceless.databinding.FragmentDashboardBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var dashViewModel: DashboardViewModel
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var dateAndTimePair: Pair<String?, String?>? = null
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var progressDialog: Dialog

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d("---onCreateView timeline called", "")
        //val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        coroutineScope = CoroutineScope(Dispatchers.Main)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        progressDialog = Dialog(requireActivity())

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("---onViewCreated timeline called", "")
        dashViewModel = ViewModelProvider(requireActivity()).get(DashboardViewModel::class.java)

        binding.ibSearchTimeline.setOnClickListener {
            val intent = Intent(activity, SearchActivity::class.java)
            startActivity(intent)
        }

        binding.ibRefreshTimeline.setOnClickListener {
            loadPosts()
        }

        loadPosts()

        FireStoreClass().getCurrentUserID { currentUID ->
            if (currentUID.isNotEmpty()){
                dashViewModel.posts.observe(viewLifecycleOwner) { posts ->
                    val adapter = RecyclerviewAdapter(requireContext(), ArrayList(posts), currentUID)
                    adapter.notifyDataSetChanged()
                    val layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerViewTimeline.layoutManager = layoutManager
                    binding.recyclerViewTimeline.adapter = adapter
                }
            }else{
                Toast.makeText(activity, "error getting current UID", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun loadPosts() {
        if (_binding != null) {
            binding.ibRefreshTimeline.setImageResource(R.drawable.ic_baseline_downloading_24)
        }
        showProgressDialog()

        coroutineScope.launch {
            val currentUserID = async { FireStoreClass().getUserID() }
            val followingList = async { FireStoreClass().getFollowingList(currentUserID.await()) }

            if (followingList.await().isNullOrEmpty()) {
                hideProgressDialog()
                Toast.makeText(activity, "following list isNullOrEmpty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val userPostsDeferred = followingList.await()?.map { user ->
                async { FireStoreClass().getPostsFromFireStore(user) }
            }

            val allPosts = userPostsDeferred?.awaitAll()?.filterNotNull()?.flatten()
            if (allPosts.isNullOrEmpty()) {
                hideProgressDialog()
                Toast.makeText(activity, "all posts isNullOrEmpty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.d("---all posts are:", "${allPosts.size}")

            val visiblePosts = ArrayList(allPosts.filter { it.visibility })

            Log.d("---timeline visible posts at beginning are:", "${visiblePosts.size}")

            val postsToUpdate = mutableListOf<PostStructure>()

            val timeJob = async { getTimeNow() }
            timeJob.await()
            Log.d("---getTimeCalled timeline", "date: $dateNow sec: $secondsNow")
            if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                for (post in allPosts){
                    if (!post.visibility){
                        if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                            postsToUpdate.add(post)
                            Log.d("---timeline posts to update are:", "${postsToUpdate.size}")
                        }
                    }
                }
            }else{
                //hideProgressDialog()
                Toast.makeText(activity, "err getting time", Toast.LENGTH_SHORT).show()
            }
            if (postsToUpdate.isNotEmpty()){
                if (postsToUpdate.size == 1) {
                    val postToBeUpdated = postsToUpdate[0]
                    Log.d("---timeline 1 post t b updated is:", "$postToBeUpdated")
                    val postHashMap = HashMap<String, Any>()
                    postHashMap["visibility"] = true
                    postHashMap["timeCreatedMillis"] = secondsNow

                    val updatePostJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().updatePostOnFireStore(requireActivity(), postToBeUpdated.userId,
                            postHashMap, postToBeUpdated.postID) { onComplete ->
                            deferredCompletable.complete(onComplete)
                        }
                        deferredCompletable.await()
                    }

                    if (updatePostJob.await()) {
                        postToBeUpdated.timeCreatedMillis = secondsNow
                        visiblePosts.add(postToBeUpdated)
                        Log.d("---timeline visible posts after adding 1 post for update:", "${visiblePosts.size}")
                    }else{
                        Toast.makeText(context, "err during update a post.", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Log.d("---timeline multiple posts to b updated is:", "${postsToUpdate.size}")
                    val postsByUser: Map<String, List<PostStructure>> = postsToUpdate.groupBy { it.userId }
                    val userUpdates = mutableMapOf<String, List<Map<String, Any>>>()
                    for ((userId, posts) in postsByUser) {
                        val updatesForUser = posts.map { post ->
                            mapOf("documentId" to post.postID, "data" to mapOf("visibility" to true, "timeCreatedMillis" to secondsNow))
                        }
                        userUpdates[userId] = updatesForUser
                    }

                    val batchUpdateJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().batchUpdatePostsForMultipleUsers(userUpdates) { successfully ->
                            deferredCompletable.complete(successfully)
                        }
                        deferredCompletable.await()
                    }

                    if (batchUpdateJob.await()) {
                        for (i in postsToUpdate){
                            i.timeCreatedMillis = secondsNow
                        }
                        visiblePosts.addAll(postsToUpdate)
                        Log.d("---timeline visible posts after adding multiple posts for update:", "${visiblePosts.size}")
                    } else {
                        Toast.makeText(context, "err during batch update posts.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val userGroupedPosts = visiblePosts.groupBy { it.userId }
            val userInfoJobs = userGroupedPosts.map { (userId, groupPosts) ->
                async {
                    val deferredUserInfo = CompletableDeferred<User?>()
                    FireStoreClass().getUserInfoWithCallback(userId) { userInfo ->
                        deferredUserInfo.complete(userInfo)
                    }
                    val userInfo = deferredUserInfo.await()
                    if (userInfo != null) {
                        Log.d("---user info is:", "$userInfo")
                        groupPosts.forEach { postItem ->
                            postItem.profilePicture = userInfo.image
                            postItem.userName = userInfo.userName
                        }
                    }
                }
            }
            userInfoJobs.awaitAll()
            Log.d("---all final visible posts are:", "${visiblePosts.size}")
            visiblePosts.sortByDescending { it.timeCreatedMillis.toLong() }
            dashViewModel.updatePosts(visiblePosts)

            hideProgressDialog()
            if (_binding != null){
                binding.ibRefreshTimeline.setImageResource(R.drawable.ic_baseline_refresh_24)
            }
        }
    }


    private suspend fun getTimeNow(){
        dateAndTimePair = GetTime().getCurrentTimeAndDate()
        if (dateAndTimePair != null){
            if (dateAndTimePair!!.first != null && dateAndTimePair!!.second != null){
                dateNow = dateAndTimePair!!.first!!
                secondsNow = dateAndTimePair!!.second!!
            }
        }
    }


    private fun showProgressDialog(){
        //progressDialog = Dialog(requireActivity())
        progressDialog.setContentView(R.layout.progress_dialog)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    private fun hideProgressDialog(){
        progressDialog.dismiss()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("---onDestroyView timeline called", "")
        _binding = null
        coroutineScope.cancel()
    }


    override fun onPause() {
        super.onPause()
        Log.d("---onPause timeline called", "")
        coroutineScope.cancel()
    }


    override fun onResume() {
        super.onResume()
        Log.d("---onResume timeline called", "")
        coroutineScope = CoroutineScope(Dispatchers.Main)
        //loadPosts()
    }


}