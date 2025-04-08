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
import com.example.wavesoffood.databinding.MenuItemBinding
import com.examples.wavesoffood.DetailsActivity
import com.examples.wavesoffood.model.MenuItem
import com.google.firebase.database.*

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val requireContext: Context
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private val foodQuantitiesSumMap = mutableMapOf<String, Int>()

    init {
        fetchCompletedOrderData()
    }

    private fun fetchCompletedOrderData() {
        val completedOrdersRef = FirebaseDatabase.getInstance().reference.child("CompletedOrder")
        completedOrdersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (orderSnapshot in snapshot.children) {
                        val foodNames = orderSnapshot.child("foodNames").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        val foodQuantities = orderSnapshot.child("foodQuantities").getValue(object : GenericTypeIndicator<List<Int>>() {}) ?: emptyList()

                        for (i in foodNames.indices) {
                            val foodName = foodNames[i]
                            val quantity = foodQuantities[i]
                            foodQuantitiesSumMap[foodName] = (foodQuantitiesSumMap[foodName] ?: 0) + quantity
                        }
                    }
                    notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MenuAdapter", "Error fetching completed order data: ${error.message}")
                showPopupMessage("Error", "Failed to load order data", true, error.message)
            }
        })
    }override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(position)
                }
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = menuItems[position]
            val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName)
                putExtra("MenuItemImage", menuItem.foodImage)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemIngredients", menuItem.foodIngredient)
                putExtra("MenuItemPrice", menuItem.foodPrice)
                putExtra("MenuItemQuantity", menuItem.foodQuantities)
            }
            requireContext.startActivity(intent)
        }

        fun bind(position: Int) {
            val menuItem = menuItems[position]
            binding.apply {
                menuFoodName.text = menuItem.foodName
                menuPrice.text = "$" + menuItem.foodPrice

                val foodName = menuItem.foodName
                val totalQuantity = foodQuantitiesSumMap[foodName] ?: 0
                menuFoodOrdered.text = totalQuantity.toString()

                val uri = Uri.parse(menuItem.foodImage)
                Glide.with(requireContext).load(uri).into(menuImage)
            }
        }
    }

    private fun showPopupMessage(title: String, message: String, isError: Boolean = false, logMessage: String? = null) {
        val builder = AlertDialog.Builder(requireContext, R.style.RoundedAlertDialog).apply {
            val dialogView = LayoutInflater.from(requireContext).inflate(R.layout.centered_dialog, null)
            dialogView.findViewById<ImageView>(R.id.icon).setImageResource(if (isError) R.drawable.ic_error else R.drawable.ic_info)
            dialogView.findViewById<TextView>(R.id.title).text = title
            dialogView.findViewById<TextView>(R.id.message).text = if (logMessage != null) "$message\n\nLog: $logMessage" else message
            setView(dialogView)
            setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).textSize = 20f
        dialog.window?.attributes?.width = requireContext.resources.displayMetrics.widthPixels - (32 * 2).dp(requireContext)
    }
    private fun Int.dp(context: Context): Int = (this * context.resources.displayMetrics.density + 0.5f).toInt()
}