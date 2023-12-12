package com.example.priceless.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.priceless.FollowRequest

class NotificationsViewModel : ViewModel() {

    private val _requests = MutableLiveData<List<FollowRequest>>()

    val requests: LiveData<List<FollowRequest>>
        get() = _requests

    fun updateRequestsForViewModel(requests: List<FollowRequest>) {
        _requests.value = requests
    }

}