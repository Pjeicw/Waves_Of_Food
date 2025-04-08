package com.examples.wavesoffood

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.wavesoffood.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storedVerificationId: String
    private lateinit var emailOtp: String
    private lateinit var emailOrPhoneEditText: EditText
    private lateinit var putOtpEditText: EditText
    private lateinit var resendOtpTextView: TextView
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var submitInformationButton: AppCompatButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = Firebase.auth
        database = Firebase.database.reference

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        emailOrPhoneEditText = findViewById(R.id.emailOrPhone)
        putOtpEditText = findViewById(R.id.putOTP)
        resendOtpTextView = findViewById(R.id.reSendOTP)
        newPasswordEditText = findViewById(R.id.newPassword)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        submitInformationButton = findViewById(R.id.submitInformation)

        emailOrPhoneEditText.addTextChangedListener(inputTextWatcher)
        putOtpEditText.addTextChangedListener(inputTextWatcher)
        newPasswordEditText.addTextChangedListener(inputTextWatcher)
        confirmPasswordEditText.addTextChangedListener(inputTextWatcher)

        submitInformationButton.setOnClickListener {
            onSubmitButtonClicked()
        }

        resendOtpTextView.setOnClickListener {
            onResendOtpClicked()
        }
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            checkInputAndProceed()
        }
    }

    private fun checkInputAndProceed() {
        val emailOrPhone = emailOrPhoneEditText.text.toString().trim()
        val otp = putOtpEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        when {
            emailOrPhone.isBlank() -> {
                submitInformationButton.isEnabled = false
                submitInformationButton.setBackgroundResource(R.drawable.graybuttonbackground)
            }
            otp.isBlank() && newPassword.isBlank() && confirmPassword.isBlank() -> {
                submitInformationButton.isEnabled = isEmailOrPhoneValid(emailOrPhone)
                updateSubmitButtonAppearance()
                submitInformationButton.text = "Send OTP"
            }
            otp.isNotBlank() && newPassword.isBlank() && confirmPassword.isBlank() -> {
                submitInformationButton.isEnabled = true
                updateSubmitButtonAppearance()
                submitInformationButton.text = "Verify OTP"
            }
            otp.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank() -> {
                submitInformationButton.isEnabled = newPassword == confirmPassword
                updateSubmitButtonAppearance()
                submitInformationButton.text = "Update Password"
            }
        }
    }

    private fun isEmailOrPhoneValid(input: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches() || isValidPhoneNumber(input)
    }

    private fun onSubmitButtonClicked() {
        val emailOrPhone = emailOrPhoneEditText.text.toString().trim()
        val otp = putOtpEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (isEmailOrPhoneValid(emailOrPhone)) {
            if (otp.isBlank()) {
                if (Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
                    sendOtpToEmail(emailOrPhone)
                } else {
                    checkPhoneNumberAndSendOtp(emailOrPhone)
                }
                submitInformationButton.text = "Verify OTP"
            } else if (newPassword.isBlank()) {
                if (Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
                    verifyEmailOtp(emailOrPhone, otp, newPassword)
                } else {
                    verifyPhoneOtpAndSendEmailOtp(otp, emailOrPhone, newPassword)
                }
            } else if (newPassword == confirmPassword){
                updatePassword(emailOrPhone, newPassword)
            } else {
                showPopupMessage("Error", "Password do not match.")
            }
        } else {
            showPopupMessage("Error", "Invalid email or phone number.")
        }
    }

    private fun onResendOtpClicked() {
        val emailOrPhone = emailOrPhoneEditText.text.toString().trim()
        if (emailOrPhone.isBlank()) {
            showPopupMessage("Info", "Please enter your email or phone number.")
            return
        }

        if (Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
            sendOtpToEmail(emailOrPhone)} else if (isValidPhoneNumber(emailOrPhone)) {
            checkPhoneNumberAndSendOtp(emailOrPhone)
        } else {
            showPopupMessage("Error", "Invalid email or phone number format.")
        }
    }

    private fun checkPhoneNumberAndSendOtp(phoneNumber: String) {
        database.child("user").orderByChild("phone").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            if (email.isNotBlank()) {
                                sendOtpToPhone(phoneNumber, email)
                                return
                            }
                        }
                        showPopupMessage("Info", "No email associated with this phone number.")
                    } else {
                        showPopupMessage("Info", "No user found with this phone number.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showPopupMessage("Error", "Failed to access database. ${error.message}")
                }
            })
    }

    private fun sendOtpToPhone(phoneNumber: String, email: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                override fun onVerificationFailed(e: FirebaseException) {
                    showPopupMessage("Error", "Failed to send OTP: ${e.message}")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneOtpAndSendEmailOtp(otp: String, phoneNumber: String, newPassword: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = task.result?.user
                val email= user?.email
                if (email != null) {
                    sendOtpToEmail(email)
                    submitInformationButton.text= "Update Password"
                } else {
                    showPopupMessage("Error", "Email not found for this phone number.")
                }
            } else {
                showPopupMessage("Error", "Invalid OTP.")
            }
        }
    }

    private fun sendOtpToEmail(email: String) {
        emailOtp = (100000..999999).random().toString()

        Log.d("OTP", "Generated OTP: $emailOtp")

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://adminwaveoffood.page.link/?email=$email&otp=$emailOtp")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(packageName, false, null)
            .build()

        auth.sendPasswordResetEmail(email, actionCodeSettings)
            .addOnSuccessListener {
                showPopupMessage("Success", "OTP sent to $email. Please check your inbox.")}
            .addOnFailureListener { exception ->
                when (exception) {
                    is FirebaseAuthInvalidUserException -> {
                        showPopupMessage("Error", "Email address not found.")
                    }
                    else -> {
                        showPopupMessage("Error", "Failed to send OTP: ${exception.message}")
                    }
                }
            }
    }



    private fun verifyEmailOtp(email: String, enteredOtp: String, newPassword: String) {
        if (enteredOtp == emailOtp) {
            submitInformationButton.text = "Update Password"
        } else {
            showPopupMessage("Error", "Invalid OTP.")
        }
    }

    private fun updatePassword(email: String, newPassword: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    finish()
                    showPopupMessage("Success", "Password reset email sent. Please check your inbox to set your new password.")
                } else {
                    showPopupMessage("Error", "Failed to send password reset email: ${task.exception?.message}")
                }
            }
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError:Boolean = false
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
        iconView.setImageResource(icon)
        titleView.text = title
        messageView.text = message

        val builder = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.textSize = 20f

        val layoutParams = dialog.window?.attributes
        layoutParams?.width = resources.displayMetrics.widthPixels - (32 * 2).dp()
        dialog.window?.attributes = layoutParams
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return Patterns.PHONE.matcher(phoneNumber).matches()
    }

    private fun updateSubmitButtonAppearance() {
        if (submitInformationButton.isEnabled) {
            submitInformationButton.setBackgroundResource(R.drawable.greenbuttongradient)
        } else {
            submitInformationButton.setBackgroundResource(R.drawable.graybuttonbackground)
        }
    }
}