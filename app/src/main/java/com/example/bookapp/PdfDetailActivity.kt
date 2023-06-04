package com.example.bookapp

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookapp.R
import com.example.bookapp.databinding.ActivityPdfDetailBinding
import com.example.bookapp.databinding.DialogCommentAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {


    private var bookTitle = ""
    private var bookUrl = ""
    private var bookId = ""
    private var isInMyFavorite = false

    private lateinit var userType: String

    private lateinit var binding: ActivityPdfDetailBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var commentArrayList: ArrayList<ModelComment>
    private lateinit var adapterComment: AdapterComment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("One moment...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("userType")!!

        bookId = intent.getStringExtra("bookId")!!

        if (userType != "none") {
           checkIsFavorite()
        }

        loadBookDetails()
        showComments()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            intent.putExtra("userType", userType)
            startActivity(intent)
        }

        binding.downloadBookBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                downloadBook()
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.favoriteBtn.setOnClickListener {
            if (firebaseAuth.currentUser == null) {
                Toast.makeText(this, "You are not authorized", Toast.LENGTH_SHORT).show()
            } else {
                if (isInMyFavorite) {
                    MyApplication.removeFromFavorite(this, bookId, bookTitle)
                } else {
                    addToFavorite()
                }
            }
        }

        binding.addCommentBtn.setOnClickListener {
            if(firebaseAuth.currentUser == null){
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            }
            else {
                addCommentDialog()
            }
        }
    }

    private fun showComments() {
        commentArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentArrayList.clear()
                    for (ds in snapshot.children){
                        val model = ds.getValue(ModelComment::class.java)
                        commentArrayList.add(model!!)
                    }

                    adapterComment = AdapterComment(this@PdfDetailActivity, commentArrayList)

                    binding.commentsRv.adapter = adapterComment
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var comment = ""

    private fun addCommentDialog() {
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        val alertDialog = builder.create()
        alertDialog.show()

        commentAddBinding.backBtn.setOnClickListener { alertDialog.dismiss() }

        commentAddBinding.submitBtn.setOnClickListener {
            comment = commentAddBinding.commentEt.text.toString().trim()
            if(comment.isEmpty()){
                Toast.makeText(this, "Enter comment...", Toast.LENGTH_SHORT).show()
            }else{
                alertDialog.dismiss()
                addComment()
            }
        }
    }

    private fun addComment() {

        progressDialog.setMessage("Adding comment...")
        progressDialog.show()

        val timestamp = "${System.currentTimeMillis()}"

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"] = "$bookId"
        hashMap["timestamp"] = "$timestamp"
        hashMap["comment"] = "$comment"
        hashMap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Comment added...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to add comment: {${e.message}}", Toast.LENGTH_SHORT).show()
            }
    }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                downloadBook()
            } else {
                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun downloadBook() {
        progressDialog.setMessage("Uploading file")
        progressDialog.show()

        var ref = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        ref.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        val fileName = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            downloadsFolder.mkdirs()
            val filePath = downloadsFolder.path + "/" + fileName

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "File uploaded in: $filePath", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            incrementDownloadCount()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to upload file: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun incrementDownloadCount() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"

                    if (downloadsCount == "" || downloadsCount == "null") {
                        downloadsCount = "0"
                    }

                    val newDownloadCount: Long = downloadsCount.toLong() + 1

                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount

                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener{
                        }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    MyApplication.loadPdfFromUrlSinglePage("$bookUrl", "$bookTitle", binding.pdfView,
                        binding.progressBar, binding.pagesTv)
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun checkIsFavorite() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()

                    if (isInMyFavorite) {
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white, 0, 0)
                        binding.favoriteBtn.text = "Delete from favourites"
                    } else {
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white, 0, 0)
                        binding.favoriteBtn.text = "Add to favourites"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

    }

    private fun addToFavorite() {
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Book ${bookTitle} added to favourites", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add to favourites: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun changeTheme() {
        binding.main.setBackgroundResource(R.drawable.back01)
        binding.toolbarRl.setBackgroundResource(R.drawable.shape_toolbar02)

        val color = resources.getColor(R.color.teal_200)

        binding.progressBar.indeterminateDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        binding.readBookBtn.setBackgroundColor(color)
        binding.downloadBookBtn.setBackgroundColor(color)
        binding.favoriteBtn.setBackgroundColor(color)
    }
}
