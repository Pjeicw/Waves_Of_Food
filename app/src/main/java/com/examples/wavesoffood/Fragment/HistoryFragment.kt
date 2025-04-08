package com.examples.wavesoffood.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.wavesoffood.R
import com.examples.wavesoffood.RecentOrderItems
import com.examples.wavesoffood.adapter.BuyAgainAdapter
import com.example.wavesoffood.databinding.FragmentHistoryBinding
import com.examples.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        retrieveBuyHistory()

        binding.recentbuyitem.setOnClickListener {
            seeItemsRecentBuy()
        }

        binding.receivedButton.setOnClickListener {
            updateOrderStatus()
        }

        return binding.root
    }

    private fun updateOrderStatus() {
        if (listOfOrderItem.isEmpty()) {
            showPopupMessage("No Orders", "You have no recent orders.", isError = false)
            return
        }
        val itemPushKey = listOfOrderItem[0].itemPushKey
        val completeOrderReference = database.reference.child("CompletedOrder").child(itemPushKey!!)

        completeOrderReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orderAccepted = snapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false
                val paymentReceived = snapshot.child("paymentReceived").getValue(Boolean::class.java) ?: false
                updateButtonVisibility(orderAccepted, paymentReceived)

                // Send broadcast after fetching and processing data
                val intent = Intent("com.examples.wavesoffood.BUY_HISTORY_CHANGED")
                requireContext().sendBroadcast(intent)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Error fetching order status", error.toException())
                showPopupMessage("Error", "Failed to fetch order status", isError = true, error.message)
            }
        })

        binding.receivedButton.setOnClickListener {
            completeOrderReference.child("paymentReceived").setValue(true).addOnSuccessListener {
                // Also update BuyHistory for the clicked item
                val userIdOfClickedItem = auth.currentUser?.uid // Make sure to get the correct user ID
                val buyHistoryReference = database.reference.child("user").child(userIdOfClickedItem!!).child("BuyHistory").child(itemPushKey)
                buyHistoryReference.child("paymentReceived").setValue(true).addOnSuccessListener {
                    showPopupMessage("Success", "Payment Successful!", isError = false)
                }.addOnFailureListener { error ->
                    Log.e("HistoryFragment", "Error updating BuyHistory payment status", error)
                    showPopupMessage("Error", "Failed to update payment status in BuyHistory", isError = true, error.message)
                }
                updateButtonVisibility(true, true) // Both orderAccepted and paymentReceived should be true
            }.addOnFailureListener { error ->
                Log.e("HistoryFragment", "Error updating order status", error)
                showPopupMessage("Error", "Failed to update order status", isError = true, error.message)
            }
        }
    }

    private fun seeItemsRecentBuy() {
        if (listOfOrderItem.isEmpty()) {
            showPopupMessage("No Orders", "You have no recent orders.", isError = false)
            return
        }
        val intent = Intent(requireContext(), RecentOrderItems::class.java)
        intent.putExtra("RecentBuyOrderItem", ArrayList(listOfOrderItem))
        startActivity(intent)
    }

    private fun retrieveBuyHistory() {
        binding.recentbuyitem.visibility = View.VISIBLE
        userId = auth.currentUser?.uid ?: ""
        val buyItemReference: DatabaseReference =
            database.reference.child("user").child(userId).child("BuyHistory")
        val shortingQuery = buyItemReference.orderByChild("CurrentTime")

        shortingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (buySnapshot in snapshot.children) {
                        val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                        buyHistoryItem?.let { listOfOrderItem.add(it) }
                    }
                    listOfOrderItem.reverse() // Reverse to show most recent first
                    if (listOfOrderItem.isNotEmpty()) {
                        setDataInRecentBuyItem()
                        setPreviousBuyItemRecyclerView()
                        updateOrderStatus()
                    } else {
                        binding.noItemsTextView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e("HistoryFragment", "Error fetching buy history", e)
                    showPopupMessage("Error", "Failed to load order history", isError = true, e.message)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Error fetching buy history", error.toException())
                showPopupMessage("Error", "Failed to load order history", isError = true, error.message)
            }
        })
    }

    private fun setDataInRecentBuyItem() {
        binding.recentbuyitem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItem.firstOrNull()
        if (recentOrderItem == null) {
            showPopupMessage("No Orders", "You have no recent orders.", isError = false)
            return
        }
        with(binding) {
            buyAgainFoodName.text = recentOrderItem.foodNames?.firstOrNull() ?: ""
            buyAgainFoodPrice.text = (recentOrderItem.totalPrice ?: "")
            val image = recentOrderItem.foodImages?.firstOrNull()
            if (image != null) {
                Glide.with(requireContext()).load(Uri.parse(image)).into(buyAgainFoodImage)
            }

            val isOrderIsAccepted = recentOrderItem.orderAccepted
            orderdStutus.setCardBackgroundColor(if (isOrderIsAccepted == true) Color.GREEN else Color.LTGRAY)
            binding.receivedButton.visibility = if (isOrderIsAccepted == true) View.VISIBLE else View.GONE
        }
    }

    private fun setPreviousBuyItemRecyclerView() {
        val buyAgainFoodName = mutableListOf<String>()
        val buyAgainFoodPrice = mutableListOf<String>()
        val buyAgainFoodImage = mutableListOf<String>()

        for (i in 1 until listOfOrderItem.size) {
            val orderDetails = listOfOrderItem[i]
            orderDetails.foodNames?.firstOrNull()?.let { buyAgainFoodName.add(it) }
            orderDetails.foodPrices?.firstOrNull()?.let { buyAgainFoodPrice.add(it) }
            orderDetails.foodImages?.firstOrNull()?.let { buyAgainFoodImage.add(it) }
        }

        if (buyAgainFoodName.isEmpty()) {
            binding.noItemsTextView.visibility = View.VISIBLE
            binding.buyAgainRecyclerView.visibility = View.GONE
        } else {
            binding.noItemsTextView.visibility = View.GONE
            binding.buyAgainRecyclerView.visibility = View.VISIBLE

            val rv = binding.buyAgainRecyclerView
            rv.layoutManager = LinearLayoutManager(requireContext())
            buyAgainAdapter = BuyAgainAdapter(
                buyAgainFoodName,
                buyAgainFoodPrice,
                buyAgainFoodImage,
                requireContext()
            )
            rv.adapter = buyAgainAdapter
        }
    }

    private fun updateButtonVisibility(orderAccepted: Boolean, paymentReceived: Boolean) {
        binding.receivedButton.visibility = if (orderAccepted && !paymentReceived) View.VISIBLE else View.GONE
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError: Boolean = false,
        logMessage: String? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
        iconView.setImageResource(icon)
        titleView.text = title
        messageView.text = if (logMessage != null) "$message\n\nLog: $logMessage" else message

        val builder = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
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

    override fun onStart() {
        super.onStart()
        retrieveBuyHistory()  // Fetch history every time fragment starts
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}
