package com.example.priceless

import android.os.Bundle
import androidx.appcompat.widget.Toolbar

@Suppress("DEPRECATION")
class TermsActivity : BaseActivity() {

    private lateinit var toolbarTerms: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        toolbarTerms = findViewById(R.id.toolbar_terms)

        setActionBar()

    }


    private fun setActionBar(){
        setSupportActionBar(toolbarTerms)
        val actionBar = supportActionBar
        actionBar?.title = ""
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back)
        }
        toolbarTerms.setNavigationOnClickListener{ onBackPressed() }
    }


}