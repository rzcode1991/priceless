package com.example.priceless

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase

class SignUpActivity : BaseActivity() {

    private lateinit var toolbarSignUp: Toolbar
    private lateinit var etUserName: TextInputEditText
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var checkBox: CheckBox
    private lateinit var tvTerms: TextView
    private lateinit var btnSignUp: Button
    private lateinit var tvGoToLogIn: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        tvGoToLogIn = findViewById(R.id.tv_goto_login_signup)
        toolbarSignUp = findViewById(R.id.toolbar_signup)
        etUserName = findViewById(R.id.et_user_name_signup)
        etFirstName = findViewById(R.id.et_first_name_signup)
        etLastName = findViewById(R.id.et_last_name_signup)
        etEmail = findViewById(R.id.et_email_signup)
        etPassword = findViewById(R.id.et_password_signup)
        etConfirmPassword = findViewById(R.id.et_confirm_password_signup)
        checkBox = findViewById(R.id.checkbox_terms_signup)
        tvTerms = findViewById(R.id.tv_terms_signup)
        btnSignUp = findViewById(R.id.btn_register_signup)
        auth = Firebase.auth


        tvGoToLogIn.setOnClickListener {
            onBackPressed()
        }

        setActionBar()

        btnSignUp.setOnClickListener {
            registerUser()
        }


    }


    private fun setActionBar(){
        setSupportActionBar(toolbarSignUp)
        val actionBar = supportActionBar
        actionBar?.title = ""
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_back)
        }
        toolbarSignUp.setNavigationOnClickListener { onBackPressed() }
    }

    private fun validateUserInput(callback: (Boolean) -> Unit) {
        val userName = etUserName.text.toString().lowercase().trim { it <= ' ' }
        val firstName = etFirstName.text.toString().trim { it <= ' ' }
        val lastName = etLastName.text.toString().trim { it <= ' ' }
        val email = etEmail.text.toString().trim { it <= ' ' }
        val password = etPassword.text.toString().trim { it <= ' ' }
        val confirmPassword = etConfirmPassword.text.toString().trim { it <= ' ' }
        when {
            TextUtils.isEmpty(userName) -> {
                showErrorSnackBar("Please Enter Your User Name", true)
                callback(false)
            }
            TextUtils.isEmpty(firstName) -> {
                showErrorSnackBar("Please Enter Your First Name", true)
                callback(false)
            }
            TextUtils.isEmpty(lastName) -> {
                showErrorSnackBar("Please Enter Your Last Name", true)
                callback(false)
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please Enter Your Email", true)
                callback(false)
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please Enter Your Password", true)
                callback(false)
            }
            TextUtils.isEmpty(confirmPassword) -> {
                showErrorSnackBar("Please Confirm Your Password", true)
                callback(false)
            }
            password != confirmPassword -> {
                showErrorSnackBar("Password Doesn't Match", true)
                callback(false)
            }
            !checkBox.isChecked -> {
                showErrorSnackBar("Please Agree To The Terms & Conditions", true)
                callback(false)
            }
            else -> {
                // Check if the username is taken
                showProgressDialog()
                FireStoreClass().isUserNameTaken(userName) { isTaken ->
                    hideProgressDialog()
                    if (isTaken) {
                        showErrorSnackBar("User Name Is Already Taken", true)
                        callback(false)
                    } else {
                        // Username is not taken, continue with registration
                        callback(true)
                    }
                }
            }
        }
    }


    private fun registerUser(){
        validateUserInput { isValid ->
            if (isValid) {
                showProgressDialog()
                val email: String = etEmail.text.toString()
                val password: String = etPassword.text.toString()
                auth.createUserWithEmailAndPassword(email, password).
                addOnCompleteListener(this) { task ->
                    if (task.isSuccessful){
                        val firebaseUser = auth.currentUser!!
                        val user = User(
                            firebaseUser.uid,
                            etUserName.text.toString().lowercase().trim { it <= ' ' },
                            etFirstName.text.toString(),
                            etLastName.text.toString(),
                            etEmail.text.toString()
                        )
                        FireStoreClass().registerUserInFireStore(this@SignUpActivity, user)
                        //auth.signOut()
                    }else{
                        hideProgressDialog()
                        showErrorSnackBar(task.exception?.message.toString(), true)
                    }
                }
            }
        }
    }


    fun registrationSuccessful(){
        hideProgressDialog()
        Toast.makeText(this, "Your Registration Was Successful",
            Toast.LENGTH_LONG).show()
        finish()
    }


}