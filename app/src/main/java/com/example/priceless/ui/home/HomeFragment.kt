package com.example.priceless.ui.home

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var fireStoreClass: FireStoreClass
    private var dateNow: String = ""
    private var secondsNow: String = ""
    private var getTime: GetTime? = null
    private var dateAndTimePair: Pair<String?, String?>? = null
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var firstRequestTimeMillis: Long = 6000L
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

        //val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //loadPosts()

        //val textView: TextView = binding.textHome
        //homeViewModel.text.observe(viewLifecycleOwner) {
        //    textView.text = it
        //}
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        binding.ibCreatePost.setOnClickListener {
            val intent = Intent(activity, CreatePostActivity::class.java)
            startActivity(intent)
        }

        binding.ibRefresh.setOnClickListener {
            loadPosts()
        }

        loadPosts()

        homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
            // Update your RecyclerView adapter with the new data
            val adapter = RecyclerviewAdapter(requireContext(), ArrayList(posts))
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = adapter
        }


    }


    private fun loadPosts() {
        Log.d("call number", "1")
        binding.ibRefresh.setImageResource(R.drawable.ic_baseline_downloading_24)
        fireStoreClass.getPostsRealTimeListener(requireActivity()) { posts, success ->
            if (success && posts != null && posts.isNotEmpty()){
                posts.sortBy { it.timeToShare.toLong() }
                Log.d("posts beginning are:", "$posts")
                val visiblePosts = ArrayList(posts.filter { it.visibility })
                Log.d("already visible posts at beginning are:", "$visiblePosts")
                val postsToUpdate = mutableListOf<PostStructure>()
                for (post in posts){
                    if (!post.visibility) {
                        if (System.currentTimeMillis() > lastRequestTimeMillis + requestCoolDownMillis) {
                            Log.d("time:", "SystemCurrentTime: ${System.currentTimeMillis()} " +
                                    "lastRequestTimeMillis: $lastRequestTimeMillis " +
                                    "requestCoolDownMillis: $requestCoolDownMillis")
                            lastRequestTimeMillis = System.currentTimeMillis()
                            coroutineScope.launch {
                                getTimeNow()
                                Log.d("getTimeCalled", "date: $dateNow sec: $secondsNow")
                                if (dateNow.isNotEmpty() && secondsNow.isNotEmpty()) {
                                    if (secondsNow.toLong() >= post.timeToShare.toLong()) {
                                        postsToUpdate.add(post)
                                        Log.d("posts to update are:", "$postsToUpdate")
                                        if (postsToUpdate.size == 1) {
                                            val postToBeUpdated = postsToUpdate[0]
                                            Log.d("1 post t b updated is:", "$postToBeUpdated")
                                            val postHashMap = HashMap<String, Any>()
                                            postHashMap["visibility"] = true
                                            postHashMap["timeCreatedMillis"] = secondsNow
                                            fireStoreClass.updatePostOnFireStore(requireActivity(), postHashMap, postToBeUpdated.postID)
                                            visiblePosts.add(postToBeUpdated)
                                            Log.d("visible posts after adding 1 post for update:", "$visiblePosts")
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
                                            fireStoreClass.batchUpdatePostsOnFireStore(requireActivity(), batchUpdates) { successfully ->
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
                                }
                            }
                        }
                    }
                }
                fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user, successful ->
                    if (successful && user != null){
                        Log.d("user is:", "$user")
                        for (i in visiblePosts){
                            i.profilePicture = user.image
                            i.userName = user.userName
                        }
                        visiblePosts.sortByDescending { it.timeCreatedMillis.toLong() }
                        Log.d("final visible posts are:", "$visiblePosts")
                        homeViewModel.updatePosts(visiblePosts)
                        binding.ibRefresh.setImageResource(R.drawable.ic_baseline_refresh_24)
                    }
                }
            }
        }
    }



    private suspend fun getTimeNow(){
        firstRequestTimeMillis = System.currentTimeMillis()
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

    override fun onResume() {
        super.onResume()
        loadPosts()
    }
}