package com.example.priceless

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class SoldPostsRvAdapter(val context: Context, private val postList: ArrayList<PostStructure>,
                         private val isSoldSituation: Boolean):
    RecyclerView.Adapter<SoldPostsRvAdapter.SoldPostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoldPostViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.sold_post_item, parent, false)
        return SoldPostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SoldPostViewHolder, position: Int) {
        val post = postList[position]

        holder.postImage.visibility = View.GONE
        holder.buyerUserName.visibility = View.GONE

        if (post.profilePicture.isNotEmpty()){
            GlideLoader(context).loadImageUri(post.profilePicture, holder.profilePic)
        }else{
            holder.profilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
        }
        holder.profilePic.setOnClickListener {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("userID", post.userId)
            context.startActivity(intent)
        }
        holder.userName.text = post.userName
        holder.postText.text = post.postText
        if (post.postImage.isNotEmpty()){
            holder.postImage.visibility = View.VISIBLE
            GlideLoader(context).loadImageUri(post.postImage, holder.postImage)
            holder.postImage.setOnClickListener {
                val intent = Intent(context, FullScreenPostImageActivity::class.java)
                intent.putExtra("post_image", post.postImage)
                context.startActivity(intent)
            }
        }else{
            holder.postImage.setImageResource(R.drawable.white_image_background)
            holder.postImage.visibility = View.GONE
        }
        holder.timeCreated.text = "created at ${post.timeCreatedToShow}"
        holder.price.text = "${post.price} TON"
        if (isSoldSituation){
            holder.buyerUserName.visibility = View.VISIBLE
            holder.buyerUserName.text = "sold to: ${post.buyerID}"
        }else{
            holder.buyerUserName.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class SoldPostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val profilePic: ImageView = itemView.findViewById(R.id.iv_profile_pic_sold_post_item)
        val userName: TextView = itemView.findViewById(R.id.tv_user_name_sold_post_item)
        val postText: TextView = itemView.findViewById(R.id.tv_post_content_sold_post_item)
        val postImage: ImageView = itemView.findViewById(R.id.iv_post_image_sold_post_item)
        val timeCreated: TextView = itemView.findViewById(R.id.tv_time_created_sold_post_item)
        val price: TextView = itemView.findViewById(R.id.tv_price_sold_post_item)
        val buyerUserName: TextView = itemView.findViewById(R.id.tv_buyer_sold_post_item)
    }

}