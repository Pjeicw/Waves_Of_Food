package com.examples.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.ActivityPayOutBinding
import com.examples.wavesoffood.Fragment.ProfileFragment
import com.examples.wavesoffood.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PayOutActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String
    private lateinit var totalAmount: String
    private lateinit var foodItemName: ArrayList<String>
    private lateinit var foodItemPrice: ArrayList<String>
    private lateinit var foodItemImage: ArrayList<String>
    private lateinit var foodItemDescription: ArrayList<String>
    private lateinit var foodItemIngredient: ArrayList<String>
    private lateinit var foodItemQuantities: ArrayList<Int>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String

    lateinit var binding: ActivityPayOutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase and user details
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference()

        // Set user data
        setUserData()

        // Disable editing for name, address, and phone fields initially
        setFieldsEditable(false)

        binding.totalAmount.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            // Optionally set background to indicate it's not editable
            background = resources.getDrawable(R.drawable.edittextshape, null) // Replace with your non-editable shape drawable
        }

        // Get user details from intent
        val intent = intent
        foodItemName = intent.getStringArrayListExtra("FoodItemName") as ArrayList<String>
        foodItemPrice = intent.getStringArrayListExtra("FoodItemPrice") as ArrayList<String>
        foodItemImage = intent.getStringArrayListExtra("FoodItemImage") as ArrayList<String>
        foodItemDescription = intent.getStringArrayListExtra("FoodItemDescription") as ArrayList<String>
        foodItemIngredient = intent.getStringArrayListExtra("FoodItemIngredient") as ArrayList<String>
        foodItemQuantities = intent.getIntegerArrayListExtra("FoodItemQuantities") as ArrayList<Int>

        totalAmount = calculateTotalAmount().toString() + "$"
        binding.totalAmount.setText(totalAmount)

        // Handle order placement
        binding.placeMyOrder.setOnClickListener {
            name = binding.name.text.toString().trim()
            address = binding.address.text.toString().trim()
            phone = binding.phone.text.toString().trim()

            if (address.isBlank() || phone.isBlank() || name.isBlank()) {
                showPopupMessage(
                    title = "Incomplete Profile",
                    message = "Please complete your profile details before placing an order.",
//                    isError = true,
                    navigateToProfile = true
                )
            } else {
                placeOrder()
            }
        }

        // Handle "Edit Information" button click
        binding.editInfoButton.setOnClickListener {
            toggleEditInfoButton()
        }

        binding.imageButton.setOnClickListener {
            finish()
        }
    }

    private fun toggleEditInfoButton() {
        if (binding.editInfoButton.text == "Edit Information?") {
            binding.editInfoButton.text = "Cancel Edit"
            setFieldsEditable(true)
        } else {
            binding.editInfoButton.text = "Edit Information?"
            setFieldsEditable(false)
        }
    }

    private fun setFieldsEditable(isEnabled: Boolean) {
        binding.name.isFocusable = isEnabled
        binding.name.isFocusableInTouchMode = isEnabled
        binding.address.isFocusable = isEnabled
        binding.address.isFocusableInTouchMode = isEnabled
        binding.phone.isFocusable = isEnabled
        binding.phone.isFocusableInTouchMode = isEnabled

        val textColor = if (isEnabled) {
            resources.getColor(R.color.black, null)
        } else {
            resources.getColor(R.color.gray, null)
        }

        binding.name.setTextColor(textColor)
        binding.address.setTextColor(textColor)
        binding.phone.setTextColor(textColor)

        if (!isEnabled) {
            binding.name.setHint("Add your name")
            binding.address.setHint("Add your address")
            binding.phone.setHint("Add your phone")
        } else {
            binding.name.hint = null
            binding.address.hint = null
            binding.phone.hint = null
        }
    }

    private fun placeOrder() {
        userId = auth.currentUser?.uid ?: ""
        val time = System.currentTimeMillis()
        val itemPushKey = databaseReference.child("OrderDetails").push().key
        val orderDetails = OrderDetails(
            userId, name, foodItemName, foodItemPrice, foodItemImage,
            foodItemQuantities, address, totalAmount, phone, time, itemPushKey, false, false
        )

        val orderReference = databaseReference.child("OrderDetails").child(itemPushKey!!)
        orderReference.setValue(orderDetails).addOnSuccessListener {
            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "Test")
            removeItemFromCart()
            addOrderToHistory(orderDetails)

        }.addOnFailureListener { error ->
            showPopupMessage(
                title = "Order Failed",
                message = "Failed to place the order. Please try again.",
                isError = true,
                logMessage = error.message
            )
        }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory")
            .child(orderDetails.itemPushKey!!)
            .setValue(orderDetails)
            .addOnSuccessListener {
                // Order added to history successfully
            }
            .addOnFailureListener { error ->
                showPopupMessage(
                    title = "Order Failed",
                    message = "Failed to add order to history.",
                    isError = true,
                    logMessage = error.message
                )
            }
    }

    private fun removeItemFromCart() {
        val cartItemsReference = databaseReference.child("user").child(userId).child("CartItems")
        cartItemsReference.removeValue().addOnFailureListener { error ->
            showPopupMessage(
                title = "Error",
                message = "Failed to remove items from cart.",
                isError = true,
                logMessage = error.message
            )
        }
    }

    private fun calculateTotalAmount(): Int {
        var totalAmount = 0
        for (i in foodItemPrice.indices) {
            val price = foodItemPrice[i]
            val lastChar = price.last()
            val priceIntValue = if (lastChar == '$') {
                price.dropLast(1).toInt()
            } else {
                price.toInt()
            }
            val quantity = foodItemQuantities[i]
            totalAmount += priceIntValue * quantity
        }
        return totalAmount
    }

    private fun setUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val names = snapshot.child("name").getValue(String::class.java) ?: ""
                        val addresses = snapshot.child("address").getValue(String::class.java) ?: ""
                        val phones = snapshot.child("phone").getValue(String::class.java) ?: ""

                        binding.apply {
                            name.setText(names)
                            address.setText(addresses)
                            phone.setText(phones)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showPopupMessage(
                        title = "Error",
                        message = "Failed to retrieve user data.",
                        isError = true,
                        logMessage = error.message
                    )
                }
            })
        }
    }

    private fun showPopupMessage(
        title: String,
        message: String,
        isError: Boolean = false,
        logMessage: String? = null,
        navigateToProfile: Boolean = false
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
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            if (navigateToProfile) {
//                val fragment = ProfileFragment()
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, fragment)
//                    .addToBackStack(null)
//                    .commit()
            }
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()

        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.textSize = 20f

        val layoutParams = dialog.window?.attributes
        val margin = 24 * resources.displayMetrics.density // Convert dp to pixels
        layoutParams?.width = resources.displayMetrics.widthPixels - (margin * 2).toInt() // Apply margins
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams
    }

}
