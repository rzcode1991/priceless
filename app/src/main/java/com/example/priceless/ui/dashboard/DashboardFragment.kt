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


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var dashViewModel: DashboardViewModel
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var progressDialog: Dialog
    private var lastRequestTimeMillis: Long = 0L
    private val requestCoolDownMillis: Long = 1000L

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        coroutineScope = CoroutineScope(Dispatchers.Main)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        progressDialog = Dialog(requireActivity())

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    binding.pbFragmentDashboard.visibility = View.GONE
                    val adapter = RecyclerviewAdapter(requireContext(), ArrayList(posts))
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
        //showProgressDialog()

        coroutineScope.launch {
            val currentUserID = async { FireStoreClass().getUserID() }
            val followingList = async { FireStoreClass().getFollowingList(currentUserID.await()) }

            if (followingList.await().isNullOrEmpty()) {
                //hideProgressDialog()
                if (_binding != null) {
                    Toast.makeText(activity, "You Are Not Following Any User.", Toast.LENGTH_SHORT).show()
                    binding.pbFragmentDashboard.visibility = View.GONE
                }
                return@launch
            }

            val userPostsDeferred = followingList.await()?.map { user ->
                async { FireStoreClass().getPostsFromFireStore(user) }
            }

            val allPostsList = userPostsDeferred?.awaitAll()?.filterNotNull()?.flatten()
            val allPosts = ArrayList<PostStructure>()
            if (!allPostsList.isNullOrEmpty()) {
                for (post in allPostsList){
                    if (post.postID.isNotEmpty() && post.buyerID.isEmpty()){
                        allPosts.add(post)
                    }
                }
            }else{
                //hideProgressDialog()
                if (_binding != null) {
                    Toast.makeText(activity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                    binding.pbFragmentDashboard.visibility = View.GONE
                    binding.recyclerViewTimeline.visibility = View.GONE
                }
                return@launch
            }

            val visiblePosts = ArrayList(allPosts.filter { it.visibility })

            val postsToUpdate = mutableListOf<PostStructure>()

            if (System.currentTimeMillis() > lastRequestTimeMillis + requestCoolDownMillis){
                lastRequestTimeMillis = System.currentTimeMillis()
                val timeJob = async { getTimeNow() }
                timeJob.await()
            }
            if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                for (post in allPosts){
                    if (!post.visibility){
                        if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                            postsToUpdate.add(post)
                        }
                    }
                }
            }else{
                /*
                //hideProgressDialog()
                if (_binding != null) {
                    Toast.makeText(activity, "Error Getting Time; Check Your Internet Connection", Toast.LENGTH_SHORT).show()
                }
                 */
            }
            if (postsToUpdate.isNotEmpty()){
                if (postsToUpdate.size == 1) {
                    val postToBeUpdated = postsToUpdate[0]
                    val postHashMap = HashMap<String, Any>()
                    postHashMap["visibility"] = true
                    postHashMap["timeCreatedMillis"] = secondsNow

                    val updatePostJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().updatePostOnFireStore(postToBeUpdated.userId,
                            postHashMap, postToBeUpdated.postID) { onComplete ->
                            deferredCompletable.complete(onComplete)
                        }
                        deferredCompletable.await()
                    }

                    if (updatePostJob.await()) {
                        postToBeUpdated.visibility = true
                        postToBeUpdated.timeCreatedMillis = secondsNow
                        visiblePosts.add(postToBeUpdated)
                    }else{
                        if (_binding != null){
                            Toast.makeText(context, "err during update a post.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
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
                            i.visibility = true
                        }
                        visiblePosts.addAll(postsToUpdate)
                    } else {
                        if (_binding != null){
                            Toast.makeText(context, "err during batch update posts.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
            if (allPosts.isNotEmpty()){
                allPosts.sortByDescending { it.timeCreatedMillis.toLong() }
                if (_binding != null){
                    binding.recyclerViewTimeline.visibility = View.VISIBLE
                    binding.ibRefreshTimeline.setImageResource(R.drawable.ic_baseline_refresh_24)
                    binding.pbFragmentDashboard.visibility = View.GONE
                }
                dashViewModel.updatePosts(allPosts)
                //hideProgressDialog()
            }else{
                if (_binding != null){
                    binding.pbFragmentDashboard.visibility = View.GONE
                    binding.recyclerViewTimeline.visibility = View.GONE
                    Toast.makeText(activity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                }
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



    /*
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

     */


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        coroutineScope.cancel()
    }


    override fun onPause() {
        super.onPause()
        coroutineScope.cancel()
    }


    override fun onResume() {
        super.onResume()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        loadPosts()
    }


}