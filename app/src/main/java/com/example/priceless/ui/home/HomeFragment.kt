package com.example.priceless.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.priceless.*
import com.example.priceless.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var fireStoreClass: FireStoreClass
    private var mProfilePicture: String = ""
    private var mUserId: String = ""
    private var mUserName: String = ""
    private var mPostText: String = ""
    private var mPostImage: String = ""
    private var mTimeCreated: String = ""
    private var mVisibility: Boolean = true
    private var mTimeToShare: String = "now"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

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

        binding.ibCreatePost.setOnClickListener {
            val intent = Intent(activity, CreatePostActivity::class.java)
            startActivity(intent)
        }

        fireStoreClass = FireStoreClass()

        fireStoreClass.getPostsRealtimeListener(requireActivity()) { posts ->
            for (post in posts) {
                fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user ->
                    post.profilePicture = user.image
                    post.userName = user.userName

                    // Check if the fragment is attached to an activity and if it is still in the resumed state
                    if (isAdded && isVisible) {
                        val adapter = context?.let {
                            RecyclerviewAdapter(it, posts)
                        }
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




        /*
        fireStoreClass.getPostsRealtimeListener(requireActivity()) { posts ->

            for (post in posts){
                mProfilePicture = post.profilePicture
                mUserId = post.userId
                mUserName = post.userName
                mPostText = post.postText
                mPostImage = post.postImage
                mTimeCreated = post.timeCreated
                mVisibility = post.visibility
                mTimeToShare = post.timeToShare
            }

            //val postsList = generatePostsList()
            val adapter = context?.let { RecyclerviewAdapter(it, posts) }
            val layoutManager = LinearLayoutManager(context)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = layoutManager

        }


         */

        /*
        fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user ->
            userName = user.userName
            userFirstAndLastName = "${user.firstName} ${user.lastName}"
            userProfilePic = user.image
            userId = user.id

            val exampleList = generateDummyList()
            val adapter = context?.let { RecyclerviewAdapter(it, exampleList) }
            val layoutManager = LinearLayoutManager(context)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = layoutManager
        }

         */



    }


    private fun generatePostsList(): ArrayList<PostStructure>{
        val postsList = ArrayList<PostStructure>()
        val newPost = PostStructure(mProfilePicture, mUserId, mUserName, mPostText, mPostImage,
            mTimeCreated, mVisibility, mTimeToShare)
        postsList.add(newPost)
        return postsList
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
    }
}