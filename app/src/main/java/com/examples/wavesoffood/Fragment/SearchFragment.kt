package com.examples.wavesoffood.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.R
import com.examples.wavesoffood.adapter.MenuAdapter
import com.example.wavesoffood.databinding.FragmentSearchBinding
import com.examples.wavesoffood.model.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MenuAdapter
    private lateinit var database: FirebaseDatabase
    private val originalMenuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance()
        retrieveMenuItem()
        setupSearchView()

        return binding.root
    }

    private fun retrieveMenuItem() {
        val menuReference: DatabaseReference = database.reference.child("menu")
        menuReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (menuSnapshot in snapshot.children) {
                        try {
                            val menuItem = menuSnapshot.getValue(MenuItem::class.java)
                            menuItem?.let { originalMenuItems.add(it) }
                        } catch (e: Exception) {
                            Log.e("SearchFragment", "Error parsing menu item", e)
                            showErrorDialog("Error", "Failed to load menu items", e.message)
                        }
                    }
                    showAllMenuItems()
                } else {
                    showNoItemsMessage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchFragment", "Error fetching menu items", error.toException())
                showErrorDialog("Error", "Failed to load menu items", error.message)
            }
        })
    }

    private fun showAllMenuItems() {
        val filteredMenuItem = ArrayList(originalMenuItems)
        setAdapter(filteredMenuItem)
    }

    private fun setAdapter(menuItems: List<MenuItem>) {
        adapter = MenuAdapter(menuItems, requireContext())
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menuRecyclerView.adapter = adapter
        checkAndShowNoItemsMessage(menuItems)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                filterMenuItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterMenuItems(newText)
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {
        val filteredMenuItems = originalMenuItems.filter { menuItem ->
            menuItem.foodName?.contains(query, ignoreCase = true) == true
        }
        setAdapter(filteredMenuItems)
    }

    private fun checkAndShowNoItemsMessage(menuItems: List<MenuItem>) {
        if (menuItems.isEmpty()) {
            binding.noItemsTextView.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.GONE
        } else {
            binding.noItemsTextView.visibility = View.GONE
            binding.menuRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showNoItemsMessage() {
        binding.noItemsTextView.visibility = View.VISIBLE
        binding.menuRecyclerView.visibility = View.GONE
    }

    private fun showErrorDialog(title: String, message: String, logMessage: String? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.centered_dialog, null)
        val iconView = dialogView.findViewById<ImageView>(R.id.icon)
        val titleView = dialogView.findViewById<TextView>(R.id.title)
        val messageView = dialogView.findViewById<TextView>(R.id.message)

        iconView.setImageResource(R.drawable.ic_error)
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
}