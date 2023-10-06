package com.example.priceless

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class RecyclerviewAdapter(val context: Context, private val postList: ArrayList<PostStructure>):
    RecyclerView.Adapter<RecyclerviewAdapter.ExampleViewHolder>() {

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
        // TODO go to a user profile page
        holder.profilePic.setOnClickListener {
            Toast.makeText(context, "you clicked on profile pic", Toast.LENGTH_SHORT).show()
        }
        holder.viewMore.setOnClickListener {
            val intent = Intent(context, EditPostActivity::class.java)
            intent.putExtra("entire_post", currentItemPost)
            context.startActivity(intent)
        }
        holder.userName.text = currentItemPost.userName
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
    }

}