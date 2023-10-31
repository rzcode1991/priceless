package com.example.priceless.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.PostStructure

class DashboardViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<PostStructure>>()

    val posts: LiveData<List<PostStructure>>
        get() = _posts

    fun updatePosts(posts: List<PostStructure>) {
        _posts.value = posts
    }

}