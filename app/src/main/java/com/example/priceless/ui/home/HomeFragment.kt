package com.example.priceless.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.priceless.*
import com.example.priceless.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import androidx.lifecycle.Observer


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var fireStoreClass: FireStoreClass
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var getTime: GetTime? = null
    private var dateAndTimePair: Pair<String?, String?>? = null
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var firstRequest: Long = 0L
    private var lastRequestTimeMillis: Long = 0L
    private val requestCoolDownMillis: Long = 5000L
    private lateinit var homeViewModel: HomeViewModel


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        fireStoreClass = FireStoreClass()
        getTime = GetTime()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val textView: TextView = binding.textHome
        //homeViewModel.text.observe(viewLifecycleOwner) {
        //    textView.text = it
        //}
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        //fireStoreClass = FireStoreClass()
        //getTime = GetTime()

        loadPosts()

        binding.ibCreatePost.setOnClickListener {
            val intent = Intent(activity, CreatePostActivity::class.java)
            startActivity(intent)
        }

        binding.ibRefresh.setOnClickListener {
            loadPosts()
        }


    }

    /*
    private fun loadPostsAlternative(){
        if (!isDetached && isAdded && isVisible){
            fireStoreClass.getPostsRealTimeListener(requireActivity()) { posts ->
                for (post in posts) {
                    val userID = post.userId
                    fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user ->
                        post.profilePicture = user.image
                        post.userName = user.userName

                        //val sortedPosts = Constants.sortedPosts
                        //sortedPosts[post.timeCreated] = post
                        // pass to recyclerView -->> sortedPosts.values

                        if (!isDetached && isAdded && isVisible) {

                            posts.sortByDescending { it.timeCreatedMillis }
                            val adapter = context?.let {
                                RecyclerviewAdapter(it, ArrayList(posts))
                            }
                            adapter?.notifyDataSetChanged()
                            val layoutManager = LinearLayoutManager(context)

                            // Check if the RecyclerView is still attached to the view hierarchy
                            if (!isDetached) {
                                binding.recyclerView.adapter = adapter
                                binding.recyclerView.layoutManager = layoutManager
                            }
                        }
                    }
                }
            }
        }
    }

     */


    private fun loadPosts() {
        Log.d("call number", "1")
        if (!isDetached && isAdded && isVisible){
            // TODO: consider adding progress dialog or changing refresh btn background to "..."
            fireStoreClass.getPostsRealTimeListener(requireActivity()) { posts, success ->
                if (!isDetached && isAdded && isVisible){
                    if (success && posts != null && posts.isNotEmpty()){
                        Log.d("posts beginning are:", "$posts")
                        val visiblePosts = ArrayList(posts.filter { it.visibility })
                        val postsToUpdate = mutableListOf<PostStructure>()
                        for (post in posts){
                            if (!post.visibility) {
                                val currentTimeMillis = System.currentTimeMillis()
                                if (currentTimeMillis > lastRequestTimeMillis + requestCoolDownMillis) {
                                    lastRequestTimeMillis = currentTimeMillis
                                    coroutineScope.launch {
                                        getTimeNow()
                                        if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                                            if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                                                postsToUpdate.add(post)
                                            }
                                        }
                                    }
                                    if (postsToUpdate.size == 1) {
                                        val postToBeUpdated = postsToUpdate[0]
                                        Log.d("1 post t b updated is:", "$postToBeUpdated")
                                        val postHashMap = HashMap<String, Any>()
                                        postHashMap["visibility"] = true
                                        postHashMap["timeCreatedMillis"] = secondsNow
                                        if (!isDetached && isAdded && isVisible){
                                            fireStoreClass.updatePostOnFireStore(requireActivity(), postHashMap, postToBeUpdated.postID)
                                            visiblePosts.add(postToBeUpdated)
                                            Log.d("visible posts after adding 1 post for update:", "$visiblePosts")
                                        }
                                    }else if (postsToUpdate.size > 1){
                                        // TODO: problem with list posts to be updated
                                        Log.d("list of posts to be updated is:", "$postsToUpdate")
                                        val batchUpdates = mutableMapOf<String, Map<String, Any>>()
                                        for (eachPost in postsToUpdate) {
                                            val postHashMap = HashMap<String, Any>()
                                            postHashMap["visibility"] = true
                                            postHashMap["timeCreatedMillis"] = secondsNow
                                            batchUpdates[eachPost.postID] = postHashMap
                                        }
                                        if (!isDetached && isAdded && isVisible){
                                            fireStoreClass.batchUpdatePostsOnFireStore(requireActivity(), batchUpdates) { success ->
                                                if (!isDetached && isAdded && isVisible){
                                                    if (success) {
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
                                    }
                                }
                            }
                        }
                        if (!isDetached && isAdded && isVisible){
                            fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user, successful ->
                                if (!isDetached && isAdded && isVisible){
                                    if (successful && user != null){
                                        Log.d("user is:", "$user")
                                        for (i in visiblePosts){
                                            i.profilePicture = user.image
                                            i.userName = user.userName
                                        }
                                        visiblePosts.sortByDescending { it.timeCreatedMillis }
                                        Log.d("so visible posts are:", "$visiblePosts")
                                        requireActivity().runOnUiThread {
                                            val adapter = RecyclerviewAdapter(requireContext(), visiblePosts)
                                            adapter.notifyDataSetChanged()
                                            val layoutManager = LinearLayoutManager(requireContext())
                                            binding.recyclerView.layoutManager = layoutManager
                                            binding.recyclerView.adapter = adapter
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /*
    private fun loadPostsCOPY() {
        Log.d("call number", "1")
        if (!isDetached && isAdded && isVisible){
            // TODO: consider adding progress dialog or changing refresh btn background to "..."
            fireStoreClass.getPostsRealTimeListener(requireActivity()) { posts, success ->
                if (!isDetached && isAdded && isVisible){
                    if (success && posts != null && posts.isNotEmpty()){
                        Log.d("posts beginning are:", "$posts")
                        val visiblePosts = ArrayList(posts.filter { it.visibility })
                        val postsToUpdate = mutableListOf<PostStructure>()
                        for (post in posts){
                            if (!post.visibility) {
                                val currentTimeMillis = System.currentTimeMillis()
                                if (currentTimeMillis > lastRequestTimeMillis + requestCoolDownMillis) {
                                    lastRequestTimeMillis = currentTimeMillis
                                    coroutineScope.launch {
                                        getTimeNow()
                                        if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                                            if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                                                postsToUpdate.add(post)
                                            }
                                        }
                                    }
                                    if (postsToUpdate.size == 1) {
                                        val postToBeUpdated = postsToUpdate[0]
                                        Log.d("1 post t b updated is:", "$postToBeUpdated")
                                        val postHashMap = HashMap<String, Any>()
                                        postHashMap["visibility"] = true
                                        postHashMap["timeCreatedMillis"] = secondsNow
                                        if (!isDetached && isAdded && isVisible){
                                            fireStoreClass.updatePostOnFireStore(requireActivity(), postHashMap, postToBeUpdated.postID)
                                            visiblePosts.add(postToBeUpdated)
                                            Log.d("visible posts after adding 1 post for update:", "$visiblePosts")
                                        }
                                    }else if (postsToUpdate.size > 1){
                                        // TODO: problem with list posts to be updated
                                        Log.d("list of posts to be updated is:", "$postsToUpdate")
                                        val batchUpdates = mutableMapOf<String, Map<String, Any>>()
                                        for (eachPost in postsToUpdate) {
                                            val postHashMap = HashMap<String, Any>()
                                            postHashMap["visibility"] = true
                                            postHashMap["timeCreatedMillis"] = secondsNow
                                            batchUpdates[eachPost.postID] = postHashMap
                                        }
                                        if (!isDetached && isAdded && isVisible){
                                            fireStoreClass.batchUpdatePostsOnFireStore(requireActivity(), batchUpdates) { success ->
                                                if (!isDetached && isAdded && isVisible){
                                                    if (success) {
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
                                    }
                                }
                            }
                        }
                        if (!isDetached && isAdded && isVisible){
                            fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user, successful ->
                                if (!isDetached && isAdded && isVisible){
                                    if (successful && user != null){
                                        Log.d("user is:", "$user")
                                        for (i in visiblePosts){
                                            i.profilePicture = user.image
                                            i.userName = user.userName
                                        }
                                        visiblePosts.sortByDescending { it.timeCreatedMillis }
                                        Log.d("so visible posts are:", "$visiblePosts")
                                        requireActivity().runOnUiThread {
                                            val adapter = RecyclerviewAdapter(requireContext(), visiblePosts)
                                            adapter.notifyDataSetChanged()
                                            val layoutManager = LinearLayoutManager(requireContext())
                                            binding.recyclerView.layoutManager = layoutManager
                                            binding.recyclerView.adapter = adapter
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
     */


    private suspend fun getTimeNow(){
        dateAndTimePair = getTime?.getCurrentTimeAndDate()
        if (dateAndTimePair != null){
            if (dateAndTimePair!!.first != null && dateAndTimePair!!.second != null){
                dateNow = dateAndTimePair!!.first!!
                secondsNow = dateAndTimePair!!.second!!
            }
        }
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
                Toast.makeText(activity, "Menu empty clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fireStoreClass.removePostsSnapshotListener()
    }
}