package com.example.priceless

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LogIn : BaseActivity(), OnClickListener {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvForgetPassWord: TextView
    private lateinit var btnLogIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        etEmail = findViewById(R.id.et_email_login)
        etPassword = findViewById(R.id.et_password_login)
        tvForgetPassWord = findViewById(R.id.tv_forget_pass_login)
        btnLogIn = findViewById(R.id.btn_login)
        btnSignUp = findViewById(R.id.btn_signup_loginActivity)
        auth = Firebase.auth

        tvForgetPassWord.setOnClickListener(this)
        btnLogIn.setOnClickListener(this)
        btnSignUp.setOnClickListener(this)

    }


    private fun validateUserInput(): Boolean {
        return when{
            TextUtils.isEmpty(etEmail.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar("Please Enter Your Email", true)
                false
            }
            TextUtils.isEmpty(etPassword.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar("Please Enter Your Password", true)
                false
            }
            else -> {
                true
            }
        }
    }


    private fun logInUser(){
        if (validateUserInput()){
            showProgressDialog()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            auth.signInWithEmailAndPassword(email, password).
            addOnCompleteListener(this) { task ->
                if (task.isSuccessful){
                    val userID = FireStoreClass().getUserID()
                    FireStoreClass().getUserInfoFromFireStore(this@LogIn, userID)
                }else{
                    hideProgressDialog()
                    showErrorSnackBar(task.exception?.message.toString(), true)
                }
            }
        }
    }


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.tv_forget_pass_login -> {
                val intent = Intent(this, ForgetPasswordActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_login -> {
                logInUser()
            }
            R.id.btn_signup_loginActivity -> {
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
            }
        }
    }


    fun successGettingUserInfoFromFireStore(user: User){
        hideProgressDialog()

        if (user.profileCompleted == 0){
            Toast.makeText(this, "please complete your profile", Toast.LENGTH_LONG).show()
            val intent = Intent(this@LogIn, ProfileActivity::class.java)
            //intent.putExtra(Constants.User_Extra_Details, user)
            startActivity(intent)
        }else{
            val intent = Intent(this@LogIn, FragmentActivity::class.java)
            //intent.putExtra(Constants.User_Extra_Details, user)
            startActivity(intent)
        }
        finish()
    }


}