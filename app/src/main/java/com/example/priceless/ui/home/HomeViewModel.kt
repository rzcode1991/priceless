package com.example.priceless.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.FireStoreClass
import com.example.priceless.FragmentActivity
import com.example.priceless.PostStructure
import com.example.priceless.User

class HomeViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<PostStructure>>()

    val posts: LiveData<List<PostStructure>>
        get() = _posts

    init {
        // Load your data from Firestore or any other source and set it in _posts
        // You can use a coroutine here or any other method.
    }

    fun updatePosts(posts: List<PostStructure>) {
        _posts.value = posts
    }

    //private val _text = MutableLiveData<String>().apply {
    //    value = "This isssss home Fragment"
    //}
    //val text: LiveData<String> = _text


}