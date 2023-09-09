package com.example.priceless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgetPasswordActivity : BaseActivity() {

    private lateinit var toolbarForgetPass: Toolbar
    private lateinit var etEmail: EditText
    private lateinit var btnResetPass: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        toolbarForgetPass = findViewById(R.id.toolbar_forgetPass)
        etEmail = findViewById(R.id.et_email_forgetPass)
        btnResetPass = findViewById(R.id.btn_reset_forgetPass)
        auth = Firebase.auth

        setActionBar()

        btnResetPass.setOnClickListener {
            val email = etEmail.text.toString().trim { it <= ' ' }
            if (email.isEmpty()){
                showErrorSnackBar("Please Enter Your Email", true)
            }else{
                showProgressDialog()
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    hideProgressDialog()
                    if (task.isSuccessful){
                        Toast.makeText(this, "Check Your Inbox To Reset Your Password",
                            Toast.LENGTH_LONG).show()
                        finish()
                    }else{
                        showErrorSnackBar(task.exception?.message.toString(), true)
                    }
                }
            }
        }

    }


    private fun setActionBar(){
        setSupportActionBar(toolbarForgetPass)
        val actionBar = supportActionBar
        actionBar?.title = ""
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back)
        }
        toolbarForgetPass.setNavigationOnClickListener{ onBackPressed() }
    }


}