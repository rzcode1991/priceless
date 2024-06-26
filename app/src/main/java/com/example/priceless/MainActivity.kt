package com.example.priceless

import android.os.Bundle
import android.widget.TextView

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
        val userID = FireStoreClass().getUserID()
        FireStoreClass().getUserInfoFromFireStore(this, userID)

    }


    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()
        userInfo = user
        tvMain.text = "first name: ${userInfo.firstName} last name: ${userInfo.lastName} email: ${userInfo.email}"
    }


}