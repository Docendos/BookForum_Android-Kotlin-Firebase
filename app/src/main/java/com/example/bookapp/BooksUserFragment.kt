package com.example.bookapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bookapp.databinding.FragmentBooksUserBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BooksUserFragment : Fragment {

    private lateinit var binding: FragmentBooksUserBinding
    private var userType = ""

    public companion object {

        public fun newInstance(
            categoryId: String,
            category: String,
            uid: String,
            userType: String
        ): BooksUserFragment {
            val fragment = BooksUserFragment()

            val args = Bundle()
            args.putString("categoryId", categoryId)
            args.putString("category", category)
            args.putString("uid", uid)
            args.putString("userType", userType)
            fragment.arguments = args
            return fragment
        }
    }

    constructor()


    private var categoryId = ""
    private var category = ""
    private var uid = ""

    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfUser: AdapterPdfUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            categoryId = args.getString("categoryId")!!
            category = args.getString("category")!!
            uid = args.getString("uid")!!
            userType = args.getString("userType")!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentBooksUserBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )

        when (category) {
            "All" -> {loadAllBooks()}
            "Most popular" -> {loadPopularBooks("viewsCount") }
            "Most loadable" -> {loadPopularBooks("downloadsCount")}
            else -> {loadCategorizedBooks()}
        }

        binding.searchEt.addTextChangedListener (object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                adapterPdfUser.filter.filter(s)
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        return binding.root
    }

    private fun loadAllBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataChangeFuncBody(context!!, snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    private fun loadPopularBooks(popularSign: String) {
       pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")

        ref.orderByChild(popularSign).limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onDataChangeFuncBody(context!!, snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

    }
    private fun loadCategorizedBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")

        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onDataChangeFuncBody(context!!, snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    private fun onDataChangeFuncBody(context: Context, snapshot: DataSnapshot) {
        pdfArrayList.clear()
        for (ds in snapshot.children) {
            val model = ds.getValue(ModelPdf::class.java)

            pdfArrayList.add(model!!)
        }

        adapterPdfUser = AdapterPdfUser(context, pdfArrayList, userType)
        binding.bookRv.adapter = adapterPdfUser
    }

}