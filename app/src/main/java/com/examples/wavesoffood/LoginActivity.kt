package com.examples.wavesoffood

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.ActivityLoginBinding
import com.examples.wavesoffood.model.UserModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class LoginActivity : AppCompatActivity() {
    private var userName: String? = null
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingDialog: AlertDialog

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        // Initial of Firebase auth
        auth = Firebase.auth
        // Initial of Firebase database
        database = Firebase.database.reference
        // Initial of Google
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Setup loading dialog
        setupLoadingDialog()

        // Login with email and password
        binding.loginButton.setOnClickListener {
            email = binding.email.text.toString().trim()
            password = binding.password.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                showPopupMessage(
                    "Info",
                    "Please enter all the details.",
                )
            } else {
                showLoadingDialog()
                createUser()
            }
        }

        binding.donthavebutton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Google Sign-in
        binding.googleButton.setOnClickListener {
            val signinIntent = googleSignInClient.signInIntent
            launcher.launch(signinIntent)
        }

        // Facebook Sign In (Placeholder)
        binding.facebookButton.setOnClickListener {
            showPopupMessage("Info", "Coming Soon.")
        }

        // Forgot Password
        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createUser() {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                hideLoadingDialog()
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUi(user)
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                saveUserdata()
                                val user = auth.currentUser
                                updateUi(user)
                            } else {
                                showPopupMessage(
                                    "Error",
                                    "Sign-in failed. ${task.exception?.message}",
                                    isError = true
                                )
                            }
                        }}
            }
    }

    private fun saveUserdata() {
        email = binding.email.text.toString().trim()
        password = binding.password.text.toString().trim()

        val user = UserModel(userName, email, password)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        database.child("user").child(userId).setValue(user)
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateUi(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showPopupMessage("Success", "Password reset email sent.")
                } else {
                    showPopupMessage(
                        "Error",
                        "Failed to send reset email. ${task.exception?.message}",
                        isError = true
                    )
                }
            }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    val account: GoogleSignInAccount? = task.result
                    val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Sign-in Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            showPopupMessage(
                                "Error",
                                "Sign in failed. ${task.exception?.message}",
                                isError = true
                            )
                        }}
                }
            } else {
                showPopupMessage(
                    "Error",
                    "Sign in failed.",
                    isError = true
                )
            }
        }

    // Loading dialog methods
    private fun setupLoadingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        val gifImageView = dialogView.findViewById<ImageView>(R.id.loadingGif)

        Glide.with(this)
            .asGif()
            .load(R.drawable.loading)
            .into(gifImageView)

        val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
        loadingDialog = builder.create()
        loadingDialog.setCancelable(false)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showLoadingDialog() {
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError: Boolean = false,
        logMessage: String? = null
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
        iconView.setImageResource(icon)
        titleView.text = title
        messageView.text = if (logMessage != null) "$message\n\nLog: $logMessage" else message

        val builder = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.textSize =20f // Set text size to 20sp

        val layoutParams = dialog.window?.attributes
        layoutParams?.width= resources.displayMetrics.widthPixels - (32 * 2).dp()
        dialog.window?.attributes = layoutParams
    }

    // Extension function to convert dp to pixels
    private fun Int.dp(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
}