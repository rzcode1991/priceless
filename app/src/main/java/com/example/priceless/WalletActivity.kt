package com.example.priceless

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class WalletActivity : BaseActivity(), OnClickListener {

    private lateinit var ivProfilePic: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var btnDeposit: Button
    private lateinit var btnWithdraw: Button
    private lateinit var layoutAmount: TextInputLayout
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnContinue: Button
    private var isDeposit: Boolean = false
    private var isWithdraw: Boolean = false
    private var currentUserID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        ivProfilePic = findViewById(R.id.iv_profile_pic_wallet)
        tvUserName = findViewById(R.id.tv_user_name_wallet)
        tvBalance = findViewById(R.id.tv_balance_wallet)
        btnDeposit = findViewById(R.id.btn_deposit)
        btnWithdraw = findViewById(R.id.btn_withdraw)
        layoutAmount = findViewById(R.id.Layout_amount_of_deposit_withdraw)
        etAmount = findViewById(R.id.et_amount_of_deposit_withdraw)
        btnContinue = findViewById(R.id.btn_continue)

        if (intent.hasExtra("current_user_id")){
            currentUserID = intent.getStringExtra("current_user_id").toString()
        }

        setButtonBackground(btnWithdraw, android.R.color.white)
        setButtonBackground(btnDeposit, android.R.color.white)

        //setUserInfo()

        btnDeposit.setOnClickListener(this@WalletActivity)
        btnWithdraw.setOnClickListener(this@WalletActivity)
        btnContinue.setOnClickListener(this@WalletActivity)

    }

    /*
    private fun setUserInfo(){
        showProgressDialog()
        FireStoreClass().getUserInfoWithCallback(currentUserID) { userInfo ->
            if (userInfo != null){
                user = userInfo
                hideProgressDialog()
                if (userInfo.image.isNotEmpty()){
                    GlideLoader(this).loadImageUri(userInfo.image, ivProfilePic)
                }else{
                    ivProfilePic.setImageResource(R.drawable.ic_baseline_account_circle_24)
                }
                tvUserName.text = userInfo.userName
                if (userInfo.balance.isNotEmpty()){
                    tvBalance.text = "${userInfo.balance} $"
                }else{
                    tvBalance.text = "0 $"
                }
            }else{
                showErrorSnackBar("Error Getting User Info. Check your Internet Connection", true)
            }
        }
    }
     */

    override fun onClick(v: View?) {
        if (v != null){
            when(v.id){
                R.id.btn_deposit -> {
                    isDeposit = true
                    isWithdraw = false
                    setButtonBackground(btnDeposit, android.R.color.darker_gray)
                    setButtonBackground(btnWithdraw, android.R.color.white)
                    layoutAmount.visibility = VISIBLE
                    etAmount.setText("")
                    btnContinue.visibility = VISIBLE
                }
                /*
                R.id.btn_withdraw -> {
                    if (user.balance.isEmpty()){
                        layoutAmount.visibility = View.GONE
                        etAmount.setText("")
                        btnContinue.visibility = View.GONE
                        setButtonBackground(btnWithdraw, android.R.color.white)
                        setButtonBackground(btnDeposit, android.R.color.white)
                        showErrorSnackBar("Your Balance Is Not Enough.", true)
                    }else{
                        isWithdraw = true
                        isDeposit = false
                        setButtonBackground(btnWithdraw, android.R.color.darker_gray)
                        setButtonBackground(btnDeposit, android.R.color.white)
                        layoutAmount.visibility = VISIBLE
                        etAmount.setText(user.balance)
                        btnContinue.visibility = VISIBLE
                    }
                }
                 */

                /*
                R.id.btn_continue -> {
                    if (btnContinue.visibility == VISIBLE){
                        if (isDeposit && !isWithdraw){
                            if (validateBeforeDeposit()){
                                Toast.makeText(this, "deposit to be continued", Toast.LENGTH_SHORT).show()
                            }
                        }else if (isWithdraw && !isDeposit){
                            if (validateBeforeWithdraw()){
                                Toast.makeText(this, "withdraw to be continued", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                 */
            }
        }
    }

    private fun setButtonBackground(button: Button, color: Int) {
        //val color = ContextCompat.getColor(this, colorResId)
        ViewCompat.setBackgroundTintList(button, ContextCompat.getColorStateList(this, color))
    }

    /*
    private fun validateBeforeDeposit(): Boolean{
        val amountOfDeposit = etAmount.text.toString().trim()
        if (amountOfDeposit.isNotEmpty()){
            if (amountOfDeposit.startsWith(".") || amountOfDeposit.endsWith(".")){
                showErrorSnackBar("Invalid deposit amount format", true)
                return false
            }
            try {
                val amount = BigDecimal(amountOfDeposit)
                if (amount <= BigDecimal.ZERO) {
                    showErrorSnackBar("Deposit amount must be greater than 0", true)
                    return false
                }
            } catch (e: NumberFormatException) {
                showErrorSnackBar("Invalid deposit amount format", true)
                return false
            }
        }else{
            showErrorSnackBar("Please Enter Amount Of Deposit.", true)
            return false
        }
        return true
    }
     */


    /*
    private fun validateBeforeWithdraw(): Boolean{
        val amountOfWithdraw = etAmount.text.toString().trim()
        if (amountOfWithdraw.isNotEmpty()){
            if (amountOfWithdraw.startsWith(".") || amountOfWithdraw.endsWith(".")){
                showErrorSnackBar("Invalid deposit amount format", true)
                return false
            }
            try {
                val amount = BigDecimal(amountOfWithdraw)
                if (amount <= BigDecimal.ZERO) {
                    showErrorSnackBar("Withdraw amount must be greater than 0", true)
                    return false
                }else if (amount > BigDecimal(user.balance)){
                    showErrorSnackBar("You Don't Have Enough Balance.", true)
                    return false
                }
            } catch (e: NumberFormatException) {
                showErrorSnackBar("Invalid withdraw amount format", true)
                return false
            }
        }else{
            showErrorSnackBar("Please Enter Amount Of Withdraw.", true)
            return false
        }
        return true
    }
     */


}