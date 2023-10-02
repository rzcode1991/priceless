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

    //private val _text = MutableLiveData<String>().apply {
    //    value = "This isssss home Fragment"
    //}
    //val text: LiveData<String> = _text

    /*
    private val userDataLiveData = MutableLiveData<User>()
    private val postsDataLiveData: MutableLiveData<ArrayList<PostStructure>> = MutableLiveData()
    private val fireStoreClass: FireStoreClass = FireStoreClass()
    private var posts = ArrayList<PostStructure>()

    fun setUserData(user: User) {
        userDataLiveData.value = user
    }

    fun getUserData(): LiveData<User> {
        return userDataLiveData
    }

    /*
    fun setPostsData(posts: ArrayList<PostStructure>){
        postsDataLiveData.value = posts
    }

     */

    private fun setPostsData(){
        postsDataLiveData.value = posts
    }

    fun getPostsData(): LiveData<ArrayList<PostStructure>> {
        fireStoreClass.getPostsFromFireStoreAndReturnThem { postsFromCloud ->
            if (postsFromCloud.isNotEmpty()) {
                postsDataLiveData.postValue(postsFromCloud)
            } else {
                Log.d("err getting posts", "in home view model")
            }
        }
        return postsDataLiveData
    }

     */


}