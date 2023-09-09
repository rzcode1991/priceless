package com.example.priceless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView

class CreatePostActivity : BaseActivity() {

    private lateinit var cbSendToFuture: CheckBox
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        cbSendToFuture = findViewById(R.id.cb_send_to_future)
        btnSelectDate = findViewById(R.id.btn_select_date)
        tvSelectedDate = findViewById(R.id.tv_selected_date)




    }



}