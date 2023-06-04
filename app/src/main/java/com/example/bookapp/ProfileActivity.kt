package com.example.bookapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.bookapp.R
import com.example.bookapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var adapterPdfFavorite: AdapterPdfFavorite
    private lateinit var booksArrayList: ArrayList<ModelPdf>
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var progrssDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.accountTypeTv.text = "undef"
        binding.memberDateTv.text = "undef"
        binding.favoriteBookCountTv.text = "undef"
        binding.accountStatusTv.text = "undef"

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        progrssDialog = ProgressDialog(this)
        progrssDialog.setTitle("Please wait...")
        progrssDialog.setCanceledOnTouchOutside(false)

        loadUserInfo()
        loadFavoriteBooks()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.profileEditBtn.setOnClickListener {
            val intent = Intent(this, ProfileEditActivity::class.java)
            startActivity(intent)
        }

        binding.accountStatusTv.setOnClickListener{
            if(firebaseUser.isEmailVerified){
                Toast.makeText(this, "Already verified...", Toast.LENGTH_SHORT).show()
            } else {
                 emailVerification()
            }
        }
    }

    private fun emailVerification() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify Email")
            .setMessage("Are you sure that you want to send email verification to your email: ${firebaseUser.email}?")
            .setPositiveButton("SEND"){d,e ->
                sendEmailVerification()
            }
            .setNegativeButton("CANCEL"){d,e ->
                d.dismiss()
            }
            .show()
    }

    private fun sendEmailVerification() {
        progrssDialog.setMessage("Sending email verification instructions to email ${firebaseUser.email}")
        progrssDialog.show()

        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                progrssDialog.dismiss()
                Toast.makeText(this, "Instructions sent to your email!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progrssDialog.dismiss()
                Toast.makeText(this, "Could not send instructions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {

        if(firebaseUser.isEmailVerified){
            binding.accountStatusTv.text = "Verified"
        }else{
            binding.accountStatusTv.text = "Not Verified"
        }

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val userType = "${snapshot.child("userType").value}"
                    val formattedDate = MyApplication.formatTimeStamp(timestamp.toLong())

                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = formattedDate

                    try {
                        Glide.with(this@ProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)

                    } catch (_: Exception) {

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }

            })
    }


    private fun loadFavoriteBooks() {
        booksArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    booksArrayList.clear()

                    for (ds in snapshot.children) {
                        val bookId = "${ds.child("bookId").value}"

                        val modelBook = ModelPdf()
                        modelBook.id = bookId

                        booksArrayList.add(modelBook)
                    }

                    val favBooksCount = "${booksArrayList.size}"
                    binding.favoriteBookCountTv.text = favBooksCount

                    if (favBooksCount == "0") {
                        binding.favoritesLabelTv.visibility = View.GONE
                    }

                    adapterPdfFavorite = AdapterPdfFavorite(this@ProfileActivity, booksArrayList)
                        //userType
                    binding.favoritesRv.adapter = adapterPdfFavorite
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

    }

    private fun changeTheme() {
        binding.main.setBackgroundResource(R.drawable.back01)
        binding.backProfileV.setBackgroundResource(R.drawable.back02)
        binding.toolbarRl.setBackgroundResource(R.drawable.shape_toolbar02)
    }
}