package com.example.bookapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import com.example.bookapp.databinding.ActivityPdfListAdminBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfListAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfListAdminBinding
    private lateinit var bookArrayList: ArrayList<ModelPdf>
    private lateinit var adapterBookAdmin: AdapterPdfAdmin
    private var categoryId = ""
    private var  category = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        categoryId = intent.getStringExtra("categoryId")!!
        category = intent.getStringExtra("category")!!

        val mSpannableCategory = SpannableString(category)
        mSpannableCategory.setSpan(UnderlineSpan(), 0, mSpannableCategory.length, 0)
        binding.subTitleTv.text = mSpannableCategory

        loadBookList()

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, DashboardAdminActivity::class.java))
            finish()
        }

        binding.searchEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    adapterBookAdmin.filter!!.filter(s)
                } catch (e: Exception) {

                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadBookList() {
        bookArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bookArrayList.clear()

                    for (ds in snapshot.children) {
                        val model = ds.getValue(ModelPdf::class.java)
                        if (model != null) {
                            bookArrayList.add(model)
                        }
                    }

                    adapterBookAdmin = AdapterPdfAdmin(
                        this@PdfListAdminActivity,
                        bookArrayList
                    )
                    binding.booksRv.adapter = adapterBookAdmin
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}