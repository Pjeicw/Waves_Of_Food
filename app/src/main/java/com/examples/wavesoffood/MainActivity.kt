package com.examples.wavesoffood

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        binding.notificationButton.setOnClickListener {
            val bottomSheetDialog = Notifacion_Bottom_Fragment()
            bottomSheetDialog.show(supportFragmentManager, "Test")
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userBuyHistoryRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("BuyHistory")

            userBuyHistoryRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var newNotificationCount = 0
                    if (snapshot.exists()) {
                        for (orderSnapshot in snapshot.children) {
                            val viewed = orderSnapshot.child("viewed").getValue(Boolean::class.java) ?: false
                            if (!viewed) {
                                newNotificationCount++
                            }
                        }
                    }
                    if (newNotificationCount == 0) {
                        binding.totalNotification.visibility = View.GONE
                    } else {
                        binding.totalNotification.visibility = View.VISIBLE
                        binding.totalNotification.text = "$newNotificationCount"
                    }
                    binding.totalNotification.text = "+" + newNotificationCount.toString()}

                override fun onCancelled(error: DatabaseError) {
                    // Handle error, e.g., show a toast or log the error
                    showErrorPopup(error.message)
                }
            })
        } else {
            // Handle user not logged in, e.g., set notification count to 0
            binding.totalNotification.text = "0"
        }
    }

    private fun showErrorPopup(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}