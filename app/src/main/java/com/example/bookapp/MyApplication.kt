package com.example.bookapp

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.text.format.DateFormat
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {

        fun formatTimeStamp(timestamp: Long) : String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp

            return DateFormat.format("dd/MM/yyyy", cal).toString()
        }

        fun loadPdfSize(pdfUrl: String, pdfTitle: String, sizeTv: TextView) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener { storageMetaData ->
                    val bytes = storageMetaData.sizeBytes.toDouble()

                    val kb = bytes/1024
                    val mb = kb/1024
                    if (mb >= 1) {
                        sizeTv.text = "${String.format("%.2f", mb)} MB"
                    } else if (kb >= 1) {
                        sizeTv.text = "${String.format("%.2f", kb)} KB"
                    } else {
                        sizeTv.text = "${String.format("%.2f", bytes)} bytes"
                    }
                }
        }

        fun loadPdfFromUrlSinglePage(
            pdfUrl: String,
            pdfTitle: String,
            pdfView: PDFView,
            progressBar: ProgressBar,
            pagesTv: TextView?
        ) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener { bytes ->

                    pdfView.fromBytes(bytes)
                        .pages(0)
                        .spacing(0)
                        .swipeHorizontal(false)
                        .enableSwipe(false)
                        .onError {
                            progressBar.visibility = View.INVISIBLE
                        }
                        .onPageError { _, _ ->
                            progressBar.visibility = View.INVISIBLE
                        }
                        .onLoad { nbPages ->
                            progressBar.visibility = View.INVISIBLE

                            if (pagesTv != null) {
                                pagesTv.text = "$nbPages"
                            }
                        }
                        .load()
                }

        }


        fun loadCategory(categoryId: String, categoryTv: TextView) {
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val category: String = "${snapshot.child("category").value}"
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }




        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle: String) {
            val progressDialog = ProgressDialog(context)

            progressDialog.setTitle("One moment")

            progressDialog.setMessage("Deleting ${bookTitle}..")

            progressDialog.setCanceledOnTouchOutside(false)

            progressDialog.show()


            val pdfRef = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            pdfRef.delete()
                .addOnSuccessListener {
                    val bookRef = FirebaseDatabase.getInstance().getReference("Books")
                    bookRef.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Could not delete book due to ${e.message}..", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Could not delete file due to: ${e.message}..", Toast.LENGTH_SHORT).show()
                }
            progressDialog.dismiss()
        }


        fun showScreen(layout: Int, layoutRoot: RelativeLayout, activity: Activity) {
            activity.setContentView(layout)
            Handler().postDelayed(Runnable {
                activity.setContentView(layoutRoot)
            }, 1100)
        }


        fun removeFromFavorite(context: Context, bookId: String, bookTitle: String) {
            val firebaseAuth = FirebaseAuth.getInstance()

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "Book ${bookTitle} was deleted from favourites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e -> Toast.makeText(context, "Could not ... : ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }



    }
}