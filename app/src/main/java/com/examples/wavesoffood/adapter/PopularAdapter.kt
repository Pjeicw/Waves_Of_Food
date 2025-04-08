package com.examples.wavesoffood.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.PopulerItemBinding
import com.examples.wavesoffood.DetailsActivity

class PopularAdapter(
    private val items: List<String>,
    private val price: List<String>,
    private val quantities: List<String>,
    private val image: List<Int>,
    private val requireContext: Context
) : RecyclerView.Adapter<PopularAdapter.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder{
        return PopularViewHolder(
            PopulerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int) {
        try {
            val item = items[position]
            val images = image[position]
            val price = price[position]
            val quantities = quantities[position]
            holder.bind(item, price, quantities, images)

            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext, DetailsActivity::class.java)
                intent.putExtra("MenuItemName", item)
                intent.putExtra("MenuItemImage", images)
                requireContext.startActivity(intent)
            }
        } catch (e: Exception) {
            holder.showPopupMessage("Error", "Failed to load item data", true, e.message)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class PopularViewHolder(private val binding: PopulerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {private val imagesView = binding.foodImage

        fun bind(item: String, price: String, quantities: String, images: Int) {
            binding.foodNamePopular.text = item
            binding.pricePopuler.text = "$" + price // Added "$" symbol
            binding.menuFoodOrdered.text = quantities
            imagesView.setImageResource(images)
        }

        fun showPopupMessage(
            title: String,
            message: String,
            isError: Boolean,
            logMessage: String?
        ) {
            val dialogView =
                LayoutInflater.from(itemView.context).inflate(R.layout.centered_dialog, null)
            val iconView = dialogView.findViewById<ImageView>(R.id.icon)
            val titleView = dialogView.findViewById<TextView>(R.id.title)
            val messageView = dialogView.findViewById<TextView>(R.id.message)

            val icon = if (isError) R.drawable.ic_error else R.drawable.ic_info
            iconView.setImageResource(icon)
            titleView.text = title
            messageView.text = if (logMessage != null) "$message\n\nLog: $logMessage" else message

            val builder = AlertDialog.Builder(itemView.context, R.style.RoundedAlertDialog)
            builder.setView(dialogView)
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_container)
            dialog.show()

            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.textSize = 20f

            val layoutParams = dialog.window?.attributes
            layoutParams?.width =
                itemView.context.resources.displayMetrics.widthPixels - (32 * 2).dp()
            dialog.window?.attributes = layoutParams
        }

        private fun Int.dp(): Int =
            (this * itemView.context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
