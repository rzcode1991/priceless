package com.example.priceless

import android.app.Dialog
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.priceless.R
import com.google.android.material.snackbar.Snackbar

open class BaseActivity : AppCompatActivity() {

    private var backButtonPressedOnce = false
    private lateinit var progressDialog: Dialog

    fun showErrorSnackBar(message: String, errorMessage: Boolean){
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        if (errorMessage){
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        }else{
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }
        snackBar.show()
    }

    fun showProgressDialog(){
        progressDialog = Dialog(this)
        progressDialog.setContentView(R.layout.progress_dialog)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    fun hideProgressDialog(){
        progressDialog.dismiss()
    }

    fun doublePressBackButtonToExit(){
        if (backButtonPressedOnce){
            super.onBackPressed()
            return
        }
        this.backButtonPressedOnce = true
        Toast.makeText(this, "press back again to exit", Toast.LENGTH_LONG).show()
        Handler().postDelayed({backButtonPressedOnce = false}, 2000)
    }


}