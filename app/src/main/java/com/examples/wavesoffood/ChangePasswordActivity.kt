package com.examples.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var submitButton: View
    private val auth= FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        oldPasswordEditText = findViewById(R.id.oldPassword)
        newPasswordEditText = findViewById(R.id.newPassword)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)
        submitButton = findViewById(R.id.submitInformation)

        // Set drawables for password fields after initialization
        val lockDrawable = resources.getDrawable(R.drawable.lock, null)
        val eyeDrawable = resources.getDrawable(R.drawable.eye, null)
        oldPasswordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(lockDrawable, null, eyeDrawable, null)
        newPasswordEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(lockDrawable, null, eyeDrawable, null)

        // Set listeners for password visibility toggles
        oldPasswordEditText.setOnTouchListener(onTouchListener)
        newPasswordEditText.setOnTouchListener(onTouchListener)

        // Disable submit button initially
        submitButton.isEnabled = false

        // Set up text change listenersfor enabling/disabling submit button
        val fields = listOf(oldPasswordEditText, newPasswordEditText, confirmPasswordEditText)
        fields.forEach { field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val allFieldsFilled = fields.all { it.text.isNotEmpty() }
                    submitButton.isEnabled = allFieldsFilled
                    if (allFieldsFilled) {
                        submitButton.setBackgroundResource(R.drawable.greenbuttongradient)
                    } else {
                        submitButton.setBackgroundResource(R.drawable.graybuttonbackground)
                    }
                }
            })
        }

        submitButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword != confirmPassword) {
                showPopupMessage(
                    "Error",
                    "New password and confirm password do not match",
                    isError = true
                )
                return@setOnClickListener
            }

            updatePassword(oldPassword, newPassword)
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private val onTouchListener = View.OnTouchListener { v, event ->
        if (v is EditText) {
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                val rightDrawable = v.compoundDrawablesRelative[DRAWABLE_RIGHT]
                if (rightDrawable != null && event.rawX >= (v.right - rightDrawable.bounds.width() - v.paddingRight)) {
                    togglePasswordVisibility(v)
                    return@OnTouchListener true
                }
            }
        }
        return@OnTouchListener false
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val drawables = editText.compoundDrawablesRelative
        val rightDrawable = drawables[2]
        if (rightDrawable != null){
            val isVisible = editText.transformationMethod == null
            editText.transformationMethod = if (isVisible) {
                PasswordTransformationMethod.getInstance()
            } else {
                null
            }
            val drawableResource = if (isVisible) R.drawable.eye_hide else R.drawable.eye
            val newDrawable = resources.getDrawable(drawableResource, null)
            newDrawable.setBounds(0, 0, rightDrawable.intrinsicWidth, rightDrawable.intrinsicHeight)
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(drawables[0], drawables[1], newDrawable, drawables[3])
        }
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null) {
            val credential = EmailAuthProvider.getCredential(user.email ?: "", oldPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (user.providerData.any { it.providerId == "password" }) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        updatePasswordInDatabase(newPassword)
                                    } else {
                                        showPopupMessage(
                                            "Error",
                                            "Failed to update password",
                                            isError = true
                                        )
                                    }
                                }
                        } else {
                            showPopupMessage(
                                "Info",
                                "You are signed in with Google. You cannot change your password here.",
                                isError = false
                            )
                        }
                    } else {
                        showPopupMessage("Error", "Authentication failed", isError = true)
                    }
                }
        } else {
            showPopupMessage("Error", "User not found", isError = true)
        }
    }

    private fun updatePasswordInDatabase(newPassword: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            userReference.child("password").setValue(newPassword)
                .addOnSuccessListener {
                    showPopupMessage("Success", "Password updated successfully", isError = false)
                    finish() // Finish the activity after successful password update
                }
                .addOnFailureListener { e ->
                    Log.e("ChangePasswordActivity", "Error updating password in database", e)
                    showPopupMessage(
                        "Error",
                        "Failed to update password in database",
                        isError = true
                    )
                }
        } else {
            showPopupMessage("Error", "User ID not found", isError = true)
        }
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError: Boolean
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
        iconView.setImageResource(icon)
        titleView.text = title
        messageView.text = message

        val dialog = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            textSize = 20f
        }

        dialog.window?.attributes = dialog.window?.attributes?.apply {
            width = resources.displayMetrics.widthPixels - (32 * 2).dp()
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
}