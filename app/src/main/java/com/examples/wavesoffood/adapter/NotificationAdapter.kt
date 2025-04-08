package com.examples.wavesoffood.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.databinding.NotificationItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationAdapter(
    private val notifications: ArrayList<NotificationData>,
    private val context: Context
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }


    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position], position) // Pass position here
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(private val binding: NotificationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationData, position: Int) {
            binding.apply {
                notificationTextView.text = notification.text
                notificationTextView.setTextColor(notification.color)
                Log.d("NotificationViewHolder", "Set notificationTextView color to: ${notification.color}")

                // Example for displaying images (replace with your actual image loading logic)
                if (notification.images.isNotEmpty()) {
                    val imageResource = notification.images[0] // Get thefirst image
                    notificationImageView.setImageResource(imageResource)
                }

                itemView.setOnClickListener {
                    // Update the viewed property in the data model
                    notifications[position].viewed = true

                    // Change the color to gray
                    notifications[position].color = Color.GRAY

                    // Notify the adapter of the data change
                    notifyItemChanged(position)

                    // Mark notification as viewed in Firebase when clicked
                    val userBuyHistoryRef = FirebaseDatabase.getInstance().reference
                        .child("user")
                        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        .child("BuyHistory")
                        .child(notification.orderKey)

                    userBuyHistoryRef.child("viewed").setValue(true)

                }

            }

        }
    }

    data class NotificationData(
        val text: String,
        val images: List<Int>,
        var color: Int,
        val orderKey: String,
        var viewed: Boolean, // Change to var
        val currentTime: Long
    )

    fun addNotification(notification: NotificationData) { // Change parameter type
        notifications.add(notification)
        notifyItemInserted(notifications.size - 1)
    }
}