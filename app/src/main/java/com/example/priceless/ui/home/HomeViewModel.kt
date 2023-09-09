package com.example.priceless.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.FireStoreClass
import com.example.priceless.FragmentActivity
import com.example.priceless.User

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This isssss home Fragment"
    }
    val text: LiveData<String> = _text
}