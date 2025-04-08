package com.examples.wavesoffood.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.examples.wavesoffood.ChangePasswordActivity
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.FragmentProfileBinding
import com.examples.wavesoffood.LoginActivity
import com.examples.wavesoffood.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var userReference:DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            userReference = database.reference.child("user").child(userId)
        } else {
            showPopupMessage("Error", "User ID not found", isError = true)
            return binding.root
        }

        setUserData()

        binding.saveInfoButton.visibility = View.GONE

        binding.changePassword.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        binding.apply {
            name.isEnabled = false
            email.isEnabled = false
            address.isEnabled = false
            phone.isEnabled = false

            editButton.setOnClickListener {
                val isEditing = !name.isEnabled
                name.isEnabled = isEditing
                email.isEnabled = isEditing
                address.isEnabled = isEditing
                phone.isEnabled = isEditing
                saveInfoButton.visibility = if (isEditing) View.VISIBLE else View.GONE

                editButton.text = if (isEditing) "Cancel Edit" else "Edit Profile"
            }
        }

        binding.saveInfoButton.setOnClickListener {
            val name = binding.name.text.toString()
            val email = binding.email.text.toString()
            val address = binding.address.text.toString()
            val phone = binding.phone.text.toString()

            if (name.isBlank() || email.isBlank() || address.isBlank() || phone.isBlank()) {
                showPopupMessage("Error", "Please fill all fields", isError = true)
            } else {
                updateUserData(name, email, address, phone)
                disableInputFields()
                binding.saveInfoButton.visibility = View.GONE
            }
        }

        binding.signOutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return binding.root
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        iconView.setImageResource(R.drawable.ic_info)
        titleView.text = "Confirm Logout"
        messageView.text = "Are you sure you want to log out?"

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
            .setView(dialogView)
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 16f
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).textSize = 16f

        dialog.window?.attributes = dialog.window?.attributes?.apply {
            width = resources.displayMetrics.widthPixels - (32 * 2).dp()
        }
    }

    private fun disableInputFields() {
        binding.apply {
            name.isEnabled = false
            email.isEnabled = false
            address.isEnabled = false
            phone.isEnabled = false
            editButton.text = "Edit Profile"
        }
    }

    private fun updateUserData(name: String, email: String, address: String, phone: String) {
        val userData = hashMapOf(
            "name" to name,
            "address" to address,
            "email" to email,
            "phone" to phone
        )

        userReference.updateChildren(userData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error updating profile", e)
                showPopupMessage("Error", "Failed to update profile", isError = true, e.message)
            }
    }

    private fun setUserData() {
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val userProfile = snapshot.getValue(UserModel::class.java)
                        with(binding){
                            name.setText(userProfile?.name)
                            address.setText(userProfile?.address)
                            email.setText(userProfile?.email)
                            phone.setText(userProfile?.phone)
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Error fetching user data", e)
                        showPopupMessage("Error", "Failed to load profile", isError = true, e.message)
                    }
                } else {
                    showPopupMessage("Info", "No profile data found", isError = false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "Error fetching user data", error.toException())
                showPopupMessage("Error", "Failed to load profile", isError = true, error.message)
            }
        })
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError: Boolean,
        logMessage: String? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
        iconView.setImageResource(icon)
        titleView.text = title
        messageView.text = logMessage?.let { "$message\n\nLog: $it" } ?: message

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
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