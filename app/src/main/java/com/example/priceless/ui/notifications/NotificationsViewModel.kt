package com.example.priceless.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.FollowRequest
import com.example.priceless.PostStructure

class NotificationsViewModel : ViewModel() {

    private val _requests = MutableLiveData<List<FollowRequest>>()

    val requests: LiveData<List<FollowRequest>>
        get() = _requests

    init {
        // Load your data from Firestore or any other source and set it in _requests
        // You can use a coroutine here or any other method.
    }

    fun updateRequestsForViewModel(requests: List<FollowRequest>) {
        _requests.value = requests
    }

}