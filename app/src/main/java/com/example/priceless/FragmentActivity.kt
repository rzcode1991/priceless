package com.example.priceless

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.priceless.databinding.ActivityFragmentBinding
import com.example.priceless.ui.home.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class FragmentActivity : BaseActivity() {

    private lateinit var binding: ActivityFragmentBinding
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            val serviceIntent = Intent(this, FireStoreListenerService::class.java)
            startService(serviceIntent)
        }else{
            val serviceIntent = Intent(this, FireStoreListenerJobIntentService::class.java)
            FireStoreListenerJobIntentService.enqueueWork(this, serviceIntent)
        }

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

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
    }


    @Deprecated("Deprecated in Java", ReplaceWith("doublePressBackButtonToExit()"))
    override fun onBackPressed() {
        doublePressBackButtonToExit()
    }

}