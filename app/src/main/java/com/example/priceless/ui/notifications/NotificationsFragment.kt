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
import com.example.priceless.FireStoreClass
import com.example.priceless.FollowRequest
import com.example.priceless.R
import com.example.priceless.RequestsRVAdapter
import com.example.priceless.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private lateinit var progressDialog: Dialog
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

        progressDialog = Dialog(requireActivity())


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
            val adapter = RequestsRVAdapter(requireContext(), ArrayList(requests))
            adapter.notifyDataSetChanged()
            val layoutManager = LinearLayoutManager(requireContext())
            if (requests.isNotEmpty()){
                binding.recyclerViewNotifications.visibility = View.VISIBLE
                binding.recyclerViewNotifications.layoutManager = layoutManager
                binding.recyclerViewNotifications.adapter = adapter
            }else{
                binding.recyclerViewNotifications.visibility = View.GONE
            }
        }


    }


    private fun showProgressDialog(){
        //progressDialog = Dialog(requireActivity())
        progressDialog.setContentView(R.layout.progress_dialog)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    private fun hideProgressDialog(){
        progressDialog.dismiss()
    }


    private fun loadReceivedRequests(){
        if (_binding != null){
            binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_downloading_24)
        }
        //showProgressDialog()
        FireStoreClass().getCurrentUserID { currentUID ->
            if (currentUID.isNotEmpty()){
                FireStoreClass().getReceivedRequests(currentUID) { requests ->
                    if (!requests.isNullOrEmpty()) {
                        //hideProgressDialog()
                        requests.sortByDescending { it.senderUserID }
                        if (_binding != null){
                            binding.recyclerViewNotifications.visibility = View.VISIBLE
                        }
                        notificationsViewModel.updateRequestsForViewModel(requests)
                        if (_binding != null){
                            binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                        }
                    }else{
                        if (_binding != null){
                            Toast.makeText(activity, "You Don't Have Any Requests.", Toast.LENGTH_LONG).show()
                            binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                            binding.recyclerViewNotifications.visibility = View.GONE
                        }
                        //hideProgressDialog()
                    }
                }
            }else{
                if (_binding != null){
                    Toast.makeText(activity, "Error Getting Current UserID; Check Your Internet Connection", Toast.LENGTH_LONG).show()
                    binding.ibRefreshNotifications.setImageResource(R.drawable.ic_baseline_refresh_24)
                    binding.recyclerViewNotifications.visibility = View.GONE
                }
                //hideProgressDialog()
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
        loadReceivedRequests()
    }

}