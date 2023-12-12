package com.example.priceless

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.*
import androidx.cardview.widget.CardView
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class BuyPostActivity : BaseActivity(), OnClickListener {

    private lateinit var cvPostInfo: CardView
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvAtWillBeVisible: TextView
    private lateinit var tvTimeCreated: TextView
    private lateinit var layoutTransaction: LinearLayout
    private lateinit var tvTellerPrice: TextView
    private lateinit var tvPostOwnerWalletAddress: TextView
    private lateinit var tvGoToTonKeeper: TextView
    private lateinit var btnConfirmTransaction: Button
    private lateinit var tvNotSetWalletBuyer: TextView
    private lateinit var tvPostNotExist: TextView
    private var postID = ""
    private var postOwnerID = ""
    private var post: PostStructure? = null
    private var postOwnerUser: User? = null
    private var currentUserID = ""
    private var currentUser: User? = null
    private var transactionSource = ""
    private var transactionDestination = ""
    private var transactionValue = ""
    private var transactionHash = ""
    private lateinit var progressBar: ProgressBar
    private var fireStoreClass: FireStoreClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_post)

        cvPostInfo = findViewById(R.id.cv_post_info_buy)
        ivProfilePic = findViewById(R.id.iv_profile_pic_buy)
        tvUserName = findViewById(R.id.tv_user_name_buy)
        tvPrice = findViewById(R.id.tv_price_buy)
        tvAtWillBeVisible = findViewById(R.id.tv_at_will_be_visible_buy)
        tvTimeCreated = findViewById(R.id.tv_time_created_buy)
        layoutTransaction = findViewById(R.id.layout_transaction_buy)
        tvTellerPrice = findViewById(R.id.tv_teller_price_buy)
        tvPostOwnerWalletAddress = findViewById(R.id.tv_post_owner_wallet_address_buy)
        tvGoToTonKeeper = findViewById(R.id.tv_go_to_ton_keeper_buy)
        btnConfirmTransaction = findViewById(R.id.btn_confirm_transaction_buy)
        tvNotSetWalletBuyer = findViewById(R.id.tv_not_set_wallet_buyer_buy)
        tvPostNotExist = findViewById(R.id.tv_post_not_exist_buy)
        progressBar = findViewById(R.id.pb_check_transaction)

        cvPostInfo.visibility = View.GONE
        layoutTransaction.visibility = View.GONE
        tvNotSetWalletBuyer.visibility = View.GONE
        tvPostNotExist.visibility = View.GONE

        if (intent.hasExtra("post_ID_and_user_ID")){
            val postIDAndUserIDPair = intent.getSerializableExtra("post_ID_and_user_ID") as Pair<*, *>?
            if (postIDAndUserIDPair != null) {
                postID = postIDAndUserIDPair.first as String
                postOwnerID = postIDAndUserIDPair.second as String
            }
        }

        tvPostOwnerWalletAddress.setOnClickListener(this@BuyPostActivity)
        tvGoToTonKeeper.setOnClickListener(this@BuyPostActivity)
        btnConfirmTransaction.setOnClickListener(this@BuyPostActivity)
        tvNotSetWalletBuyer.setOnClickListener(this@BuyPostActivity)

        getPostInfo()

    }


    private fun getPostInfo(){
        fireStoreClass = FireStoreClass()
        fireStoreClass?.listenForSinglePostChanges(postOwnerID, postID) { postInfo ->
            if (postInfo != null && postInfo.buyerID.isEmpty()){
                post = postInfo
                cvPostInfo.visibility = VISIBLE
                setPostInfo()
            }else{
                cvPostInfo.visibility = View.GONE
                layoutTransaction.visibility = View.GONE
                tvNotSetWalletBuyer.visibility = View.GONE
                tvPostNotExist.visibility = VISIBLE
                //postOwnerUser = null
            }
        }
    }

    private fun setPostInfo(){
        tvPrice.text = "${post!!.price} TON"
        val millis = post!!.timeToShare.toLong()*1000
        val timeToShareToShow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(millis))
        tvAtWillBeVisible.text = "This Post Will Be Visible At: $timeToShareToShow"
        tvTimeCreated.text = "created at ${post!!.timeCreatedToShow}"
        CoroutineScope(Dispatchers.Main).launch {
            val postOwnerJob = async {
                val deferredPostOwner = CompletableDeferred<User?>()
                FireStoreClass().getUserInfoWithCallback(postOwnerID) { postOwnerInfo ->
                    deferredPostOwner.complete(postOwnerInfo)
                }
                postOwnerUser = deferredPostOwner.await()
            }
            postOwnerJob.await()
            if (postOwnerUser != null){
                if (postOwnerUser!!.image.isNotEmpty()){
                    GlideLoader(this@BuyPostActivity).loadImageUri(postOwnerUser!!.image, ivProfilePic)
                }else{
                    ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
                }
                tvUserName.text = postOwnerUser!!.userName
                getCurrentUserInfo()
            }else{
                showErrorSnackBar("Error Getting Post Owner Info", true)
            }
        }
    }

    private fun getCurrentUserInfo(){
        CoroutineScope(Dispatchers.Main).launch {
            val currentUserIDJob = async {
                val deferredCurrentUserID = CompletableDeferred<String>()
                FireStoreClass().getCurrentUserID { currentUID ->
                    deferredCurrentUserID.complete(currentUID)
                }
                currentUserID = deferredCurrentUserID.await()
            }
            currentUserIDJob.await()

            if (currentUserID.isNotEmpty()){
                val currentUserInfoJob = async {
                    val deferredCurrentUserInfo = CompletableDeferred<User?>()
                    FireStoreClass().getUserInfoWithCallback(currentUserID) { currentUserInfo ->
                        deferredCurrentUserInfo.complete(currentUserInfo)
                    }
                    currentUser = deferredCurrentUserInfo.await()
                }
                currentUserInfoJob.await()
                if (currentUser != null){
                    setLayoutTransaction()
                }else{
                    showErrorSnackBar("Error Getting Current User Info", true)
                }
            }else{
                showErrorSnackBar("Error Getting Current User ID", true)
            }
        }
    }

    private fun setLayoutTransaction(){
        if (currentUser!!.wallet.isNotEmpty()){
            layoutTransaction.visibility = VISIBLE
            tvNotSetWalletBuyer.visibility = View.GONE
            tvTellerPrice.text = "To Buy This Post, Send ${post!!.price} TON to This Wallet Address:"
            tvPostOwnerWalletAddress.text = postOwnerUser!!.wallet
        }else{
            layoutTransaction.visibility = View.GONE
            tvNotSetWalletBuyer.visibility = VISIBLE
        }
    }

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.tv_post_owner_wallet_address_buy -> {
                    if (layoutTransaction.visibility == VISIBLE){
                        val textToCopy: CharSequence = tvPostOwnerWalletAddress.text
                        copyToClipboard(textToCopy)
                        Toast.makeText(this, "Wallet Address Copied to Clipboard.", Toast.LENGTH_LONG).show()
                    }
                }
                R.id.tv_go_to_ton_keeper_buy -> {
                    if (layoutTransaction.visibility == VISIBLE){
                        val packageName = "com.ton_keeper"
                        if (isAppInstalled()) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                            startActivity(launchIntent)
                        } else {
                            redirectToPlayStore()
                        }
                    }
                }
                R.id.btn_confirm_transaction_buy -> {
                    if (layoutTransaction.visibility == VISIBLE && btnConfirmTransaction.visibility == VISIBLE){
                        progressBar.visibility = VISIBLE
                        btnConfirmTransaction.visibility = View.GONE
                        CoroutineScope(Dispatchers.Main).launch {
                            val transactionJob = async { getTransaction() }
                            transactionJob.await()
                            if (transactionSource.isNotEmpty() && transactionDestination.isNotEmpty()
                                && transactionValue.isNotEmpty() && transactionHash.isNotEmpty()){
                                checkTransaction()
                            }else{
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    btnConfirmTransaction.visibility = VISIBLE
                                    showErrorSnackBar("Invalid Transaction or Network Error.", true)
                                }
                            }
                        }
                    }
                }
                R.id.tv_not_set_wallet_buyer_buy -> {
                    val intent = Intent(this@BuyPostActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun transactionOK(){
        if (post != null){
            post!!.buyerID = currentUserID

            val postHashMap = HashMap<String, Any>()
            postHashMap["buyerID"] = currentUserID
            FireStoreClass().updatePostOnFireStore(postOwnerID, postHashMap, postID) { onSuccess ->
                if (onSuccess){
                    FireStoreClass().addPostToBoughtPosts(currentUserID, post!!) { success ->
                        if (success){
                            FireStoreClass().saveTransaction(transactionHash) { onComplete ->
                                if (onComplete){
                                    FireStoreClass().addPostToSoldPosts(postOwnerID, post!!) { yep ->
                                        if (yep){
                                            progressBar.visibility = View.GONE
                                            Toast.makeText(this, "You Bought This Post " +
                                                    "Successfully.", Toast.LENGTH_LONG).show()
                                            val intent = Intent(this@BuyPostActivity,
                                                BoughtPostsActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }else{
                                            progressBar.visibility = View.GONE
                                            btnConfirmTransaction.visibility = VISIBLE
                                            showErrorSnackBar("error network operation 4", true)
                                        }
                                    }
                                }else{
                                    progressBar.visibility = View.GONE
                                    btnConfirmTransaction.visibility = VISIBLE
                                    showErrorSnackBar("error network operation 3", true)
                                }
                            }
                        }else{
                            progressBar.visibility = View.GONE
                            btnConfirmTransaction.visibility = VISIBLE
                            showErrorSnackBar("error network operation 2", true)
                        }
                    }
                }else{
                    progressBar.visibility = View.GONE
                    btnConfirmTransaction.visibility = VISIBLE
                    showErrorSnackBar("error network operation 1", true)
                }
            }
        }
    }

    private fun checkTransaction(){
        FireStoreClass().transactionExists(transactionHash) { exists ->
            if (exists){
                progressBar.visibility = View.GONE
                btnConfirmTransaction.visibility = VISIBLE
                showErrorSnackBar("You Have Used This Transaction Before.", true)
            }else{
                val amountOfTransactionInTon = BigDecimal(transactionValue).divide(BigDecimal("1000000000"))
                val postPriceInBigDecimal = BigDecimal(post?.price)
                if (transactionSource == currentUser?.wallet &&
                    transactionDestination == postOwnerUser?.wallet &&
                    amountOfTransactionInTon == postPriceInBigDecimal){
                    transactionOK()
                }else{
                    progressBar.visibility = View.GONE
                    btnConfirmTransaction.visibility = VISIBLE
                    showErrorSnackBar("Invalid Transaction", true)
                }
            }
        }
    }

    private suspend fun getTransaction(){
        val result = TonCenter().makeHttpRequest(currentUser!!.wallet)
        if (result.isSuccess) {
            val transaction = result.getOrNull()
            if (transaction != null) {
                transactionSource = transaction.source
                transactionDestination = transaction.destination
                transactionValue = transaction.value
                transactionHash = transaction.bodyHash
            }
        } else {
            val exception = result.exceptionOrNull()
            if (exception != null) {
                Log.e("Error getting transaction", exception.message.toString(), exception)
            }
        }

    }

    private fun isAppInstalled(): Boolean {
        val packageManager = packageManager
        return try {
            packageManager.getPackageInfo("com.ton_keeper", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun redirectToPlayStore() {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.ton_keeper")
            )
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.ton_keeper")
            )
            startActivity(intent)
        }
    }

    private fun copyToClipboard(text: CharSequence) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("wallet", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onDestroy() {
        super.onDestroy()
        fireStoreClass?.stopPostListener()
    }

}