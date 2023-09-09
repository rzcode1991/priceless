package com.example.priceless

import android.content.Context
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

class RecyclerviewAdapter(val context: Context, private val postList: ArrayList<PostStructure>):
    RecyclerView.Adapter<RecyclerviewAdapter.ExampleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return ExampleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        val currentItemPost = postList[position]
        //holder.profilePictureForPost.setImageResource(currentItem.profilePicture)
        GlideLoader(context).loadImageUri(currentItemPost.profilePicture, holder.profilePictureForPost)
        holder.postText.text = currentItemPost.postText
        holder.timeCreatedText.text = currentItemPost.timeCreated
        holder.profilePictureForPost.setOnClickListener {
            Toast.makeText(context, "you clicked on profile pic", Toast.LENGTH_SHORT).show()
        }
        holder.userName.text = currentItemPost.userName

        //if(context is FragmentActivity){
        //    context.callAFunction(currentItem)
        //}
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class ExampleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val profilePictureForPost: ImageView = itemView.findViewById(R.id.iv_profile_post_item)
        val postText: TextView = itemView.findViewById(R.id.tv_post_content_post_item)
        val timeCreatedText: TextView = itemView.findViewById(R.id.tv_time_created_post_item)
        val userName: TextView = itemView.findViewById(R.id.tv_user_name)
    }

}