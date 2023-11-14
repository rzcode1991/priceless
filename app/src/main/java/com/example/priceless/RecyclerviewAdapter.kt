package com.example.priceless

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.priceless.PostStructure
import com.example.priceless.R
import com.example.priceless.ui.home.HomeFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RecyclerviewAdapter(val context: Context, private val postList: ArrayList<PostStructure>,
                          private val currentUserID: String):
    RecyclerView.Adapter<RecyclerviewAdapter.ExampleViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return ExampleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        val currentItemPost = postList[position]
        if (currentItemPost.profilePicture.isNotEmpty()){
            GlideLoader(context).loadImageUri(currentItemPost.profilePicture, holder.profilePic)
        }else{
            holder.profilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        if (currentItemPost.postImage.isNotEmpty()){
            holder.postImage.visibility = View.VISIBLE
            GlideLoader(context).loadImageUri(currentItemPost.postImage, holder.postImage)
            holder.postImage.setOnClickListener {
                val intent = Intent(context, FullScreenPostImageActivity::class.java)
                intent.putExtra("post_image", currentItemPost.postImage)
                context.startActivity(intent)
            }
        }else{
            holder.postImage.visibility = View.GONE
            //holder.postText.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.postImage.setImageResource(R.drawable.white_image_background)
        }
        holder.postText.text = currentItemPost.postText
        if (currentItemPost.timeCreatedToShow.isNotEmpty()){
            holder.timeCreatedText.text = "created at ${currentItemPost.timeCreatedToShow}"
        }else{
            holder.timeCreatedText.text = "err getting time online"
        }
        if (currentItemPost.timeTraveler){
            holder.tvPriceless.visibility = View.VISIBLE
        }else{
            holder.tvPriceless.visibility = View.GONE
        }
        holder.profilePic.setOnClickListener {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("userID", currentItemPost.userId)
            context.startActivity(intent)
        }
        if (currentUserID == currentItemPost.userId){
            holder.viewMore.visibility = View.VISIBLE
            holder.viewMore.setOnClickListener {
                val intent = Intent(context, EditPostActivity::class.java)
                intent.putExtra("entire_post", currentItemPost)
                context.startActivity(intent)
            }
        }else{
            holder.viewMore.visibility = View.GONE
        }
        holder.userName.text = currentItemPost.userName

        FireStoreClass().getNumberOfLikesForPost(currentItemPost.postID, currentItemPost.userId) { number ->
            if (number != null && number != 0){
                holder.tvLikes.visibility = View.VISIBLE
                holder.tvLikes.text = "$number"
            }else{
                holder.tvLikes.visibility = View.GONE
            }
        }

        var likeSituation = false

        FireStoreClass().getLikeSituationForPost(currentUserID, currentItemPost.postID,
            currentItemPost.userId) { yep ->
            if (yep){
                likeSituation = true
                holder.ibLike.setImageResource(R.drawable.thumb_liked)
            }else{
                likeSituation = false
                holder.ibLike.setImageResource(R.drawable.thumb_like)
            }
        }

        holder.ibLike.setOnClickListener {
            if (likeSituation){
                FireStoreClass().unLikePost(currentUserID, currentItemPost.postID,
                    currentItemPost.userId) { onSuccess ->
                    if (onSuccess){
                        likeSituation = false
                        holder.ibLike.setImageResource(R.drawable.thumb_like)
                        FireStoreClass().getNumberOfLikesForPost(currentItemPost.postID, currentItemPost.userId) { number ->
                            if (number != null && number != 0){
                                holder.tvLikes.visibility = View.VISIBLE
                                holder.tvLikes.text = "$number"
                            }else{
                                holder.tvLikes.visibility = View.GONE
                            }
                        }
                    }
                }
            }else{
                FireStoreClass().likePost(currentUserID, currentItemPost.postID,
                    currentItemPost.userId) { success ->
                    if (success){
                        likeSituation = true
                        holder.ibLike.setImageResource(R.drawable.thumb_liked)
                        FireStoreClass().getNumberOfLikesForPost(currentItemPost.postID, currentItemPost.userId) { number ->
                            if (number != null && number != 0){
                                holder.tvLikes.visibility = View.VISIBLE
                                holder.tvLikes.text = "$number"
                            }else{
                                holder.tvLikes.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }

        holder.tvGoToComments.setOnClickListener {
            val intent = Intent(context, CommentsActivity::class.java)
            intent.putExtra("post", currentItemPost)
            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class ExampleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val profilePic: ImageView = itemView.findViewById(R.id.iv_profile_post_item)
        val userName: TextView = itemView.findViewById(R.id.tv_user_name_post_item)
        val postText: TextView = itemView.findViewById(R.id.tv_post_content_post_item)
        val postImage: ImageView = itemView.findViewById(R.id.iv_post_image_post_item)
        val timeCreatedText: TextView = itemView.findViewById(R.id.tv_time_created_post_item)
        val tvPriceless: TextView = itemView.findViewById(R.id.tv_priceless)
        val viewMore: ImageView = itemView.findViewById(R.id.iv_tap_for_more_post_item)
        val tvLikes: TextView = itemView.findViewById(R.id.tv_likes)
        val ibLike: ImageButton = itemView.findViewById(R.id.ib_like)
        val tvGoToComments: TextView = itemView.findViewById(R.id.tv_goto_comments)
    }

}