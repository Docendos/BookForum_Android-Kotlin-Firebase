package com.example.bookapp

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //login
        binding.loginBtn.setOnClickListener{
            //later
            startActivity(Intent(this, LoginActivity::class.java))
        }

        //skip login
        binding.skipBtn.setOnClickListener{
            //later
            startActivity(Intent(this, DashboardUserActivity::class.java))
        }

    }
}