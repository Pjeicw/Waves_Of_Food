package com.examples.wavesoffood.Fragment

import android.content.Intent
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
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.FragmentCartBinding
import com.examples.wavesoffood.PayOutActivity
import com.examples.wavesoffood.adapter.CartAdapter
import com.examples.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartFragment : Fragment() {
    private lateinit var binding: FragmentCartBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var cartAdapter: CartAdapter
    private lateinit var userId: String

    // Define lists to hold cart data
    private val foodNames = mutableListOf<String>()
    private val foodPrices = mutableListOf<String>()
    private val foodDescriptions = mutableListOf<String>()
    private val foodImageUri = mutableListOf<String>()
    private val foodIngredients = mutableListOf<String>()
    private val quantities = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Retrieve cart items when the view is created
        retrieveCartItems()

        binding.proceedButton.visibility = View.GONE // Hides the button immediately

        // Handle proceed button click
        binding.proceedButton.setOnClickListener {
            getOrderItemsDetail()
        }

        return binding.root
    }

    // Fetches the cart items for the current user from Firebase
    private fun retrieveCartItems() {
        userId = auth.currentUser?.uid ?: ""
        val cartReference: DatabaseReference = database.reference.child("user").child(userId).child("CartItems")

        cartReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (foodSnapshot in snapshot.children) {
                        try {
                            val cartItem = foodSnapshot.getValue(CartItems::class.java)
                            cartItem?.let {
                                foodNames.add(it.foodName ?: "")
                                foodPrices.add(it.foodPrice ?: "")
                                foodDescriptions.add(it.foodDescription ?: "")
                                foodImageUri.add(it.foodImage ?: "")
                                foodIngredients.add(it.foodIngredient ?: "")
                                quantities.add(it.foodQuantity ?: 1)
                            }
                        } catch (e: Exception) {
                            Log.e("CartFragment", "Error parsing cart item", e)
                            showPopupMessage(
                                "Error",
                                "Failed to load cart items",
                                isError = true,
                                e.message
                            )
                        }
                    }
                    setAdapter()
                } else {
                    showEmptyCartMessage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartFragment", "Error fetching cart items", error.toException())
                showPopupMessage(
                    "Error",
                    "Failed to load cart items",
                    isError = true,
                    error.message
                )
            }
        })
    }

    // Sets up the adapter for RecyclerView
    private fun setAdapter() {
        if (foodNames.isEmpty()) {
            showEmptyCartMessage()
        } else {
            cartAdapter = CartAdapter(
                requireContext(),
                foodNames,
                foodPrices,
                foodDescriptions,
                foodImageUri,
                quantities,
                foodIngredients
            )
            binding.cartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.cartRecyclerView.adapter = cartAdapter
            binding.noItemsTextView.visibility = View.GONE

            // Show proceed button if items are present
            binding.proceedButton.visibility = View.VISIBLE
            binding.proceedButton.isEnabled = true
        }
    }

    // Handles the order process by collecting the necessary details and launching PayOutActivity
    private fun getOrderItemsDetail() {
        val foodQuantities = cartAdapter.getUpdatedItemsQuantities()

        orderNow(
            foodNames,
            foodPrices,
            foodDescriptions,
            foodImageUri,
            foodIngredients,
            foodQuantities
        )
    }

    private fun orderNow(
        foodName: MutableList<String>,
        foodPrice: MutableList<String>,
        foodDescription: MutableList<String>,
        foodImage: MutableList<String>,
        foodIngredient: MutableList<String>,
        foodQuantities: MutableList<Int>
    ) {
        val intent = Intent(requireContext(), PayOutActivity::class.java).apply {
            putExtra("FoodItemName", ArrayList(foodName))
            putExtra("FoodItemPrice", ArrayList(foodPrice))
            putExtra("FoodItemImage", ArrayList(foodImage))
            putExtra("FoodItemDescription", ArrayList(foodDescription))
            putExtra("FoodItemIngredient", ArrayList(foodIngredient))
            putExtra("FoodItemQuantities", ArrayList(foodQuantities))
        }
        startActivity(intent)
    }

    // Hides the proceed button and shows an empty cart message instantly
    private fun showEmptyCartMessage() {

        binding.cartRecyclerView.visibility = View.GONE
        binding.noItemsTextView.visibility = View.VISIBLE
    }



    // Shows an error or info message in a popup dialog
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
        messageView.text = logMessage?.let { "$message\n\nLog: $logMessage" } ?: message

        val builder = AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 20f
        dialog.window?.attributes?.width = resources.displayMetrics.widthPixels - (32 * 2).dp()
    }

    // Extension function to convert pixels to dp
    private fun Int.dp(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
}
