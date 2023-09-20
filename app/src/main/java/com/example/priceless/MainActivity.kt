package com.example.priceless

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE
import androidx.appcompat.app.AppCompatActivity

class MainActivity : BaseActivity() {

    private lateinit var tvMain: TextView
    private lateinit var userInfo: User
    private lateinit var toolbarMain: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMain = findViewById(R.id.tv_main)
        toolbarMain = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbarMain)

        showProgressDialog()
        FireStoreClass().getUserInfoFromFireStore(this)

    }


    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
        userInfo = user
        tvMain.text = "first name: ${userInfo.firstName} last name: ${userInfo.lastName} email: ${userInfo.email}"
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(Constants.User_Extra_Details, userInfo)
                startActivity(intent)
                true
            }
            R.id.menu_empty -> {
                Toast.makeText(this, "Menu empty clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}