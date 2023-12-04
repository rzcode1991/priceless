package com.example.priceless.ui.notifications

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.priceless.*
import com.example.priceless.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private lateinit var notificationsViewModel: NotificationsViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        Log.d("--------", "onCreateView called")
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("--------", "onViewCreated called")
        //notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)
        notificationsViewModel = ViewModelProvider(requireActivity()).get(NotificationsViewModel::class.java)

        binding.ibRefreshNotifications.setOnClickListener {
            loadReceivedRequests()
        }

        loadReceivedRequests()

        notificationsViewModel.requests.observe(viewLifecycleOwner) { requests ->
            val adapter = binding.recyclerViewNotifications.adapter as? RequestsRVAdapter
            if (adapter == null) {
                // Adapter is not created yet, create a new one
                val newAdapter = RequestsRVAdapter(requireContext(), ArrayList(requests))
                val layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerViewNotifications.layoutManager = layoutManager
                binding.recyclerViewNotifications.adapter = newAdapter
            } else {
                // Clear existing data and update the adapter with new data
                adapter.clearData()
                adapter.updateData(requests)
            }
        }


        /*
        notificationsViewModel.requests.observe(viewLifecycleOwner) { requests ->
            val adapter = RequestsRVAdapter(requireContext(), ArrayList(requests))
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewNotifications.layoutManager = layoutManager
            binding.recyclerViewNotifications.adapter = adapter
        }

         */

    }

    private fun loadReceivedRequests(){
        if (_binding != null){
            binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_downloading_24)
        }
        FireStoreClass().getCurrentUserID { currentUID ->
            if (currentUID.isNotEmpty()){
                FireStoreClass().getReceivedRequestsRealTime(currentUID) { requests ->
                    if (!requests.isNullOrEmpty()) {
                        val finalRequests = ArrayList<FollowRequest>()
                        finalRequests.addAll(requests)
                        Log.d("---request size from fire store:", finalRequests.size.toString())
                        CoroutineScope(Dispatchers.Main).launch {
                            val userGroupedRequests = finalRequests.groupBy { it.senderUserID }
                            val userInfoJobs = userGroupedRequests.map { (userId, groupRequests) ->
                                async {
                                    val deferredUserInfo = CompletableDeferred<User?>()
                                    FireStoreClass().getUserInfoWithCallback(userId) { userInfo ->
                                        deferredUserInfo.complete(userInfo)
                                    }
                                    val userInfo = deferredUserInfo.await()
                                    if (userInfo != null) {
                                        groupRequests.forEach { requestItem ->
                                            requestItem.senderProfilePic = userInfo.image
                                            requestItem.senderUserName = userInfo.userName
                                        }
                                    }
                                }
                            }
                            userInfoJobs.awaitAll()

                            finalRequests.sortByDescending { it.senderUserID }
                            notificationsViewModel.updateRequestsForViewModel(finalRequests)
                            if (_binding != null){
                                binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                            }
                        }
                    }else{
                        if (_binding != null){
                            Toast.makeText(activity, "You Don't Have Any Requests.", Toast.LENGTH_LONG).show()
                            binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                            binding.recyclerViewNotifications.visibility = View.GONE
                        }
                    }
                }
            }else{
                if (_binding != null){
                    Toast.makeText(activity, "Error Getting Current UserID; Check Your Internet Connection", Toast.LENGTH_LONG).show()
                    binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                    binding.recyclerViewNotifications.visibility = View.GONE
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("--------", "onDestroyView called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("--------", "onResume called")
        //loadReceivedRequests()
    }

}