package com.example.priceless

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RecyclerviewAdapter(val context: Context, private val postList: ArrayList<PostStructure>):
    RecyclerView.Adapter<RecyclerviewAdapter.ExampleViewHolder>() {

    private val currentUserID = FireStoreClass().getUserID()

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

        holder.layoutPostContent.visibility = View.GONE
        holder.postImage.visibility = View.GONE
        holder.layoutBuyPost.visibility = View.GONE
        holder.tvPricelessPost.visibility = View.GONE
        holder.tvPriceless.visibility = View.GONE
        holder.tvPrice.visibility = View.GONE
        holder.viewMore.visibility = View.GONE
        holder.tvDeleteInvisiblePost.visibility = View.GONE
        holder.tvLikes.visibility = View.GONE

        if (currentUserID.isNotEmpty()){
            if (currentItemPost.visibility){
                holder.layoutBuyPost.visibility = View.GONE
                holder.tvPricelessPost.visibility = View.GONE
                holder.layoutPostContent.visibility = View.VISIBLE
                if (currentItemPost.visibility && currentItemPost.postImage.isNotEmpty()){
                    holder.postImage.visibility = View.VISIBLE
                    GlideLoader(context).loadImageUri(currentItemPost.postImage, holder.postImage)
                    holder.postImage.setOnClickListener {
                        val intent = Intent(context, FullScreenPostImageActivity::class.java)
                        intent.putExtra("post_image", currentItemPost.postImage)
                        context.startActivity(intent)
                    }
                }else{
                    holder.postImage.visibility = View.GONE
                    holder.postImage.setImageResource(R.drawable.white_image_background)
                }
                holder.postText.text = currentItemPost.postText
                if (currentItemPost.visibility && currentItemPost.timeTraveler
                    && currentItemPost.price.isNotEmpty()){
                    holder.tvPrice.visibility = View.VISIBLE
                    holder.tvPrice.text = "${currentItemPost.price} TON, Not Sold"
                }else{
                    holder.tvPrice.visibility = View.GONE
                }
                if (currentItemPost.visibility && currentUserID == currentItemPost.userId){
                    holder.viewMore.visibility = View.VISIBLE
                    holder.viewMore.setOnClickListener {
                        val intent = Intent(context, EditPostActivity::class.java)
                        intent.putExtra("entire_post", currentItemPost)
                        context.startActivity(intent)
                    }
                }else{
                    holder.viewMore.visibility = View.GONE
                }
                if (currentItemPost.visibility && currentItemPost.timeTraveler && currentItemPost.price.isEmpty()){
                    holder.tvPriceless.visibility = View.VISIBLE
                }else{
                    holder.tvPriceless.visibility = View.GONE
                }
            }else{
                holder.layoutPostContent.visibility = View.GONE
                if (currentItemPost.price.isNotEmpty()){
                    holder.layoutBuyPost.visibility = View.VISIBLE
                    holder.tvPriceToBuy.text = "${currentItemPost.price} TON"
                    holder.btnGoToBuy.setOnClickListener {
                        val intent = Intent(context, BuyPostActivity::class.java)
                        intent.putExtra("post_ID_and_user_ID", Pair(currentItemPost.postID, currentItemPost.userId))
                        context.startActivity(intent)
                    }
                    val millis = currentItemPost.timeToShare.toLong()*1000
                    val timeToShareToShow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(millis))
                    holder.tvTimeToShare.text = "Or It Will Be Visible At: $timeToShareToShow"
                }else{
                    holder.tvPricelessPost.visibility = View.VISIBLE
                }
            }

            if (currentUserID == currentItemPost.userId && !currentItemPost.visibility){
                holder.tvDeleteInvisiblePost.visibility = View.VISIBLE
                holder.tvDeleteInvisiblePost.setOnClickListener {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("DELETE POST?")
                    builder.setMessage("post will be deleted permanently!")
                    builder.setIcon(R.drawable.ic_round_warning_24)
                    builder.setPositiveButton("Yes") { dialog, _ ->
                        showProgressDialog()
                        FireStoreClass().deletePostOnFireStoreWithCallback(currentItemPost.userId,
                            currentItemPost.postID) { onComplete ->
                            hideProgressDialog()
                            if (onComplete){
                                if (currentItemPost.postImage.isNotEmpty()){
                                    FireStoreClass().deleteImageFromCloudStorage(currentItemPost.postImage) { ok ->
                                        if (ok){
                                            val itemPosition = postList.indexOf(currentItemPost)
                                            if (itemPosition != -1) {
                                                postList.removeAt(itemPosition)
                                                notifyItemRemoved(itemPosition)
                                            }
                                            Toast.makeText(context, "Post Deleted.", Toast.LENGTH_LONG).show()
                                        }else{
                                            Toast.makeText(context, "Error While Deleting Post.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }else{
                                    val itemPosition = postList.indexOf(currentItemPost)
                                    if (itemPosition != -1) {
                                        postList.removeAt(itemPosition)
                                        notifyItemRemoved(itemPosition)
                                    }
                                    Toast.makeText(context, "Post Deleted.", Toast.LENGTH_LONG).show()
                                }
                            }else{
                                Toast.makeText(context, "Error While Deleting Post.", Toast.LENGTH_LONG).show()
                            }
                        }
                        dialog.dismiss()
                    }
                    builder.setNeutralButton("Cancel") {dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.setCancelable(false)
                    builder.create().show()
                }
            }else{
                holder.tvDeleteInvisiblePost.visibility = View.GONE
            }
            if (currentItemPost.profilePicture.isNotEmpty()){
                GlideLoader(context).loadImageUri(currentItemPost.profilePicture, holder.profilePic)
            }else{
                holder.profilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
            }

            if (currentItemPost.timeCreatedToShow.isNotEmpty()){
                holder.timeCreatedText.text = "created at ${currentItemPost.timeCreatedToShow}"
            }else{
                holder.timeCreatedText.text = "err getting time online"
            }

            holder.profilePic.setOnClickListener {
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("userID", currentItemPost.userId)
                context.startActivity(intent)
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
        val tvPrice: TextView = itemView.findViewById(R.id.tv_price)
        val layoutPostContent: LinearLayout = itemView.findViewById(R.id.layout_post_content)
        val layoutBuyPost: LinearLayout = itemView.findViewById(R.id.layout_buy_post)
        val tvPriceToBuy: TextView = itemView.findViewById(R.id.tv_price_to_buy)
        val btnGoToBuy: Button = itemView.findViewById(R.id.btn_go_to_buy)
        val tvTimeToShare: TextView = itemView.findViewById(R.id.tv_time_to_share)
        val tvDeleteInvisiblePost: TextView = itemView.findViewById(R.id.tv_delete_invisible_post)
        val tvPricelessPost: TextView = itemView.findViewById(R.id.tv_priceless_post)
    }

}