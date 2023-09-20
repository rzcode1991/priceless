package com.example.priceless.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var fireStoreClass: FireStoreClass

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

        if (!isDetached){
            fireStoreClass.getPostsRealtimeListener(requireActivity()) { posts ->
                for (post in posts) {
                    fireStoreClass.getUserInfoRealtimeListener(requireActivity()) { user ->
                        post.profilePicture = user.image
                        post.userName = user.userName

                        //val sortedPosts = Constants.sortedPosts
                        //sortedPosts[post.timeCreated] = post
                        // pass to recyclerView -->> sortedPosts.values

                        if (isAdded && isVisible) {

                            posts.sortByDescending { it.timeCreated }
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