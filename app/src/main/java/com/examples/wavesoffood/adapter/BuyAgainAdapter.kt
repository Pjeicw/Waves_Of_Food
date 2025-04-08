package com.examples.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.BuyAgainItemBinding
import com.examples.wavesoffood.DetailsActivity

// BuyAgainAdapter class handles the list of items in the RecyclerView.
class BuyAgainAdapter(
    private val buyAgainFoodName: MutableList<String>,   // 1. List of food names
    private val buyAgainFoodPrice: MutableList<String>,  // 2. List of food prices
    private val buyAgainFoodImage: MutableList<String>,  // 3. List of food image URLs
    private var requireContext: Context                  // 4. Context for starting activities and inflating layouts
) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>() {

    // onBindViewHolder binds data to each ViewHolder.
    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        try {
            // 1. Bind the food name, price, and image to the ViewHolder at the given position.
            holder.bind(
                buyAgainFoodName[position],
                "$" + buyAgainFoodPrice[position],  // 2. Adding a dollar sign to the price
                buyAgainFoodImage[position]
            )

            // 3. Set an item click listener to navigate to DetailsActivity.
            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                    // 4. Pass data to DetailsActivity when clicked.
                    putExtra("MenuItemName", buyAgainFoodName[position])
                    putExtra("MenuItemDescription", "Description for the food") // Placeholder description
                    putExtra("MenuItemIngredients", "Ingredients of the food")  // Placeholder ingredients
                    putExtra("MenuItemPrice", buyAgainFoodPrice[position])
                    putExtra("MenuItemImage", buyAgainFoodImage[position])
                }
                // 5. Start DetailsActivity.
                requireContext.startActivity(intent)
            }
        } catch (e: Exception) {
            // 6. Log the error and show an error dialog if binding fails.
            Log.e("BuyAgainAdapter", "Error binding data at position $position", e)
            showErrorDialog("Error", "Failed to load item", e.message)
        }
    }

    // onCreateViewHolder inflates the layout for each item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        // 1. Inflate the layout for the RecyclerView item.
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // 2. Return a new ViewHolder with the inflated layout.
        return BuyAgainViewHolder(binding)
    }

    // getItemCount returns the total number of items.
    override fun getItemCount(): Int {
        // 1. Return the size of the buyAgainFoodName list (total number of items).
        return buyAgainFoodName.size
    }

    // Inner class BuyAgainViewHolder holds the views for each item.
    inner class BuyAgainViewHolder(private val binding: BuyAgainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // bind binds the food details (name, price, and image) to the views.
        fun bind(foodName: String, foodPrice: String, foodImage: String) {
            // 1. Bind the food name to the TextView.
            binding.buyAgainFoodName.text = foodName
            // 2. Bind the food price to the TextView.
            binding.buyAgainFoodPrice.text = foodPrice

            try {
                // 3. Parse the image URL.
                val uri = Uri.parse(foodImage)
                // 4. Load the image using Glide into the ImageView.
                Glide.with(requireContext).load(uri).into(binding.buyAgainFoodImage)
            } catch (e: Exception) {
                // 5. Log an error and show an error dialog if image loading fails.
                Log.e("BuyAgainAdapter", "Error loading image: $foodImage", e)
                showErrorDialog("Error", "Failed to load image", e.message)
            }

            // 6. Set a click listener for the "Buy Again" button.
            binding.buyAgainFoodButton.setOnClickListener {
                val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                    // 7. Pass food details to DetailsActivity when button is clicked.
                    putExtra("MenuItemName", foodName)
                    putExtra("MenuItemDescription", "Description for the food")  // Placeholder description
                    putExtra("MenuItemIngredients", "Ingredients of the food")   // Placeholder ingredients
                    putExtra("MenuItemPrice", foodPrice.replace("$", ""))        // Remove dollar sign
                    putExtra("MenuItemImage", foodImage)
                }
                // 8. Start DetailsActivity.
                requireContext.startActivity(intent)
            }
        }
    }

    // showErrorDialog shows an error dialog when an error occurs.
    private fun showErrorDialog(title: String, message: String, logMessage: String? = null) {
        // 1. Inflate the custom dialog layout.
        val dialogView = LayoutInflater.from(requireContext)
            .inflate(R.layout.centered_dialog, null)

        // 2. Find the dialog views (icon, title, and message).
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        // 3. Set the error icon and the dialog text.
        iconView.setImageResource(R.drawable.ic_error)
        titleView.text = title
        messageView.text = if (logMessage != null) "$message\n\nLog: $logMessage" else message

        // 4. Build and display the AlertDialog with the custom layout.
        val builder = AlertDialog.Builder(requireContext, R.style.RoundedAlertDialog)
        builder.setView(dialogView)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }  // Dismiss the dialog when "OK" is clicked.
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)  // Set custom background.
        dialog.show()

        // 5. Customize the OK button's text size.
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        button.textSize = 20f

        // 6. Adjust the dialog width based on screen size.
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = requireContext.resources.displayMetrics.widthPixels - (32 * 2).dp()
        dialog.window?.attributes = layoutParams
    }

    // Helper function to convert dp to pixels based on screen density.
    private fun Int.dp(): Int {
        // 1. Convert dp to actual pixels based on screen density.
        return (this * requireContext.resources.displayMetrics.density + 0.5f).toInt()
    }
}
