package com.examples.wavesoffood

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.FragmentNotifacionBottomBinding
import com.examples.wavesoffood.adapter.NotificationAdapter
import com.examples.wavesoffood.adapter.NotificationAdapter.NotificationData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Notifacion_Bottom_Fragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentNotifacionBottomBinding
    private lateinit var adapter: NotificationAdapter
    private lateinit var databaseReference: DatabaseReference

    // Notification channel ID
    private val CHANNEL_ID = "new_order_channel"

    // List to store existing order keys
    private val existingOrderKeys = mutableSetOf<String>()

    private var notificationTextForPermission: String? = null

    // Set to store notification IDs
    private val displayedNotificationIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = FirebaseDatabase.getInstance().reference.child("OrderDetails")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotifacionBottomBinding.inflate(layoutInflater, container, false)

        val notifications = arrayListOf<NotificationData>()
        adapter = NotificationAdapter(notifications, requireContext())
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationRecyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userBuyHistoryRef =
                FirebaseDatabase.getInstance().reference.child("user").child(userId)
                    .child("BuyHistory")

            // Create notification channel (if not already created)
            createNotificationChannel()

            userBuyHistoryRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.noItemsTextView.visibility = View.GONE
                        notifications.clear() // Clear the list before adding new items

                        for (orderSnapshot in snapshot.children) {
                            val orderKey = orderSnapshot.key

                            val orderAccepted =
                                orderSnapshot.child("orderAccepted").getValue(Boolean::class.java)
                                    ?: false
                            val paymentReceived =
                                orderSnapshot.child("paymentReceived").getValue(Boolean::class.java)
                                    ?: false
                            val viewed =
                                orderSnapshot.child("viewed").getValue(Boolean::class.java) ?: false

                            val foodNames = orderSnapshot.child("foodNames")
                                .getValue(object : GenericTypeIndicator<List<String>>() {})
                                ?: emptyList()
                            val foodPrices = orderSnapshot.child("foodPrices")
                                .getValue(object : GenericTypeIndicator<List<String>>() {})
                                ?: emptyList()
                            val foodQuantities = orderSnapshot.child("foodQuantities")
                                .getValue(object : GenericTypeIndicator<List<Int>>() {})
                                ?: emptyList()
                            val currentTime = orderSnapshot.child("currentTime").getValue(Long::class.java) ?: 0L
                            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()) // Date format
                            val formattedTime =dateFormat.format(Date(currentTime)) // Formatted date time

                            val totalPrice =
                                orderSnapshot.child("totalPrice").getValue(String::class.java) ?: ""


                            val details = foodNames.zip(foodPrices).zip(foodQuantities)
                                .joinToString("\n") { (namePrice, quantity) ->
                                    val (name, price) = namePrice
                                    "$name | $$price, Quan: $quantity"
                                }


                            val notificationText = when {
                                paymentReceived -> {
                                    val baseText = "Completed Order:\n$details\nTotal: $totalPrice\nDate: $formattedTime"
                                    val spannableText = SpannableString(baseText)
                                    spannableText.setSpan(
                                        StyleSpan(Typeface.BOLD),
                                        0, // Start index of "Completed Order:"
                                        15, // End index of "Completed Order:"
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    spannableText
                                }
                                orderAccepted -> {
                                    val baseText = "Order Accepted & Delivery:\n$details\nTotal: $totalPrice\nDate: $formattedTime"
                                    val spannableText = SpannableString(baseText)
                                    spannableText.setSpan(
                                        StyleSpan(Typeface.BOLD),
                                        0, // Start index of "Order Accepted & Delivery:"
                                        26, // End index of "Order Accepted & Delivery:"
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    spannableText
                                }
                                else -> {
                                    val baseText = "Waiting Accept Order:\n$details\nTotal: $totalPrice\nDate: $formattedTime"
                                    val spannableText = SpannableString(baseText)
                                    spannableText.setSpan(
                                        StyleSpan(Typeface.BOLD),
                                        0, // Start index of "Waiting Accept Order:"
                                        21, // End index of "Waiting Accept Order:"
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    spannableText
                                }
                            }

                            val notificationImages = when {
                                paymentReceived -> listOf(R.drawable.congrats)
                                orderAccepted -> listOf(R.drawable.truck)
                                else -> listOf(R.drawable.sademoji)
                            }

                            val notificationColor = if (viewed) Color.GRAY else Color.GREEN
                            val timestamp = orderSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                            val notificationData = NotificationAdapter.NotificationData(
                                notificationText.toString(), // Convert to String
                                notificationImages,
                                notificationColor,
                                orderSnapshot.key!!,
                                viewed,
                                currentTime
                            )

                            // Check if this is a new order and not viewed
                            if (orderKey != null && !existingOrderKeys.contains(orderKey) && !notificationData.viewed) {
                                existingOrderKeys.add(orderKey)

                                // Show notification for new order
                                showNotificationForNewOrder(notificationText.toString())
                            }

                            // Add notification to the list
                            notifications.add(notificationData)
                        }

                        // Filter notifications based on viewed status
                        notifications.filter { !it.viewed }

                        // Sort notifications by timestamp (newest first)
                        notifications.sortByDescending { it.currentTime }
                        adapter.notifyDataSetChanged()

                    } else {
                        binding.noItemsTextView.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorPopup(error.message)
                }
            })
        } else {
            // Handle user not logged in
            binding.noItemsTextView.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description =descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun showNotificationForNewOrder(notificationText: String) {
        notificationTextForPermission = notificationText // Store the notification text

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent: PendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationId = System.currentTimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.bell)
            .setContentTitle("New Order Received")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show notification
                showNotificationWithSound(notificationText)
            } else {
                // Permission not granted, request it
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_PERMISSION_CODE)
            }
        } else {
            // For older Android versions, notification permission is granted by default
            showNotificationWithSound(notificationText)
        }
    }

    private fun showNotificationWithSound(notificationText: String) {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent: PendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationId = System.currentTimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.bell)
            .setContentTitle("New Order Received")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Set sound

        try{
            // Check if notification permission is granted
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, set the sound
                notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            } else {
                // Permission not granted, handle accordingly (e.g., log a message or request permission)
                // ...
            }
        } catch (e: SecurityException) {
            // Handle SecurityException (e.g., log the error or display a message to the user)
            // ...
        }

        if (notificationId !in displayedNotificationIds) {
            displayedNotificationIds.add(notificationId)

            with(NotificationManagerCompat.from(requireContext())) {
                notify(notificationId, notificationBuilder.build())
            }
        } else {
            // Notification with this ID already exists, skip creating a new one

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, re-show the notification with soundnotificationTextForPermission?.let { showNotificationForNewOrder(it) }
            } else {
                // Permission denied, handle accordingly (e.g., display a message to the user)
                // ...
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1
    }

    private fun showErrorPopup(errorMessage: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(errorMessage).setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}