package com.example.priceless

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class RequestsRVAdapter(val context: Context, private val requestList: ArrayList<FollowRequest>):
    RecyclerView.Adapter<RequestsRVAdapter.RequestViewHolder>(){

    private lateinit var progressDialog: Dialog
    private fun showProgressDialog(){
        progressDialog = Dialog(context)
        progressDialog.setContentView(R.layout.progress_dialog)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    private fun hideProgressDialog(){
        progressDialog.dismiss()
    }

    fun updateData(newData: List<FollowRequest>) {
        requestList.addAll(newData)
        notifyDataSetChanged()
    }

    fun clearData() {
        requestList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)
        return RequestViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        // we are talking about received Requests for now
        val currentRequest = requestList[position]

        holder.layOutActionRequest.visibility = View.GONE

        val senderProfilePic = currentRequest.senderProfilePic
        val senderUserName = currentRequest.senderUserName

        if (senderProfilePic.isNotEmpty()){
            GlideLoader(context).loadImageUriCircleCrop(senderProfilePic, holder.profilePic)
        }else{
            holder.profilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        holder.profilePic.setOnClickListener {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("userID", currentRequest.senderUserID)
            context.startActivity(intent)
        }
        if (currentRequest.accepted){
            holder.tvRequest.text = "$senderUserName Is Following You."
            holder.layOutActionRequest.visibility = View.GONE
        }else{
            holder.tvRequest.text = "$senderUserName Wants To Follow You."
            holder.layOutActionRequest.visibility = View.VISIBLE
            holder.btnAcceptRequest.setOnClickListener {
                showProgressDialog()
                FireStoreClass().acceptFollowRequest(currentRequest.receiverUserID,
                    currentRequest.senderUserID) { success ->
                    hideProgressDialog()
                    if (success){
                        Toast.makeText(context, "Accept Follow Request Successful.", Toast.LENGTH_LONG).show()
                        //
                    }else{
                        Toast.makeText(context, "Error While Accepting Follow Request.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            holder.btnRejectRequest.setOnClickListener {
                showProgressDialog()
                FireStoreClass().deleteFollowRequest(currentRequest.senderUserID,
                    currentRequest.receiverUserID) { successful ->
                    hideProgressDialog()
                    if (successful){
                        Toast.makeText(context, "Delete Follow Request Successful.", Toast.LENGTH_LONG).show()
                        //
                    }else{
                        Toast.makeText(context, "Error While Canceling Follow Request.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    class RequestViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val cardView: CardView = itemView.findViewById(R.id.card_view_request_item)
        //val layoutUserInfo: LinearLayout = itemView.findViewById(R.id.layout_user_info)
        val profilePic: ImageView = itemView.findViewById(R.id.iv_profile_pic_request)
        val tvRequest: TextView = itemView.findViewById(R.id.tv_request)
        val layOutActionRequest: LinearLayout = itemView.findViewById(R.id.layout_action_request)
        val btnAcceptRequest: Button = itemView.findViewById(R.id.btn_accept_request)
        val btnRejectRequest: Button = itemView.findViewById(R.id.btn_reject_request)
    }

}