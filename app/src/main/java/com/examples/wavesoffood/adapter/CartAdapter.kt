package com.examples.wavesoffood.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.CartItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private var cartDescriptions: MutableList<String>,
    private var cartImages: MutableList<String>,
    private val cartQuality: MutableList<Int>,
    private var cartIngredient: MutableList<String>
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // 1. Initialize Firebase Auth
    private val auth = FirebaseAuth.getInstance()

    // 2. Initialize necessary variables
    init {
        // 2.1 Get instance of Firebase Database
        val database = FirebaseDatabase.getInstance()
        // 2.2 Get current user's ID
        val userId = auth.currentUser?.uid ?: ""
        // 2.3 Get the number of cart items
        val cartItemNumber = cartItems.size
        // 2.4 Initialize item quantities with a default of 1
        itemQuantities = IntArray(cartItemNumber) { 1 }
        // 2.5 Reference to the user's cart items in Firebase
        cartItemsReference = database.reference.child("user").child(userId).child("CartItems")
    }

    companion object {
        // 3. Define variables for item quantities and Firebase reference
        private var itemQuantities: IntArray = intArrayOf()
        private lateinit var cartItemsReference: DatabaseReference
    }

    // 4. Create a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        // 4.1 Inflate the layout for cart item
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // 4.2 Return a new ViewHolder
        return CartViewHolder(binding)
    }

    // 5. Bind data to the ViewHolder
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        try {
            // 5.1 Call the bind method of the ViewHolder
            holder.bind(position)
        } catch (e: Exception) {
            // 5.2 Log error and show error dialog if binding fails
            Log.e("CartAdapter", "Error binding data at position $position", e)
            showErrorDialog("Error", "Failed to load item", e.message)
        }
    }

    // 6. Get the total item count
    override fun getItemCount(): Int = cartItems.size

    // 7. Get updated item quantities
    fun getUpdatedItemsQuantities(): MutableList<Int> {
        // 7.1 Create a mutable list for item quantities
        val itemQuantity = mutableListOf<Int>()
        // 7.2 Add all item quantities to the list
        itemQuantity.addAll(itemQuantities.toList())
        return itemQuantity
    }

    // 8. Inner class for ViewHolder
    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 9. Bind data to the ViewHolder's views
        fun bind(position: Int) {
            binding.apply {
                // 9.1 Check if cartItems and itemQuantities are not empty
                if (cartItems.isNotEmpty() && itemQuantities.isNotEmpty() && position in 0 until cartItems.size) {
                    // 9.2 Get the quantity for the current position
                    val quantity = itemQuantities[position]
                    // 9.3 Set food name and price
                    cartFoodName.text = cartItems[position]
                    cartItemPrice.text = "$" + cartItemPrices[position]

                    // 9.4 Get the image URI for the current item
                    val uriString = cartImages.getOrNull(position)
                    if (uriString != null) {
                        try {
                            // 9.5 Parse the URI and load the image using Glide
                            val uri = Uri.parse(uriString)
                            Glide.with(context).load(uri).into(cartImage)
                        } catch (e: Exception) {
                            // 9.6 Log error and show error dialog if image loading fails
                            Log.e("CartAdapter", "Error loading image: $uriString", e)
                            showErrorDialog("Error", "Failed to load image", e.message)
                        }
                    }

                    // 9.7 Set the quantity text
                    cartQuantity.text = quantity.toString()
                    // 9.8 Set click listeners for quantity buttons
                    minusbutton.setOnClickListener { decreaseQuantity(position) }
                    plusbutton.setOnClickListener { increaseQuantity(position) }
                    deleteButton.setOnClickListener {
                        val itemPosition = adapterPosition
                        // 9.9 Show confirmation dialog before deletion
                        if (itemPosition != RecyclerView.NO_POSITION) {
                            showDeleteConfirmationDialog(itemPosition)
                        }
                    }
                } else {
                    // 9.10 Hide views if cart is empty
                    cartFoodName.visibility = View.GONE
                    cartItemPrice.visibility = View.GONE
                    cartImage.visibility = View.GONE
                    // Consider showing an empty cart message here
                }
            }
        }

        // 10. Decrease the quantity of the item
        private fun decreaseQuantity(position: Int) {
            // 10.1 Check if quantity is greater than 1
            if (itemQuantities.getOrNull(position)?.let { it > 1 } == true) {
                itemQuantities[position]-- // 10.2 Decrease the quantity
                cartQuality[position] = itemQuantities[position] // 10.3 Update cart quality
                binding.cartQuantity.text = itemQuantities[position].toString() // 10.4 Update quantity text
            }
        }

        // 11. Increase the quantity of the item
        private fun increaseQuantity(position: Int) {
            // 11.1 Check if quantity is less than 10
            if (itemQuantities.getOrNull(position)?.let { it < 10 } == true) {
                itemQuantities[position]++ // 11.2 Increase the quantity
                cartQuality[position] = itemQuantities[position] // 11.3 Update cart quality
                binding.cartQuantity.text = itemQuantities[position].toString() // 11.4 Update quantity text
            }
        }

        // 12. Show confirmation dialog before deleting an item
        private fun showDeleteConfirmationDialog(position: Int) {
            // 12.1 Create AlertDialog builder
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirm Deletion")
            builder.setMessage("Are you sure you want to delete this item from your cart?")

            // 12.2 Set positive button for confirming deletion
            builder.setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()  // Close the dialog
                deleteItem(position) // 12.3 Proceed with deletion
            }

            // 12.4 Set negative button for canceling
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

            // 12.5 Show the confirmation dialog
            val dialog = builder.create()
            dialog.show()

            // 12.6 Customize button appearance if needed
            val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            deleteButton.setTextColor(context.resources.getColor(R.color.colorPrimar))  // Set 'Delete' button color
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancelButton.setTextColor(context.resources.getColor(R.color.black))  // Set 'Cancel' button color
        }

        // 13. Delete the item from the cart
        private fun deleteItem(position: Int) {
            val positionRetrieve = position
            getUniqueKeyAtPosition(positionRetrieve) { uniqueKey ->
                // 13.1 Check if unique key is valid
                if (uniqueKey != null) {
                    removeItem(position, uniqueKey) // 13.2 Proceed to remove item
                } else {
                    showErrorDialog("Error", "Failed to delete item") // 13.3 Show error if unique key is null
                }
            }
        }

        // 14. Remove the item from Firebase and update the adapter
        private fun removeItem(position: Int, uniqueKey: String) {
            // 14.1 Check if cartItems is not empty
            if (cartItems.isNotEmpty() && position in 0 until cartItems.size) {
                // 14.2 Remove item from Firebase
                cartItemsReference.child(uniqueKey).removeValue()
                    .addOnSuccessListener {
                        try {
                            // 14.3 Remove item from local lists
                            cartItems.removeAt(position)
                            cartImages.removeAt(position)
                            cartItemPrices.removeAt(position)
                            cartDescriptions.removeAt(position)
                            cartIngredient.removeAt(position)
                            cartQuality.removeAt(position)

                            // 14.4 Update item quantities
                            itemQuantities =
                                itemQuantities.filterIndexed { index, _ -> index != position }
                                    .toIntArray()

                            // 14.5 Notify adapter of item removal
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, cartItems.size)

                            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show() // 14.6 Show success message
                        } catch (e: IndexOutOfBoundsException) {
                            Log.e("CartAdapter", "Error removing item: ${e.message}") // 14.7 Log error
                            showErrorDialog("Error", "Error deleting item", e.message) // 14.8 Show error dialog
                        }
                    }
                    .addOnFailureListener {
                        showErrorDialog("Error", "Failed to delete item", it.message) // 14.9 Show error if deletion fails
                    }
            } else {
                Toast.makeText(context, "Cart is empty or invalid item", Toast.LENGTH_SHORT).show() // 14.10 Show empty cart message
            }
        }

        // 15. Get unique key for the item at a given position
        private fun getUniqueKeyAtPosition(positionRetrieve: Int, onComplete: (String?) -> Unit) {
            cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var uniqueKey: String? = null
                    // 15.1 Check if snapshot exists
                    if (snapshot.exists()) {
                        snapshot.children.forEachIndexed { index, dataSnapshot ->
                            // 15.2 Find unique key for the specific position
                            if (index == positionRetrieve) {
                                uniqueKey = dataSnapshot.key
                                return@forEachIndexed
                            }
                        }
                    }
                    onComplete(uniqueKey) // 15.3 Return the unique key
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CartAdapter", "Error getting unique key", error.toException()) // 15.4 Log error on cancellation
                    showErrorDialog("Error", "Failed to delete item", error.message) // 15.5 Show error dialog
                    onComplete(null) // 15.6 Return null if cancelled
                }
            })
        }
    }

    // 16. Show error dialog with a custom layout
    private fun showErrorDialog(title: String, message: String, logMessage: String? = null) {
        // 16.1 Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        iconView.setImageResource(R.drawable.ic_error) // 16.2 Set the error icon
        titleView.text = title // 16.3 Set the dialog title
        messageView.text = if (logMessage != null) "$message\n\nLog: $logMessage" else message // 16.4 Set the dialog message

        // 16.5 Create and configure the dialog
        val builder = AlertDialog.Builder(context, R.style.RoundedAlertDialog)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container) // 16.6 Set background
        dialog.show() // 16.7 Show the dialog

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.textSize = 20f // 16.8 Set button text size

        // 16.9 Adjust dialog width
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = context.resources.displayMetrics.widthPixels - (32 * 2).dp()
        dialog.window?.attributes = layoutParams
    }

    // 17. Extension function to convert dp to pixels
    private fun Int.dp(): Int = (this * context.resources.displayMetrics.density).toInt()
}
