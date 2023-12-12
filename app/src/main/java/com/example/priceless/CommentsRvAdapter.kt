package com.example.priceless

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class CommentsRvAdapter(val context: Context,
                        private val commentsList: ArrayList<CommentStructure>):
    RecyclerView.Adapter<CommentsRvAdapter.CommentViewHolder>(){

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]

        holder.ivCommentPhoto.visibility = View.GONE
        holder.tvPrivateComment.visibility = View.GONE
        holder.tvDeleteComment.visibility = View.GONE
        holder.tvEditComment.visibility = View.GONE
        holder.tvReply.visibility = View.GONE
        holder.tvNumberOfLikes.visibility = View.GONE

        if (currentUserID.isNotEmpty()){
            if (comment.writerProfilePic.isNotEmpty()){
                GlideLoader(context).loadImageUri(comment.writerProfilePic, holder.ivProfilePic)
            }else{
                holder.ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
            }
            holder.ivProfilePic.setOnClickListener {
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("userID", comment.writerUID)
                context.startActivity(intent)
            }
            holder.tvUserName.text = comment.writerUserName
            holder.tvCommentText.text = comment.text
            if (comment.commentPhoto.isNotEmpty()){
                holder.ivCommentPhoto.visibility = View.VISIBLE
                GlideLoader(context).loadImageUri(comment.commentPhoto, holder.ivCommentPhoto)
                holder.ivCommentPhoto.setOnClickListener {
                    val intent = Intent(context, FullScreenPostImageActivity::class.java)
                    intent.putExtra("post_image", comment.commentPhoto)
                    context.startActivity(intent)
                }
            }else{
                holder.ivCommentPhoto.visibility = View.GONE
            }
            holder.tvTimeCreated.text = comment.timeCreatedToShow

            if (comment.isPrivate){
                holder.tvPrivateComment.visibility = View.VISIBLE
                if (comment.topCommentIDForReply.isNotEmpty()){
                    holder.tvPrivateComment.text = "Private Reply"
                    FireStoreClass().getNumberOfLikesForPrivateReply(comment) { number ->
                        if (number != null && number != 0){
                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                            holder.tvNumberOfLikes.text = "$number"
                        }else{
                            holder.tvNumberOfLikes.visibility = View.GONE
                        }
                    }

                    var likeSituationForPrivateReply = false

                    FireStoreClass().getLikeSituationForPrivateReply(comment, currentUserID) { yep ->
                        if (yep){
                            likeSituationForPrivateReply = true
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                        }else{
                            likeSituationForPrivateReply = false
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                        }
                    }

                    holder.ibLikeComment.setOnClickListener {
                        if (likeSituationForPrivateReply){
                            FireStoreClass().unlikePrivateReply(comment, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    likeSituationForPrivateReply = false
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                                    FireStoreClass().getNumberOfLikesForPrivateReply(comment) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }else{
                            FireStoreClass().likePrivateReply(comment, currentUserID) { success ->
                                if (success){
                                    likeSituationForPrivateReply = true
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                                    FireStoreClass().getNumberOfLikesForPrivateReply(comment) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //
                }else{
                    FireStoreClass().getNumberOfLikesForPrivateComment(comment.postOwnerUID, comment.postID,
                        comment.commentID, comment.writerUID) { number ->
                        if (number != null && number != 0){
                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                            holder.tvNumberOfLikes.text = "$number"
                        }else{
                            holder.tvNumberOfLikes.visibility = View.GONE
                        }
                    }

                    var likeSituationForPrivateComment = false

                    FireStoreClass().getLikeSituationForPrivateComment(comment.postOwnerUID, comment.postID,
                        comment.commentID, comment.writerUID, currentUserID) { yep ->
                        if (yep){
                            likeSituationForPrivateComment = true
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                        }else{
                            likeSituationForPrivateComment = false
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                        }
                    }

                    holder.ibLikeComment.setOnClickListener {
                        if (likeSituationForPrivateComment){
                            FireStoreClass().unLikePrivateComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, comment.writerUID, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    likeSituationForPrivateComment = false
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                                    FireStoreClass().getNumberOfLikesForPrivateComment(comment.postOwnerUID, comment.postID,
                                        comment.commentID, comment.writerUID) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }else{
                            FireStoreClass().likePrivateComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, comment.writerUID, currentUserID) { success ->
                                if (success){
                                    likeSituationForPrivateComment = true
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                                    FireStoreClass().getNumberOfLikesForPrivateComment(comment.postOwnerUID, comment.postID,
                                        comment.commentID, comment.writerUID) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }else{
                holder.tvPrivateComment.visibility = View.GONE
                if (comment.topCommentIDForReply.isNotEmpty()){
                    FireStoreClass().getNumberOfLikesForPublicReply(comment) { number ->
                        if (number != null && number != 0){
                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                            holder.tvNumberOfLikes.text = "$number"
                        }else{
                            holder.tvNumberOfLikes.visibility = View.GONE
                        }
                    }

                    var likeSituationForPublicReply = false

                    FireStoreClass().getLikeSituationForPublicReply(comment, currentUserID) { yep ->
                        if (yep){
                            likeSituationForPublicReply = true
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                        }else{
                            likeSituationForPublicReply = false
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                        }
                    }

                    holder.ibLikeComment.setOnClickListener {
                        if (likeSituationForPublicReply){
                            FireStoreClass().unlikePublicReply(comment, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    likeSituationForPublicReply = false
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                                    FireStoreClass().getNumberOfLikesForPublicReply(comment) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }else{
                            FireStoreClass().likePublicReply(comment, currentUserID) { success ->
                                if (success){
                                    likeSituationForPublicReply = true
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                                    FireStoreClass().getNumberOfLikesForPublicReply(comment) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //
                }else{
                    FireStoreClass().getNumberOfLikesForPublicComment(comment.postOwnerUID, comment.postID,
                        comment.commentID) { number ->
                        if (number != null && number != 0){
                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                            holder.tvNumberOfLikes.text = "$number"
                        }else{
                            holder.tvNumberOfLikes.visibility = View.GONE
                        }
                    }

                    var likeSituationForPublicComment = false

                    FireStoreClass().getLikeSituationForPublicComment(comment.postOwnerUID, comment.postID,
                        comment.commentID, currentUserID) { yep ->
                        if (yep){
                            likeSituationForPublicComment = true
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                        }else{
                            likeSituationForPublicComment = false
                            holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                        }
                    }

                    holder.ibLikeComment.setOnClickListener {
                        if (likeSituationForPublicComment){
                            FireStoreClass().unLikePublicComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, currentUserID) { onSuccess ->
                                if (onSuccess){
                                    likeSituationForPublicComment = false
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_like)
                                    FireStoreClass().getNumberOfLikesForPublicComment(comment.postOwnerUID,
                                        comment.postID, comment.commentID) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }else{
                            FireStoreClass().likePublicComment(comment.postOwnerUID, comment.postID,
                                comment.commentID, currentUserID) { success ->
                                if (success){
                                    likeSituationForPublicComment = true
                                    holder.ibLikeComment.setImageResource(R.drawable.thumb_liked)
                                    FireStoreClass().getNumberOfLikesForPublicComment(comment.postOwnerUID,
                                        comment.postID, comment.commentID) { number ->
                                        if (number != null && number != 0){
                                            holder.tvNumberOfLikes.visibility = View.VISIBLE
                                            holder.tvNumberOfLikes.text = "$number"
                                        }else{
                                            holder.tvNumberOfLikes.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentUserID == comment.writerUID || currentUserID == comment.postOwnerUID){
                holder.tvDeleteComment.visibility = View.VISIBLE
                holder.tvDeleteComment.setOnClickListener {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("DELETE COMMENT?")
                    builder.setMessage("comment will be deleted permanently!")
                    builder.setIcon(R.drawable.ic_round_warning_24)
                    builder.setPositiveButton("Yes") { dialog, _ ->
                        showProgressDialog()
                        if (comment.isPrivate){
                            if (comment.topCommentIDForReply.isNotEmpty()){
                                FireStoreClass().deletePrivateReply(comment) { success ->
                                    dialog.dismiss()
                                    hideProgressDialog()
                                    if (success){
                                        if (comment.commentPhoto.isNotEmpty()){
                                            FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { ok ->
                                                if (ok){
                                                    val itemPosition = commentsList.indexOf(comment)
                                                    if (itemPosition != -1) {
                                                        commentsList.removeAt(itemPosition)
                                                        notifyItemRemoved(itemPosition)
                                                    }
                                                    Toast.makeText(context, "Reply Deleted.", Toast.LENGTH_SHORT).show()
                                                }else{
                                                    Toast.makeText(context, "Error While Deleting Reply.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }else{
                                            val itemPosition = commentsList.indexOf(comment)
                                            if (itemPosition != -1) {
                                                commentsList.removeAt(itemPosition)
                                                notifyItemRemoved(itemPosition)
                                            }
                                            Toast.makeText(context, "Reply Deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }else{
                                        Toast.makeText(context, "Error While Deleting Reply.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                //
                            }else{
                                FireStoreClass().deletePrivateComment(comment.postOwnerUID, comment.postID,
                                    comment.writerUID, comment.commentID) { success ->
                                    dialog.dismiss()
                                    hideProgressDialog()
                                    if (success){
                                        if (comment.commentPhoto.isNotEmpty()){
                                            FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yep ->
                                                if (yep){
                                                    val itemPosition = commentsList.indexOf(comment)
                                                    if (itemPosition != -1) {
                                                        commentsList.removeAt(itemPosition)
                                                        notifyItemRemoved(itemPosition)
                                                    }
                                                    Toast.makeText(context, "Comment Deleted.", Toast.LENGTH_SHORT).show()
                                                }else{
                                                    Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }else{
                                            val itemPosition = commentsList.indexOf(comment)
                                            if (itemPosition != -1) {
                                                commentsList.removeAt(itemPosition)
                                                notifyItemRemoved(itemPosition)
                                            }
                                            Toast.makeText(context, "Comment Deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }else{
                                        Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }else{
                            if (comment.topCommentIDForReply.isNotEmpty()){
                                FireStoreClass().deletePublicReply(comment) { onSuccess ->
                                    dialog.dismiss()
                                    hideProgressDialog()
                                    if (onSuccess){
                                        if (comment.commentPhoto.isNotEmpty()){
                                            FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yep ->
                                                if (yep){
                                                    val commentPosition = commentsList.indexOf(comment)
                                                    if (commentPosition != -1) {
                                                        commentsList.removeAt(commentPosition)
                                                        notifyItemRemoved(commentPosition)
                                                    }
                                                    Toast.makeText(context, "Comment Was Deleted.", Toast.LENGTH_SHORT).show()
                                                }else{
                                                    Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }else{
                                            val commentPosition = commentsList.indexOf(comment)
                                            if (commentPosition != -1) {
                                                commentsList.removeAt(commentPosition)
                                                notifyItemRemoved(commentPosition)
                                            }
                                            Toast.makeText(context, "Comment Was Deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }else{
                                        Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                //
                            }else{
                                FireStoreClass().deletePublicComment(comment.writerUID,
                                    comment.postOwnerUID, comment.postID,
                                    comment.commentID) { onSuccess ->
                                    dialog.dismiss()
                                    hideProgressDialog()
                                    if (onSuccess){
                                        if (comment.commentPhoto.isNotEmpty()){
                                            FireStoreClass().deleteImageFromCloudStorage(comment.commentPhoto) { yes ->
                                                if (yes){
                                                    val commentPosition = commentsList.indexOf(comment)
                                                    if (commentPosition != -1) {
                                                        commentsList.removeAt(commentPosition)
                                                        notifyItemRemoved(commentPosition)
                                                    }
                                                    Toast.makeText(context, "Comment Was Deleted.", Toast.LENGTH_SHORT).show()
                                                }else{
                                                    Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }else{
                                            val commentPosition = commentsList.indexOf(comment)
                                            if (commentPosition != -1) {
                                                commentsList.removeAt(commentPosition)
                                                notifyItemRemoved(commentPosition)
                                            }
                                            //commentsList.removeAt(position)
                                            //notifyDataSetChanged()
                                            //holder.cardViewCommentItem.visibility = View.GONE
                                            Toast.makeText(context, "Comment Was Deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }else{
                                        Toast.makeText(context, "Error While Deleting Comment.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    builder.setNeutralButton("Cancel") {dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.setCancelable(false)
                    builder.create().show()
                }
            }else{
                holder.tvDeleteComment.visibility = View.GONE
            }

            if (currentUserID == comment.writerUID){
                holder.tvEditComment.visibility = View.VISIBLE

                holder.tvEditComment.setOnClickListener {
                    val intent = Intent(context, EditCommentActivity::class.java)
                    intent.putExtra("comment", comment)
                    context.startActivity(intent)
                }

            }else{
                holder.tvEditComment.visibility = View.GONE
            }

            if (comment.topCommentIDForReply.isNotEmpty()){
                // we don't want reply for replies
                holder.tvReply.visibility = View.GONE
            }else{
                holder.tvReply.visibility = View.VISIBLE
                holder.tvReply.setOnClickListener {
                    val intent = Intent(context, ReplyCommentActivity::class.java)
                    intent.putExtra("com.example.priceless.comment", comment)
                    context.startActivity(intent)
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return commentsList.size
    }

    class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val ivProfilePic: ImageView = itemView.findViewById(R.id.iv_profile_comment_item)
        val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name_comment_item)
        val tvCommentText: TextView = itemView.findViewById(R.id.tv_comment_text_comment_item)
        val ivCommentPhoto: ImageView = itemView.findViewById(R.id.iv_comment_photo_comment_item)
        val tvTimeCreated: TextView = itemView.findViewById(R.id.tv_time_created_comment_item)
        val tvPrivateComment: TextView = itemView.findViewById(R.id.tv_private_comment_item)
        val tvDeleteComment: TextView = itemView.findViewById(R.id.tv_delete_comment)
        val tvEditComment: TextView = itemView.findViewById(R.id.tv_edit_comment)
        val tvNumberOfLikes: TextView = itemView.findViewById(R.id.tv_number_of_likes_comment_item)
        val ibLikeComment: ImageButton = itemView.findViewById(R.id.ib_like_comment_item)
        val tvReply: TextView = itemView.findViewById(R.id.tv_reply_comment)
        //val cardViewCommentItem: CardView = itemView.findViewById(R.id.card_view_comment_item)
    }

}