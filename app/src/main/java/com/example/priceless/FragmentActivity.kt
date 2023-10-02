package com.example.priceless

import android.app.Activity
import android.content.ClipData
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.priceless.databinding.ActivityFragmentBinding
import com.example.priceless.ui.home.HomeViewModel

class FragmentActivity : BaseActivity() {

    private lateinit var binding: ActivityFragmentBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fireStoreClass: FireStoreClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        fireStoreClass = FireStoreClass()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        //fireStoreClass.getUserInfoFromFireStore(this)
        //
        //fireStoreClass.getPostsRealtimeListener()
        //fireStoreClass.getPostsFromFireStore(this)
    }


    /*
    fun successGettingUserInfoFromFireStore(user: User){
        homeViewModel.setUserData(user)
        //hideProgressDialog()
    }


    fun successGettingPostsFromFireStore(posts: ArrayList<PostStructure>){
        //homeViewModel.setPostsData(posts)
    }

     */


    override fun onBackPressed() {
        doublePressBackButtonToExit()
    }

}