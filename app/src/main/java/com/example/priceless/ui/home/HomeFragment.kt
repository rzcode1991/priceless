package com.example.priceless.ui.home

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.priceless.*
import com.example.priceless.databinding.FragmentHomeBinding
import kotlinx.coroutines.*


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private lateinit var dateAndTimePair: Pair<String, String>
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var progressDialog: Dialog
    private var lastRequestTimeMillis: Long = 0L
    private val requestCoolDownMillis: Long = 1000L


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        Log.d("onCreate called", "")
        //fireStoreClass = FireStoreClass()
        //getTime = GetTime()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        coroutineScope = CoroutineScope(Dispatchers.Main)
        Log.d("onCreateView called", "")
        //val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        progressDialog = Dialog(requireActivity())

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("onViewCreated called", "")

        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.ibCreatePost.setOnClickListener {
            val intent = Intent(activity, CreatePostActivity::class.java)
            startActivity(intent)
            //activity?.finish()
            // we could finish the activity and override onBackPressed, if too many request bothers
        }

        binding.ibRefresh.setOnClickListener {
            loadPosts()
        }

        loadPosts()

        val currentUserID = FireStoreClass().getUserID()

        homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
            binding.pbFragmentHome.visibility = View.GONE
            val adapter = RecyclerviewAdapter(requireContext(), ArrayList(posts), currentUserID)
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = adapter
        }


    }



    private fun loadPosts() {
        if (_binding != null){
            binding.ibRefresh.setImageResource(R.drawable.ic_baseline_downloading_24)
        }
        //showProgressDialog()
        coroutineScope.launch {
            val deferredUserID = async { FireStoreClass().getUserID() }
            val userID = deferredUserID.await()

            val deferredAllPosts = async { FireStoreClass().getPostsFromFireStore(userID) }
            val allPosts = deferredAllPosts.await()

            if (allPosts.isNullOrEmpty()) {
                //hideProgressDialog()
                if (_binding != null){
                    Toast.makeText(activity, "There Are No Posts To Show.", Toast.LENGTH_SHORT).show()
                    binding.pbFragmentHome.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                }
                return@launch
            }

            Log.d("--- all posts beginning:", "${allPosts.size}")
            val visiblePosts = ArrayList(allPosts.filter { it.visibility })
            Log.d("--- already visible posts beginning:", "${visiblePosts.size}")
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
                            Log.d("--- posts to update are:", "${postsToUpdate.size}")
                        }
                    }
                }
            }else{
                /*
                if (_binding != null) {
                    Toast.makeText(activity, "Error Getting Time; Check Your Internet Connection", Toast.LENGTH_SHORT).show()
                }
                 */
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
                        FireStoreClass().updatePostOnFireStore(requireActivity(), postToBeUpdated.userId,
                            postHashMap, postToBeUpdated.postID) { onComplete ->
                            deferredCompletable.complete(onComplete)
                        }
                        deferredCompletable.await()
                    }
                    if (updatePostJob.await()) {
                        postToBeUpdated.visibility = true
                        postToBeUpdated.timeCreatedMillis = secondsNow
                        visiblePosts.add(postToBeUpdated)
                        Log.d("--- visible posts after adding 1 post for update:", "${visiblePosts.size}")
                    }else{
                        if (_binding != null){
                            Toast.makeText(context, "err during update a post.", Toast.LENGTH_SHORT).show()
                        }
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
                        eachPost.visibility = true
                    }
                    val batchUpdateJob = async {
                        val deferredCompletable = CompletableDeferred<Boolean>()
                        FireStoreClass().batchUpdatePostsOnFireStore(userID, batchUpdates) { successfully ->
                            deferredCompletable.complete(successfully)
                        }
                        deferredCompletable.await()
                    }
                    if (batchUpdateJob.await()) {
                        visiblePosts.addAll(postsToUpdate)
                        Log.d("--- visible posts after adding multiple posts for update:", "${visiblePosts.size}")
                    } else {
                        if (_binding != null){
                            Toast.makeText(context, "err during batch update posts.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            val userInfoJob = async {
                val deferredUserInfo = CompletableDeferred<User?>()
                FireStoreClass().getUserInfoWithCallback(userID) { userInfo ->
                    deferredUserInfo.complete(userInfo)
                }
                val userInfo = deferredUserInfo.await()
                if (userInfo != null) {
                    Log.d("--- user info is:", "$userInfo")
                    allPosts.forEach { postItem ->
                        postItem.profilePicture = userInfo.image
                        postItem.userName = userInfo.userName
                    }
                }
            }
            userInfoJob.await()
            //Log.d("--- all final visible posts are:", "${visiblePosts.size}")
            allPosts.sortByDescending { it.timeCreatedMillis.toLong() }
            if (_binding != null){
                binding.recyclerView.visibility = View.VISIBLE
                binding.ibRefresh.setImageResource(R.drawable.ic_baseline_refresh_24)
                binding.pbFragmentHome.visibility = View.GONE
            }
            homeViewModel.updatePosts(allPosts)
            //hideProgressDialog()
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_options, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                val intent = Intent(activity, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_empty -> {
                activity?.finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("onDestroyView called", "")
        _binding = null
        coroutineScope.cancel()
        //FragmentActivity().finish()
    }

    /*
    override fun onDestroy() {
        super.onDestroy()
        fireStoreClass.removePostsSnapshotListener()
        coroutineScope.cancel()
        //FragmentActivity().finish()
    }

     */

    override fun onPause() {
        super.onPause()
        Log.d("onPause called", "")
        coroutineScope.cancel()
    }



    override fun onResume() {
        super.onResume()
        Log.d("onResume called", "")
        coroutineScope = CoroutineScope(Dispatchers.Main)
        loadPosts()
    }




}