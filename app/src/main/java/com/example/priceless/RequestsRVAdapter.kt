package com.example.priceless

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class RequestsRVAdapter(val context: Context, private val requestList: ArrayList<FollowRequest>,
                        private val senderProfilePic: String, private val senderUserName: String):
    RecyclerView.Adapter<RequestsRVAdapter.RequestViewHolder>(){

    private lateinit var progressDialog: Dialog
    fun showProgressDialog(){
        progressDialog = Dialog(context)
        progressDialog.setContentView(R.layout.progress_dialog)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    fun hideProgressDialog(){
        progressDialog.dismiss()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)
        return RequestViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        // we are talking about received Requests for now
        val currentRequest = requestList[position]
        if (senderProfilePic.isNotEmpty()){
            GlideLoader(context).loadImageUri(senderProfilePic, holder.profilePic)
        }else{
            holder.profilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        if (currentRequest.accepted){
            holder.tvRequest.text = "$senderUserName Is Now Following You."
            holder.layOutActionRequest.visibility = View.GONE
        }else{
            holder.tvRequest.text = "$senderUserName Wants To Follow You."
            holder.layOutActionRequest.visibility = View.VISIBLE
            holder.btnAcceptRequest.setOnClickListener {
                showProgressDialog()
                FireStoreClass().acceptFollowRequest(currentRequest.receiverUserID,
                    currentRequest.senderUserID) { success ->
                    if (success){
                        holder.tvRequest.text = "$senderUserName Is Now Following You."
                        holder.layOutActionRequest.visibility = View.GONE
                    } // we could here say: else -> hideProgressDialog
                }
            }
            holder.btnRejectRequest.setOnClickListener {
                Toast.makeText(context, "you clicked on reject request", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    class RequestViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val profilePic: ImageView = itemView.findViewById(R.id.iv_profile_pic_request)
        val tvRequest: TextView = itemView.findViewById(R.id.tv_request)
        val layOutActionRequest: LinearLayout = itemView.findViewById(R.id.layout_action_request)
        val btnAcceptRequest: Button = itemView.findViewById(R.id.btn_accept_request)
        val btnRejectRequest: Button = itemView.findViewById(R.id.btn_reject_request)
    }

}