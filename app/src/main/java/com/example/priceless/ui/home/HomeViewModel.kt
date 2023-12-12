package com.example.priceless.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.PostStructure

class HomeViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<PostStructure>>()

    val posts: LiveData<List<PostStructure>>
        get() = _posts

    fun updatePosts(posts: List<PostStructure>) {
        _posts.value = posts
    }

    //private val _text = MutableLiveData<String>().apply {
    //    value = "This is home Fragment"
    //}
    //val text: LiveData<String> = _text


}