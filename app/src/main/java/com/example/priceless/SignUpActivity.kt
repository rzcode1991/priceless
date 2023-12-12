package com.example.priceless

import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Suppress("DEPRECATION")
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
        val allowedRegexForUserName = Regex("^[a-z0-9_-]*$")
        val allowedRegexForName = Regex("^[a-zA-Z0-9_-]*$")
        //val passWordRegex = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{}|\\\\:;'\"<>,.?/]).{8,}\$")
        val reservedUsernames = listOf("admin", "root", "moderator", "support", "official",
            "anonymous", "system", "bot", "test", "report", "feedback", "contact", "help", "terms",
            "privacy", "security", "register", "login", "logout", "signup", "settings", "profile",
            "account", "user", "users", "blocked", "banned", "spam", "feedback", "master", "invalid",
            "unavailable", "service", "error", "server", "guest")
        when {
            TextUtils.isEmpty(userName) -> {
                showErrorSnackBar("Please Enter Your User Name", true)
                callback(false)
            }
            userName.length !in 3..20 -> {
                showErrorSnackBar("User Name Should Be At Least 3 And Max 20 Characters", true)
                callback(false)
            }
            !userName.matches(allowedRegexForUserName) -> {
                showErrorSnackBar("Allowed Characters For UserName Are: letters (a-z), numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
                callback(false)
            }
            userName in reservedUsernames -> {
                showErrorSnackBar("User Name Is Already Taken", true)
                callback(false)
            }
            TextUtils.isEmpty(firstName) -> {
                showErrorSnackBar("Please Enter Your First Name", true)
                callback(false)
            }
            firstName.length > 30 -> {
                showErrorSnackBar("First Name Too Long", true)
                callback(false)
            }
            !firstName.matches(allowedRegexForName) -> {
                showErrorSnackBar("Allowed Characters For FirstName Are: letters (a-z),(A-Z) numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
                callback(false)
            }
            TextUtils.isEmpty(lastName) -> {
                showErrorSnackBar("Please Enter Your Last Name", true)
                callback(false)
            }
            lastName.length > 30 -> {
                showErrorSnackBar("Last Name Too Long", true)
                callback(false)
            }
            !lastName.matches(allowedRegexForName) -> {
                showErrorSnackBar("Allowed Characters For LastName Are: letters (a-z),(A-Z) numbers (0-9), " +
                        "underscores (_), and hyphens (-)", true)
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